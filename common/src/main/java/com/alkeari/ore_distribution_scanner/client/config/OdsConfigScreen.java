package com.alkeari.ore_distribution_scanner.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import com.alkeari.ore_distribution_scanner.config.ModConfig;
import com.alkeari.ore_distribution_scanner.config.ModConfigDefaults;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * Cloth Config screen for editing the mod's settings in-game, reading through {@link ModConfig} and
 * writing back on save. Client-only and only loaded where Cloth is present (the Fabric ModMenu entry).
 * Scan-parameter edits apply on the next scan; ore whitelist/blacklist edits trigger the rejoin notice.
 */
public final class OdsConfigScreen {
    private OdsConfigScreen() {
    }

    private static String key(String suffix) {
        return "config.ore_distribution_scanner." + suffix;
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(key("title")))
                .setSavingRunnable(() -> {
                });
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory scan = builder.getOrCreateCategory(Component.translatable(key("category.scan")));
        scan.addEntry(eb.startIntField(Component.translatable(key("scan_radius_chunks")), ModConfig.getScanRadiusChunks())
                .setDefaultValue(ModConfigDefaults.DEFAULT_SCAN_RADIUS_CHUNKS)
                .setMin(ModConfigDefaults.MIN_SCAN_RADIUS_CHUNKS).setMax(ModConfigDefaults.MAX_SCAN_RADIUS_CHUNKS)
                .setTooltip(Component.translatable(key("scan_radius_chunks.tooltip")))
                .setSaveConsumer(ModConfig::setScanRadiusChunks).build());
        scan.addEntry(eb.startIntField(Component.translatable(key("biome_cluster_size")), ModConfig.getBiomeClusterSize())
                .setDefaultValue(ModConfigDefaults.DEFAULT_BIOME_CLUSTER_SIZE)
                .setMin(ModConfigDefaults.MIN_BIOME_CLUSTER_SIZE).setMax(ModConfigDefaults.MAX_BIOME_CLUSTER_SIZE)
                .setTooltip(Component.translatable(key("biome_cluster_size.tooltip")))
                .setSaveConsumer(ModConfig::setBiomeClusterSize).build());
        scan.addEntry(eb.startIntField(Component.translatable(key("chunk_budget")), ModConfig.getChunkBudget())
                .setDefaultValue(ModConfigDefaults.DEFAULT_CHUNK_BUDGET)
                .setMin(ModConfigDefaults.MIN_CHUNK_BUDGET).setMax(ModConfigDefaults.MAX_CHUNK_BUDGET)
                .setTooltip(Component.translatable(key("chunk_budget.tooltip")))
                .setSaveConsumer(ModConfig::setChunkBudget).build());
        scan.addEntry(eb.startIntField(Component.translatable(key("progress_update_interval_seconds")), ModConfig.getProgressUpdateIntervalSeconds())
                .setDefaultValue(ModConfigDefaults.DEFAULT_PROGRESS_UPDATE_INTERVAL_SECONDS)
                .setMin(ModConfigDefaults.MIN_PROGRESS_UPDATE_INTERVAL_SECONDS).setMax(ModConfigDefaults.MAX_PROGRESS_UPDATE_INTERVAL_SECONDS)
                .setTooltip(Component.translatable(key("progress_update_interval_seconds.tooltip")))
                .setSaveConsumer(ModConfig::setProgressUpdateIntervalSeconds).build());
        scan.addEntry(eb.startIntField(Component.translatable(key("low_confidence_sample_threshold")), ModConfig.getLowConfidenceSampleThreshold())
                .setDefaultValue(ModConfigDefaults.DEFAULT_LOW_CONFIDENCE_SAMPLE_THRESHOLD)
                .setMin(ModConfigDefaults.MIN_LOW_CONFIDENCE_SAMPLE_THRESHOLD).setMax(ModConfigDefaults.MAX_LOW_CONFIDENCE_SAMPLE_THRESHOLD)
                .setTooltip(Component.translatable(key("low_confidence_sample_threshold.tooltip")))
                .setSaveConsumer(ModConfig::setLowConfidenceSampleThreshold).build());
        scan.addEntry(eb.startIntField(Component.translatable(key("scan_permission_level")), ModConfig.getScanPermissionLevel())
                .setDefaultValue(ModConfigDefaults.DEFAULT_SCAN_PERMISSION_LEVEL)
                .setMin(ModConfigDefaults.MIN_SCAN_PERMISSION_LEVEL).setMax(ModConfigDefaults.MAX_SCAN_PERMISSION_LEVEL)
                .setTooltip(Component.translatable(key("scan_permission_level.tooltip")))
                .setSaveConsumer(ModConfig::setScanPermissionLevel).build());

        ConfigCategory ores = builder.getOrCreateCategory(Component.translatable(key("category.ores")));
        ores.addEntry(eb.startStrList(Component.translatable(key("ore_whitelist")), ModConfig.getOreWhitelist())
                .setDefaultValue(new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_WHITELIST))
                .setTooltip(Component.translatable(key("ore_whitelist.tooltip")))
                .setSaveConsumer(ModConfig::setOreWhitelist).build());
        ores.addEntry(eb.startStrList(Component.translatable(key("ore_blacklist")), ModConfig.getOreBlacklist())
                .setDefaultValue(new ArrayList<>(ModConfigDefaults.DEFAULT_ORE_BLACKLIST))
                .setTooltip(Component.translatable(key("ore_blacklist.tooltip")))
                .setSaveConsumer(ModConfig::setOreBlacklist).build());

        return builder.build();
    }
}
