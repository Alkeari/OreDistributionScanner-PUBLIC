package com.alkeari.ore_distribution_scanner.network;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * S2C payload carrying the server's resolved ore-id set (ore tags plus whitelist, minus blacklist),
 * pushed on join and when the list changes.
 */
public record OreListPayload(List<ResourceLocation> ids) implements CustomPacketPayload {
    public static final Type<OreListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OreDistributionScanner.MOD_ID, "ore_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreListPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> ScanCodec.encodeOreList(buf, payload.ids()),
            buf -> new OreListPayload(ScanCodec.decodeOreList(buf)));

    @Override
    public Type<OreListPayload> type() {
        return TYPE;
    }
}
