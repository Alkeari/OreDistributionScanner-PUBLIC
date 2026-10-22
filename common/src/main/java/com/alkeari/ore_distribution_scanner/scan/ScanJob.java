package com.alkeari.ore_distribution_scanner.scan;

import com.mojang.datafixers.util.Pair;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import com.alkeari.ore_distribution_scanner.data.ScanStorage;
import com.alkeari.ore_distribution_scanner.network.ScanNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * One dimension scan. Owns the background loop and all aggregation. The server thread only ever runs
 * the small, bounded snapshot and biome-locate tasks this submits; the heavy decode and tally run
 * here on the worker so the main thread never stalls and the Watchdog is never tripped.
 */
public final class ScanJob {
    public enum State {
        RUNNING, COMPLETED, CANCELLED, FAILED
    }

    private static final long SERVER_TASK_TIMEOUT_SECONDS = 60L;

    private final MinecraftServer server;
    private final ServerLevel level;
    private final ResourceLocation dimensionId;
    private final net.minecraft.commands.CommandSourceStack source;
    private final ServerPlayer player;
    private final BlockPos origin;
    private final OreSelection ores;

    private final int radiusChunks;
    private final int biomeClusterSize;
    private final int budget;
    private final int progressIntervalSeconds;
    private final int batchIndex;
    private final int batchTotal;

    private final int minY;
    private final int repY;
    private final DimensionScanData data;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger chunksDone = new AtomicInteger(0);
    private volatile int chunksTotal;
    private volatile State state = State.RUNNING;
    private long lastProgressPost;

    public ScanJob(MinecraftServer server, ServerLevel level, net.minecraft.commands.CommandSourceStack source,
                   OreSelection ores, int radiusChunks, int biomeClusterSize, int budget, int progressIntervalSeconds,
                   int batchIndex, int batchTotal) {
        this.server = server;
        this.level = level;
        this.dimensionId = level.dimension().location();
        this.source = source;
        this.player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        this.origin = BlockPos.containing(source.getPosition());
        this.ores = ores;
        this.radiusChunks = radiusChunks;
        this.biomeClusterSize = biomeClusterSize;
        this.budget = budget;
        this.progressIntervalSeconds = progressIntervalSeconds;
        this.batchIndex = batchIndex;
        this.batchTotal = batchTotal;
        this.minY = level.getMinBuildHeight();
        this.repY = Math.max(minY, Math.min(level.getSeaLevel(), minY + level.getHeight() - 1));
        this.data = new DimensionScanData(dimensionId, minY, level.getHeight());
        this.data.setLowConfidenceThreshold(com.alkeari.ore_distribution_scanner.config.ModConfig.getLowConfidenceSampleThreshold());
    }

    public void cancel() {
        cancelled.set(true);
    }

    public State getState() {
        return state;
    }

    public ResourceLocation getDimensionId() {
        return dimensionId;
    }

    public ScanProgress progress() {
        return new ScanProgress(state, dimensionId, chunksDone.get(), chunksTotal);
    }

    public void run() {
        try {
            List<ChunkPos> positions = buildSamplePositions();
            chunksTotal = positions.size();

            Path regionDir = ScanStorage.regionDirectory(server, level.dimension());
            for (ChunkPos pos : positions) {
                if (cancelled.get()) {
                    break;
                }
                acquireAndTally(regionDir, pos);
                chunksDone.incrementAndGet();
                maybePostProgress();
            }

            state = cancelled.get() ? State.CANCELLED : State.COMPLETED;
        } catch (Exception e) {
            state = State.FAILED;
            OreDistributionScanner.LOGGER.error("Scan of {} failed", dimensionId, e);
        } finally {
            finish();
        }
    }

