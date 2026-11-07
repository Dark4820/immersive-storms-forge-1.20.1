# Immersive Storms — unofficial Forge 1.20.1 port

Version: **1.0.0-forge.1**
Date of modification: **2026-11-07**

This is an unofficial port, made for personal use in a private modpack, of **Immersive Storms** by **TheDeathlyCow**. It is not affiliated with or endorsed by the original author. Please report issues with this port here, not to the original author.

## Origin

- Original mod: Immersive Storms by TheDeathlyCow
- Source: https://github.com/TheDeathlyCow/immersive-storms
- Base of this port: tag `1.0.0`, commit `34499b0a183f2612065c14fcde06bb8d9a204826` (Fabric, Minecraft 1.21.5)
- Black rain feature taken from: tag `2.0.0+26.1.x-beta.3`, commit `66c883b5c1facba2219c9a31e1f93acfe2e333c9` (Minecraft 26.1)

## License

Immersive Storms is licensed under the **GNU Lesser General Public License v3.0** (LGPL-3.0). This port is a modified version of that work and is distributed under the same license. The full license text is in `LICENSE`, included in the jar.

Sound effects are from Pixabay; see `CREDITS.md` (also included in the jar).

## What changed

### Platform
- Fabric 1.21.5 → Forge 1.20.1 (47.4.x), Java 21 → Java 17.
- Yarn names translated to official Mojang names.
- Mod id `immersive-storms` → `immersive_storms` (Forge does not allow `-`). Assets, tags, config file and translation keys use the `immersive_storms` namespace.
- Fabric API replaced by Forge equivalents: `DeferredRegister` for particles and sounds, `RegisterParticleProvidersEvent`, `TickEvent.ClientTickEvent` (instead of `ClientTickEvents.END_WORLD_TICK`).
- Mod Menu integration replaced by the Forge mod list config button (Cloth Config / AutoConfig screen).
- Cloth Config 18 (Fabric) → Cloth Config 11.1.136 (Forge).

### Fog
- The `BackgroundRendererMixin` was replaced by Forge's `ViewportEvent.ComputeFogColor` and `ViewportEvent.RenderFog` events. No mixin is used for fog.
- Fog is no longer changed while the player has Blindness or Darkness (upstream would push that vanilla fog further away inside storms).

### Sounds
- The 1.21.5 vanilla desert ambient wind (`AmbientDesertBlockSounds`, `BLOCK_SAND_WIND`) does not exist in 1.20.1. The accessor mixin was removed, and the ambient wind now uses a new sound event, `immersive_storms:weather.soft_wind`, built from the mod's own `soft1.ogg` and `soft2.ogg` (credited in `CREDITS.md`). It also plays in deserts and badlands, and respects "Enable ambient wind sounds".
- `/tick freeze` checks were removed (the tick manager does not exist in 1.20.1).

### Biome tags
- Fabric conventional tags (`c:*`) were replaced by Forge and vanilla tags (`forge:is_desert`, `forge:is_snowy`, `forge:is_swamp`, `forge:is_mountain`, `minecraft:is_badlands`, `minecraft:is_savanna`, `minecraft:is_mountain`, the vanilla windswept biomes). The `c:*` tags are kept as optional entries.
- All entries are optional (`"required": false`), including `minecraft:pale_garden` (provided in 1.20.1 by Vanilla Backport).
- Added Terralith biomes:
  - sandstorms: `ancient_sands`, `red_oasis`, `white_mesa`, `warped_mesa`, `arid_highlands`, `ashen_savanna`
  - windy: `ancient_sands`, `red_oasis`, `white_mesa`, `warped_mesa`, `windswept_spires`, `alpine_highlands`, `yosemite_cliffs`, `granite_cliffs`, `basalt_cliffs`, `white_cliffs`
  - blizzards: `frozen_cliffs`, `glacial_chasm`, `scarlet_mountains`

### Black rain (from Immersive Storms 2.0)
- Rain columns and rain splash particles turn black in biomes tagged `immersive_storms:is_black_rain_affected` (default: `minecraft:pale_garden`, optional).
- Upstream 2.0 is built on the 26.1 weather renderer, which does not exist in 1.20.1. This port uses a client-only mixin on `LevelRenderer` (`renderSnowAndRain` and `tickRain`) with `@WrapOperation` only (priority 1100), so it chains with other mods that wrap the same calls.
- Subtle Effects compatibility: when its "biome color rain" option is on, black rain darkens that biome-colored rain (same behavior as the 2.0 compat). The option is read by reflection; Subtle Effects is not required.
- New particle `immersive_storms:black_rain` (based on the vanilla splash) and new config option "Enable black rain".
- MixinExtras is bundled with Forge Jar-in-Jar.

### Config
- Same options as 1.0.0, plus "Enable black rain".
- Port addition: the sandstorm particle render distance is capped at 64 and the density multiplier at 4.

### Performance
- Nothing runs on the server tick. All effects are client-side; on the common side the mod only registers its particle types and sound events.
- Ambient wind: at most 3 spawn attempts per client tick.
- Sandstorm: about 27 particle spawn attempts per client tick with the default config, at most about 1092 with the caps above. Vanilla particle settings (Decreased/Minimal) and the vanilla particle limit still apply.

## Building

Requires JDK 17.

```
gradlew clean build
```

The jar to install is `build/libs/immersive_storms-1.0.0-forge.1.jar`. Do not install the `-slim` jar: it does not include MixinExtras.

Files in `dev/` and `run-mods/` are for the development environment only and are not included in the jar.
