package com.thedeathlycow.immersive.storms.registry;

import com.mojang.serialization.Codec;
import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import com.thedeathlycow.immersive.storms.particle.DustGrainParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ISParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ImmersiveStorms.MOD_ID);

    public static final RegistryObject<ParticleType<DustGrainParticleOptions>> DUST_GRAIN = PARTICLE_TYPES.register(
            "dust_grain",
            () -> new ParticleType<DustGrainParticleOptions>(false, DustGrainParticleOptions.DESERIALIZER) {
                @Override
                public Codec<DustGrainParticleOptions> codec() {
                    return DustGrainParticleOptions.CODEC;
                }
            }
    );

    private ISParticleTypes() {

    }
}
