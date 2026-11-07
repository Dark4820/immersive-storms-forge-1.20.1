package com.thedeathlycow.immersive.storms.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thedeathlycow.immersive.storms.registry.ISParticleTypes;
import net.minecraft.core.particles.DustParticleOptionsBase;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

/**
 * Port of DustGrainParticleEffect (1.21.5 AbstractDustParticleEffect -> 1.20.1 DustParticleOptionsBase).
 */
public class DustGrainParticleOptions extends DustParticleOptionsBase {
    public static final Codec<DustGrainParticleOptions> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            ExtraCodecs.VECTOR3F
                                    .fieldOf("color")
                                    .forGetter(DustGrainParticleOptions::getColor),
                            Codec.FLOAT
                                    .fieldOf("scale")
                                    .forGetter(DustGrainParticleOptions::getScale)
                    )
                    .apply(instance, DustGrainParticleOptions::new)
    );

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<DustGrainParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public DustGrainParticleOptions fromCommand(ParticleType<DustGrainParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            Vector3f color = DustParticleOptionsBase.readVector3f(reader);
            reader.expect(' ');
            float scale = reader.readFloat();
            return new DustGrainParticleOptions(color, scale);
        }

        @Override
        public DustGrainParticleOptions fromNetwork(ParticleType<DustGrainParticleOptions> type, FriendlyByteBuf buf) {
            return new DustGrainParticleOptions(DustParticleOptionsBase.readVector3f(buf), buf.readFloat());
        }
    };

    public DustGrainParticleOptions(Vector3f color, float scale) {
        super(color, scale);
    }

    public DustGrainParticleOptions(int color, float scale) {
        this(new Vector3f(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f
        ), scale);
    }

    @Override
    public ParticleType<DustGrainParticleOptions> getType() {
        return ISParticleTypes.DUST_GRAIN.get();
    }
}
