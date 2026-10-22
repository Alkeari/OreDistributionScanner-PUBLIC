package com.alkeari.ore_distribution_scanner.fabric;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import net.fabricmc.api.ModInitializer;

public class OreDistributionScannerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OreDistributionScanner.init();
    }
}
