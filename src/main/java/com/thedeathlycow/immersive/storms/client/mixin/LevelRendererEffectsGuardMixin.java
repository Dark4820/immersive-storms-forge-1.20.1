package com.thedeathlycow.immersive.storms.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wraps the four Forge dimension-effects hooks called from LevelRenderer. The original hook is always called;
 * see {@link OverworldEffectsGuard} for when its result is overridden. {@code require = 0}: if another mod changed
 * one of these spots, that hook is skipped (and reported in the log) instead of crashing the game.
 */
@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class LevelRendererEffectsGuardMixin {
    @WrapOperation(
            method = "renderSnowAndRain",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;renderSnowAndRain(Lnet/minecraft/client/multiplayer/ClientLevel;IFLnet/minecraft/client/renderer/LightTexture;DDD)Z"),
            require = 0
    )
    private boolean immersiveStorms$guardRain(
            DimensionSpecialEffects effects, ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
            double camX, double camY, double camZ, Operation<Boolean> original
    ) {
        boolean handled = original.call(effects, level, ticks, partialTick, lightTexture, camX, camY, camZ);
        return OverworldEffectsGuard.filter(OverworldEffectsGuard.Hook.RAIN, effects, level, handled);
    }

    @WrapOperation(
            method = "tickRain",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;tickRain(Lnet/minecraft/client/multiplayer/ClientLevel;ILnet/minecraft/client/Camera;)Z"),
            require = 0
    )
    private boolean immersiveStorms$guardRainTick(
            DimensionSpecialEffects effects, ClientLevel level, int ticks, Camera camera, Operation<Boolean> original
    ) {
        boolean handled = original.call(effects, level, ticks, camera);
        return OverworldEffectsGuard.filter(OverworldEffectsGuard.Hook.RAIN_TICK, effects, level, handled);
    }

    @WrapOperation(
            method = "renderClouds",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;renderClouds(Lnet/minecraft/client/multiplayer/ClientLevel;IFLcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)Z"),
            require = 0
    )
    private boolean immersiveStorms$guardClouds(
            DimensionSpecialEffects effects, ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            double camX, double camY, double camZ, Matrix4f projection, Operation<Boolean> original
    ) {
        boolean handled = original.call(effects, level, ticks, partialTick, poseStack, camX, camY, camZ, projection);
        return OverworldEffectsGuard.filter(OverworldEffectsGuard.Hook.CLOUDS, effects, level, handled);
    }

    @WrapOperation(
            method = "renderSky",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;renderSky(Lnet/minecraft/client/multiplayer/ClientLevel;IFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;Lorg/joml/Matrix4f;ZLjava/lang/Runnable;)Z"),
            require = 0
    )
    private boolean immersiveStorms$guardSky(
            DimensionSpecialEffects effects, ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            Camera camera, Matrix4f projection, boolean isFoggy, Runnable setupFog, Operation<Boolean> original
    ) {
        boolean handled = original.call(effects, level, ticks, partialTick, poseStack, camera, projection, isFoggy, setupFog);
        return OverworldEffectsGuard.filter(OverworldEffectsGuard.Hook.SKY, effects, level, handled);
    }
}
