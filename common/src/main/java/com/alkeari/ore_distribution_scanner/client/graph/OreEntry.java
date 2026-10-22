package com.alkeari.ore_distribution_scanner.client.graph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

/**
 * One ore's distribution display, shared as the "recipe"/"display" object across the JEI, EMI, and
 * REI adapters. Holds the ore's block id and the currently selected biome index (mutated by the
 * graph's biome selector). Identity is the ore id so viewers de-duplicate correctly.
 */
public final class OreEntry {
    private final ResourceLocation oreId;
    private int selectedBiome;

    public OreEntry(ResourceLocation oreId) {
        this.oreId = oreId;
    }

    public ResourceLocation oreId() {
        return oreId;
    }

    public int selectedBiome() {
        return selectedBiome;
    }

    public void setSelectedBiome(int selectedBiome) {
        this.selectedBiome = selectedBiome;
    }

    public ItemStack icon() {
        Block block = BuiltInRegistries.BLOCK.get(oreId);
        return new ItemStack(block);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OreEntry other && oreId.equals(other.oreId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oreId);
    }
}
