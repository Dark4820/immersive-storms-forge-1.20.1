package com.thedeathlycow.immersive.storms.client.particle;

import com.thedeathlycow.immersive.storms.client.world.BlackRainEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Black rain splash, brought over from Immersive Storms 2.0.
 */
public class BlackWaterDropParticle extends WaterDropParticle {
    public BlackWaterDropParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.rCol = BlackRainEffect.COLOR_FLOAT;
        this.gCol = BlackRainEffect.COLOR_FLOAT;
        this.bCol = BlackRainEffect.COLOR_FLOAT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed
        ) {
            BlackWaterDropParticle particle = new BlackWaterDropParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
