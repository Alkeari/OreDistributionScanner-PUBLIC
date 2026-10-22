package com.alkeari.ore_distribution_scanner.scan;

import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.data.DimensionScanData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Path B of the hybrid chunk acquisition: generated-but-unloaded chunks read directly off the
 * worker thread from the dimension's region files, without routing through the server's IO worker.
 * This is the most version-sensitive piece: it parses the saved chunk NBT, including the per-section
 * {@code block_states} and {@code biomes} paletted containers and their bit-packed index arrays.
 */
public final class RegionChunkReader {
    private RegionChunkReader() {
    }

    private static int columnIndex(int localX, int localZ) {
        return (localZ << 4) | localX;
    }

    /**
     * Reads and tallies one generated chunk from its region file. Returns false if the region file
     * or chunk is absent, or the chunk has not finished generating (it is then left uncounted,
     * surfacing as low coverage rather than fabricated data).
     */
    public static boolean readAndTally(Path regionDir, ChunkPos pos, int minY, int repY,
                                       OreSelection ores, DimensionScanData data) {
        Path regionFile = regionDir.resolve("r." + (pos.x >> 5) + "." + (pos.z >> 5) + ".mca");
        if (!Files.isRegularFile(regionFile)) {
            return false;
        }

        CompoundTag root;
        try (RegionFile region = new RegionFile(regionFile, regionDir, false)) {
            if (!region.hasChunk(pos)) {
                return false;
            }
            try (DataInputStream in = region.getChunkDataInputStream(pos)) {
                if (in == null) {
                    return false;
                }
                root = NbtIo.read(in);
            }
        } catch (Exception e) {
            OreDistributionScanner.LOGGER.debug("Failed to read chunk {} from {}: {}", pos, regionFile, e.toString());
            return false;
        }

        String status = root.getString("Status");
        if (!status.equals("minecraft:full") && !status.equals("full")) {
            return false;
        }

        ListTag sections = root.getList("sections", Tag.TAG_COMPOUND);
        if (sections.isEmpty()) {
            return false;
        }

        Set<ResourceLocation> oreIds = ores.oreIds();
        ResourceLocation[] columnBiomes = decodeColumnBiomes(sections, repY);
        ChunkAcquisition.recordSampledColumns(columnBiomes, data);

        for (int i = 0; i < sections.size(); i++) {
            tallySection(sections.getCompound(i), columnBiomes, minY, oreIds, data);
        }
        return true;
    }

    static void tallySection(CompoundTag section, ResourceLocation[] columnBiomes, int minY,
                             Set<ResourceLocation> oreIds, DimensionScanData data) {
        int sectionY = section.getByte("Y");
        CompoundTag blockStates = section.getCompound("block_states");
        ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty()) {
            return;
        }

        ResourceLocation[] paletteOre = new ResourceLocation[palette.size()];
        boolean anyOre = false;
        for (int p = 0; p < palette.size(); p++) {
            ResourceLocation id = ResourceLocation.tryParse(palette.getCompound(p).getString("Name"));
            if (id != null && oreIds.contains(id)) {
                paletteOre[p] = id;
                anyOre = true;
            }
        }
        if (!anyOre) {
            return;
        }

        int sectionBaseY = sectionY * 16;
        long[] dataArray = blockStates.getLongArray("data");
        if (palette.size() == 1) {
            ResourceLocation id = paletteOre[0];
            for (int cell = 0; cell < 4096; cell++) {
                bucket(cell, id, columnBiomes, sectionBaseY, minY, data);
            }
            return;
        }

        int bits = Math.max(4, ceilLog2(palette.size()));
        int entriesPerLong = 64 / bits;
        long mask = (1L << bits) - 1L;
        for (int cell = 0; cell < 4096; cell++) {
            int paletteIndex = unpack(dataArray, cell, bits, entriesPerLong, mask);
            ResourceLocation id = paletteOre[paletteIndex];
            if (id != null) {
                bucket(cell, id, columnBiomes, sectionBaseY, minY, data);
            }
        }
    }

    private static void bucket(int cell, ResourceLocation ore, ResourceLocation[] columnBiomes,
                               int sectionBaseY, int minY, DimensionScanData data) {
        int x = cell & 15;
        int z = (cell >> 4) & 15;
        int y = (cell >> 8) & 15;
        ResourceLocation biome = columnBiomes[columnIndex(x, z)];
        if (biome != null) {
            data.biome(biome).addOreFinds(ore, sectionBaseY + y - minY, 1);
        }
    }

    static ResourceLocation[] decodeColumnBiomes(ListTag sections, int repY) {
        ResourceLocation[] columnBiomes = new ResourceLocation[256];
        int repSectionY = Math.floorDiv(repY, 16);

        CompoundTag chosen = null;
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag section = sections.getCompound(i);
            if (section.getByte("Y") == repSectionY && section.contains("biomes")) {
                chosen = section;
                break;
            }
        }
        if (chosen == null) {
            for (int i = 0; i < sections.size(); i++) {
                if (sections.getCompound(i).contains("biomes")) {
                    chosen = sections.getCompound(i);
                    break;
                }
            }
        }
        if (chosen == null) {
            return columnBiomes;
        }

        CompoundTag biomes = chosen.getCompound("biomes");
        ListTag palette = biomes.getList("palette", Tag.TAG_STRING);
        if (palette.isEmpty()) {
            return columnBiomes;
        }

        int qy = Math.floorMod(repY, 16) >> 2;
        if (palette.size() == 1) {
            ResourceLocation biome = ResourceLocation.tryParse(palette.getString(0));
            for (int idx = 0; idx < columnBiomes.length; idx++) {
                columnBiomes[idx] = biome;
            }
            return columnBiomes;
        }

        long[] dataArray = biomes.getLongArray("data");
        int bits = Math.max(1, ceilLog2(palette.size()));
        int entriesPerLong = 64 / bits;
        long mask = (1L << bits) - 1L;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int qx = lx >> 2;
                int qz = lz >> 2;
                int biomeCell = (qy << 4) | (qz << 2) | qx;
                int paletteIndex = unpack(dataArray, biomeCell, bits, entriesPerLong, mask);
                columnBiomes[columnIndex(lx, lz)] = ResourceLocation.tryParse(palette.getString(paletteIndex));
            }
        }
        return columnBiomes;
    }

    private static int unpack(long[] data, int index, int bits, int entriesPerLong, long mask) {
        if (data.length == 0) {
            return 0;
        }
        int longIndex = index / entriesPerLong;
        int bitOffset = (index % entriesPerLong) * bits;
        return (int) ((data[longIndex] >>> bitOffset) & mask);
    }

    private static int ceilLog2(int value) {
        if (value <= 1) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(value - 1);
    }
}
