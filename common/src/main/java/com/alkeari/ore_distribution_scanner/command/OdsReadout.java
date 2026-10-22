package com.alkeari.ore_distribution_scanner.command;

import com.alkeari.ore_distribution_scanner.data.BiomeScanData;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import com.alkeari.ore_distribution_scanner.data.ScanStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.alkeari.ore_distribution_scanner.client.graph.AlkeariColors;

/**
 * Builds the text readouts for {@code /ods info} and {@code /ods list} from server-side scan data.
 * Viewer-independent: works on a dedicated server console with no recipe viewer installed. A "chunk"
 * is 256 sampled columns; per-chunk averages divide column counts by 256.
 */
public final class OdsReadout {
    private static final int GOLD = AlkeariColors.rgb(AlkeariColors.FORGED_GOLD);
    private static final int COLUMNS_PER_CHUNK = 256;
    private static final int SECONDARY_MIN_GAP = 16;

    private OdsReadout() {
    }

    static MutableComponent gold(String s) {
        return Component.literal(s).withStyle(style -> style.withColor(GOLD));
    }

    private static int chunks(int sampledColumns) {
        return Math.round((float) sampledColumns / COLUMNS_PER_CHUNK);
    }

    private static boolean lowConfidence(DimensionScanData data, int sampledColumns) {
        return sampledColumns < data.getLowConfidenceThreshold() * COLUMNS_PER_CHUNK;
    }

    public static List<Component> oreInfo(DimensionScanData data, ResourceLocation ore, ResourceLocation biome) {
        BiomeScanData scope = biome == null ? data.aggregateBiome() : data.getBiomes().get(biome);
        if (scope == null) {
            return List.of(Component.literal("No data for biome " + biome + " in " + data.getDimension() + "."));
        }
        int[] counts = scope.getOreCounts(ore);
        int sampled = scope.getSampledColumns();
        if (counts == null || total(counts) == 0 || sampled == 0) {
            return List.of(Component.literal("No data for " + ore + " in " + data.getDimension()
                    + (biome != null ? " (" + biome + ")" : "") + ". Run /ods scan to populate."));
        }

        int minY = data.getMinY();
        int peak = argMax(counts);
        boolean low = lowConfidence(data, sampled);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(ore + " in " + data.getDimension()
                + (biome != null ? " (" + biome.getPath() + ")" : " (all biomes)")));
        lines.add(Component.literal("Peak Y ").append(gold(Integer.toString(minY + peak)))
                .append(Component.literal(", "))
                .append(gold(String.format("%.2f", avgPerChunk(counts[peak], sampled))))
                .append(Component.literal(" avg per chunk" + (low ? " (low confidence)" : ""))));

        int secondary = secondaryPeak(counts, peak);
        if (secondary >= 0) {
            lines.add(Component.literal("Secondary peak Y ").append(gold(Integer.toString(minY + secondary)))
                    .append(Component.literal(", " + String.format("%.2f", avgPerChunk(counts[secondary], sampled))
                            + " avg per chunk")));
        }

        if (biome == null) {
            List<Component> top = topBiomes(data, ore);
            if (!top.isEmpty()) {
                lines.add(Component.literal("Top biomes:"));
                lines.addAll(top);
            }
        }

