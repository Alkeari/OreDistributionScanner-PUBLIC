package com.alkeari.ore_distribution_scanner.scan;

import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Path A of the hybrid chunk acquisition: currently loaded chunks. The snapshot is taken on the
 * server thread (cheap, bounded) by copying each non-empty section's block states and the
 * per-column representative biome into plain immutable arrays; the heavy tally then runs on the
 * worker thread reading only those snapshots, never live chunk objects.
 */
public final class ChunkAcquisition {
    private ChunkAcquisition() {
    }

    public record SectionSnapshot(int sectionY, BlockState[] states) {
    }

    public record LoadedChunkSnapshot(ResourceLocation[] columnBiomes, List<SectionSnapshot> sections) {
    }

    private static int columnIndex(int localX, int localZ) {
        return (localZ << 4) | localX;
    }

    /**
     * Server-thread only. Returns null if the chunk is not currently loaded (it never generates).
     */
    public static LoadedChunkSnapshot snapshotLoaded(ServerLevel level, ChunkPos pos, int repY) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            return null;
        }

        ResourceLocation[] columnBiomes = new ResourceLocation[256];
        int baseX = pos.getMinBlockX();
        int baseZ = pos.getMinBlockZ();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                Holder<Biome> biome = level.getBiome(new BlockPos(baseX + lx, repY, baseZ + lz));
                columnBiomes[columnIndex(lx, lz)] = biome.unwrapKey().map(ResourceKey::location).orElse(null);
            }
        }

        List<SectionSnapshot> sections = new ArrayList<>();
        LevelChunkSection[] chunkSections = chunk.getSections();
        int minSection = chunk.getMinSection();
        for (int i = 0; i < chunkSections.length; i++) {
            LevelChunkSection section = chunkSections[i];
            if (section.hasOnlyAir()) {
                continue;
            }
            BlockState[] states = new BlockState[4096];
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        states[(y << 8) | (z << 4) | x] = section.getBlockState(x, y, z);
                    }
                }
            }
            sections.add(new SectionSnapshot(minSection + i, states));
        }
        return new LoadedChunkSnapshot(columnBiomes, sections);
    }

    /**
     * Worker-thread tally of a loaded-chunk snapshot.
     */
    public static void tallyLoaded(LoadedChunkSnapshot snapshot, OreSelection ores, DimensionScanData data, int minY) {
        recordSampledColumns(snapshot.columnBiomes(), data);

        for (SectionSnapshot section : snapshot.sections()) {
            BlockState[] states = section.states();
            int sectionBaseY = section.sectionY() * 16;
            for (int idx = 0; idx < states.length; idx++) {
                BlockState state = states[idx];
                if (state.isAir()) {
                    continue;
                }
                Block block = state.getBlock();
                if (!ores.isOre(block)) {
                    continue;
                }
                int x = idx & 15;
                int z = (idx >> 4) & 15;
                int y = (idx >> 8) & 15;
                ResourceLocation biome = snapshot.columnBiomes()[columnIndex(x, z)];
                if (biome != null) {
                    data.biome(biome).addOreFinds(ores.idOf(block), sectionBaseY + y - minY, 1);
                }
            }
        }
    }

    public static void recordSampledColumns(ResourceLocation[] columnBiomes, DimensionScanData data) {
        for (ResourceLocation biome : columnBiomes) {
            if (biome != null) {
                data.biome(biome).addSampledColumns(1);
            }
        }
    }
}
