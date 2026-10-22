package com.alkeari.ore_distribution_scanner.client;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Client-side holder for the server's resolved ore-id set (the ore tags plus whitelist, minus
 * blacklist). Viewer adapters build one display entry per id here, so whitelisted ores appear and
 * blacklisted ones do not. Populated by an S2C push on join; empty until then, in which case viewers
 * fall back to a local resolution.
 */
public final class ClientOreList {
    private static final ClientOreList INSTANCE = new ClientOreList();

    public static ClientOreList get() {
        return INSTANCE;
    }

    private ClientOreList() {
    }

    private volatile List<ResourceLocation> oreIds = List.of();

    public void update(List<ResourceLocation> ids) {
        this.oreIds = List.copyOf(ids);
    }

    public List<ResourceLocation> oreIds() {
        return oreIds;
    }

    public boolean isEmpty() {
        return oreIds.isEmpty();
    }

    public void clear() {
        this.oreIds = List.of();
    }
}
