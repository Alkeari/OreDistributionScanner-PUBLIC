package com.alkeari.ore_distribution_scanner.client.graph;

import com.alkeari.ore_distribution_scanner.client.ClientScanHolder;
import com.alkeari.ore_distribution_scanner.data.BiomeScanData;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The single graph renderer shared by the JEI, EMI, and REI adapters. The plot reads horizontally:
 * depth runs left (lowest Y) to right (highest Y) and frequency rises as Forged Gold bars from a
 * baseline, so showing more depth widens the panel rather than heightening it. Height is fixed and
 * width is derived from the GUI scale factor (a stable value read identically by the viewer's frame
 * and by render, so they never diverge). The plot condenses to the ore's actual generation band; on
 * hover the readout snaps to the nearest data Y. A "chunk" is 256 sampled columns.
 */
public final class OreDistributionGraph {
    private static final int CONTENT_H = 210;
    private static final int COLUMNS_PER_CHUNK = 256;

    // Fixed-pixel chrome and plot insets.
    private static final int PLOT_TOP = 84;
    private static final int FREQ_AXIS_X = 22;
    private static final int RIGHT_MARGIN = 8;
    private static final int DEPTH_AXIS_BOTTOM = 26;

    private OreDistributionGraph() {
    }

    /**
     * Panel width, derived from the GUI scale factor (lower scale = more room = wider). Read the same
     * way for the viewer's frame and for render so they always agree at any GUI scale.
     */
    public static int width() {
        int scale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        return switch (Math.max(1, Math.min(4, scale))) {
            case 1 -> 440;
            case 2 -> 360;
            case 3 -> 300;
            default -> 264;
        };
    }

    public static int height() {
        return CONTENT_H;
    }

    // -- public API used by the JEI / EMI / REI adapters ----------------------------------------

