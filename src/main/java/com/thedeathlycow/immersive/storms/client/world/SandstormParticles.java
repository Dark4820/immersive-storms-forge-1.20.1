package com.thedeathlycow.immersive.storms.client.world;

import com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient;
import com.thedeathlycow.immersive.storms.config.ImmersiveStormsConfig;
import com.thedeathlycow.immersive.storms.config.SandstormConfig;
import com.thedeathlycow.immersive.storms.particle.DustGrainParticleOptions;
import com.thedeathlycow.immersive.storms.registry.ISBiomeTags;
import com.thedeathlycow.immersive.storms.util.WeatherEffectType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.function.Consumer;

public final class SandstormParticles implements Consumer<ClientLevel> {
    public static final Vector3f COLOR = Objects.requireNonNull(WeatherEffectType.SANDSTORM.getColor()).toVector3f();

    private static final float PARTICLE_SCALE = 10f;
    private static final float PARTICLE_VELOCITY = -1f;
    private static final float BASE_PARTICLE_CHANCE = 1f / 60f;

    @Override
    public void accept(ClientLevel level) {
        if (!level.isRaining()) {
            return;
        }

        ImmersiveStormsConfig config = ImmersiveStormsClient.getConfig();
        SandstormConfig sandstormConfig = config.getSandstorm();
        final int renderDistance = sandstormConfig.getSandstormParticleRenderDistance();
        boolean enabled = sandstormConfig.isEnableSandstormParticles()
                && config.isEnabled(WeatherEffectType.SANDSTORM)
                && renderDistance > 0;

        if (!enabled) {
            return;
        }

        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return; // no camera for whatever reason
        }

        // main particle loop
        final BlockPos cameraPos = camera.getBlockPosition();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final ParticleOptions particle = new DustGrainParticleOptions(COLOR, PARTICLE_SCALE);
        final float rarity = BASE_PARTICLE_CHANCE * sandstormConfig.getSandstormParticleDensityMultiplier();
        final int cameraY = cameraPos.getY();
        final int xOffset = renderDistance / 2;

        for (int x = cameraPos.getX() - renderDistance; x < cameraPos.getX() + renderDistance; x++) {
            for (int z = cameraPos.getZ() - renderDistance; z < cameraPos.getZ() + renderDistance; z++) {
                // adjust to account for the fact that particles travel along the x-axis
                // makes the area the particles come from look less empty
                int adjustedX = x + xOffset;

                int y = cameraY + level.random.nextIntBetweenInclusive(-renderDistance / 2, (renderDistance + 1) / 2);
                y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING, adjustedX, z));

                pos.set(adjustedX, y, z);
                addParticle(level, particle, pos, rarity);
            }
        }
    }

    private static void addParticle(ClientLevel level, ParticleOptions particle, BlockPos pos, float rarity) {
        boolean addParticle = level.random.nextFloat() < rarity
                && !level.isRainingAt(pos) // for compatibility with seasons mods
                && level.getBiome(pos).is(ISBiomeTags.HAS_SANDSTORMS); // faster than checking the weather effects api

        if (addParticle) {
            level.addParticle(
                    particle,
                    pos.getX() + level.random.nextDouble(),
                    pos.getY() + level.random.nextDouble(),
                    pos.getZ() + level.random.nextDouble(),
                    PARTICLE_VELOCITY, 0, 0
            );
        }
    }
}
