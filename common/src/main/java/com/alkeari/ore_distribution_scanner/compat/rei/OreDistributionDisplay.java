package com.alkeari.ore_distribution_scanner.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntry;

import java.util.List;

public class OreDistributionDisplay implements Display {
    private final OreEntry entry;
    private final List<EntryIngredient> outputs;

    public OreDistributionDisplay(OreEntry entry) {
        this.entry = entry;
        this.outputs = List.of(EntryIngredients.of(entry.icon()));
    }

    public OreEntry entry() {
        return entry;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return List.of();
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return OreDistributionReiCategory.ID;
    }
}