    public static void render(GuiGraphics g, int x, int y, OreEntry entry, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int w = width();
        int h = CONTENT_H;

        g.fill(x, y, x + w, y + h, AlkeariColors.OBSIDIAN_BLACK);
        border(g, x, y, x + w, y + h, AlkeariColors.DEEP_GRAPHITE);
        border(g, x + 2, y + 2, x + w - 2, y + h - 2, AlkeariColors.INNER_HAIRLINE);

        ItemStack icon = entry.icon();
        drawTitle(g, font, x, y, w);
        drawHeader(g, font, x, y, w, icon);

        DimensionScanData data = currentData();
        ResourceLocation dimension = currentDimension();
        if (data == null) {
            drawEmptyState(g, font, x, y, w, h, dimension);
            return;
        }

        List<BiomeOption> options = buildOptions(data, entry.oreId());
        int idx = Math.floorMod(entry.selectedBiome(), options.size());
        entry.setSelectedBiome(idx);
        BiomeOption option = options.get(idx);

        drawSelector(g, font, x, y, w, option, options.size());
        if (option.lowConfidence()) {
            drawLowConfidenceChip(g, font, x, y, w, chunks(option.sampleColumns()));
        }
        drawSubline(g, font, x, y, w, option, options.size() - 1);
        drawPlot(g, font, x, y, w, h, data, option);
        drawHover(g, font, x, y, w, h, data, option, mouseX, mouseY);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.footer_populated").getString(),
                x + w / 2, y + h - 10, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);
    }

    public static List<Component> tooltip(int x, int y, OreEntry entry, double mouseX, double mouseY) {
        return List.of();
    }

    /** Cycle the selected biome by a direction (-1 previous, +1 next), wrapping. */
    public static void cycleBiome(OreEntry entry, int direction) {
        DimensionScanData data = currentData();
        if (data == null) {
            return;
        }
        int count = buildOptions(data, entry.oreId()).size();
        if (count > 1) {
            entry.setSelectedBiome(Math.floorMod(entry.selectedBiome() + direction, count));
        }
    }

    public static boolean mouseClicked(int x, int y, OreEntry entry, double mouseX, double mouseY) {
        DimensionScanData data = currentData();
        if (data == null) {
            return false;
        }
        int count = buildOptions(data, entry.oreId()).size();
        int localY = (int) (mouseY - y);
        if (localY < 40 || localY > 56) {
            return false;
        }
        int localX = (int) (mouseX - x);
        int w = width();
        if (localX >= 6 && localX <= 22 && count > 1) {
            entry.setSelectedBiome(Math.floorMod(entry.selectedBiome() - 1, count));
            return true;
        }
        if (localX >= w - 22 && localX <= w - 6 && count > 1) {
            entry.setSelectedBiome(Math.floorMod(entry.selectedBiome() + 1, count));
            return true;
        }
        return false;
    }

    // -- header (unchanged by the rotation) -----------------------------------------------------

    private static void drawTitle(GuiGraphics g, Font font, int x, int y, int w) {
        text(g, font, Component.translatable("gui.ore_distribution_scanner.title").getString(),
                x + w / 2, y + 4, 1.0f, AlkeariColors.FORGED_GOLD, Align.CENTER);
        g.fill(x + 4, y + 15, x + w - 4, y + 16, AlkeariColors.DEEP_GRAPHITE);
        int cw = (int) (w * 0.22);
        g.fill(x + w / 2 - cw / 2, y + 15, x + w / 2 + cw / 2, y + 16, AlkeariColors.WARM_GOLD_SHADOW);
    }

    private static void drawHeader(GuiGraphics g, Font font, int x, int y, int w, ItemStack icon) {
        int sx = x + 6;
        int sy = y + 20;
        g.fill(sx, sy, sx + 18, sy + 18, AlkeariColors.SLOT_FILL);
        border(g, sx, sy, sx + 18, sy + 18, AlkeariColors.SLOT_BORDER);
        g.fill(sx + 1, sy + 16, sx + 17, sy + 17, AlkeariColors.SLOT_BEVEL);

        text(g, font, trim(icon.getHoverName().getString(), 24), x + 28, y + 21, 1.0f,
                AlkeariColors.SOFT_CHAMPAGNE, Align.LEFT);
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(icon.getItem());
        text(g, font, id.toString(), x + 28, y + 32, 0.5f, AlkeariColors.STEEL_GRAY, Align.LEFT);

        int bw = 44;
        int bx = x + w - bw - 4;
        int by = y + 20;
        g.fill(bx, by, bx + bw, by + 12, AlkeariColors.PILL_FILL);
        border(g, bx, by, bx + bw, by + 12, AlkeariColors.WARM_GOLD_SHADOW);
        text(g, font, dimensionName(currentDimension()), bx + bw / 2, by + 3, 0.5f,
                AlkeariColors.FORGED_GOLD, Align.CENTER);
    }

    private static void drawSelector(GuiGraphics g, Font font, int x, int y, int w, BiomeOption option, int count) {
        int px = x + 24;
        int py = y + 40;
        int pw = w - 48;
        int ph = 14;
        g.fill(px, py, px + pw, py + ph, AlkeariColors.PILL_FILL);
        border(g, px, py, px + pw, py + ph,
                count > 1 ? AlkeariColors.WARM_GOLD_SHADOW : AlkeariColors.PILL_BORDER_INACTIVE);
        int nameColor = count > 1 ? AlkeariColors.FORGED_GOLD : AlkeariColors.STEEL_GRAY;
        text(g, font, trim(option.label().getString(), 28), px + pw / 2, py + 3, 0.5f, nameColor, Align.CENTER);

        int arrowColor = count > 1 ? AlkeariColors.FORGED_GOLD : AlkeariColors.ARROW_DISABLED;
        text(g, font, "<", x + 13, py + 3, 1.0f, arrowColor, Align.CENTER);
        text(g, font, ">", x + w - 13, py + 3, 1.0f, arrowColor, Align.CENTER);
    }

    private static void drawLowConfidenceChip(GuiGraphics g, Font font, int x, int y, int w, int chunks) {
        int cw = 120;
        int cx = x + w / 2 - cw / 2;
        int cy = y + 58;
        int ch = 11;
        g.fill(cx, cy, cx + cw, cy + ch, AlkeariColors.AMBER_CHIP_FILL);
        border(g, cx, cy, cx + cw, cy + ch, AlkeariColors.ALERT_AMBER);
        int tx = cx + 5;
        g.fill(tx, cy + 7, tx + 5, cy + 8, AlkeariColors.ALERT_AMBER);
        g.fill(tx + 1, cy + 4, tx + 4, cy + 7, AlkeariColors.ALERT_AMBER);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.low_conf_chip", chunks).getString(),
                cx + cw / 2 + 4, cy + 3, 0.5f, AlkeariColors.ALERT_AMBER, Align.CENTER);
    }

    private static void drawSubline(GuiGraphics g, Font font, int x, int y, int w, BiomeOption option, int biomeCount) {
        String s;
        if (option.biomeId().equals(DimensionScanData.AGGREGATE_BIOME)) {
            s = Component.translatable("gui.ore_distribution_scanner.subline_aggregate",
                    String.format("%,d", chunks(option.sampleColumns())), biomeCount).getString();
        } else {
            s = Component.translatable("gui.ore_distribution_scanner.subline_biome",
                    String.format("%,d", chunks(option.sampleColumns()))).getString();
        }
        text(g, font, s, x + w / 2, y + 72, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);
    }

    // -- plot (horizontal depth) ----------------------------------------------------------------

    private static void drawPlot(GuiGraphics g, Font font, int x, int y, int w, int h, DimensionScanData data, BiomeOption option) {
        int leftX = x + FREQ_AXIS_X;
        int rightX = x + w - RIGHT_MARGIN;
        int plotTop = y + PLOT_TOP;
        int baseline = y + h - DEPTH_AXIS_BOTTOM;
        int plotW = rightX - leftX;
        int freqH = baseline - plotTop;
        int dimMinY = data.getMinY();

        int[] counts = option.counts();
        int[] range = plotRange(counts, dimMinY);
        int rangeMinY = range[0];
        int rangeMaxY = range[1];
        int max = range[2];

        for (int worldY : axisLabels(rangeMinY, rangeMaxY)) {
            int px = pixelX(worldY, rangeMinY, rangeMaxY, leftX, rightX);
            g.fill(px, plotTop, px + 1, baseline, AlkeariColors.GRIDLINE);
            g.fill(px, baseline, px + 1, baseline + 3, AlkeariColors.STEEL_GRAY);
            text(g, font, Integer.toString(worldY), px, baseline + 4, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);
        }
        g.fill(leftX, baseline, rightX, baseline + 1, AlkeariColors.STEEL_GRAY);
        g.fill(leftX, plotTop, leftX + 1, baseline, AlkeariColors.STEEL_GRAY);
        text(g, font, "0", leftX - 2, baseline - 3, 0.5f, AlkeariColors.STEEL_GRAY, Align.RIGHT);

        if (max == 0) {
            return;
        }
        text(g, font, String.format("%.2f", (double) max * COLUMNS_PER_CHUNK / option.sampleColumns()),
                leftX - 2, plotTop - 1, 0.5f, AlkeariColors.STEEL_GRAY, Align.RIGHT);

        if (option.lowConfidence()) {
            drawDiscreteBars(g, counts, dimMinY, rangeMinY, rangeMaxY, max, leftX, baseline, freqH, plotW);
            border(g, leftX, plotTop, rightX, baseline, AlkeariColors.LOW_CONF_BORDER);
        } else {
            int[] colMax = new int[plotW + 1];
            for (int wy = rangeMinY; wy <= rangeMaxY; wy++) {
                int i = wy - dimMinY;
                if (i < 0 || i >= counts.length) {
                    continue;
                }
                int col = rangeMaxY == rangeMinY ? 0
                        : (int) Math.round((double) (wy - rangeMinY) / (rangeMaxY - rangeMinY) * plotW);
                colMax[col] = Math.max(colMax[col], counts[i]);
            }
            for (int col = 0; col <= plotW; col++) {
                if (colMax[col] <= 0) {
                    continue;
                }
                int barH = Math.max(1, Math.round((float) colMax[col] / max * freqH));
                int px = leftX + col;
                g.fill(px, baseline - barH, px + 1, baseline, AlkeariColors.FORGED_GOLD);
                g.fill(px, baseline - barH, px + 1, baseline - barH + 1, AlkeariColors.WARM_GOLD_SHADOW);
            }
        }
    }

    private static void drawDiscreteBars(GuiGraphics g, int[] counts, int dimMinY, int rangeMinY, int rangeMaxY, int max,
                                         int leftX, int baseline, int freqH, int plotW) {
        int barW = 4;
        int step = 6;
        int bars = Math.max(1, plotW / step);
        int rangeLen = rangeMaxY - rangeMinY + 1;
        int perBar = Math.max(1, (int) Math.ceil((double) rangeLen / bars));
        for (int b = 0; b < bars; b++) {
            int yLo = rangeMinY + b * perBar;
            if (yLo > rangeMaxY) {
                break;
            }
            int yHi = Math.min(rangeMaxY, yLo + perBar - 1);
            int bandMax = 0;
            for (int wy = yLo; wy <= yHi; wy++) {
                int i = wy - dimMinY;
                if (i >= 0 && i < counts.length) {
                    bandMax = Math.max(bandMax, counts[i]);
                }
            }
            if (bandMax <= 0) {
                continue;
            }
            int barH = Math.max(1, Math.round((float) bandMax / max * freqH));
            int px = leftX + 1 + b * step;
            g.fill(px, baseline - barH, px + barW, baseline, AlkeariColors.FORGED_GOLD);
        }
    }

    private static void drawHover(GuiGraphics g, Font font, int x, int y, int w, int h, DimensionScanData data,
                                  BiomeOption option, double mouseX, double mouseY) {
        int leftX = x + FREQ_AXIS_X;
        int rightX = x + w - RIGHT_MARGIN;
        int plotTop = y + PLOT_TOP;
        int baseline = y + h - DEPTH_AXIS_BOTTOM;
        int plotW = rightX - leftX;
        int freqH = baseline - plotTop;
        if (mouseX < leftX || mouseX > rightX || mouseY < plotTop || mouseY > baseline) {
            return;
        }

        int dimMinY = data.getMinY();
        int[] range = plotRange(option.counts(), dimMinY);
        int rangeMinY = range[0];
        int rangeMaxY = range[1];
        int max = range[2];
        double fraction = (double) (mouseX - leftX) / plotW;
        int hoveredY = (int) Math.round(rangeMinY + fraction * (rangeMaxY - rangeMinY));
        int worldY = snapToData(option.counts(), dimMinY, rangeMinY, rangeMaxY, hoveredY, plotW);

        int idx = worldY - dimMinY;
        int raw = (idx >= 0 && idx < option.counts().length) ? option.counts()[idx] : 0;
        int barH = max == 0 ? 0 : Math.round((float) raw / max * freqH);
        int px = pixelX(worldY, rangeMinY, rangeMaxY, leftX, rightX);
        int markerY = baseline - barH;
        boolean low = option.lowConfidence();

        g.fill(px, markerY, px + 1, baseline, AlkeariColors.HOVER_GUIDE);
        g.fill(px - 1, markerY - 1, px + 2, markerY + 2, low ? AlkeariColors.ALERT_AMBER : AlkeariColors.FORGED_GOLD);
        g.fill(px, markerY, px + 1, markerY + 1, AlkeariColors.SOFT_CHAMPAGNE);

        int cw = 124;
        int ch = low ? 44 : 36;
        int cx = px < x + w / 2 ? rightX - cw - 2 : leftX + 2;
        cx = Math.max(x + 4, Math.min(cx, x + w - cw - 4));
        int cy = plotTop + 2;
        g.fill(cx, cy, cx + cw, cy + ch, AlkeariColors.TOOLTIP_FILL);
        border(g, cx, cy, cx + cw, cy + ch, low ? AlkeariColors.ALERT_AMBER : AlkeariColors.FORGED_GOLD);

        double avg = option.sampleColumns() > 0 ? (double) raw * COLUMNS_PER_CHUNK / option.sampleColumns() : 0.0;
        int tx = cx + 5;
        text(g, font, Component.translatable("gui.ore_distribution_scanner.readout_y", worldY).getString(),
                tx, cy + 4, 1.0f, AlkeariColors.FORGED_GOLD, Align.LEFT);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.readout_avg",
                String.format("%.2f", avg)).getString(), tx, cy + 16, 0.5f, AlkeariColors.SOFT_CHAMPAGNE, Align.LEFT);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.readout_sub",
                        String.format("%,d", raw), String.format("%,d", chunks(option.sampleColumns()))).getString(),
                tx, cy + 25, 0.5f, AlkeariColors.STEEL_GRAY, Align.LEFT);
        if (low) {
            text(g, font, Component.translatable("gui.ore_distribution_scanner.readout_low").getString(),
                    tx, cy + 34, 0.5f, AlkeariColors.ALERT_AMBER, Align.LEFT);
        }
    }

    /**
     * Snaps the hovered Y to the nearest Y that has data, within the band of Y levels the hovered
     * pixel covers, so every data-bearing Y is readable even when the plot is narrower than the range.
     */
    static int snapToData(int[] counts, int dimMinY, int rangeMinY, int rangeMaxY, int hoveredY, int pixelSpan) {
        int yPerPixel = Math.max(1, (int) Math.ceil((double) (rangeMaxY - rangeMinY) / Math.max(1, pixelSpan)));
        int bestY = hoveredY;
        int bestDist = Integer.MAX_VALUE;
        for (int dy = -yPerPixel; dy <= yPerPixel; dy++) {
            int wy = hoveredY + dy;
            int i = wy - dimMinY;
            if (i < 0 || i >= counts.length || counts[i] <= 0) {
                continue;
            }
            int dist = Math.abs(dy);
            if (dist < bestDist) {
                bestDist = dist;
                bestY = wy;
            }
        }
        return bestY;
    }

    private static void drawEmptyState(GuiGraphics g, Font font, int x, int y, int w, int h, ResourceLocation dimension) {
        int leftX = x + FREQ_AXIS_X;
        int rightX = x + w - RIGHT_MARGIN;
        int plotTop = y + PLOT_TOP;
        int baseline = y + h - DEPTH_AXIS_BOTTOM;
        g.fill(leftX, baseline, rightX, baseline + 1, AlkeariColors.EMPTY_AXIS);
        g.fill(leftX, plotTop, leftX + 1, baseline, AlkeariColors.EMPTY_AXIS);
        for (int worldY : new int[]{-64, 0, 128, 320}) {
            int px = pixelX(worldY, -64, 320, leftX, rightX);
            text(g, font, Integer.toString(worldY), px, baseline + 4, 0.5f, AlkeariColors.EMPTY_AXIS_LABEL, Align.CENTER);
        }

        int px = x + 24;
        int py = y + 40;
        g.fill(px, py, px + w - 48, py + 14, AlkeariColors.PILL_FILL);
        border(g, px, py, px + w - 48, py + 14, AlkeariColors.PILL_BORDER_INACTIVE);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.all_biomes").getString(),
                px + (w - 48) / 2, py + 3, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);
        text(g, font, "<", x + 13, py + 3, 1.0f, AlkeariColors.ARROW_DISABLED, Align.CENTER);
        text(g, font, ">", x + w - 13, py + 3, 1.0f, AlkeariColors.ARROW_DISABLED, Align.CENTER);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.no_data_yet").getString(),
                x + w / 2, y + 72, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);

        int cx = x + w / 2;
        int cy = (plotTop + baseline) / 2 - 10;
        int rad = 11;
        ringOutline(g, cx, cy, rad, AlkeariColors.EMPTY_SCAN_GLYPH);
        g.fill(cx - rad + 2, cy, cx + rad - 2, cy + 1, AlkeariColors.EMPTY_SCAN_GLYPH);
        for (int i = 1; i <= rad - 2; i++) {
            g.fill(cx + rad + i - 2, cy + i - 1, cx + rad + i, cy + i + 1, AlkeariColors.EMPTY_SCAN_GLYPH);
        }
        text(g, font, Component.translatable("gui.ore_distribution_scanner.placeholder").getString(),
                cx, cy + rad + 4, 1.0f, AlkeariColors.FORGED_GOLD, Align.CENTER);
        text(g, font, Component.translatable("gui.ore_distribution_scanner.empty_subtext",
                dimensionName(dimension)).getString(), cx, cy + rad + 15, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);

        String chip = "/ods scan";
        int chipW = (int) (font.width(chip) * 0.5f) + 12;
        int chipX = cx - chipW / 2;
        int chipY = cy + rad + 26;
        g.fill(chipX, chipY, chipX + chipW, chipY + 12, AlkeariColors.PILL_FILL);
        border(g, chipX, chipY, chipX + chipW, chipY + 12, AlkeariColors.WARM_GOLD_SHADOW);
        text(g, font, chip, chipX + chipW / 2, chipY + 3, 0.5f, AlkeariColors.FORGED_GOLD, Align.CENTER);

        text(g, font, Component.translatable("gui.ore_distribution_scanner.footer_empty").getString(),
                x + w / 2, y + h - 10, 0.5f, AlkeariColors.STEEL_GRAY, Align.CENTER);
    }

    // -- data + math helpers --------------------------------------------------------------------

    private static int chunks(int sampledColumns) {
        return Math.round((float) sampledColumns / COLUMNS_PER_CHUNK);
    }

    private static DimensionScanData currentData() {
        ResourceLocation dim = currentDimension();
        return dim == null ? null : ClientScanHolder.get().get(dim);
    }

    private static ResourceLocation currentDimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : mc.level.dimension().location();
    }

    static List<BiomeOption> buildOptions(DimensionScanData data, ResourceLocation ore) {
        int threshold = data.getLowConfidenceThreshold();
        List<BiomeOption> options = new ArrayList<>();
        BiomeScanData aggregate = data.aggregateBiome();
        options.add(new BiomeOption(DimensionScanData.AGGREGATE_BIOME,
                Component.translatable("gui.ore_distribution_scanner.all_biomes"),
                aggregate.getSampledColumns(), isLowConfidence(aggregate.getSampledColumns(), threshold),
                countsFor(aggregate, ore, data.getHeight())));

        data.getBiomes().entrySet().stream()
                .sorted(Comparator.comparingInt((java.util.Map.Entry<ResourceLocation, BiomeScanData> e) ->
                        e.getValue().getSampledColumns()).reversed())
                .forEach(e -> {
                    BiomeScanData biome = e.getValue();
                    options.add(new BiomeOption(e.getKey(), biomeLabel(e.getKey()),
                            biome.getSampledColumns(), isLowConfidence(biome.getSampledColumns(), threshold),
                            countsFor(biome, ore, data.getHeight())));
                });
        return options;
    }

    static boolean isLowConfidence(int sampledColumns, int thresholdChunks) {
        return sampledColumns < thresholdChunks * COLUMNS_PER_CHUNK;
    }

    private static int[] countsFor(BiomeScanData biome, ResourceLocation ore, int height) {
        int[] counts = biome.getOreCounts(ore);
        return counts != null ? counts : new int[height];
    }

    /**
     * The Y range to plot: from the lowest to the highest Y where this ore was found, so the empty
     * space outside the ore's generation band is condensed away. Returns {rangeMinY, rangeMaxY,
     * maxCount}; falls back to the full dimension range when no data.
     */
    static int[] plotRange(int[] counts, int dimMinY) {
        int max = 0;
        int lo = -1;
        int hi = -1;
        for (int i = 0; i < counts.length; i++) {
            int c = counts[i];
            if (c > max) {
                max = c;
            }
            if (c > 0) {
                if (lo < 0) {
                    lo = i;
                }
                hi = i;
            }
        }
        if (lo < 0) {
            return new int[]{dimMinY, dimMinY + counts.length - 1, 0};
        }
        return new int[]{dimMinY + lo, dimMinY + hi, max};
    }

    static int[] axisLabels(int minY, int maxY) {
        java.util.TreeSet<Integer> set = new java.util.TreeSet<>();
        int divisions = 8;
        for (int i = 0; i <= divisions; i++) {
            int v = (int) Math.round(minY + (double) i / divisions * (maxY - minY));
            set.add(Math.max(minY, Math.min(maxY, Math.round(v / 16f) * 16)));
        }
        set.add(minY);
        set.add(maxY);
        if (minY <= 0 && maxY >= 0) {
            set.add(0);
        }
        int[] out = new int[set.size()];
        int i = 0;
        for (int v : set) {
            out[i++] = v;
        }
        return out;
    }

    private static int pixelX(int worldY, int minY, int maxY, int leftX, int rightX) {
        if (maxY == minY) {
            return leftX;
        }
        return leftX + (int) Math.round((double) (worldY - minY) / (maxY - minY) * (rightX - leftX));
    }

    private static Component biomeLabel(ResourceLocation biome) {
        return Component.translatable("biome." + biome.getNamespace() + "." + biome.getPath());
    }

    private static String dimensionName(ResourceLocation dimension) {
        if (dimension == null) {
            return "";
        }
        String path = dimension.getPath().replace('_', ' ');
        return path.isEmpty() ? path : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    // -- drawing primitives ---------------------------------------------------------------------

    private enum Align {LEFT, CENTER, RIGHT}

    private static void text(GuiGraphics g, Font font, String s, int anchorX, int topY, float scale, int color, Align align) {
        int wpx = (int) (font.width(s) * scale);
        int dx = align == Align.CENTER ? -wpx / 2 : align == Align.RIGHT ? -wpx : 0;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(anchorX + dx, topY, 0);
        pose.scale(scale, scale, 1f);
        g.drawString(font, s, 0, 0, color, false);
        pose.popPose();
    }

    private static void border(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private static void ringOutline(GuiGraphics g, int cx, int cy, int radius, int color) {
        int segments = 36;
        for (int i = 0; i < segments; i++) {
            double a = 2 * Math.PI * i / segments;
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }
}
