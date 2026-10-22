package com.alkeari.ore_distribution_scanner.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.ClientOreList;
import com.alkeari.ore_distribution_scanner.client.ClientScanHolder;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loader-agnostic S2C sync of scan results via Architectury's NetworkManager, using the 1.21 custom
 * payload API. The client registers receivers; a dedicated server registers only the payload type so
 * it can send (registering an S2C receiver on a dedicated server is unsupported on Fabric and the
 * payload type is what sendToPlayer needs to resolve). The wire format is {@link ScanCodec}.
 */
public final class ScanNetworking {
    private ScanNetworking() {
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, ScanDataPayload.TYPE, ScanDataPayload.STREAM_CODEC,
                    (payload, context) -> {
                        DimensionScanData data = payload.data();
                        context.queue(() -> {
                            ClientScanHolder.get().update(data);
                            OreDistributionScanner.LOGGER.info("Client received scan data for {}: {} biomes.",
                                    data.getDimension(), data.getBiomes().size());
                        });
                    });
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, OreListPayload.TYPE, OreListPayload.STREAM_CODEC,
                    (payload, context) -> {
                        List<ResourceLocation> ids = payload.ids();
                        context.queue(() -> ClientOreList.get().update(ids));
                    });
        } else {
            NetworkManager.registerS2CPayloadType(ScanDataPayload.TYPE, ScanDataPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OreListPayload.TYPE, OreListPayload.STREAM_CODEC);
        }
    }

    public static void sendOreListToPlayer(ServerPlayer player, Collection<ResourceLocation> ids) {
        NetworkManager.sendToPlayer(player, new OreListPayload(new ArrayList<>(ids)));
    }

    public static void sendOreListToAll(MinecraftServer server, Collection<ResourceLocation> ids) {
        OreListPayload payload = new OreListPayload(new ArrayList<>(ids));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }

    public static void sendToPlayer(ServerPlayer player, DimensionScanData data) {
        NetworkManager.sendToPlayer(player, new ScanDataPayload(data));
    }

    public static void sendToAll(MinecraftServer server, DimensionScanData data) {
        ScanDataPayload payload = new ScanDataPayload(data);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }
}
