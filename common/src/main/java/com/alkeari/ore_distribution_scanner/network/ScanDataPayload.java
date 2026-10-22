package com.alkeari.ore_distribution_scanner.network;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload carrying one dimension's scan results. The codec reuses {@link ScanCodec}'s sparse
 * encode/decode (only non-zero Y levels) so packets stay small.
 */
public record ScanDataPayload(DimensionScanData data) implements CustomPacketPayload {
    public static final Type<ScanDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OreDistributionScanner.MOD_ID, "scan_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> ScanCodec.encode(buf, payload.data()),
            buf -> new ScanDataPayload(ScanCodec.decode(buf)));

    @Override
    public Type<ScanDataPayload> type() {
        return TYPE;
    }
}
