package com.alkeari.ore_distribution_scanner.scan;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces the spread sample of chunk positions around a scan origin. Rather than a solid blob of
 * the nearest chunks (which a tight biome cluster could dominate), it places samples on an evenly
 * spaced lattice within the radius, widening the spacing until the count fits the chunk budget, so
 * a fixed budget touches as much nearby biome variety as possible. Results are ordered nearest
 * first so a cancelled or partial scan still covers the center.
 */
public final class SpreadSampler {
    private SpreadSampler() {
    }

    public static List<ChunkPos> spread(int originChunkX, int originChunkZ, int radiusChunks, int budget) {
        if (radiusChunks < 0 || budget <= 0) {
            return List.of();
        }
        long radiusSq = (long) radiusChunks * radiusChunks;

        for (int step = 1; ; step++) {
            List<ChunkPos> sample = latticeWithin(originChunkX, originChunkZ, radiusChunks, radiusSq, step);
            if (sample.size() <= budget) {
                sample.sort((a, b) -> Long.compare(
                        distSq(a, originChunkX, originChunkZ),
                        distSq(b, originChunkX, originChunkZ)));
                return sample;
            }
        }
    }

    private static List<ChunkPos> latticeWithin(int ox, int oz, int radius, long radiusSq, int step) {
        List<ChunkPos> points = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                if ((long) dx * dx + (long) dz * dz <= radiusSq) {
                    points.add(new ChunkPos(ox + dx, oz + dz));
                }
            }
        }
        return points;
    }

    private static long distSq(ChunkPos pos, int ox, int oz) {
        long dx = pos.x - ox;
        long dz = pos.z - oz;
        return dx * dx + dz * dz;
    }
}
