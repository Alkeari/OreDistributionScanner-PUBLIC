package com.alkeari.ore_distribution_scanner.compat.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntries;

/**
 * Shared REI plugin logic. Fabric discovers it via the {@code rei_client} entrypoint; Forge uses a
 * thin {@code @REIPluginClient}-annotated subclass (the annotation lives in the REI forge artifact).
 */
public class OdsReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new OreDistributionReiCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        var entries = OreEntries.forClient();
        OreDistributionScanner.LOGGER.info("REI: collected {} ore distribution displays.", entries.size());
        entries.forEach(entry -> registry.add(new OreDistributionDisplay(entry)));
    }
}
