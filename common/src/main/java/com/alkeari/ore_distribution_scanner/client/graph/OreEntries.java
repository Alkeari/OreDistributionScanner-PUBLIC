package com.alkeari.ore_distribution_scanner.client.graph;

import com.alkeari.ore_distribution_scanner.client.ClientOreList;
import com.alkeari.ore_distribution_scanner.config.ModConfig;
import com.alkeari.ore_distribution_scanner.scan.OreSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

/**
 * Builds the candidate ore entries shown in the viewers, one per id in the server's resolved ore set
 * (tags plus whitelist, minus blacklist), so whitelisted ores appear and blacklisted ones do not.
 * That set arrives via an S2C push on join ({@link ClientOreList}); until it does (or in
 * singleplayer where config is local), it falls back to resolving locally. Editing the whitelist or
 * blacklist changes which entries exist, so it needs a rejoin to re-collect, with a server notice.
 */
public final class OreEntries {
    private OreEntries() {
    }

    public static List<OreEntry> forClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }

        List<ResourceLocation> ids = ClientOreList.get().oreIds();
        if (ids.isEmpty()) {
            OreSelection selection = OreSelection.resolve(mc.level.registryAccess(),
                    ModConfig.getOreWhitelist(), ModConfig.getOreBlacklist());
            ids = List.copyOf(selection.oreIds());
        }
        return ids.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(OreEntry::new)
                .toList();
    }
}