    private void acquireAndTally(Path regionDir, ChunkPos pos) {
        ChunkAcquisition.LoadedChunkSnapshot snapshot =
                onServer(() -> ChunkAcquisition.snapshotLoaded(level, pos, repY));
        if (snapshot != null) {
            ChunkAcquisition.tallyLoaded(snapshot, ores, data, minY);
        } else {
            RegionChunkReader.readAndTally(regionDir, pos, minY, repY, ores, data);
        }
    }

    private List<ChunkPos> buildSamplePositions() {
        Set<Long> packed = new LinkedHashSet<>();
        ChunkPos originChunk = new ChunkPos(origin);

        for (ChunkPos pos : SpreadSampler.spread(originChunk.x, originChunk.z, radiusChunks, budget)) {
            packed.add(pos.toLong());
        }
        addBiomeSeek(packed);

        List<ChunkPos> positions = new ArrayList<>(packed.size());
        for (long key : packed) {
            if (positions.size() >= budget) {
                break;
            }
            positions.add(new ChunkPos(key));
        }
        return positions;
    }

    private void addBiomeSeek(Set<Long> packed) {
        if (biomeClusterSize <= 0) {
            return;
        }
        BiomeSource biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        int searchRadius = radiusChunks * 16;
        int clusterRadius = Math.max(1, (int) Math.ceil(Math.sqrt(biomeClusterSize)));

        for (Holder<Biome> holder : biomeSource.possibleBiomes()) {
            if (cancelled.get()) {
                return;
            }
            Optional<ResourceKey<Biome>> key = holder.unwrapKey();
            if (key.isEmpty()) {
                continue;
            }
            Pair<BlockPos, Holder<Biome>> found = onServer(() ->
                    level.findClosestBiome3d(h -> h.is(key.get()), origin, searchRadius, 32, 64));
            if (found == null) {
                continue;
            }
            ChunkPos located = new ChunkPos(found.getFirst());
            for (ChunkPos cluster : SpreadSampler.spread(located.x, located.z, clusterRadius, biomeClusterSize)) {
                packed.add(cluster.toLong());
            }
        }
    }

    private void maybePostProgress() {
        long now = System.currentTimeMillis();
        if (now - lastProgressPost < progressIntervalSeconds * 1000L) {
            return;
        }
        lastProgressPost = now;
        if (player == null) {
            return;
        }
        Component message = Component.literal(String.format("Scanning %s%s: %d/%d chunks (%d%%)",
                dimensionId, batchTag(), chunksDone.get(), chunksTotal, progress().percent()));
        onServerVoid(() -> player.displayClientMessage(message, true));
    }

    private void finish() {
        if (state == State.COMPLETED) {
            ScanStorage.save(server, data);
            server.execute(() -> ScanNetworking.sendToAll(server, data));
        }
        Component summary = switch (state) {
            case COMPLETED -> Component.literal(String.format(
                    "Ore Distribution Scanner: scan of %s%s complete. Sampled %d chunks across %d biomes.",
                    dimensionId, batchTag(), chunksDone.get(), data.getBiomes().size()));
            case CANCELLED -> Component.literal(String.format(
                    "Ore Distribution Scanner: scan of %s cancelled after %d/%d chunks.",
                    dimensionId, chunksDone.get(), chunksTotal));
            default -> Component.literal(String.format(
                    "Ore Distribution Scanner: scan of %s failed. See the server log.", dimensionId));
        };
        onServerVoid(() -> source.sendSystemMessage(summary));
    }

    public DimensionScanData getData() {
        return data;
    }

    private String batchTag() {
        return batchTotal > 1 ? String.format(" (%d of %d)", batchIndex, batchTotal) : "";
    }

    private <T> T onServer(Supplier<T> task) {
        try {
            return server.submit(task).get(SERVER_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelled.set(true);
            return null;
        } catch (Exception e) {
            OreDistributionScanner.LOGGER.error("Server-thread scan task failed", e);
            cancelled.set(true);
            return null;
        }
    }

    private void onServerVoid(Runnable task) {
        server.submit(task);
    }
}
