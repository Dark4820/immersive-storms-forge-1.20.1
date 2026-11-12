# Changes

Changes made in this unofficial Forge 1.20.1 port of [Immersive Storms](https://github.com/TheDeathlyCow/immersive-storms)
by TheDeathlyCow, relative to the original Immersive Storms **1.0.0** for Fabric 1.21.5
(tag `1.0.0`, commit `34499b0a183f2612065c14fcde06bb8d9a204826`).

Public releases on CurseForge: **1.0.0-forge.1** and **1.0.0-forge.4**. Versions 1.0.0-forge.2 and
1.0.0-forge.3 were internal builds that were never published; their changes are first released in 1.0.0-forge.4.

## 1.0.0-forge.4 — 2026-11-12 (public)

Next public release after 1.0.0-forge.1. Same code as the internal 1.0.0-forge.3 build; summary of everything
that changed since 1.0.0-forge.1:

- **Fixed:** crash when saving the config screen (`IllegalAccessException: Can not set static final int field
  SandstormConfig.MAX_RENDER_DISTANCE`). Saving any option crashed the game in 1.0.0-forge.1.
- **New:** option "Restore vanilla Overworld weather and sky" (on by default). If another mod's dimension effects
  skip vanilla rain, rain splashes and sounds, clouds or sky in the Overworld without drawing them (seen with
  Mofu's Better End together with The Beyond), vanilla draws them anyway. It only acts in the real Overworld, never
  in other dimensions, and logs once per hook which mod caused it. See `docs/mofu-effects-bug.md`.
- **License:** the jar now contains the full license texts, `COPYING` (GNU GPL-3.0) and `COPYING.LESSER`
  (GNU LGPL-3.0), besides `LICENSE`, plus `CHANGES.md`. The build fails if any license or notice file is missing.
- **Docs:** `README.md` with the unofficial-port notice, this `CHANGES.md`, credits for bundled libraries
  (MixinExtras, MIT) in `CREDITS.md`.
- The jar manifest no longer depends on the folder the project is built in (fixed Gradle project name).

## 1.0.0-forge.3 — 2026-11-09 (internal, not published)

- New option "Restore vanilla Overworld weather and sky" (on by default). Some mods replace the Overworld dimension
  effects and, through a bug, skip vanilla rain, rain splashes and sounds, clouds and sky in the Overworld without
  drawing them (Mofu's Better End when The Beyond is also installed, see `docs/mofu-effects-bug.md`). The port now
  wraps the four Forge dimension-effects hooks in `LevelRenderer` (`renderSnowAndRain`, `tickRain`, `renderClouds`,
  `renderSky`); the original hook is always called, and only when the real level is `minecraft:overworld`, the
  effects are not vanilla and the hook returned `true`, vanilla renders anyway. Logs once per hook which mod did it.
  Other dimensions are not touched.
- Added `docs/mofu-effects-bug.md` (technical analysis of that bug).
- Development only (not packaged): rain probe self test.

## 1.0.0-forge.2 — 2026-11-09 (internal, not published)

- Fixed a crash when saving the config screen (`IllegalAccessException: Can not set static final int field
  SandstormConfig.MAX_RENDER_DISTANCE`): the particle caps were moved out of the config classes into `ConfigLimits`,
  because AutoConfig's screen treats every field of a config class as an option.
- Tests: a unit test that config classes only have writable instance fields, and an in-game config screen self test
  (development only, not packaged).

## 1.0.0-forge.1 — 2026-11-07 (public)

First port.

### Platform
- Fabric 1.21.5 → Forge 1.20.1 (47.4.x), Java 21 → Java 17. Yarn names translated to official Mojang names.
- Mod id `immersive-storms` → `immersive_storms` (Forge does not allow `-`); assets, tags, config file and translation
  keys use the `immersive_storms` namespace.
- Fabric API replaced by Forge equivalents: `DeferredRegister` for particles and sounds,
  `RegisterParticleProvidersEvent`, `TickEvent.ClientTickEvent` instead of `ClientTickEvents.END_WORLD_TICK`.
- Mod Menu integration replaced by the config button in the Forge mod list (Cloth Config / AutoConfig).
- Cloth Config 18 (Fabric) → Cloth Config 11.1.136 (Forge).

### Fog
- `BackgroundRendererMixin` replaced by Forge's `ViewportEvent.ComputeFogColor` and `ViewportEvent.RenderFog`.
- Fog is not changed while the player has Blindness or Darkness.

### Sounds
- The 1.21.5 vanilla desert ambient wind (`AmbientDesertBlockSounds`, `BLOCK_SAND_WIND`) does not exist in 1.20.1: the
  accessor mixin was removed and ambient wind uses a new sound event `immersive_storms:weather.soft_wind`, made from
  the mod's own `soft1.ogg` and `soft2.ogg`. It also plays in deserts and badlands.
- `/tick freeze` checks removed (no tick manager in 1.20.1).

### Biome tags
- Fabric conventional `c:` tags replaced by Forge and vanilla tags; `c:` tags kept as optional entries. All entries are
  optional, including `minecraft:pale_garden` (provided in 1.20.1 by Vanilla Backport).
- Terralith biomes added to the sandstorm, windy and blizzard tags.

### Black rain (from Immersive Storms 2.0.0+26.1.x-beta.3)
- Rain columns and splashes turn black in biomes tagged `immersive_storms:is_black_rain_affected` (default
  `minecraft:pale_garden`). Rewritten for the 1.20.1 renderer with a client mixin on `LevelRenderer`
  (`@WrapOperation` only).
- Subtle Effects compatibility: darkens its biome-colored rain (read by reflection, no hard dependency).
- New particle `immersive_storms:black_rain`, new option "Enable black rain". MixinExtras bundled with Jar-in-Jar.

### Config
- Same options as 1.0.0, plus "Enable black rain". Sandstorm particle render distance capped at 64 and density
  multiplier at 4.

### Packaging
- Version scheme `1.0.0-forge.N`. `LICENSE`, `CREDITS.md` and `README-PORT.md` included in the jar.
