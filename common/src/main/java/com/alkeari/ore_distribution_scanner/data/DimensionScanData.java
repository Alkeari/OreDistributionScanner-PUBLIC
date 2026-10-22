package com.alkeari.ore_distribution_scanner.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The full result of one dimension scan: the dimension id, its vertical extent ({@code minY} and
 * {@code height = maxY - minY + 1}), and a per-biome breakdown. A dimension-wide aggregate across
 * all biomes is derived on demand via {@link #aggregateBiome()} for the default "all biomes" view.
 */
public final class DimensionScanData {
    public static final ResourceLocation AGGREGATE_BIOME = ResourceLocation.fromNamespaceAndPath("ore_distribution_scanner", "all_biomes");

    private static final int DEFAULT_LOW_CONFIDENCE_THRESHOLD = 8;

    private final ResourceLocation dimension;
    private final int minY;
    private final int height;
    private int lowConfidenceThreshold = DEFAULT_LOW_CONFIDENCE_THRESHOLD;
    private final Map<ResourceLocation, BiomeScanData> biomes = new ConcurrentHashMap<>();

    public DimensionScanData(ResourceLocation dimension, int minY, int height) {
        this.dimension = dimension;
        this.minY = minY;
        this.height = height;
    }

    public int getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(int lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public int getMinY() {
        return minY;
    }

    public int getHeight() {
        return height;
    }

    public BiomeScanData biome(ResourceLocation biome) {
        return biomes.computeIfAbsent(biome, k -> new BiomeScanData(height));
    }

    public Map<ResourceLocation, BiomeScanData> getBiomes() {
        return biomes;
    }

    /**
     * Sums every biome's sample counts and ore counts into a single combined view.
     */
    public BiomeScanData aggregateBiome() {
        BiomeScanData aggregate = new BiomeScanData(height);
        for (BiomeScanData data : biomes.values()) {
            aggregate.addSampledColumns(data.getSampledColumns());
            Map<ResourceLocation, int[]> ores = data.snapshotOreCounts();
            for (Map.Entry<ResourceLocation, int[]> entry : ores.entrySet()) {
                int[] counts = entry.getValue();
                for (int y = 0; y < counts.length; y++) {
                    aggregate.addOreFinds(entry.getKey(), y, counts[y]);
                }
            }
        }
        return aggregate;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("dimension", dimension.toString());
        root.addProperty("minY", minY);
        root.addProperty("height", height);
        root.addProperty("lowConfidenceThreshold", lowConfidenceThreshold);

        JsonObject biomesJson = new JsonObject();
        for (Map.Entry<ResourceLocation, BiomeScanData> entry : biomes.entrySet()) {
            BiomeScanData data = entry.getValue();
            JsonObject biomeJson = new JsonObject();
            biomeJson.addProperty("sampledColumns", data.getSampledColumns());

            JsonObject oresJson = new JsonObject();
            for (Map.Entry<ResourceLocation, int[]> ore : data.snapshotOreCounts().entrySet()) {
                JsonArray counts = new JsonArray(ore.getValue().length);
                for (int c : ore.getValue()) {
                    counts.add(c);
                }
                oresJson.add(ore.getKey().toString(), counts);
            }
            biomeJson.add("ores", oresJson);
            biomesJson.add(entry.getKey().toString(), biomeJson);
        }
        root.add("biomes", biomesJson);
        return root;
    }

    public static DimensionScanData fromJson(JsonObject root) {
        ResourceLocation dimension = ResourceLocation.parse(root.get("dimension").getAsString());
        int minY = root.get("minY").getAsInt();
        int height = root.get("height").getAsInt();
        DimensionScanData data = new DimensionScanData(dimension, minY, height);
        if (root.has("lowConfidenceThreshold")) {
            data.setLowConfidenceThreshold(root.get("lowConfidenceThreshold").getAsInt());
        }

        JsonObject biomesJson = root.getAsJsonObject("biomes");
        for (Map.Entry<String, com.google.gson.JsonElement> biomeEntry : biomesJson.entrySet()) {
            JsonObject biomeJson = biomeEntry.getValue().getAsJsonObject();
            BiomeScanData biome = data.biome(ResourceLocation.parse(biomeEntry.getKey()));
            biome.addSampledColumns(biomeJson.get("sampledColumns").getAsInt());

            JsonObject oresJson = biomeJson.getAsJsonObject("ores");
            for (Map.Entry<String, com.google.gson.JsonElement> oreEntry : oresJson.entrySet()) {
                ResourceLocation ore = ResourceLocation.parse(oreEntry.getKey());
                JsonArray counts = oreEntry.getValue().getAsJsonArray();
                for (int y = 0; y < counts.size(); y++) {
                    biome.addOreFinds(ore, y, counts.get(y).getAsInt());
                }
            }
        }
        return data;
    }
}
