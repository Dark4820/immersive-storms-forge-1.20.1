package com.thedeathlycow.immersive.storms.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thedeathlycow.immersive.storms.client.world.BlackRainEffect;
import com.thedeathlycow.immersive.storms.registry.ISParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Black rain for 1.20.1. Only chain-friendly injectors (@WrapOperation), so it coexists with Subtle Effects,
 * which wraps the same calls. Priority 1100 so this wrap is applied outside Subtle Effects' one.
 */
@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class LevelRendererMixin {
    @Unique
    private final float[] immersiveStorms$blackRainColor = new float[3];

    @Unique
    private long immersiveStorms$lastColumn = Long.MIN_VALUE;

    @Unique
    private boolean immersiveStorms$lastColumnBlack = false;

    /**
     * Tints rain columns in black rain biomes. In those columns the chain is not continued
     * (Subtle Effects would otherwise replace the color with the biome water color); everywhere else
     * the original call, including other mods' wrappers, runs untouched.
     */
    @WrapOperation(
            method = "renderSnowAndRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer immersiveStorms$colorBlackRain(
            VertexConsumer instance,
            float red, float green, float blue, float alpha,
            Operation<VertexConsumer> original,
            @Local Level level,
            @Local Biome.Precipitation precipitation,
            @Local BlockPos.MutableBlockPos pos
    ) {
        if (precipitation == Biome.Precipitation.RAIN && level != null) {
            // 4 vertices per column share the same position: evaluate the biome once per column
            long column = pos.asLong();
            if (column != this.immersiveStorms$lastColumn) {
                this.immersiveStorms$lastColumn = column;
                this.immersiveStorms$lastColumnBlack = BlackRainEffect.isBlackRain(level, pos);
                if (this.immersiveStorms$lastColumnBlack) {
                    BlackRainEffect.getColumnColor(level, pos, this.immersiveStorms$blackRainColor);
                }
            }

            if (this.immersiveStorms$lastColumnBlack) {
                float[] color = this.immersiveStorms$blackRainColor;
                return instance.color(color[0], color[1], color[2], alpha);
            }
        } else {
            this.immersiveStorms$lastColumn = Long.MIN_VALUE;
        }

        return original.call(instance, red, green, blue, alpha);
    }

    /**
     * Swaps the rain splash particle for a black one in black rain biomes. Always calls the original,
     * so other wrappers (e.g. Subtle Effects cauldron ripples) still run.
     */
    @WrapOperation(
            method = "tickRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void immersiveStorms$blackRainSplash(
            ClientLevel instance,
            ParticleOptions particle,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            Operation<Void> original,
            @Local(ordinal = 2) BlockPos pos
    ) {
        if (particle == ParticleTypes.RAIN && BlackRainEffect.isBlackRain(instance, pos)) {
            particle = ISParticleTypes.BLACK_RAIN.get();
        }

        original.call(instance, particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
