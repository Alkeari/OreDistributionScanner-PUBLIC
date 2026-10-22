package com.alkeari.ore_distribution_scanner.scan;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.config.ModConfig;
import com.alkeari.ore_distribution_scanner.network.ScanNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

/**
 * Server-side watcher that resolves the ore-id set for clients and detects ore whitelist/blacklist
 * edits. Because each viewer collects its display entries once on join (no programmatic reload), an
 * edited ore list only takes effect on rejoin; this throttled check messages players to do so.
 */
public final class OreListWatcher {
    private static final int CHECK_INTERVAL_TICKS = 40;

    private static int tickCounter;
    private static List<String> lastWhitelist;
    private static List<String> lastBlacklist;

    private OreListWatcher() {
    }

    public static List<ResourceLocation> resolve(MinecraftServer server) {
        OreSelection selection = OreSelection.resolve(server.registryAccess(),
                ModConfig.getOreWhitelist(), ModConfig.getOreBlacklist());
        return selection.oreIds().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        List<String> whitelist = ModConfig.getOreWhitelist();
        List<String> blacklist = ModConfig.getOreBlacklist();
        if (lastWhitelist == null) {
            lastWhitelist = whitelist;
            lastBlacklist = blacklist;
            return;
        }
        if (whitelist.equals(lastWhitelist) && blacklist.equals(lastBlacklist)) {
            return;
        }

        lastWhitelist = whitelist;
        lastBlacklist = blacklist;
        ScanNetworking.sendOreListToAll(server, resolve(server));

        Component notice = Component.translatable("message.ore_distribution_scanner.ore_list_changed");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(notice);
        }
        OreDistributionScanner.LOGGER.info("Ore whitelist/blacklist changed; notified players to rejoin.");
    }

    public static void reset() {
        tickCounter = 0;
        lastWhitelist = null;
        lastBlacklist = null;
    }
}
