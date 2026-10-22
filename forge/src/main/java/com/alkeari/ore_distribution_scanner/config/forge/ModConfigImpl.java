package com.alkeari.ore_distribution_scanner.config.forge;

import com.alkeari.ore_distribution_scanner.config.ModConfigDefaults;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public final class ModConfigImpl {
    private ModConfigImpl() {
    }

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.IntValue SCAN_RADIUS_CHUNKS;
    private static final ForgeConfigSpec.IntValue BIOME_CLUSTER_SIZE;
    private static final ForgeConfigSpec.IntValue CHUNK_BUDGET;
    private static final ForgeConfigSpec.IntValue PROGRESS_UPDATE_INTERVAL_SECONDS;
    private static final ForgeConfigSpec.IntValue LOW_CONFIDENCE_SAMPLE_THRESHOLD;
    private static final ForgeConfigSpec.IntValue SCAN_PERMISSION_LEVEL;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ORE_WHITELIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ORE_BLACKLIST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        SCAN_RADIUS_CHUNKS = builder
                .comment("Radius in chunks of the spread sample around the scan origin.")
                .defineInRange(ModConfigDefaults.KEY_SCAN_RADIUS_CHUNKS,
                        ModConfigDefaults.DEFAULT_SCAN_RADIUS_CHUNKS,
                        ModConfigDefaults.MIN_SCAN_RADIUS_CHUNKS,
                        ModConfigDefaults.MAX_SCAN_RADIUS_CHUNKS);

        BIOME_CLUSTER_SIZE = builder
                .comment("Chunks sampled in a small cluster around each located biome position.")
                .defineInRange(ModConfigDefaults.KEY_BIOME_CLUSTER_SIZE,
                        ModConfigDefaults.DEFAULT_BIOME_CLUSTER_SIZE,
                        ModConfigDefaults.MIN_BIOME_CLUSTER_SIZE,
                        ModConfigDefaults.MAX_BIOME_CLUSTER_SIZE);

        CHUNK_BUDGET = builder
                .comment("Maximum number of chunks sampled in a single scan.")
                .defineInRange(ModConfigDefaults.KEY_CHUNK_BUDGET,
                        ModConfigDefaults.DEFAULT_CHUNK_BUDGET,
                        ModConfigDefaults.MIN_CHUNK_BUDGET,
                        ModConfigDefaults.MAX_CHUNK_BUDGET);

        PROGRESS_UPDATE_INTERVAL_SECONDS = builder
                .comment("How often, in seconds, scan progress is posted to the command sender.")
                .defineInRange(ModConfigDefaults.KEY_PROGRESS_UPDATE_INTERVAL_SECONDS,
                        ModConfigDefaults.DEFAULT_PROGRESS_UPDATE_INTERVAL_SECONDS,
                        ModConfigDefaults.MIN_PROGRESS_UPDATE_INTERVAL_SECONDS,
                        ModConfigDefaults.MAX_PROGRESS_UPDATE_INTERVAL_SECONDS);

        LOW_CONFIDENCE_SAMPLE_THRESHOLD = builder
                .comment("Biomes with fewer sampled columns than this are flagged low-confidence.")
                .defineInRange(ModConfigDefaults.KEY_LOW_CONFIDENCE_SAMPLE_THRESHOLD,
                        ModConfigDefaults.DEFAULT_LOW_CONFIDENCE_SAMPLE_THRESHOLD,
                        ModConfigDefaults.MIN_LOW_CONFIDENCE_SAMPLE_THRESHOLD,
                        ModConfigDefaults.MAX_LOW_CONFIDENCE_SAMPLE_THRESHOLD);

        SCAN_PERMISSION_LEVEL = builder
                .comment("Permission level required for /ods scan, cancel, clear, and reload. Default 0 means any player can use them with no cheats; raise it to restrict these to operators.")
                .defineInRange(ModConfigDefaults.KEY_SCAN_PERMISSION_LEVEL,
                        ModConfigDefaults.DEFAULT_SCAN_PERMISSION_LEVEL,
                        ModConfigDefaults.MIN_SCAN_PERMISSION_LEVEL,
                        ModConfigDefaults.MAX_SCAN_PERMISSION_LEVEL);

        ORE_WHITELIST = builder
                .comment("Extra block ids to count as ores, e.g. \"minecraft:ancient_debris\".")
                .defineList(ModConfigDefaults.KEY_ORE_WHITELIST,
                        new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_WHITELIST),
                        o -> o instanceof String);

        ORE_BLACKLIST = builder
                .comment("Block ids to exclude even if the ore tag includes them.")
                .defineList(ModConfigDefaults.KEY_ORE_BLACKLIST,
                        new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_BLACKLIST),
                        o -> o instanceof String);

        SPEC = builder.build();
    }

    public static int getScanRadiusChunks() {
        return SCAN_RADIUS_CHUNKS.get();
    }

    public static int getBiomeClusterSize() {
        return BIOME_CLUSTER_SIZE.get();
    }

    public static int getChunkBudget() {
        return CHUNK_BUDGET.get();
    }

    public static int getProgressUpdateIntervalSeconds() {
        return PROGRESS_UPDATE_INTERVAL_SECONDS.get();
    }

    public static int getLowConfidenceSampleThreshold() {
        return LOW_CONFIDENCE_SAMPLE_THRESHOLD.get();
    }

    public static int getScanPermissionLevel() {
        return SCAN_PERMISSION_LEVEL.get();
    }

    public static List<String> getOreWhitelist() {
        return new ArrayList<>(ORE_WHITELIST.get());
    }

    public static List<String> getOreBlacklist() {
        return new ArrayList<>(ORE_BLACKLIST.get());
    }

    public static void setScanRadiusChunks(int value) {
        SCAN_RADIUS_CHUNKS.set(value);
        SCAN_RADIUS_CHUNKS.save();
    }

    public static void setBiomeClusterSize(int value) {
        BIOME_CLUSTER_SIZE.set(value);
        BIOME_CLUSTER_SIZE.save();
    }

    public static void setChunkBudget(int value) {
        CHUNK_BUDGET.set(value);
        CHUNK_BUDGET.save();
    }

    public static void setProgressUpdateIntervalSeconds(int value) {
        PROGRESS_UPDATE_INTERVAL_SECONDS.set(value);
        PROGRESS_UPDATE_INTERVAL_SECONDS.save();
    }

    public static void setLowConfidenceSampleThreshold(int value) {
        LOW_CONFIDENCE_SAMPLE_THRESHOLD.set(value);
        LOW_CONFIDENCE_SAMPLE_THRESHOLD.save();
    }

    public static void setScanPermissionLevel(int value) {
        SCAN_PERMISSION_LEVEL.set(value);
        SCAN_PERMISSION_LEVEL.save();
    }

    public static void setOreWhitelist(List<String> value) {
        ORE_WHITELIST.set(new ArrayList<>(value));
        ORE_WHITELIST.save();
    }

    public static void setOreBlacklist(List<String> value) {
        ORE_BLACKLIST.set(new ArrayList<>(value));
        ORE_BLACKLIST.save();
    }

    public static void reload() {
        // Forge config values are read live from the loaded spec, so no explicit re-read is needed.
    }
}
