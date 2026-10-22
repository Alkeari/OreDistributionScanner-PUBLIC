package com.alkeari.ore_distribution_scanner.forge.compat.rei;

import me.shedaniel.rei.forge.REIPluginClient;
import com.alkeari.ore_distribution_scanner.compat.rei.OdsReiClientPlugin;

/**
 * Forge discovery shim for the REI plugin. The real logic lives in the common
 * {@link OdsReiClientPlugin}; this subclass only carries the Forge-only {@code @REIPluginClient}
 * annotation that REI scans for.
 */
@REIPluginClient
public class OdsReiClientPluginForge extends OdsReiClientPlugin {
}
