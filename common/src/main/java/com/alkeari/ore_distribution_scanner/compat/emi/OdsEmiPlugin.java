package com.alkeari.ore_distribution_scanner.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@EmiEntrypoint
public class OdsEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(OreDistributionScanner.MOD_ID, "ore_distribution"),
            EmiStack.of(new ItemStack(Items.IRON_ORE)));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        var entries = OreEntries.forClient();
        OreDistributionScanner.LOGGER.info("EMI: collected {} ore distribution entries.", entries.size());
        entries.forEach(entry -> registry.addRecipe(new OreDistributionEmiRecipe(entry)));
    }
}
