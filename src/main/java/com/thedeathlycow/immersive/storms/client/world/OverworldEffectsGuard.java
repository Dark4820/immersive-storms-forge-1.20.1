package com.thedeathlycow.immersive.storms.client.world;

import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Files;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * "Restore vanilla Overworld weather and sky".
 * <p>
 * Forge lets a mod replace a dimension's {@link DimensionSpecialEffects} and skip vanilla rain, rain ticking,
 * clouds or sky by returning {@code true} from the matching hook ("I rendered it myself"). Some mods register
 * effects under the {@code minecraft:overworld} key for their own dimensions and then, through a bug, also
 * return {@code true} in the real Overworld without drawing anything (see docs/mofu-effects-bug.md).
 * <p>
 * When the real level is {@code minecraft:overworld} (the level key, not the effects key), the effects are not
 * vanilla and a hook returned {@code true}, this makes vanilla render anyway. The mod's hook is always called first,
 * so anything it draws is still drawn. Other dimensions (including ones that reuse the overworld effects key) are
 * never touched. Logs once per hook.
 */
public final class OverworldEffectsGuard {
    public enum Hook {
        RAIN("renderSnowAndRain", "rain and snow"),
        RAIN_TICK("tickRain", "rain splashes and rain sounds"),
        CLOUDS("renderClouds", "clouds"),
        SKY("renderSky", "sky");

        final String method;
        final String what;

        Hook(String method, String what) {
            this.method = method;
            this.what = what;
        }
    }

    private static final Map<Hook, Long> ACTIVATIONS = new EnumMap<>(Hook.class);
    private static final Map<Hook, Long> CALLS = new EnumMap<>(Hook.class);
    private static final Map<Hook, Long> CALLS_HANDLED_IN_OVERWORLD = new EnumMap<>(Hook.class);
    private static final Set<Hook> WARNED = EnumSet.noneOf(Hook.class);
    private static final Set<Hook> SEEN = EnumSet.noneOf(Hook.class);
    private static int ticksInLevel = 0;
    private static boolean hookCheckDone = false;

    /**
     * @param handled what the dimension effects hook returned ({@code true} = vanilla rendering is skipped)
     * @return the value to give back to vanilla
     */
    public static boolean filter(Hook hook, DimensionSpecialEffects effects, ClientLevel level, boolean handled) {
        SEEN.add(hook);
        CALLS.merge(hook, 1L, Long::sum);
        if (handled && level != null && level.dimension() == Level.OVERWORLD) {
            CALLS_HANDLED_IN_OVERWORLD.merge(hook, 1L, Long::sum);
        }
        if (!handled || level == null || level.dimension() != Level.OVERWORLD || isVanilla(effects)) {
            return handled;
        }
        if (!ImmersiveStormsClient.getConfig().isRestoreVanillaOverworldWeatherAndSky()) {
            return true;
        }
        ACTIVATIONS.merge(hook, 1L, Long::sum);
        if (WARNED.add(hook)) {
            ImmersiveStorms.LOGGER.warn(
                    "{} ({}) replaced the Overworld dimension effects and its {} hook skipped vanilla rendering in the Overworld; "
                            + "drawing vanilla {} instead. Disable \"Restore vanilla Overworld weather and sky\" in the Immersive Storms config to turn this off.",
                    ownerMod(effects.getClass()), effects.getClass().getName(), hook.method, hook.what
            );
        }
        return false;
    }

    /**
     * Called every client tick: once, after the player has been in a level for a while, warns if a hook was never
     * reached (another mod may have changed that code, so the option cannot act on it).
     */
    public static void onClientTick(Minecraft mc) {
        if (hookCheckDone || mc.level == null || mc.isPaused()) {
            return;
        }
        if (++ticksInLevel < 200) {
            return;
        }
        hookCheckDone = true;
        for (Hook hook : Hook.values()) {
            if (hook == Hook.CLOUDS && mc.options.getCloudsType() == CloudStatus.OFF) {
                continue; // clouds are not rendered at all, so the hook is never called
            }
            if (!SEEN.contains(hook)) {
                ImmersiveStorms.LOGGER.warn("The dimension effects {} hook was never reached; \"Restore vanilla Overworld weather and sky\" "
                        + "cannot act on {} (another mod may have changed LevelRenderer)", hook.method, hook.what);
            }
        }
    }

    public static long getActivations(Hook hook) {
        return ACTIVATIONS.getOrDefault(hook, 0L);
    }

    /** Diagnostics: how many times the hook was reached at all. */
    public static long getCalls(Hook hook) {
        return CALLS.getOrDefault(hook, 0L);
    }

    /** Diagnostics: how many times the hook returned true while the real level was the Overworld. */
    public static long getHandledInOverworld(Hook hook) {
        return CALLS_HANDLED_IN_OVERWORLD.getOrDefault(hook, 0L);
    }

    private static boolean isVanilla(DimensionSpecialEffects effects) {
        Class<?> type = effects.getClass();
        return type == DimensionSpecialEffects.OverworldEffects.class
                || type == DimensionSpecialEffects.NetherEffects.class
                || type == DimensionSpecialEffects.EndEffects.class;
    }

    private static String ownerMod(Class<?> type) {
        String path = type.getName().replace('.', '/') + ".class";
        for (IModInfo info : ModList.get().getMods()) {
            try {
                if (Files.exists(info.getOwningFile().getFile().findResource(path))) {
                    return info.getModId();
                }
            } catch (Exception ignored) {
            }
        }
        return "unknown mod";
    }

    private OverworldEffectsGuard() {

    }
}
