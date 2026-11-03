# Actually Additions — Fabric 26.3 port

This module is the ongoing full port of the upstream mod. It is not a finished release.
The repository root preserves the upstream NeoForge source and original assets.

## Requirements

Minecraft 26.3, Fabric Loader 0.19.5+, Fabric API 0.160.7+26.3, Java 25,
and Balm Fabric 26.3.0.1+.

## Build and test

Run `gradle build` from this directory with Java 25 and Gradle 9.7.1.
Run `python tools/sync_upstream_content.py` from any directory after adding supported content;
it migrates upstream recipes, item assets, tags, loot and world generation.

Game tests: `gradle runGameTest`.
The optional properties `-Pexcompressum_jar=/path/to/excompressum.jar` and
`-Pomnia_jar=/path/to/omnia.jar` include the actual external mods for integration tests.
They are never bundled in the output mod.

## Implemented systems

- Coal generation and three energy relay tiers, linking, rate budgets and range upgrades.
- Atomic Reconstructor, conversion recipes and all six specialized lenses.
- Empowerer and Display Stands, both Crushers and two-lane Powered Furnace.
- Five battery tiers, charge-preserving crafting, Energizer, Enervator and Battery Box.
- Canola Press, Fermenting Barrel, four oil fluids and buckets, Oil Generator.
- Canola, rice, flax and coffee crops; harvesting and upgraded oil seeds.
- Heat Collector, Leaf Generator and Bioreactor.
- Breaker, Placer, Dropper, Long Range Breaker, Vertical Digger and Lava Factory.
- Fluid Collector and Fluid Placer with transactional fluid storage.
- Farmer, Feeder, reusable filters, Ranged Collector and XP Solidifier.
- All six Phantomface variants and Phantom Boosters, with loaded-target/range checks.
- Six AIOTs, portable crafting, Teleport Staff, sixteen drills and nine real upgrades.
- Traveler's Sack, Coffee Machine and native potion-effect coffee cups.
- Original material and decorative block families, lamps, Greenhouse Glass and black quartz ore.
- Transactional item/fluid relay networks, priorities, filters, interfaces and hopping interfaces.
- Growth/magnet rings, leaf blowers, bat wings, goggles, tag selectors, sacks, filler and water bowls.
- Worms, tiny torches, crystal clusters, player interfaces, firework boxes, lamp controllers and shock suppressors.
- Searchable manual, server-synchronized JEI processing recipes and native 26.3 loot conditions.
- Engineer profession and trades, village houses, natural crop patches and dungeon/bat loot.
- Machine drops preserve energy, fluids and settings; inventory contents drop separately.
- Fabric JSON configuration at `config/actuallyadditions.json` for machine, worldgen and gameplay settings.

`UPSTREAM_CONTENT_STATUS.json` records the migrated recipe counts and content IDs.
All active upstream item IDs and crafting/processing recipes are migrated. This is
still a development build: client presentation and multiplayer recipe synchronization
are under validation. Optional integrations with NeoForge-only APIs do not apply.

Validation checkpoint: 37 required game tests passed with the actual Ex Compressum
and Ex Nihilo Omnia Fabric JARs loaded together. Tests cover processing, power,
transaction rollback, stale network bindings, native loot, machine placement and drops,
worms, lighting and village trade/pool registration. This does not establish that every
client screen and optional integration has been exercised.
