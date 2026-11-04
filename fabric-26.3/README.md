# Actually Additions — Fabric 26.3 port

Release `0.3.0-beta.1` ports the active upstream content to Fabric Minecraft 26.3.
It is ready for gameplay testing. The repository root preserves the upstream
NeoForge source and original assets; the Fabric implementation is in this module.

## Requirements

Minecraft 26.3, Fabric Loader 0.19.5+, Fabric API 0.160.7+26.3, Java 25,
and Balm Fabric 26.3.0.1+.

## Build and test

Run `./gradlew build` from this directory with Java 25. The wrapper downloads Gradle 9.7.1.
Install `tools/requirements.txt` before regenerating resources.
Run `python tools/sync_upstream_content.py` from any directory after adding supported content;
it migrates upstream recipes, item assets, tags, loot and world generation.

Game tests: `./gradlew runGameTest`. Client checks: `./gradlew runClientGameTest` (requires a graphics session).
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
All 231 active upstream item IDs, 259 crafting/smelting/stonecutting recipes and
251 processing recipes are migrated. The manual is included; JEI 31.17.0.108 is
optional and displays all nine processing categories using recipes synchronized
from the server, including datapack reloads. Processing overrides belong under
`data/<namespace>/aa_processing/`.

## Validation

- 38 required game tests passed with Ex Compressum and Ex Nihilo Omnia loaded.
- Client game test passed with both companion mods and JEI loaded.
- All 231 item models resolved; all 251 JEI processing layouts were constructed.
- Native server menus for Display Stand, Crusher, Firework Box and Coffee Machine,
  the coffee Start button and the searchable manual were exercised and rendered.
- Native machine drop/placement, crop/ore/slab loot, worm watering, network rollback,
  stale connections, fluid budgets and engineer-house placement are covered.
- Packaged JAR checked for archive integrity and accidental test/dependency files.

## Port details

The Fabric interfaces replace the original NeoForge screens. Configuration uses
`config/actuallyadditions.json` and takes effect after restart. Farmer and leaf
harvesting areas support odd sizes from 1 through 65. Original NeoForge-only
optional integrations, such as Curios and Immersive Engineering cloche recipes,
are outside the Fabric implementation. Audio events and original sound assets are
included; sound could not be auditioned on the headless test host.

### 0.3.0-beta.2: village texture corrections

Thirteen machine model files contained faces referencing an unassigned
`#missing` material. These faces now resolve to each model's existing casing
texture; the correction also applies to the inherited item models. The resource
synchronizer preserves the repair. `tools/verify_asset_materials.py` resolves
parent models and texture variables, so a valid parent path cannot hide an
unassigned face material again.
