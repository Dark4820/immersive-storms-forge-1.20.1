# Immersive Storms — unofficial Forge 1.20.1 port

> **This is an unofficial port.** It is based on [Immersive Storms](https://github.com/TheDeathlyCow/immersive-storms)
> by **TheDeathlyCow** and is not affiliated with or endorsed by the original author.
> Please report problems with this port in this repository, not to the original author.

Immersive weather effects for mountains, deserts, swamps and more: sandstorms with dust particles and sand-colored
fog, blizzards, dense fog in swamps and the Pale Garden, ambient wind, and black rain in the Pale Garden.

| | |
|---|---|
| Original mod | [Immersive Storms](https://github.com/TheDeathlyCow/immersive-storms) by TheDeathlyCow |
| Based on | Immersive Storms **1.0.0** for Fabric 1.21.5 (tag `1.0.0`, commit `34499b0a183f2612065c14fcde06bb8d9a204826`, 2025-06-16) |
| Black rain taken from | Immersive Storms **2.0.0+26.1.x-beta.3** (tag `2.0.0+26.1.x-beta.3`, commit `66c883b5c1facba2219c9a31e1f93acfe2e333c9`) |
| This port | Minecraft **1.20.1**, Forge **47.4.x**, Java 17 |
| Requires | [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) 11.1.x for Forge (client) |
| License | **LGPL-3.0**, same as the original (see [License](#license)) |

## Modifications

This is a modified version of Immersive Storms. Summary of the changes (full list with dates in
[CHANGES.md](CHANGES.md), technical details in [README-PORT.md](README-PORT.md)):

- **2026-11-07 — 1.0.0-forge.1 (public):** first port. Fabric 1.21.5 → Forge 1.20.1 (Yarn names translated to
  Mojang names, mod id `immersive-storms` → `immersive_storms`); fog via Forge events instead of a mixin; new ambient
  wind sound (the 1.21.5 vanilla desert wind does not exist in 1.20.1); biome tags moved to Forge/vanilla tags, with
  Terralith biomes added; Cloth Config screen in the Forge mod list; black rain in the Pale Garden ported from
  Immersive Storms 2.0 (with Subtle Effects compatibility); sandstorm particle caps.
- **2026-11-12 — 1.0.0-forge.4 (public):** fixed a crash when saving the config screen; new option "Restore vanilla
  Overworld weather and sky" for mods whose dimension effects hide vanilla rain, clouds and sky in the Overworld
  (see [docs/mofu-effects-bug.md](docs/mofu-effects-bug.md)); full GPL-3.0 and LGPL-3.0 texts in the jar.

Only **1.0.0-forge.1** and **1.0.0-forge.4** were published on CurseForge. 1.0.0-forge.2 and 1.0.0-forge.3 are
internal builds (tagged in this repository for reference) that were never released; their changes are part of
1.0.0-forge.4.

## License

Immersive Storms is licensed under the GNU Lesser General Public License v3.0, and so is this port:

- [`COPYING.LESSER`](COPYING.LESSER): GNU LGPL-3.0 (also kept as [`LICENSE`](LICENSE), the file name used by the
  original repository; both files are identical).
- [`COPYING`](COPYING): GNU GPL-3.0, which the LGPL-3.0 supplements.

Both texts, [`CREDITS.md`](CREDITS.md) and [`README-PORT.md`](README-PORT.md) are also included in the mod jar.
Original work by TheDeathlyCow; all credits of the original mod are kept in [`CREDITS.md`](CREDITS.md).

Bundled library: [MixinExtras](https://github.com/LlamaLad7/MixinExtras) by LlamaLad7 (MIT), included with Forge
Jar-in-Jar together with its license text.

## Building

Requires JDK 17.

```
gradlew clean build
```

Install `build/libs/immersive_storms-<version>.jar` (not the `-slim` jar, which does not contain MixinExtras).
Everything under `src/devtest/` and `dev/` is for the development environment only and is not packaged.

## Credits

See [CREDITS.md](CREDITS.md): sound effects from Pixabay (credited by the original mod), the original mod, and
bundled libraries.
