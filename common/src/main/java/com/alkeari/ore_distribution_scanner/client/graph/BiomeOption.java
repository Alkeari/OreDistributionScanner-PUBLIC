package com.alkeari.ore_distribution_scanner.client.graph;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * One selectable entry in an ore graph's biome selector: either the dimension-wide aggregate or a
 * single biome. Carries this ore's counts for that scope, the sample count, and whether the scope
 * is below the low-confidence threshold.
 */
public record BiomeOption(ResourceLocation biomeId, Component label, int sampleColumns,
                          boolean lowConfidence, int[] counts) {
}
