package com.alkeari.ore_distribution_scanner.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.List;

/**
 * Single entry point for all configuration. Gameplay code reads config only through this class;
 * platform-specific impls live at {@code com.alkeari.ore_distribution_scanner.<platform>.config.ModConfigImpl}.
 */
public final class ModConfig {
    private ModConfig() {
    }

    @ExpectPlatform
    public static int getScanRadiusChunks() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getBiomeClusterSize() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getChunkBudget() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getProgressUpdateIntervalSeconds() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getLowConfidenceSampleThreshold() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getScanPermissionLevel() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<String> getOreWhitelist() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<String> getOreBlacklist() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setScanRadiusChunks(int value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setBiomeClusterSize(int value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setChunkBudget(int value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setProgressUpdateIntervalSeconds(int value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setLowConfidenceSampleThreshold(int value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setScanPermissionLevel(int value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setOreWhitelist(List<String> value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setOreBlacklist(List<String> value) {
        throw new AssertionError();
    }

    /** Force a re-read of the config file so externally edited values take effect immediately. */
    @ExpectPlatform
    public static void reload() {
        throw new AssertionError();
    }
}
