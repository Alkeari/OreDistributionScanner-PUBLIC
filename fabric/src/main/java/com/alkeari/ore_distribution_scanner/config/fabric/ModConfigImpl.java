package com.alkeari.ore_distribution_scanner.config.fabric;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.config.ModConfigDefaults;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModConfigImpl {
    private ModConfigImpl() {
    }

    private static FileConfig config;

    private static synchronized FileConfig config() {
        if (config == null) {
            Path path = FabricLoader.getInstance().getConfigDir()
                    .resolve(OreDistributionScanner.MOD_ID + ".toml");
            FileConfig loaded = FileConfig.builder(path)
                    .autoreload()
                    .build();
            loaded.load();
            writeDefaults(loaded);
            config = loaded;
        }
        return config;
    }

    private static void writeDefaults(FileConfig config) {
        boolean changed = false;
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_SCAN_RADIUS_CHUNKS, ModConfigDefaults.DEFAULT_SCAN_RADIUS_CHUNKS);
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_BIOME_CLUSTER_SIZE, ModConfigDefaults.DEFAULT_BIOME_CLUSTER_SIZE);
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_CHUNK_BUDGET, ModConfigDefaults.DEFAULT_CHUNK_BUDGET);
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_PROGRESS_UPDATE_INTERVAL_SECONDS, ModConfigDefaults.DEFAULT_PROGRESS_UPDATE_INTERVAL_SECONDS);
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_LOW_CONFIDENCE_SAMPLE_THRESHOLD, ModConfigDefaults.DEFAULT_LOW_CONFIDENCE_SAMPLE_THRESHOLD);
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_SCAN_PERMISSION_LEVEL, ModConfigDefaults.DEFAULT_SCAN_PERMISSION_LEVEL);
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_ORE_WHITELIST, new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_WHITELIST));
        changed |= putIfAbsent(config, ModConfigDefaults.KEY_ORE_BLACKLIST, new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_BLACKLIST));
        if (changed) {
            config.save();
        }
    }

    private static boolean putIfAbsent(FileConfig config, String key, Object value) {
        if (config.get(key) == null) {
            config.set(key, value);
            return true;
        }
        return false;
    }

    private static int getClampedInt(String key, int def, int min, int max) {
        int raw = config().getIntOrElse(key, def);
        int clamped = ModConfigDefaults.clamp(raw, min, max);
        if (clamped != raw) {
            OreDistributionScanner.LOGGER.warn("Config value {}={} out of range [{}, {}], clamping to {}.",
                    key, raw, min, max, clamped);
        }
        return clamped;
    }

    public static int getScanRadiusChunks() {
        return getClampedInt(ModConfigDefaults.KEY_SCAN_RADIUS_CHUNKS,
                ModConfigDefaults.DEFAULT_SCAN_RADIUS_CHUNKS,
                ModConfigDefaults.MIN_SCAN_RADIUS_CHUNKS,
                ModConfigDefaults.MAX_SCAN_RADIUS_CHUNKS);
    }

    public static int getBiomeClusterSize() {
        return getClampedInt(ModConfigDefaults.KEY_BIOME_CLUSTER_SIZE,
                ModConfigDefaults.DEFAULT_BIOME_CLUSTER_SIZE,
                ModConfigDefaults.MIN_BIOME_CLUSTER_SIZE,
                ModConfigDefaults.MAX_BIOME_CLUSTER_SIZE);
    }

    public static int getChunkBudget() {
        return getClampedInt(ModConfigDefaults.KEY_CHUNK_BUDGET,
                ModConfigDefaults.DEFAULT_CHUNK_BUDGET,
                ModConfigDefaults.MIN_CHUNK_BUDGET,
                ModConfigDefaults.MAX_CHUNK_BUDGET);
    }

    public static int getProgressUpdateIntervalSeconds() {
        return getClampedInt(ModConfigDefaults.KEY_PROGRESS_UPDATE_INTERVAL_SECONDS,
                ModConfigDefaults.DEFAULT_PROGRESS_UPDATE_INTERVAL_SECONDS,
                ModConfigDefaults.MIN_PROGRESS_UPDATE_INTERVAL_SECONDS,
                ModConfigDefaults.MAX_PROGRESS_UPDATE_INTERVAL_SECONDS);
    }

    public static int getLowConfidenceSampleThreshold() {
        return getClampedInt(ModConfigDefaults.KEY_LOW_CONFIDENCE_SAMPLE_THRESHOLD,
                ModConfigDefaults.DEFAULT_LOW_CONFIDENCE_SAMPLE_THRESHOLD,
                ModConfigDefaults.MIN_LOW_CONFIDENCE_SAMPLE_THRESHOLD,
                ModConfigDefaults.MAX_LOW_CONFIDENCE_SAMPLE_THRESHOLD);
    }

    public static int getScanPermissionLevel() {
        return getClampedInt(ModConfigDefaults.KEY_SCAN_PERMISSION_LEVEL,
                ModConfigDefaults.DEFAULT_SCAN_PERMISSION_LEVEL,
                ModConfigDefaults.MIN_SCAN_PERMISSION_LEVEL,
                ModConfigDefaults.MAX_SCAN_PERMISSION_LEVEL);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getOreWhitelist() {
        List<String> list = (List<String>) config().getOrElse(ModConfigDefaults.KEY_ORE_WHITELIST,
                new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_WHITELIST));
        return new ArrayList<>(list);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getOreBlacklist() {
        List<String> list = (List<String>) config().getOrElse(ModConfigDefaults.KEY_ORE_BLACKLIST,
                new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_BLACKLIST));
        return new ArrayList<>(list);
    }

    private static synchronized void set(String key, Object value) {
        config().set(key, value);
        config().save();
    }

    public static void setScanRadiusChunks(int value) {
        set(ModConfigDefaults.KEY_SCAN_RADIUS_CHUNKS, value);
    }

    public static void setBiomeClusterSize(int value) {
        set(ModConfigDefaults.KEY_BIOME_CLUSTER_SIZE, value);
    }

    public static void setChunkBudget(int value) {
        set(ModConfigDefaults.KEY_CHUNK_BUDGET, value);
    }

    public static void setProgressUpdateIntervalSeconds(int value) {
        set(ModConfigDefaults.KEY_PROGRESS_UPDATE_INTERVAL_SECONDS, value);
    }

    public static void setLowConfidenceSampleThreshold(int value) {
        set(ModConfigDefaults.KEY_LOW_CONFIDENCE_SAMPLE_THRESHOLD, value);
    }

    public static void setScanPermissionLevel(int value) {
        set(ModConfigDefaults.KEY_SCAN_PERMISSION_LEVEL, value);
    }

    public static void setOreWhitelist(List<String> value) {
        set(ModConfigDefaults.KEY_ORE_WHITELIST, new ArrayList<>(value));
    }

    public static void setOreBlacklist(List<String> value) {
        set(ModConfigDefaults.KEY_ORE_BLACKLIST, new ArrayList<>(value));
    }

    public static synchronized void reload() {
        config().load();
    }
}
