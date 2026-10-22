package com.alkeari.ore_distribution_scanner.config;

import java.util.List;

/**
 * Shared config key names, defaults, and clamp bounds so the Forge and Fabric impls expose
 * identical TOML files. Keys are flat (no category prefix) and identical across loaders so
 * configs are portable between platforms.
 */
public final class ModConfigDefaults {
    private ModConfigDefaults() {
    }

    public static final String KEY_SCAN_RADIUS_CHUNKS = "scan_radius_chunks";
    public static final int DEFAULT_SCAN_RADIUS_CHUNKS = 32;
    public static final int MIN_SCAN_RADIUS_CHUNKS = 1;
    public static final int MAX_SCAN_RADIUS_CHUNKS = 512;

    public static final String KEY_BIOME_CLUSTER_SIZE = "biome_cluster_size";
    public static final int DEFAULT_BIOME_CLUSTER_SIZE = 4;
    public static final int MIN_BIOME_CLUSTER_SIZE = 0;
    public static final int MAX_BIOME_CLUSTER_SIZE = 64;

    public static final String KEY_CHUNK_BUDGET = "chunk_budget";
    public static final int DEFAULT_CHUNK_BUDGET = 2048;
    public static final int MIN_CHUNK_BUDGET = 16;
    public static final int MAX_CHUNK_BUDGET = 131072;

    public static final String KEY_PROGRESS_UPDATE_INTERVAL_SECONDS = "progress_update_interval_seconds";
    public static final int DEFAULT_PROGRESS_UPDATE_INTERVAL_SECONDS = 1;
    public static final int MIN_PROGRESS_UPDATE_INTERVAL_SECONDS = 1;
    public static final int MAX_PROGRESS_UPDATE_INTERVAL_SECONDS = 60;

    public static final String KEY_LOW_CONFIDENCE_SAMPLE_THRESHOLD = "low_confidence_sample_threshold";
    public static final int DEFAULT_LOW_CONFIDENCE_SAMPLE_THRESHOLD = 8;
    public static final int MIN_LOW_CONFIDENCE_SAMPLE_THRESHOLD = 0;
    public static final int MAX_LOW_CONFIDENCE_SAMPLE_THRESHOLD = 4096;

    public static final String KEY_SCAN_PERMISSION_LEVEL = "scan_permission_level";
    // Default 0 lets the mod be used with no cheats enabled. This level gates /ods scan, cancel,
    // clear, and reload; server admins can raise it to restrict those commands to operators.
    public static final int DEFAULT_SCAN_PERMISSION_LEVEL = 0;
    public static final int MIN_SCAN_PERMISSION_LEVEL = 0;
    public static final int MAX_SCAN_PERMISSION_LEVEL = 4;

    public static final String KEY_ORE_WHITELIST = "ore_whitelist";
    public static final List<String> DEFAULT_ORE_WHITELIST = List.of();

    public static final String KEY_ORE_BLACKLIST = "ore_blacklist";
    public static final List<String> DEFAULT_ORE_BLACKLIST = List.of();

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
