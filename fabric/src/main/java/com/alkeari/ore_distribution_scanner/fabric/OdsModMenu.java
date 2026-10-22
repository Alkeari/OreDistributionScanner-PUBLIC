package com.alkeari.ore_distribution_scanner.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.alkeari.ore_distribution_scanner.client.config.OdsConfigScreen;
import net.fabricmc.loader.api.FabricLoader;

public class OdsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return parent -> null;
        }
        return OdsConfigScreen::create;
    }
}
