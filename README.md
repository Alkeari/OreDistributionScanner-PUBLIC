# Ore Distribution Scanner

Stop guessing which Y level to strip-mine. Scan the terrain you have already got, then look any ore up in JEI, EMI or REI and read its real depth distribution for your world, your seed and your modpack.

> **Minecraft 1.21.1 on Fabric or NeoForge, and 1.20.1 on Fabric or Forge. Needs Architectury API.**

---

## Availability

- [Modrinth](https://modrinth.com/project/MUv97Uew)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ore-distribution-scanner)

## What It Does

- **Scan without stopping the server** - `/ods scan` sweeps the terrain around you on a background worker. Loaded chunks are read live, unloaded ones straight from the region files.
- **It never changes your world** - the scan is read-only: it generates no chunks and edits no block.
- **Scan as much or as little as you want** - one dimension, a comma-separated list, or `all`, with an optional radius. `/ods status` reports progress and `/ods cancel` stops it.
- **Counted properly, not averaged into mush** - every ore is counted by type and exact Y level, bucketed per biome and normalized by how much of that biome was actually sampled.
- **Thin data is labeled, not hidden** - a biome sampled below the threshold is flagged low-confidence instead of being quietly folded into the average.
- **The answer appears where you already look** - JEI, EMI and REI draw the distribution as a graph: depth left to right, frequency as gold bars, condensed to the ore's real generation band.
  - A biome selector steps between the dimension-wide aggregate and each sampled biome.
  - Low-confidence biomes draw as discrete bars with a warning badge instead of a smooth plot.
  - Hovering a point gives the exact average per chunk and the raw sample count behind it.
- **Or read it in chat** - `/ods info` and `/ods list` print the same results, and both work from a dedicated server console with no client attached.
- **Results stick** - saved per world and re-sent to a player when they rejoin.
- **Everyone can use it** - all commands are permission level 0 by default, so it works in singleplayer without cheats. Raise `scan_permission_level` to restrict the write-ish commands to operators.
- **Modded ores are covered** - ore selection runs off ore tags, with a whitelist and blacklist for blocks that fall outside them.

---

## Requirements

- Minecraft 1.21.1 on Fabric or NeoForge, and 1.20.1 on Fabric or Forge.
- Architectury API 13.0.8 or later on 1.21.1, and 9.2.14 or later on 1.20.1.
- Fabric API and Fabric Loader 0.16.3 or later, on Fabric.
- NeoForge 21.1.0 or later, on NeoForge, and Forge 47.4 or later, on Forge.
- Java 21 or later on 1.21.1, and Java 17 or later on 1.20.1.
- Cloth Config on Fabric, optional on both 1.20.1 and 1.21.1, where it adds the in-game settings screen.
- JEI, EMI or REI, if you want the graphs in a recipe viewer.

## Installation

1. Install your loader and Architectury API, plus Fabric API if you are on Fabric, and a recipe viewer if you want the graphs.
2. Load a world and run `/ods scan`. When it finishes, look up any ore in your recipe viewer.

## Configuration

On Fabric the settings are in `config/ore_distribution_scanner.toml`, and with Cloth Config and Mod Menu installed they are also editable in game. On NeoForge and Forge they are in `config/ore_distribution_scanner-common.toml`, editable by hand, and on NeoForge also through its own Mods Config button.

`/ods reload` re-reads the file without a restart. Editing the ore whitelist or blacklist asks you to rejoin so the recipe viewers pick the change up.

Scan data lives in an `ore_distribution_scanner` folder inside the world save. `/ods clear <dimension>` deletes one dimension's data and `/ods clear all confirm` deletes the lot.

## Compatibility

- **Existing worlds.** It reads what is already there, so an existing world is exactly what it is for. Nothing is generated and nothing is changed.
- **Client or server.** The scan and the commands run on the server or the integrated server; the graphs are drawn by your recipe viewer on the client.
- On 1.21.1, JEI, EMI and REI all have Fabric and NeoForge builds, so any of the three works on either loader.
- On Fabric, on 1.20.1 and 1.21.1 alike, Cloth Config is suggested rather than required: without it the mod still works, just without the in-game settings screen.

## Support

- Include your Minecraft version, your loader and its version, the mod version, your recipe viewer, and your other mods.

## License

License terms are in the [Alkeari License Agreement](https://gist.github.com/Alkeari/2c6ec0cdf3dafee375b1a00b28ca190a).
