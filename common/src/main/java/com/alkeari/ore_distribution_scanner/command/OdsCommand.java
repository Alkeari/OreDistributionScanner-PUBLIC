package com.alkeari.ore_distribution_scanner.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.alkeari.ore_distribution_scanner.config.ModConfig;
import com.alkeari.ore_distribution_scanner.config.ModConfigDefaults;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import com.alkeari.ore_distribution_scanner.data.ScanStorage;
import com.alkeari.ore_distribution_scanner.scan.OreSelection;
import com.alkeari.ore_distribution_scanner.scan.ScanManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class OdsCommand {
    private OdsCommand() {
    }

    private record DimResult(List<ServerLevel> levels, List<String> failed) {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ods")
                .executes(ctx -> usage(ctx.getSource()))
                .then(Commands.literal("scan")
                        .requires(OdsCommand::canScan)
                        .executes(ctx -> startScan(ctx.getSource(), List.of(ctx.getSource().getLevel()), defaultRadius()))
                        .then(Commands.argument("dimensions", StringArgumentType.greedyString())
                                .suggests(DIMENSIONS_OR_ALL)
                                .executes(OdsCommand::scanGreedy)))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("cancel")
                        .requires(OdsCommand::canScan)
                        .executes(ctx -> cancel(ctx.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("ore", ResourceLocationArgument.id()).suggests(SCANNED_ORES)
                                .executes(ctx -> info(ctx, false, false))
                                .then(Commands.argument("dimension", ResourceLocationArgument.id()).suggests(SCANNED_DIMENSIONS)
                                        .executes(ctx -> info(ctx, true, false))
                                        .then(Commands.argument("biome", ResourceLocationArgument.id()).suggests(BIOMES_WITH_DATA)
                                                .executes(ctx -> info(ctx, true, true))))))
                .then(Commands.literal("list")
                        .executes(ctx -> listDimensions(ctx.getSource()))
                        .then(Commands.argument("dimension", ResourceLocationArgument.id()).suggests(SCANNED_DIMENSIONS)
                                .executes(OdsCommand::listOres)))
                .then(Commands.literal("clear")
                        .requires(OdsCommand::canScan)
                        .then(Commands.literal("all")
                                .executes(ctx -> clearAll(ctx.getSource(), false))
                                .then(Commands.literal("confirm").executes(ctx -> clearAll(ctx.getSource(), true))))
                        .then(Commands.argument("dimension", ResourceLocationArgument.id()).suggests(SCANNED_DIMENSIONS)
                                .executes(OdsCommand::clearOne)))
                .then(Commands.literal("reload")
                        .requires(OdsCommand::canScan)
                        .executes(ctx -> reload(ctx.getSource()))));
    }

    private static boolean canScan(CommandSourceStack source) {
        return source.hasPermission(ModConfig.getScanPermissionLevel());
    }

    private static int defaultRadius() {
        return ModConfig.getScanRadiusChunks();
    }

    // -- scanning -------------------------------------------------------------------------------

    private static int scanGreedy(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String raw = StringArgumentType.getString(ctx, "dimensions").trim();
        int radius = defaultRadius();
        String dimsPart = raw;
        int lastSpace = raw.lastIndexOf(' ');
        if (lastSpace >= 0) {
            String tail = raw.substring(lastSpace + 1).trim();
            try {
                radius = clampRadius(source, Integer.parseInt(tail));
                dimsPart = raw.substring(0, lastSpace).trim();
            } catch (NumberFormatException ignored) {
                // No trailing radius; the whole string is the dimension list.
            }
        }
        DimResult result = parseDimensions(source.getServer(), dimsPart);
        if (!result.failed().isEmpty()) {
            source.sendFailure(Component.literal(
                    "Unknown or unloaded dimensions: " + String.join(", ", result.failed())));
        }
        return startScan(source, result.levels(), radius);
    }

    private static DimResult parseDimensions(MinecraftServer server, String raw) {
        List<ServerLevel> levels = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        if (raw.trim().equalsIgnoreCase("all")) {
            for (ServerLevel level : server.getAllLevels()) {
                levels.add(level);
            }
            return new DimResult(levels, failed);
        }
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (id.isEmpty()) {
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ServerLevel level = rl == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
            if (level == null) {
                failed.add(id);
            } else if (seen.add(rl)) {
                levels.add(level);
            }
        }
        return new DimResult(levels, failed);
    }

    private static int clampRadius(CommandSourceStack source, int requested) {
        int max = ModConfigDefaults.MAX_SCAN_RADIUS_CHUNKS;
        int clamped = Math.max(1, Math.min(requested, max));
        if (clamped != requested) {
            source.sendSuccess(() -> Component.literal(
                    "Radius " + requested + " clamped to " + max + " chunks."), false);
        }
        return clamped;
    }

    private static int startScan(CommandSourceStack source, List<ServerLevel> levels, int radius) {
        MinecraftServer server = source.getServer();
        ScanManager manager = ScanManager.get();
        if (manager.isBusy()) {
            source.sendFailure(Component.literal("A scan is already running. Use /ods status or /ods cancel."));
            return 0;
        }
        if (levels.isEmpty()) {
            source.sendFailure(Component.literal("No valid dimensions to scan."));
            return 0;
        }
        OreSelection ores = OreSelection.resolve(server.registryAccess(),
                ModConfig.getOreWhitelist(), ModConfig.getOreBlacklist());
        if (ores.size() == 0) {
            source.sendFailure(Component.literal(
                    "No ores resolved to scan. Check the ore tag and the config whitelist/blacklist."));
            return 0;
        }
        boolean started = manager.startBatch(server, levels, source, ores, radius,
                ModConfig.getBiomeClusterSize(), ModConfig.getChunkBudget(), ModConfig.getProgressUpdateIntervalSeconds());
        if (!started) {
            source.sendFailure(Component.literal("Could not start the scan. A scan may already be running."));
            return 0;
        }
        int oreCount = ores.size();
        if (levels.size() == 1) {
            ResourceLocation dim = levels.get(0).dimension().location();
            source.sendSuccess(() -> Component.literal("Ore Distribution Scanner: started scanning ")
                    .append(OdsReadout.gold(dim.toString()))
                    .append(Component.literal(" (radius " + radius + ", " + oreCount + " ores).")), true);
        } else {
            ResourceLocation first = levels.get(0).dimension().location();
            int count = levels.size();
            source.sendSuccess(() -> Component.literal("Ore Distribution Scanner: queued " + count
                    + " dimensions (radius " + radius + "). Starting ").append(OdsReadout.gold(first.toString()))
                    .append(Component.literal(".")), true);
        }
        return 1;
    }

    private static int status(CommandSourceStack source) {
        Optional<ScanManager.BatchStatus> status = ScanManager.get().currentStatus();
        if (status.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Ore Distribution Scanner: idle. No scan running."), false);
            return 1;
        }
        ScanManager.BatchStatus s = status.get();
        MutableComponent out = Component.literal("Ore Distribution Scanner: scanning ")
                .append(OdsReadout.gold(s.dimension().toString()));
        if (s.total() > 1) {
            out.append(Component.literal(", " + s.index() + " of " + s.total()));
        }
        out.append(Component.literal(", "))
                .append(OdsReadout.gold(s.percent() + " percent"))
                .append(Component.literal(" (" + s.chunksDone() + "/" + s.chunksTotal() + " chunks)."));
        source.sendSuccess(() -> out, false);
        return 1;
    }

    private static int cancel(CommandSourceStack source) {
        ScanManager.CancelInfo info = ScanManager.get().cancel();
        if (info == null) {
            source.sendFailure(Component.literal("No scan is currently running."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ore Distribution Scanner: cancelling. Completed "
                + info.completed().size() + ", skipped " + info.skipped() + "."), true);
        return 1;
    }

    // -- reading --------------------------------------------------------------------------------

    private static int info(CommandContext<CommandSourceStack> ctx, boolean hasDim, boolean hasBiome) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation ore = ResourceLocationArgument.getId(ctx, "ore");
        ResourceLocation dimension = hasDim
                ? ResourceLocationArgument.getId(ctx, "dimension")
                : source.getLevel().dimension().location();
        ResourceLocation biome = hasBiome ? ResourceLocationArgument.getId(ctx, "biome") : null;
        Optional<DimensionScanData> data = ScanManager.get().getResult(source.getServer(), dimension);
        if (data.isEmpty()) {
            source.sendFailure(Component.literal("No scan data for " + dimension + ". Run /ods scan."));
            return 0;
        }
        for (Component line : OdsReadout.oreInfo(data.get(), ore, biome)) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int listDimensions(CommandSourceStack source) {
        for (Component line : OdsReadout.listDimensions(source.getServer())) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int listOres(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation dimension = ResourceLocationArgument.getId(ctx, "dimension");
        Optional<DimensionScanData> data = ScanManager.get().getResult(source.getServer(), dimension);
        if (data.isEmpty()) {
            source.sendFailure(Component.literal("No scan data for " + dimension + "."));
            return 0;
        }
        for (Component line : OdsReadout.listOres(data.get())) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    // -- managing -------------------------------------------------------------------------------

    private static int clearAll(CommandSourceStack source, boolean confirmed) {
        MinecraftServer server = source.getServer();
        if (ScanManager.get().isBusy()) {
            source.sendFailure(Component.literal("Cannot clear data while a scan is running. Cancel it first."));
            return 0;
        }
        if (!confirmed) {
            int count = ScanStorage.scannedDimensions(server).size();
            source.sendFailure(Component.literal("This will delete saved data for " + count
                    + " dimensions. Run /ods clear all confirm to proceed."));
            return 0;
        }
        int deleted = ScanStorage.deleteAll(server);
        ScanManager.get().clearResults();
        source.sendSuccess(() -> Component.literal("Ore Distribution Scanner: cleared " + deleted + " dimensions."), true);
        return 1;
    }

    private static int clearOne(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation dimension = ResourceLocationArgument.getId(ctx, "dimension");
        if (ScanManager.get().isScanning(dimension)) {
            source.sendFailure(Component.literal("Cannot clear " + dimension + " while it is being scanned."));
            return 0;
        }
        boolean deleted = ScanStorage.delete(source.getServer(), dimension);
        ScanManager.get().dropResult(dimension);
        if (deleted) {
            source.sendSuccess(() -> Component.literal("Ore Distribution Scanner: cleared " + dimension + "."), true);
            return 1;
        }
        source.sendFailure(Component.literal("No saved data for " + dimension + "."));
        return 0;
    }

    // -- admin ----------------------------------------------------------------------------------

    private static int reload(CommandSourceStack source) {
        ModConfig.reload();
        source.sendSuccess(() -> Component.literal(
                "Ore Distribution Scanner: config reloaded. Scan-parameter changes apply on the next scan."), true);
        source.sendSuccess(() -> Component.literal(
                "Ore whitelist/blacklist changes still need a rejoin (singleplayer) or reconnect (multiplayer) to apply."), false);
        return 1;
    }

    private static int usage(CommandSourceStack source) {
        boolean op = canScan(source);
        List<String> lines = new ArrayList<>();
        lines.add("Ore Distribution Scanner commands:");
        if (op) {
            lines.add("/ods scan [dimensions] [radius] - scan one or more dimensions, or all");
        }
        lines.add("/ods status - show scan progress");
        if (op) {
            lines.add("/ods cancel - cancel the current scan and queue");
        }
        lines.add("/ods info <ore> [dimension] [biome] - ore distribution summary");
        lines.add("/ods list [dimension] - list saved scans, or ores in a dimension");
        if (op) {
            lines.add("/ods clear <dimension|all> - delete saved data");
            lines.add("/ods reload - reload the config file");
        }
        source.sendSuccess(() -> Component.literal(String.join("\n", lines)), false);
        return 1;
    }

    // -- suggestions ----------------------------------------------------------------------------

    private static final SuggestionProvider<CommandSourceStack> DIMENSIONS_OR_ALL = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        String remaining = builder.getRemaining();
        int comma = remaining.lastIndexOf(',');
        String segment = remaining.substring(comma + 1).toLowerCase(Locale.ROOT);
        var b = builder.createOffset(builder.getStart() + comma + 1);
        for (ResourceKey<Level> key : server.levelKeys()) {
            String id = key.location().toString();
            if (id.toLowerCase(Locale.ROOT).startsWith(segment)) {
                b.suggest(id);
            }
        }
        if (comma < 0 && "all".startsWith(segment)) {
            b.suggest("all");
        }
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SCANNED_DIMENSIONS = (ctx, builder) -> {
        for (ResourceLocation id : ScanStorage.scannedDimensions(ctx.getSource().getServer())) {
            builder.suggest(id.toString());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SCANNED_ORES = (ctx, builder) -> {
        Set<String> ores = new LinkedHashSet<>();
        for (DimensionScanData data : ScanStorage.loadAll(ctx.getSource().getServer())) {
            data.aggregateBiome().snapshotOreCounts().keySet().forEach(id -> ores.add(id.toString()));
        }
        ores.forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> BIOMES_WITH_DATA = (ctx, builder) -> {
        ResourceLocation ore = ResourceLocationArgument.getId(ctx, "ore");
        ResourceLocation dimension = ResourceLocationArgument.getId(ctx, "dimension");
        ScanManager.get().getResult(ctx.getSource().getServer(), dimension).ifPresent(data ->
                data.getBiomes().forEach((biome, biomeData) -> {
                    if (biomeData.getOreCounts(ore) != null) {
                        builder.suggest(biome.toString());
                    }
                }));
        return builder.buildFuture();
    };
}
