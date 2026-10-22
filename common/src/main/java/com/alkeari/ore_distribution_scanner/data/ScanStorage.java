package com.alkeari.ore_distribution_scanner.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads and writes scan results as one JSON file per dimension, under the world save directory
 * (per-world rather than the global config dir, so scans of different saves do not collide). Also
 * resolves the on-disk region directory for a dimension so path B can open its .mca files.
 */
public final class ScanStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ScanStorage() {
    }

    private static Path scanDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(OreDistributionScanner.MOD_ID);
    }

    private static String fileName(ResourceLocation dimension) {
        return dimension.getNamespace() + "_" + dimension.getPath().replace('/', '_') + ".json";
    }

    public static Path regionDirectory(MinecraftServer server, ResourceKey<Level> dimension) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        return DimensionType.getStorageFolder(dimension, worldRoot).resolve("region");
    }

    public static void save(MinecraftServer server, DimensionScanData data) {
        Path dir = scanDir(server);
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName(data.getDimension()));
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data.toJson(), writer);
            }
        } catch (IOException e) {
            OreDistributionScanner.LOGGER.error("Failed to write scan data for {}", data.getDimension(), e);
        }
    }

    public static List<DimensionScanData> loadAll(MinecraftServer server) {
        Path dir = scanDir(server);
        List<DimensionScanData> all = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return all;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(file -> {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    all.add(DimensionScanData.fromJson(GSON.fromJson(reader, JsonObject.class)));
                } catch (Exception e) {
                    OreDistributionScanner.LOGGER.error("Failed to read scan data file {}", file, e);
                }
            });
        } catch (IOException e) {
            OreDistributionScanner.LOGGER.error("Failed to list scan data directory {}", dir, e);
        }
        return all;
    }

    public static Optional<DimensionScanData> load(MinecraftServer server, ResourceLocation dimension) {
        Path file = scanDir(server).resolve(fileName(dimension));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            return Optional.of(DimensionScanData.fromJson(root));
        } catch (Exception e) {
            OreDistributionScanner.LOGGER.error("Failed to read scan data for {}", dimension, e);
            return Optional.empty();
        }
    }

    /** Dimension ids that have a saved result, read from each file's dimension field. */
    public static List<ResourceLocation> scannedDimensions(MinecraftServer server) {
        Path dir = scanDir(server);
        List<ResourceLocation> ids = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return ids;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(file -> {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root != null && root.has("dimension")) {
                        ResourceLocation id = ResourceLocation.tryParse(root.get("dimension").getAsString());
                        if (id != null) {
                            ids.add(id);
                        }
                    }
                } catch (Exception e) {
                    OreDistributionScanner.LOGGER.error("Failed to read scan data file {}", file, e);
                }
            });
        } catch (IOException e) {
            OreDistributionScanner.LOGGER.error("Failed to list scan data directory {}", dir, e);
        }
        return ids;
    }

    public static Optional<Instant> lastModified(MinecraftServer server, ResourceLocation dimension) {
        Path file = scanDir(server).resolve(fileName(dimension));
        try {
            if (Files.isRegularFile(file)) {
                return Optional.of(Files.getLastModifiedTime(file).toInstant());
            }
        } catch (IOException e) {
            OreDistributionScanner.LOGGER.error("Failed to stat scan data for {}", dimension, e);
        }
        return Optional.empty();
    }

    public static boolean delete(MinecraftServer server, ResourceLocation dimension) {
        Path file = scanDir(server).resolve(fileName(dimension));
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            OreDistributionScanner.LOGGER.error("Failed to delete scan data for {}", dimension, e);
            return false;
        }
    }

    public static int deleteAll(MinecraftServer server) {
        Path dir = scanDir(server);
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        int deleted = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                if (Files.deleteIfExists(file)) {
                    deleted++;
                }
            }
        } catch (IOException e) {
            OreDistributionScanner.LOGGER.error("Failed to delete scan data directory {}", dir, e);
        }
        return deleted;
    }
}
