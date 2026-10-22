package com.alkeari.ore_distribution_scanner.scan;

import net.minecraft.resources.ResourceLocation;

public record ScanProgress(ScanJob.State state, ResourceLocation dimension, int chunksDone, int chunksTotal) {
    public int percent() {
        return chunksTotal <= 0 ? 0 : (int) (100L * chunksDone / chunksTotal);
    }
}
