package com.alkeari.ore_distribution_scanner.neoforge;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.config.neoforge.ModConfigImpl;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(OreDistributionScanner.MOD_ID)
public final class OreDistributionScannerNeoForge {
    public OreDistributionScannerNeoForge(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ModConfigImpl.SPEC);
        OreDistributionScanner.init();
    }
}
