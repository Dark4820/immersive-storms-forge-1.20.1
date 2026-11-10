# Mofu's Better End: Overworld rain, clouds and sky disappear (dimension effects bug)

Analysis of `mofus_better_end_-alpha-1.0.0-forge-1.20.1.jar` (mod id `mofus_better_end_`), Forge 1.20.1 47.4.28.
Date: 2026-11-09. Found while porting Immersive Storms; the compat fix belongs in the The Beyond port, not here.

## Symptom

In the Overworld: rain and snow are not drawn, there are no rain splashes and no vanilla rain sounds, no vanilla
clouds and no vanilla sky (sun, moon, stars, sky gradient). The sky still darkens during rain because the rain
level itself is unaffected. With a shaderpack (Complementary) the sky and clouds come from the shaderpack, so only
the missing rain is obvious. Mofu's own dimensions (`broken_constellation`, `aasf`) lose their custom sky, clouds
and rain too.

Measured with the Immersive Storms diagnostic build (`/immersive_storms debug rain`) in the affected pack:
the Overworld effects hook `renderSnowAndRain` returned `true` in 4119/4119 calls and `tickRain` in 2160/2160,
so vanilla never rendered. In a dev environment without The Beyond the same hooks return `false`.

## Background: how Mofu customizes its dimensions

Both Mofu dimension types use `"effects": "minecraft:overworld"`
(`data/mofus_better_end_/dimension_type/broken_constellation.json`, `aasf.json`), so the only way for Mofu to give
them a custom sky is to replace the **Overworld** dimension effects:

- `net.mcreator.mofusbetterend.procedures.BrokenCostellationSkyProcedure`
  (`@Mod.EventBusSubscriber(bus = MOD, value = CLIENT)`), `setupDimensions(RegisterDimensionSpecialEffectsEvent)`
  with `priority = LOWEST`, registers `createOverworldEffects(false, false, false)` under `Level.OVERWORLD`
  (`minecraft:overworld`). The instance is `BrokenCostellationSkyProcedure$1`, a subclass of
  `BrokenCostellationSkyProcedure$MofusBetterEndModDimensionSpecialEffects`.
- That class has five `public static final` hook sets, all empty `HashSet`s from its static initializer:

  | Set | Used by | Effects method |
  |---|---|---|
  | `CUSTOM_WEATHER` (`Set<Predicate<Object[]>>`) | rain and snow | `renderSnowAndRain(...)` |
  | `CUSTOM_EFFECTS` (`Set<Predicate<Object[]>>`) | rain ticking (splashes, sounds) | `tickRain(...)` |
  | `CUSTOM_CLOUDS` (`Set<Predicate<Object[]>>`) | clouds | `renderClouds(...)` |
  | `CUSTOM_SKY` (`Set<Predicate<Object[]>>`) | sky and stars | `renderSky(...)` |
  | `CUSTOM_LIGHTS` (`Set<Consumer<Object[]>>`) | lightmap | `adjustLightmapColors(...)` |

- Each effects method follows the same pattern (bytecode, simplified):

  ```java
  public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lt, double x, double y, double z) {
      if (CUSTOM_WEATHER == null || CUSTOM_WEATHER.isEmpty()) {
          return true;                 // <- "handled": Forge skips vanilla rain, nothing is drawn
      }
      boolean handled = false;
      for (Predicate<Object[]> p : CUSTOM_WEATHER) handled |= p.test(new Object[]{level, ticks, partialTick, lt, x, y, z});
      return handled;
  }
  ```

  `tickRain`, `renderClouds` and `renderSky` are identical with their own set. With an **empty** set they return
  `true`. `adjustLightmapColors` with an empty set does nothing (same as vanilla), so lighting is not affected.
