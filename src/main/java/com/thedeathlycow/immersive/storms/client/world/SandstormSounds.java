package com.thedeathlycow.immersive.storms.client.world;

import com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient;
import com.thedeathlycow.immersive.storms.client.WeatherEffectsClient;
import com.thedeathlycow.immersive.storms.config.ImmersiveStormsConfig;
import com.thedeathlycow.immersive.storms.registry.ISSoundEvents;
import com.thedeathlycow.immersive.storms.util.WeatherEffectType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;
import java.util.function.Consumer;

public final class SandstormSounds implements Consumer<ClientLevel> {
    private static final int MAX_SOUND_Y_DIFF = 10;

    private static final int MAX_XZ_OFFSET = 10;

    private int timer = 0;

    @Override
    public void accept(ClientLevel level) {
        if (level.getRainLevel(1f) < 0.7f) {
            return;
        }

        ImmersiveStormsConfig config = ImmersiveStormsClient.getConfig();
        boolean enabled = config.getSandstorm().isEnableSandstormSounds()
                && config.isEnabled(WeatherEffectType.SANDSTORM);

        if (!enabled) {
            return;
        }

        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return; // no camera for whatever reason
        }

        if (!level.isRaining()) {
            return;
        }

        if (level.random.nextInt(3) >= this.timer) {
            this.timer++;
            return;
        }
        this.timer = 0;

        this.chooseSpotForWindSound(level, camera).ifPresent(
                pos -> level.playLocalSound(
                        pos,
                        ISSoundEvents.WEATHER_STRONG_WIND.get(),
                        SoundSource.WEATHER,
                        0.5f, 1f,
                        false
                )
        );
    }

    private Optional<BlockPos> chooseSpotForWindSound(ClientLevel level, Camera camera) {
        BlockPos cameraPos = camera.getBlockPosition();

        // same offsets as upstream 1.0.0
        int dx = level.random.nextIntBetweenInclusive(-MAX_XZ_OFFSET, MAX_SOUND_Y_DIFF);
        int dy = level.random.nextIntBetweenInclusive(-MAX_XZ_OFFSET, MAX_SOUND_Y_DIFF);
        int dz = level.random.nextIntBetweenInclusive(-MAX_XZ_OFFSET, MAX_SOUND_Y_DIFF);
        BlockPos soundPos = cameraPos.offset(dx, dy, dz);

        WeatherEffectType.WeatherData weatherData = WeatherEffectsClient.getCurrentType(level, soundPos, true)
                .getWeatherData(level);
        if (weatherData != null && weatherData.windy()) {
            return Optional.of(soundPos);
        }

        return Optional.empty();
    }
}
