package com.alkeari.ore_distribution_scanner.forge;

import dev.architectury.platform.forge.EventBuses;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.config.forge.ModConfigImpl;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OreDistributionScanner.MOD_ID)
public class OreDistributionScannerForge {
    @SuppressWarnings("removal")
    public OreDistributionScannerForge() {
        EventBuses.registerModEventBus(OreDistributionScanner.MOD_ID,
                FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigImpl.SPEC);
        OreDistributionScanner.init();
    }
}
