package com.thedeathlycow.immersive.storms.client.particle;

import com.thedeathlycow.immersive.storms.particle.DustGrainParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;

public class DustGrainParticle extends DustParticleBase<DustGrainParticleOptions> {
    public DustGrainParticle(
            ClientLevel level,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            DustGrainParticleOptions options,
            SpriteSet sprites
    ) {
        // DustParticleBase already applies the same random darkening (0.6-1.0) to the color as upstream
        super(level, x, y, z, velocityX, velocityY, velocityZ, options, sprites);
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.gravity = 1e-1f;
    }

    public static class Provider implements ParticleProvider<DustGrainParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(
                DustGrainParticleOptions options,
                ClientLevel level,
                double x, double y, double z,
                double velocityX, double velocityY, double velocityZ
        ) {
            return new DustGrainParticle(level, x, y, z, velocityX, velocityY, velocityZ, options, this.sprites);
        }
    }
}
