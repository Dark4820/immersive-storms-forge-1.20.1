package com.thedeathlycow.immersive.storms.client.world;

import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient;
import com.thedeathlycow.immersive.storms.registry.ISBiomeTags;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;

/**
 * Black rain, brought over from Immersive Storms 2.0 (originally built on the 26.1 weather renderer).
 */
public final class BlackRainEffect {
    public static final int COLOR = 0x20;
    public static final float COLOR_FLOAT = COLOR / 255f;

    public static boolean isBlackRain(Level level, BlockPos pos) {
        return ImmersiveStormsClient.getConfig().isEnableBlackRain()
                && level.getBiome(pos).is(ISBiomeTags.IS_BLACK_RAIN_AFFECTED);
    }

    /**
     * Writes the RGB color of a black rain column into {@code out}.
     * When Subtle Effects tints rain with the biome water color, that color is darkened instead
     * (same as the Subtle Effects compat in Immersive Storms 2.0).
     */
    public static void getColumnColor(Level level, BlockPos pos, float[] out) {
        if (SubtleEffectsCompat.isBiomeColorRain()) {
            int waterColor = BiomeColors.getAverageWaterColor(level, pos);
            out[0] = ((waterColor >> 16) & 0xFF) / 255f * COLOR_FLOAT;
            out[1] = ((waterColor >> 8) & 0xFF) / 255f * COLOR_FLOAT;
            out[2] = (waterColor & 0xFF) / 255f * COLOR_FLOAT;
        } else {
            out[0] = COLOR_FLOAT;
            out[1] = COLOR_FLOAT;
            out[2] = COLOR_FLOAT;
        }
    }

    /**
     * Optional, reflection-only compat: Subtle Effects is never required.
     */
    private static final class SubtleEffectsCompat {
        private static boolean initialized = false;
        private static Object environmentConfigs = null;
        private static Field biomeColorRainField = null;

        static boolean isBiomeColorRain() {
            if (!initialized) {
                initialized = true;
                if (ModList.get().isLoaded("subtle_effects")) {
                    try {
                        Class<?> modConfigs = Class.forName("einstein.subtle_effects.init.ModConfigs");
                        environmentConfigs = modConfigs.getField("ENVIRONMENT").get(null);
                        biomeColorRainField = environmentConfigs.getClass().getField("biomeColorRain");
                        ImmersiveStorms.LOGGER.info("Subtle Effects detected, black rain will darken its biome-colored rain");
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                        ImmersiveStorms.LOGGER.warn("Subtle Effects detected but its rain color option could not be read; using plain black rain", e);
                        environmentConfigs = null;
                        biomeColorRainField = null;
                    }
                }
            }

            if (biomeColorRainField == null) {
                return false;
            }

            try {
                return biomeColorRainField.getBoolean(environmentConfigs);
            } catch (ReflectiveOperationException | RuntimeException e) {
                biomeColorRainField = null;
                return false;
            }
        }
    }

    private BlackRainEffect() {

    }
}
