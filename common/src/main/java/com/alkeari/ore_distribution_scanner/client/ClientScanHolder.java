package com.alkeari.ore_distribution_scanner.client;

import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mutable singleton holding the latest scan data per dimension. The viewer draw code
 * (later phases) reads live from this holder every frame, so a completed scan or an on-join push
 * shows up the next time an ore entry is opened, with no viewer reload. Only client code touches it.
 */
public final class ClientScanHolder {
    private static final ClientScanHolder INSTANCE = new ClientScanHolder();

    public static ClientScanHolder get() {
        return INSTANCE;
    }

    private ClientScanHolder() {
    }

    private final Map<ResourceLocation, DimensionScanData> byDimension = new ConcurrentHashMap<>();
    private volatile int revision;

    public void update(DimensionScanData data) {
        byDimension.put(data.getDimension(), data);
        revision++;
    }

    public DimensionScanData get(ResourceLocation dimension) {
        return byDimension.get(dimension);
    }

    public Collection<DimensionScanData> all() {
        return byDimension.values();
    }

    public int revision() {
        return revision;
    }

    public void clear() {
        byDimension.clear();
        revision++;
    }
}
