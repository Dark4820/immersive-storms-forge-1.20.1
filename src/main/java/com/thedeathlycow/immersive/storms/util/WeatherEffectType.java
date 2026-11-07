package com.thedeathlycow.immersive.storms.util;

import com.thedeathlycow.immersive.storms.registry.ISBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

public enum WeatherEffectType implements StringRepresentable {
    NONE,
    SANDSTORM(
            "sandstorm",
            ISBiomeTags.HAS_SANDSTORMS,
            Vec3.fromRGB24(0xD9AA84),
            new WeatherData(WeatherData.LIGHT_FOG, true),
            new WeatherData(WeatherData.THICK_FOG, true),
            false
    ),
    BLIZZARD(
            "blizzard",
            ISBiomeTags.HAS_BLIZZARDS,
            Vec3.fromRGB24(0xBBBBBB),
            null,
            new WeatherData(WeatherData.LIGHT_FOG, false),
            false
    ),
    DENSE_FOG(
            "dense_fog",
            ISBiomeTags.HAS_DENSE_FOG,
            null,
            new WeatherData(WeatherData.LIGHT_FOG, false),
            new WeatherData(WeatherData.THICK_FOG, false),
            true
    );

    private final String name;

    @Nullable
    private final TagKey<Biome> biomeTag;

    @Nullable
    private final Vec3 color;

    @Nullable
    private final WeatherData rainWeatherData;

    @Nullable
    private final WeatherData thunderWeatherData;

    private final boolean allowedWithRain;

    WeatherEffectType() {
        this("none", null, null, null, null, true);
    }

    WeatherEffectType(
            String name,
            @Nullable TagKey<Biome> biomeTag,
            @Nullable Vec3 color,
            @Nullable WeatherData rainWeatherData,
            @Nullable WeatherData thunderWeatherData,
            boolean allowedWithRain
    ) {
        this.name = name;
        this.biomeTag = biomeTag;
        this.color = color;
        this.rainWeatherData = rainWeatherData;
        this.thunderWeatherData = thunderWeatherData;
        this.allowedWithRain = allowedWithRain;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Nullable
    public TagKey<Biome> getBiomeTag() {
        return biomeTag;
    }

    @Nullable
    public Vec3 getColor() {
        return color;
    }

    @Nullable
    public WeatherData getWeatherData(Level level) {
        if (this.thunderWeatherData != null && level.isThundering()) {
            return this.thunderWeatherData;
        } else if (level.isRaining()) {
            return this.rainWeatherData;
        } else {
            return null;
        }
    }

    @Nullable
    public WeatherData getRainWeatherData() {
        return rainWeatherData;
    }

    @Nullable
    public WeatherData getThunderWeatherData() {
        return thunderWeatherData;
    }

    public boolean allowedWithRain() {
        return this.allowedWithRain;
    }

    @ApiStatus.Internal
    public static WeatherEffectType forBiome(Holder<Biome> biome, BiPredicate<WeatherEffectType, Holder<Biome>> allowed) {
        for (WeatherEffectType value : values()) {
            if (allowed.test(value, biome)) {
                return value;
            }
        }

        return NONE;
    }

    public record WeatherData(
            Vec3 fogDistance,
            boolean windy
    ) {
        private static final Vec3 LIGHT_FOG = new Vec3(32, 64, 0);
        private static final Vec3 THICK_FOG = new Vec3(16, 32, 0);
    }
}