        lines.add(Component.literal("Total found ").append(gold(String.format("%,d", total(counts))))
                .append(Component.literal(" across ")).append(gold(String.format("%,d", chunks(sampled))))
                .append(Component.literal(" chunks")));
        return lines;
    }

    private static List<Component> topBiomes(DimensionScanData data, ResourceLocation ore) {
        record Entry(ResourceLocation biome, int peakY, double avg, int sampled) {
        }
        int minY = data.getMinY();
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, BiomeScanData> e : data.getBiomes().entrySet()) {
            int[] counts = e.getValue().getOreCounts(ore);
            int sampled = e.getValue().getSampledColumns();
            if (counts == null || total(counts) == 0 || sampled == 0) {
                continue;
            }
            int peak = argMax(counts);
            entries.add(new Entry(e.getKey(), minY + peak, avgPerChunk(counts[peak], sampled), sampled));
        }
        entries.sort(Comparator.comparingDouble(Entry::avg).reversed());
        List<Component> lines = new ArrayList<>();
        for (Entry entry : entries.stream().limit(3).toList()) {
            boolean low = lowConfidence(data, entry.sampled());
            lines.add(Component.literal("  " + entry.biome().getPath() + ": peak Y ")
                    .append(gold(Integer.toString(entry.peakY())))
                    .append(Component.literal(", " + String.format("%.2f", entry.avg()) + " avg per chunk"
                            + (low ? " (low confidence)" : ""))));
        }
        return lines;
    }

    public static List<Component> listDimensions(MinecraftServer server) {
        List<DimensionScanData> all = ScanStorage.loadAll(server);
        if (all.isEmpty()) {
            return List.of(Component.literal("No saved scans. Run /ods scan to populate."));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Saved scans:"));
        all.sort(Comparator.comparing(d -> d.getDimension().toString()));
        for (DimensionScanData data : all) {
            BiomeScanData aggregate = data.aggregateBiome();
            String ago = ScanStorage.lastModified(server, data.getDimension())
                    .map(OdsReadout::ago).orElse("unknown");
            lines.add(gold(data.getDimension().toString())
                    .copy().append(Component.literal(": " + String.format("%,d", chunks(aggregate.getSampledColumns()))
                            + " chunks, " + data.getBiomes().size() + " biomes, scanned " + ago)));
        }
        return lines;
    }

    public static List<Component> listOres(DimensionScanData data) {
        BiomeScanData aggregate = data.aggregateBiome();
        Map<ResourceLocation, int[]> ores = aggregate.snapshotOreCounts();
        if (ores.isEmpty()) {
            return List.of(Component.literal("No ores found in " + data.getDimension() + "."));
        }
        int minY = data.getMinY();
        boolean low = lowConfidence(data, aggregate.getSampledColumns());

        record OreEntry(ResourceLocation ore, int peakY) {
        }
        List<OreEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, int[]> e : ores.entrySet()) {
            entries.add(new OreEntry(e.getKey(), minY + argMax(e.getValue())));
        }
        entries.sort(Comparator.comparingInt(OreEntry::peakY).reversed());

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Ores in " + data.getDimension() + (low ? " (low confidence)" : "") + ":"));
        for (OreEntry entry : entries) {
            lines.add(Component.literal("  " + entry.ore() + ": peak Y ").append(gold(Integer.toString(entry.peakY()))));
        }
        return lines;
    }

    private static double avgPerChunk(int count, int sampledColumns) {
        return sampledColumns > 0 ? (double) count * COLUMNS_PER_CHUNK / sampledColumns : 0.0;
    }

    private static long total(int[] counts) {
        long sum = 0;
        for (int c : counts) {
            sum += c;
        }
        return sum;
    }

    private static int argMax(int[] counts) {
        int best = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[best]) {
                best = i;
            }
        }
        return best;
    }

    /** The highest count at least SECONDARY_MIN_GAP away from the primary peak, if it is significant. */
    private static int secondaryPeak(int[] counts, int primary) {
        int best = -1;
        for (int i = 0; i < counts.length; i++) {
            if (Math.abs(i - primary) < SECONDARY_MIN_GAP || counts[i] == 0) {
                continue;
            }
            if (best < 0 || counts[i] > counts[best]) {
                best = i;
            }
        }
        if (best >= 0 && counts[best] >= counts[primary] * 0.25) {
            return best;
        }
        return -1;
    }

    private static String ago(Instant when) {
        Duration d = Duration.between(when, Instant.now());
        long s = Math.max(0, d.getSeconds());
        if (s < 60) {
            return "just now";
        }
        if (s < 3600) {
            return (s / 60) + "m ago";
        }
        if (s < 86400) {
            return (s / 3600) + "h ago";
        }
        return (s / 86400) + "d ago";
    }
}
