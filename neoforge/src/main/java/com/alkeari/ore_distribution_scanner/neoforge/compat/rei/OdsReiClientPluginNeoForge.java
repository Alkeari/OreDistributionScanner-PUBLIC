package com.alkeari.ore_distribution_scanner.neoforge.compat.rei;

import me.shedaniel.rei.forge.REIPluginClient;
import com.alkeari.ore_distribution_scanner.compat.rei.OdsReiClientPlugin;

/**
 * NeoForge discovery shim for the REI plugin. The real logic lives in the common
 * {@link OdsReiClientPlugin}; this subclass only carries the {@code @REIPluginClient}
 * annotation that REI scans for.
 */
@REIPluginClient
public class OdsReiClientPluginNeoForge extends OdsReiClientPlugin {
}
