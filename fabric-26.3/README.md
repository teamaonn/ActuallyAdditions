# Actually Additions: Fabric Power — Minecraft 26.3

Version 0.1.0, the first Fabric stage of Joshua's Actually Additions fork.

This stage ports the Coal Generator, three Energy Laser Relay tiers, Laser Wrench
and relay range upgrade. It retains the original block models and textures.
The remaining Actually Additions machines, crystals, reconstructor, world
generation, item/fluid relays and guidebook are not included in this stage.

## Install

Add `actuallyadditions-fabric-power-26.3-0.1.0.jar` to both client and server with:

- Minecraft 26.3 and Java 25.
- Fabric Loader 0.19.5 or newer.
- Fabric API 0.160.7+26.3 or a compatible newer release.
- Balm 26.3.0.1 for Fabric (the same dependency used by Ex Compressum).

Ex Compressum and Omnia are not required to load the power module, but the
integration checks use Ex Compressum 26.3.0.0-omnia.1 and Omnia 0.5.0-alpha.3.
This JAR uses the `actuallyadditions` mod ID. Install only one Actually Additions
JAR in an instance. The original Forge/NeoForge JAR is not compatible with Fabric.

## Quick start: power the Auto Sieve

1. Craft a Coal Generator and place it touching the Ex Compressum Auto Sieve.
2. Right-click the generator and insert coal, charcoal or another furnace fuel.
3. Install an Omnia mesh and input material in the sieve as usual.

The generator produces **20 energy per tick**, stores **60,000**, and can output
up to **80 per tick** from that buffer. These values follow the upstream
Coal Generator. It accepts vanilla 26.3 cooking-fuel components, including wood,
coal blocks, blaze rods and lava buckets, with the actual configured burn times.
Lava leaves an empty bucket.

An Auto Sieve costs 40 per working tick by default. One generator powers it at a
reduced average speed, two directly connected generators sustain normal speed,
and three through relays cover the transmission loss at normal speed. The Auto
Heavy Sieve costs more energy; scale your generator count accordingly.

Use hoppers on the top/sides for fuel and underneath for empty buckets. Shift-click
moves fuel into/out of the generator. A redstone signal pauses fuel consumption
and generation; the existing energy buffer can still supply machines. A full
buffer pauses burning instead of wasting the remaining fuel.

## Wireless energy relays

1. Place a relay touching a generator, and another touching the receiving machine.
2. Hold the **Laser Wrench** and right-click the first relay, then the second.
3. They connect if both chunks are loaded, both relays are in the same dimension,
   and they are no more than **15 blocks** apart.
4. Repeat the two clicks to remove that connection.

Relays can form chains or branching networks. They have no energy buffer; energy
passes directly from the source to connected receiving machines. Active links
emit blue particle beams. Particles can be hidden by the client's particle
settings. Relays do not force-load chunks or transfer across dimensions.

- Right-click with an empty hand to read status.
- Sneak-right-click with an empty hand to cycle BOTH / INPUT_ONLY / OUTPUT_ONLY.
  Input-only accepts energy into the network; output-only delivers it to machines.
- Sneak-right-click a relay with the wrench to remove all its connections.
- Sneak-right-click air with the wrench to clear the selected first endpoint.
- Right-click a relay with a **Laser Relay Range Upgrade** to install it. Both
  endpoints need the upgrade for a connection beyond 15 blocks, up to **35**.
  Breaking an upgraded relay returns the upgrade.

| Relay tier | Transfer cap per tick | Transmission loss |
| --- | ---: | ---: |
| Energy | 1,000 | 5% |
| Advanced | 10,000 | 8% |
| Extreme | 100,000 | 10% |

The rate/loss values follow the upstream tiers. A path uses its lowest remaining
relay throughput and highest loss percentage. Loss is charged once per transfer,
not repeatedly at each hop. Small one-unit packets retain the upstream loss
exemption. Multiple callers share each relay's tick budget. Cycles do not multiply
energy, and a receiver touched by several relays is considered once per transfer.

## Temporary crafting recipes

The crystal-production chain is not ported yet. These recipes deliberately use
vanilla materials so the power stage is obtainable in an existing Omnia world.
They are not claimed to be the original Actually Additions recipes.

Rows below describe a 3x3 crafting grid; `.` is an empty slot.

| Result | Rows | Ingredients |
| --- | --- | --- |
| Coal Generator | `CIC / CFC / CRC` | C = cobblestone, I = iron ingot, F = furnace, R = redstone dust |
| 4 Energy Laser Relays | `OBO / RGR / OBO` | O = obsidian, B = redstone block, R = redstone dust, G = gold ingot |
| Advanced Relay | `.I. / RLR / .I.` | I = iron block, R = redstone block, L = Energy Laser Relay |
| Extreme Relay | `.D. / RLR / .D.` | D = diamond block, R = redstone block, L = Advanced Relay |
| Laser Wrench | `G.. / .I. / ..I` | G = gold ingot, I = iron ingot |
| Range Upgrade | `.Q. / RER / .Q.` | Q = quartz, R = redstone dust, E = ender pearl |

## Saved state and breaking blocks

Generator fuel, energy and remaining burn time survive saves/reloads. Relay
connections, node identities, modes and range upgrades are saved. A removed or
replaced relay cannot inherit stale links from a different node at the same
coordinates. Breaking a generator drops its unconsumed fuel; its stored energy
and ongoing burn are not carried by the dropped machine block.

## Build and validation

Run `./gradlew assemble` (or `gradlew.bat assemble`) in this directory with Java 25.
The upstream NeoForge project at the repository root is left intact.

Integration tests need external fixture JARs, supplied explicitly:

```
./gradlew runGameTest -Pexcompressum_jar=/absolute/path/to/excompressum.jar -Pomnia_jar=/absolute/path/to/omnia.jar
```

For a standalone build without integration fixture JARs, use `./gradlew assemble`.
`build` includes the dedicated integration checks and therefore needs those two
properties. Fixture JARs, Balm and Fabric API are not bundled in this release.

Validation for 0.1.0: Java compilation passed and **all nine required server tests
passed**, including Fabric's registration check and eight power checks. These
cover real furnace fuel and lava remainders; fuel/energy/burn persistence; full
buffers and redstone; simulated transfers, losses and per-tick limits; cyclic
networks, modes and replacement identities; saved links and range; real wrench
interaction; and original Ex Compressum Auto Sieve jobs using direct and wireless
power. Client appearance and third-party energy providers still need in-game
checks. The intended Ex Compressum connection is directly verified.

## Credits

Original Actually Additions code, designs, models and textures: Ellpeck and the
Actually Additions contributors, including the credits retained in the upstream
project. The port adapts their generator and energy-relay mechanics to Fabric
26.3 and Balm's energy interface. Upstream MIT LICENSE is included in the JAR.