- The predicates themselves are correct: each returns `true` (and draws Mofu's custom effect) only when the level is
  `ResourceKey.create(Registries.DIMENSION, "mofus_better_end_:broken_constellation")`, and `false` everywhere
  else, so vanilla renders normally in the real Overworld. They do not use MCreator saved variables, NBT or any world
  or player data.

## The bug: how the sets are filled

Five procedures, each `@Mod.EventBusSubscriber(bus = MOD, value = CLIENT)`, add their `private static final
PREDICATE` in an `FMLClientSetupEvent` handler:

| Class (`net.mcreator.mofusbetterend.procedures`) | Handler | Field looked up | Adds |
|---|---|---|---|
| `BcRainProcedure` | `weatherSetup(FMLClientSetupEvent)` | `CUSTOM_WEATHER` | `BcRainProcedure.PREDICATE` |
| `BcRainSetupProcedure` | `effectsSetup(FMLClientSetupEvent)` | `CUSTOM_EFFECTS` | `BcRainSetupProcedure.PREDICATE` |
| `BCcloudsProcedure` | `cloudsSetup(FMLClientSetupEvent)` | `CUSTOM_CLOUDS` | `BCcloudsProcedure.PREDICATE` |
| `SkyRenderProcedure` | `skySetup(FMLClientSetupEvent)` | `CUSTOM_SKY` | `SkyRenderProcedure.PREDICATE` |
| `StarProcedure` | `skySetup(FMLClientSetupEvent)` | `CUSTOM_SKY` | `StarProcedure.PREDICATE` |

All five use the same code (decompiled from `BcRainProcedure.weatherSetup`):

```java
try {
    Field field = DimensionSpecialEffectsManager.class.getDeclaredField("EFFECTS");
    field.setAccessible(true);
    for (DimensionSpecialEffects effects : ((ImmutableMap<ResourceLocation, DimensionSpecialEffects>) field.get(null)).values()) {
        Class<?> type = effects.getClass();
        ((Set) type.getField("CUSTOM_WEATHER").get(null)).add(PREDICATE);   // NoSuchFieldException for any non-Mofu effects
    }
} catch (Exception e) {
    // swallowed, no log
}
```

The single `try`/`catch` wraps the **whole loop**. The first effects object that is not a Mofu class (vanilla
Nether/End effects, The Sift's, The Beyond's, Mofu's own `BrokenConstellationDimension$BrokenConstellationSpecialEffectsHandler$1`,
which extends `DimensionSpecialEffects` directly) makes `Class.getField` throw `NoSuchFieldException`, the exception
is swallowed and the loop stops. If Mofu's Overworld effects had not been visited yet, its set stays empty and all
four hooks above return `true` from then on.

### Why it depends on the mod list

`DimensionSpecialEffectsManager.init()` fills a `HashMap<ResourceLocation, DimensionSpecialEffects>` (vanilla entries
first, then `RegisterDimensionSpecialEffectsEvent` in mod order) and freezes it with `ImmutableMap.copyOf(hashMap)`,
which keeps the **HashMap iteration order**: bucket order of `ResourceLocation.hashCode()`
(`31 * namespace.hashCode() + path.hashCode()`, spread with `h ^ (h >>> 16)`, table size 16). Any mod that registers
dimension effects changes that order. Simulated with the same hash function:

| Environment | Registered keys | Iteration order | First visited | Result |
|---|---|---|---|---|
| Dev (Mofu only) | `minecraft:overworld` (Mofu), `minecraft:the_nether`, `minecraft:the_end`, `mofus_better_end_:broken_constellation` | overworld, the_end, broken_constellation, the_nether | Mofu Overworld effects | predicate added, then the loop stops on `the_end`; rain works |
| Pack | the above plus `the_sift:the_sift` (The Sift) and `the_beyond:the_end` (The Beyond) | **the_beyond:the_end**, overworld, the_end, broken_constellation, the_sift, the_nether | The Beyond's End effects | `NoSuchFieldException` on the first entry, all five sets stay empty |

So adding The Beyond (registers `the_beyond:the_end`, whose bucket comes first) is what broke the Overworld in the
pack. It worked before only by luck of the hash order.

## What would load the sets correctly

**In Mofu (proper fix).** The loop is unnecessary: the sets are `static`, so there is one set per class, shared by
all instances. Either add directly:

```java
BrokenCostellationSkyProcedure.MofusBetterEndModDimensionSpecialEffects.CUSTOM_WEATHER.add(PREDICATE);
```

or keep the reflective loop but skip effects without the field (`instanceof` check, or `try`/`catch` **inside** the
loop per entry).

**From another mod (compat, e.g. in the The Beyond port), without changing Mofu's jar:**

1. Only if `ModList.get().isLoaded("mofus_better_end_")`.
2. Run after Mofu's `FMLClientSetupEvent` handlers, e.g. in `FMLLoadCompleteEvent`, or in `FMLClientSetupEvent`
   with `event.enqueueWork(...)` and an optional `ordering = "AFTER"` dependency on `mofus_better_end_` in
   `mods.toml`.
3. By reflection, for each pair below, read the procedure's `private static final PREDICATE` (`setAccessible(true)`)
   and `add` it to the `public static final` set of
   `net.mcreator.mofusbetterend.procedures.BrokenCostellationSkyProcedure$MofusBetterEndModDimensionSpecialEffects`.
   `Set.add` is idempotent, so it is harmless if Mofu's own setup did work:

   | Procedure class | Set |
   |---|---|
   | `BcRainProcedure` | `CUSTOM_WEATHER` |
   | `BcRainSetupProcedure` | `CUSTOM_EFFECTS` |
   | `BCcloudsProcedure` | `CUSTOM_CLOUDS` |
   | `SkyRenderProcedure` | `CUSTOM_SKY` |
   | `StarProcedure` | `CUSTOM_SKY` |

4. Wrap each pair in its own `try`/`catch` and log once if a class or field is missing (another Mofu version).
5. This restores Mofu's own dimensions too. Re-ordering or re-keying The Beyond's effects is not a fix: any other mod
   that registers effects can break the order again.

## What Immersive Storms 1.0.0-forge.3 does

It does not touch Mofu. The option "Restore vanilla Overworld weather and sky" (on by default) wraps the four Forge
dimension-effects hooks in `LevelRenderer` (`renderSnowAndRain`, `tickRain`, `renderClouds`, `renderSky`). It always
calls the original hook, and when the **real level** is `minecraft:overworld`, the effects are not vanilla and the
hook returned `true`, it lets vanilla render anyway and logs once per hook which mod replaced the effects. Mofu's
dimensions, The Beyond's End and The Sift are not affected, so Mofu's custom dimension sky stays broken until the
compat above (or a Mofu update) is applied.
