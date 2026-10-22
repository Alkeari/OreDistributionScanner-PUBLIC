package com.alkeari.ore_distribution_scanner.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.graph.OreDistributionGraph;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class OreDistributionEmiRecipe implements EmiRecipe {
    private final OreEntry entry;
    private final EmiStack output;
    private final ResourceLocation id;

    public OreDistributionEmiRecipe(OreEntry entry) {
        this.entry = entry;
        this.output = EmiStack.of(entry.icon());
        // Leading '/' marks this as a synthetic recipe (not backed by the recipe manager), which
        // suppresses EMI's "not present in recipe manager" error for each entry.
        this.id = ResourceLocation.fromNamespaceAndPath(OreDistributionScanner.MOD_ID,
                "/ore_distribution/" + entry.oreId().getNamespace() + "/" + entry.oreId().getPath());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return OdsEmiPlugin.CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(output);
    }

    @Override
    public int getDisplayWidth() {
        return OreDistributionGraph.width();
    }

    @Override
    public int getDisplayHeight() {
        return OreDistributionGraph.height();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.add(new GraphWidget(entry));
        widgets.addSlot(output, 7, 21);
    }
}
