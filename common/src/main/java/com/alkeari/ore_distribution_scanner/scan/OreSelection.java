package com.alkeari.ore_distribution_scanner.scan;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the set of block ids counted as ores: a vanilla baseline unioned with the ore tags, plus
 * the configured whitelist, minus the configured blacklist.
 *
 * <p>The ore tag situation is loader-specific on 1.20.1: {@code forge:ores} only populates on Forge,
 * and Fabric does not auto-tag vanilla ores into {@code c:ores}. To behave identically on both
 * loaders we union both tags and seed a vanilla ore baseline, so modded ores still come from the
 * conventional tags while vanilla ores are always covered. On 1.21.1 the convention is {@code c:ores}.
 */
public final class OreSelection {
    private static final TagKey<Block> FORGE_ORES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));
    private static final TagKey<Block> COMMON_ORES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    private static final List<String> VANILLA_ORES = List.of(
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris");

    private final Set<ResourceLocation> oreIds;
    private final Set<Block> oreBlocks;
    private final Map<Block, ResourceLocation> idByBlock;

    private OreSelection(Set<ResourceLocation> oreIds, Map<Block, ResourceLocation> idByBlock) {
        this.oreIds = oreIds;
        this.idByBlock = idByBlock;
        this.oreBlocks = idByBlock.keySet();
    }

    public boolean isOre(ResourceLocation blockId) {
        return oreIds.contains(blockId);
    }

    public boolean isOre(Block block) {
        return oreBlocks.contains(block);
    }

    public ResourceLocation idOf(Block block) {
        return idByBlock.get(block);
    }

    public Set<ResourceLocation> oreIds() {
        return Set.copyOf(oreIds);
    }

    public int size() {
        return oreIds.size();
    }

    public static OreSelection resolve(RegistryAccess access, List<String> whitelist, List<String> blacklist) {
        Registry<Block> blocks = access.registryOrThrow(Registries.BLOCK);
        Set<ResourceLocation> ids = new HashSet<>();

        for (String entry : VANILLA_ORES) {
            ResourceLocation id = ResourceLocation.parse(entry);
            if (blocks.containsKey(id)) {
                ids.add(id);
            }
        }
        addTag(blocks, FORGE_ORES, ids);
        addTag(blocks, COMMON_ORES, ids);

        for (String entry : whitelist) {
            ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id != null && blocks.containsKey(id)) {
                ids.add(id);
            }
        }
        for (String entry : blacklist) {
            ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id != null) {
                ids.remove(id);
            }
        }

        Map<Block, ResourceLocation> idByBlock = new HashMap<>();
        for (ResourceLocation id : ids) {
            blocks.getOptional(id).ifPresent(block -> idByBlock.put(block, id));
        }
        return new OreSelection(ids, idByBlock);
    }

    private static void addTag(Registry<Block> blocks, TagKey<Block> tag, Set<ResourceLocation> ids) {
        blocks.getTag(tag).ifPresent(holders ->
                holders.forEach(holder -> holder.unwrapKey().ifPresent(key -> ids.add(key.location()))));
    }
}
