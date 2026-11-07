package com.thedeathlycow.immersive.storms.client.world;

import com.mojang.blaze3d.shaders.FogShape;
import com.thedeathlycow.immersive.storms.client.WeatherEffectsClient;
import com.thedeathlycow.immersive.storms.config.ImmersiveStormsConfig;
import com.thedeathlycow.immersive.storms.util.WeatherEffectType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Port of FogEffects + BackgroundRendererMixin. Uses Forge's ViewportEvent instead of a mixin:
 * <ul>
 *     <li>ComputeFogColor replaces the injection into BackgroundRenderer#getFogColor</li>
 *     <li>RenderFog (cancelled so that Forge applies the new values) replaces the @ModifyReturnValue on applyFog</li>
 * </ul>
 */
public final class FogEffects {
    private static final float START_FOG_SPHERE_RAIN_GRADIENT = 0.75f;

    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event, ImmersiveStormsConfig config) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Vec3 color = getFogColor(
                level, event.getCamera(),
                event.getRed(), event.getGreen(), event.getBlue(),
                (float) event.getPartialTick(),
                config
        );

        if (color != null) {
            event.setRed((float) color.x);
            event.setGreen((float) color.y);
            event.setBlue((float) color.z);
        }
    }

    public static void onRenderFog(ViewportEvent.RenderFog event, ImmersiveStormsConfig config) {
        Camera camera = event.getCamera();
        Entity focused = camera.getEntity();
        if (focused == null) {
            return;
        }

        // Port addition: vanilla blindness/darkness fog is much closer than storm fog; upstream would push it
        // further away. Leave those effects untouched.
        if (focused instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return;
        }

        Level level = focused.level();
        float partialTick = (float) event.getPartialTick();
        final float rainGradient = level.getRainLevel(partialTick);

        if (config.isEnableFogChanges() && event.getType() == FogType.NONE && rainGradient > 0f) {
            if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
                if (rainGradient > START_FOG_SPHERE_RAIN_GRADIENT) {
                    event.setFogShape(FogShape.CYLINDER);
                    event.setCanceled(true);
                }
                return;
            }

            final Vec3 baseRadius = new Vec3(event.getNearPlaneDistance(), event.getFarPlaneDistance(), 0);

            final float thunderGradient = level.getThunderLevel(partialTick);

            Vec3 rainDistance = lerpFogDistance(camera.getPosition(), level, baseRadius, WeatherEffectType::getRainWeatherData);

            Vec3 thunderDistance = thunderGradient > 0
                    ? lerpFogDistance(camera.getPosition(), level, baseRadius, WeatherEffectType::getThunderWeatherData)
                    : null;

            // lerp fog distances for smooth transition when weather changes
            updateFogRadius(event, rainDistance, thunderDistance, rainGradient, thunderGradient, config);
        }
    }

    @Nullable
    public static Vec3 getFogColor(
            ClientLevel level,
            Camera camera,
            float baseRed, float baseGreen, float baseBlue,
            float partialTick,
            ImmersiveStormsConfig config
    ) {
        float gradient = level.getRainLevel(partialTick);
        if (!config.isEnableFogChanges() || gradient <= 0f) {
            return null;
        }

        final Vec3 normalColor = new Vec3(baseRed, baseGreen, baseBlue);

        // same transformation as vanilla, kept from upstream for consistency
        float skyAngle = Mth.clamp(
                Mth.cos(level.getTimeOfDay(partialTick) * ((float) Math.PI * 2f)) * 2.0F + 0.5F,
                0.0F, 1.0F
        );

        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();

        return CubicSampler.gaussianSampleVec3(
                camera.getPosition(),
                (x, y, z) -> {
                    samplePos.set(x, y, z);
                    Holder<Biome> biome = level.getBiomeManager().getBiome(samplePos);
                    WeatherEffectType sampledType = WeatherEffectType.forBiome(biome, WeatherEffectsClient::isWeatherEffectTypeEnabled);
                    Vec3 color = sampledType.getColor();

                    return level.effects().getBrightnessDependentFogColor(
                            color != null
                                    ? normalColor.lerp(color, gradient)
                                    : normalColor,
                            skyAngle
                    );
                }
        );
    }

    private static Vec3 lerpFogDistance(
            Vec3 pos,
            Level level,
            Vec3 baseRadius,
            Function<WeatherEffectType, WeatherEffectType.WeatherData> fogDataSupplier
    ) {
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();

        // tri lerp fog distances to make less jarring biome transition
        // start is stored in X and end in Y
        return CubicSampler.gaussianSampleVec3(pos, (x, y, z) -> {
            samplePos.set(x, y, z);
            WeatherEffectType sampledType = WeatherEffectsClient.getCurrentType(level, samplePos, true);

            WeatherEffectType.WeatherData fogData = fogDataSupplier.apply(sampledType);
            if (fogData != null) {
                return fogData.fogDistance();
            }

            return baseRadius;
        });
    }

    private static void updateFogRadius(
            ViewportEvent.RenderFog event,
            Vec3 rainDistance,
            @Nullable Vec3 thunderDistance,
            float rainGradient,
            float thunderGradient,
            ImmersiveStormsConfig config
    ) {
        float fogStart = Mth.lerp(rainGradient, event.getNearPlaneDistance(), (float) rainDistance.x);
        float fogEnd = Mth.lerp(rainGradient, event.getFarPlaneDistance(), (float) rainDistance.y);

        if (thunderDistance != null) {
            fogStart = Mth.lerp(thunderGradient, fogStart, (float) thunderDistance.x);
            fogEnd = Mth.lerp(thunderGradient, fogEnd, (float) thunderDistance.y);
        }

        fogStart *= config.getFogDistanceMultiplier();
        fogEnd *= config.getFogDistanceMultiplier();

        event.setNearPlaneDistance(fogStart);
        event.setFarPlaneDistance(fogEnd);
        event.setCanceled(true);
    }

    private FogEffects() {

    }
}
