package com.alkeari.ore_distribution_scanner.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.ClientOreList;
import com.alkeari.ore_distribution_scanner.client.ClientScanHolder;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Loader-agnostic S2C sync of scan results via Architectury's NetworkManager, using the 1.20 packet
 * API: a channel id plus a raw buffer. Only the client registers receivers; S2C receivers are a client
 * concern, and on a dedicated server Forge's dist cleaner strips the client-only receiver impl, so
 * registering there throws, while the server only ever sends. The wire format is {@link ScanCodec}.
 */
public final class ScanNetworking {
    public static final ResourceLocation SCAN_DATA =
            new ResourceLocation(OreDistributionScanner.MOD_ID, "scan_data");
    public static final ResourceLocation ORE_LIST =
            new ResourceLocation(OreDistributionScanner.MOD_ID, "ore_list");

    private ScanNetworking() {
    }

    public static void register() {
        if (Platform.getEnvironment() != Env.CLIENT) {
            return;
        }
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SCAN_DATA, (buf, context) -> {
            DimensionScanData data = ScanCodec.decode(buf);
            context.queue(() -> {
                ClientScanHolder.get().update(data);
                OreDistributionScanner.LOGGER.info("Client received scan data for {}: {} biomes.",
                        data.getDimension(), data.getBiomes().size());
            });
        });
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ORE_LIST, (buf, context) -> {
            List<ResourceLocation> ids = ScanCodec.decodeOreList(buf);
            context.queue(() -> ClientOreList.get().update(ids));
        });
    }

    public static void sendOreListToPlayer(ServerPlayer player, Collection<ResourceLocation> ids) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ScanCodec.encodeOreList(buf, ids);
        NetworkManager.sendToPlayer(player, ORE_LIST, buf);
    }

    public static void sendOreListToAll(MinecraftServer server, Collection<ResourceLocation> ids) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendOreListToPlayer(player, ids);
        }
    }

    public static void sendToPlayer(ServerPlayer player, DimensionScanData data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ScanCodec.encode(buf, data);
        NetworkManager.sendToPlayer(player, SCAN_DATA, buf);
    }

    public static void sendToAll(MinecraftServer server, DimensionScanData data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player, data);
        }
    }
}
