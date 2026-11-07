package com.thedeathlycow.immersive.storms.client;

import com.thedeathlycow.immersive.storms.util.WeatherEffectType;
import com.thedeathlycow.immersive.storms.util.WeatherEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class WeatherEffectsClient {
    public static WeatherEffectType getCurrentType(
            Level level,
            BlockPos pos,
            boolean aboveSurface
    ) {
        return WeatherEffects.getCurrentType(
                level,
                pos,
                aboveSurface,
                WeatherEffectsClient::isWeatherEffectTypeEnabled
        );
    }

    public static boolean isWeatherEffectTypeEnabled(WeatherEffectType type, Holder<Biome> biome) {
        // Fabric's ClientTags.isInWithLocalFallback -> plain tag check (single player: tags are always present)
        return type.getBiomeTag() != null
                && ImmersiveStormsClient.getConfig().isEnabled(type)
                && biome.is(type.getBiomeTag());
    }

    private WeatherEffectsClient() {

    }
}
