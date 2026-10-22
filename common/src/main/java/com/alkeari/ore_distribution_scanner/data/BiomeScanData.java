package com.alkeari.ore_distribution_scanner.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-biome ore tallies for one dimension scan. Holds the number of columns sampled in this biome
 * and, per ore, a count of finds indexed by Y (array index {@code y - minY}). Mutation is
 * synchronized so the scan worker can aggregate safely; the renderer normalizes counts by
 * {@link #getSampledColumns()} so unequal sample sizes do not skew biomes against each other.
 */
public final class BiomeScanData {
    private final int height;
    private int sampledColumns;
    private final Map<ResourceLocation, int[]> oreCountsByY = new HashMap<>();

    public BiomeScanData(int height) {
        this.height = height;
    }

    public synchronized void addSampledColumns(int columns) {
        sampledColumns += columns;
    }

    public synchronized void addOreFind(ResourceLocation ore, int yIndex) {
        addOreFinds(ore, yIndex, 1);
    }

    public synchronized void addOreFinds(ResourceLocation ore, int yIndex, int count) {
        if (count <= 0 || yIndex < 0 || yIndex >= height) {
            return;
        }
        oreCountsByY.computeIfAbsent(ore, k -> new int[height])[yIndex] += count;
    }

    public synchronized int getSampledColumns() {
        return sampledColumns;
    }

    public synchronized int[] getOreCounts(ResourceLocation ore) {
        int[] counts = oreCountsByY.get(ore);
        return counts == null ? null : counts.clone();
    }

    public synchronized long getOreTotal(ResourceLocation ore) {
        int[] counts = oreCountsByY.get(ore);
        if (counts == null) {
            return 0L;
        }
        long total = 0L;
        for (int c : counts) {
            total += c;
        }
        return total;
    }

    public synchronized Map<ResourceLocation, int[]> snapshotOreCounts() {
        Map<ResourceLocation, int[]> copy = new HashMap<>();
        for (Map.Entry<ResourceLocation, int[]> entry : oreCountsByY.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }
}
