package com.alkeari.ore_distribution_scanner.scan;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import com.alkeari.ore_distribution_scanner.data.ScanStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns the single background scan worker and the queue of dimensions to scan. Only one scan runs at
 * a time; a multi-dimension request is processed as a sequential queue, advanced from the server tick
 * as each dimension finishes. Completed results are cached in memory so the reading commands can
 * answer without re-reading the JSON. All public methods run on the server thread.
 */
public final class ScanManager {
    private static final ScanManager INSTANCE = new ScanManager();

    public static ScanManager get() {
        return INSTANCE;
    }

    private ScanManager() {
    }

    private record QueuedScan(ServerLevel level, OreSelection ores, int radius, int cluster, int budget, int interval) {
    }

    public record BatchStatus(ResourceLocation dimension, int index, int total, int percent, int chunksDone, int chunksTotal) {
    }

    public record CancelInfo(List<ResourceLocation> completed, int skipped) {
    }

    private ExecutorService executor;
    private volatile ScanJob active;
    private final Deque<QueuedScan> queue = new ArrayDeque<>();
    private final Map<ResourceLocation, DimensionScanData> results = new ConcurrentHashMap<>();
    private final List<ResourceLocation> completedThisBatch = new ArrayList<>();
    private MinecraftServer server;
    private CommandSourceStack batchSource;
    private int batchTotal;
    private int batchCompleted;

    public synchronized boolean isRunning() {
        return active != null && active.getState() == ScanJob.State.RUNNING;
    }

    public synchronized boolean isBusy() {
        return isRunning() || !queue.isEmpty();
    }

    /** Enqueue one or more dimensions and start scanning the first. Returns false if already busy. */
    public synchronized boolean startBatch(MinecraftServer server, List<ServerLevel> levels, CommandSourceStack source,
                                           OreSelection ores, int radius, int cluster, int budget, int interval) {
        if (isBusy()) {
            return false;
        }
        this.server = server;
        this.batchSource = source;
        this.batchTotal = levels.size();
        this.batchCompleted = 0;
        this.completedThisBatch.clear();
        for (ServerLevel level : levels) {
            queue.add(new QueuedScan(level, ores, radius, cluster, budget, interval));
        }
        startNext();
        return true;
    }

    private void startNext() {
        QueuedScan next = queue.poll();
        if (next == null) {
            return;
        }
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "ods-scan-worker");
                thread.setDaemon(true);
                return thread;
            });
        }
        ScanJob job = new ScanJob(server, next.level(), batchSource, next.ores(),
                next.radius(), next.cluster(), next.budget(), next.interval(),
                batchCompleted + 1, batchTotal);
        active = job;
        executor.submit(job::run);
    }

    /** Advances the queue when a dimension finishes. Driven from the server tick. */
    public synchronized void tick(MinecraftServer server) {
        ScanJob job = active;
        if (job == null || job.getState() == ScanJob.State.RUNNING) {
            return;
        }
        if (job.getState() == ScanJob.State.COMPLETED) {
            results.put(job.getDimensionId(), job.getData());
            completedThisBatch.add(job.getDimensionId());
        }
        batchCompleted++;
        active = null;

        if (!queue.isEmpty()) {
            startNext();
        } else if (batchTotal > 1 && batchSource != null) {
            List<ResourceLocation> done = List.copyOf(completedThisBatch);
            batchSource.sendSystemMessage(Component.literal(
                    "Ore Distribution Scanner: batch complete. Scanned " + done.size() + " of " + batchTotal + " dimensions."));
            batchTotal = 0;
        }
    }

    public synchronized CancelInfo cancel() {
        if (!isBusy()) {
            return null;
        }
        int skipped = queue.size() + (isRunning() ? 1 : 0);
        queue.clear();
        if (active != null) {
            active.cancel();
        }
        List<ResourceLocation> done = List.copyOf(completedThisBatch);
        batchTotal = 0;
        return new CancelInfo(done, skipped);
    }

    public synchronized Optional<BatchStatus> currentStatus() {
        ScanJob job = active;
        if (job == null || job.getState() != ScanJob.State.RUNNING) {
            return Optional.empty();
        }
        ScanProgress p = job.progress();
        return Optional.of(new BatchStatus(p.dimension(), batchCompleted + 1, Math.max(batchTotal, 1),
                p.percent(), p.chunksDone(), p.chunksTotal()));
    }

    public boolean isScanning(ResourceLocation dimension) {
        ScanJob job = active;
        if (job != null && job.getState() == ScanJob.State.RUNNING && job.getDimensionId().equals(dimension)) {
            return true;
        }
        synchronized (this) {
            return queue.stream().anyMatch(q -> q.level().dimension().location().equals(dimension));
        }
    }

    /** The server-side source of truth for a dimension: cached in-memory result, else the saved JSON. */
    public Optional<DimensionScanData> getResult(MinecraftServer server, ResourceLocation dimension) {
        DimensionScanData cached = results.get(dimension);
        if (cached != null) {
            return Optional.of(cached);
        }
        return ScanStorage.load(server, dimension);
    }

    public void dropResult(ResourceLocation dimension) {
        results.remove(dimension);
    }

    public void clearResults() {
        results.clear();
    }

    public synchronized void shutdown() {
        queue.clear();
        if (active != null) {
            active.cancel();
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        active = null;
        batchTotal = 0;
        results.clear();
        OreDistributionScanner.LOGGER.debug("Scan worker shut down.");
    }
}
