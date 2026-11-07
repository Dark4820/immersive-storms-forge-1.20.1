package com.thedeathlycow.immersive.storms.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.BiPredicate;

public final class WeatherEffects {
    public static WeatherEffectType getCurrentType(
            Level level,
            BlockPos pos,
            boolean aboveSurface,
            BiPredicate<WeatherEffectType, Holder<Biome>> tagInclusion
    ) {
        if (!isWeatherEffected(level, pos, aboveSurface)) {
            return WeatherEffectType.NONE;
        }

        Holder<Biome> biome = level.getBiomeManager().getNoiseBiomeAtPosition(pos);
        WeatherEffectType type = WeatherEffectType.forBiome(biome, tagInclusion);

        if (!type.allowedWithRain() && aboveSurface && level.isRainingAt(pos)) {
            return WeatherEffectType.NONE;
        }

        return type;
    }

    public static WeatherEffectType getCurrentType(
            Level level,
            BlockPos pos,
            boolean aboveSurface
    ) {
        return getCurrentType(
                level,
                pos,
                aboveSurface,
                (type, biome) -> type.getBiomeTag() != null && biome.is(type.getBiomeTag())
        );
    }

    private static boolean isWeatherEffected(
            Level level,
            BlockPos pos,
            boolean aboveSurface
    ) {
        if (!level.isRaining()) {
            return false;
        } else if (aboveSurface && !level.canSeeSky(pos)) {
            return false;
        } else if (aboveSurface && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        } else {
            return true;
        }
    }

    private WeatherEffects() {

    }
}
