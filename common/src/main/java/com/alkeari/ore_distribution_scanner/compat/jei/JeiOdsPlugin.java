package com.alkeari.ore_distribution_scanner.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntries;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JeiOdsPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(OreDistributionScanner.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new OreDistributionCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var entries = OreEntries.forClient();
        OreDistributionScanner.LOGGER.info("JEI: collected {} ore distribution entries.", entries.size());
        registration.addRecipes(OreDistributionCategory.RECIPE_TYPE, entries);
    }
}
