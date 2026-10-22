package com.alkeari.ore_distribution_scanner.network;

import com.alkeari.ore_distribution_scanner.data.BiomeScanData;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wire format of the scan sync, shared by both Minecraft lines: {@link ScanNetworking} owns only the
 * transport, which differs by line. Counts are sent sparsely (only non-zero Y levels) to keep packets small.
 */
final class ScanCodec {
    private ScanCodec() {
    }

    static void encodeOreList(FriendlyByteBuf buf, Collection<ResourceLocation> ids) {
        buf.writeVarInt(ids.size());
        for (ResourceLocation id : ids) {
            buf.writeResourceLocation(id);
        }
    }

    static List<ResourceLocation> decodeOreList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buf.readResourceLocation());
        }
        return ids;
    }

    static void encode(FriendlyByteBuf buf, DimensionScanData data) {
        buf.writeResourceLocation(data.getDimension());
        buf.writeInt(data.getMinY());
        buf.writeVarInt(data.getHeight());
        buf.writeVarInt(data.getLowConfidenceThreshold());

        Map<ResourceLocation, BiomeScanData> biomes = data.getBiomes();
        buf.writeVarInt(biomes.size());
        for (Map.Entry<ResourceLocation, BiomeScanData> biomeEntry : biomes.entrySet()) {
            BiomeScanData biome = biomeEntry.getValue();
            buf.writeResourceLocation(biomeEntry.getKey());
            buf.writeVarInt(biome.getSampledColumns());

            Map<ResourceLocation, int[]> ores = biome.snapshotOreCounts();
            buf.writeVarInt(ores.size());
            for (Map.Entry<ResourceLocation, int[]> oreEntry : ores.entrySet()) {
                buf.writeResourceLocation(oreEntry.getKey());
                int[] counts = oreEntry.getValue();
                int nonZero = 0;
                for (int c : counts) {
                    if (c > 0) {
                        nonZero++;
                    }
                }
                buf.writeVarInt(nonZero);
                for (int y = 0; y < counts.length; y++) {
                    if (counts[y] > 0) {
                        buf.writeVarInt(y);
                        buf.writeVarInt(counts[y]);
                    }
                }
            }
        }
    }

    static DimensionScanData decode(FriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        int minY = buf.readInt();
        int height = buf.readVarInt();
        DimensionScanData data = new DimensionScanData(dimension, minY, height);
        data.setLowConfidenceThreshold(buf.readVarInt());

        int biomeCount = buf.readVarInt();
        for (int b = 0; b < biomeCount; b++) {
            ResourceLocation biomeId = buf.readResourceLocation();
            BiomeScanData biome = data.biome(biomeId);
            biome.addSampledColumns(buf.readVarInt());

            int oreCount = buf.readVarInt();
            for (int o = 0; o < oreCount; o++) {
                ResourceLocation oreId = buf.readResourceLocation();
                int entries = buf.readVarInt();
                for (int e = 0; e < entries; e++) {
                    int y = buf.readVarInt();
                    int count = buf.readVarInt();
                    biome.addOreFinds(oreId, y, count);
                }
            }
        }
        return data;
    }
}
