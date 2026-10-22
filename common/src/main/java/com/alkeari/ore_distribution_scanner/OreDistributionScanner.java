package com.alkeari.ore_distribution_scanner;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import com.alkeari.ore_distribution_scanner.command.OdsCommand;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import com.alkeari.ore_distribution_scanner.data.ScanStorage;
import com.alkeari.ore_distribution_scanner.network.ScanNetworking;
import com.alkeari.ore_distribution_scanner.scan.OreListWatcher;
import com.alkeari.ore_distribution_scanner.scan.ScanManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OreDistributionScanner {
    public static final String MOD_ID = "ore_distribution_scanner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        // Each line's ScanNetworking.register() decides what the client and a dedicated server register.
        ScanNetworking.register();
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> OdsCommand.register(dispatcher));
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            ScanManager.get().shutdown();
            OreListWatcher.reset();
        });
        TickEvent.SERVER_POST.register(server -> {
            ScanManager.get().tick(server);
            OreListWatcher.tick(server);
        });
        PlayerEvent.PLAYER_JOIN.register(player -> {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            ScanNetworking.sendOreListToPlayer(player, OreListWatcher.resolve(server));
            for (DimensionScanData data : ScanStorage.loadAll(server)) {
                ScanNetworking.sendToPlayer(player, data);
            }
        });
        LOGGER.info("{} loaded.", MOD_ID);
    }
}
