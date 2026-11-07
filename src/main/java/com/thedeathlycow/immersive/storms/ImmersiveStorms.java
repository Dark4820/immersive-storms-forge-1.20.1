package com.thedeathlycow.immersive.storms;

import com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient;
import com.thedeathlycow.immersive.storms.registry.ISParticleTypes;
import com.thedeathlycow.immersive.storms.registry.ISSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ImmersiveStorms.MOD_ID)
public class ImmersiveStorms {
    public static final String MOD_ID = "immersive_storms";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ImmersiveStorms(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ISParticleTypes.PARTICLE_TYPES.register(modBus);
        ISSoundEvents.SOUND_EVENTS.register(modBus);

        // Everything else in this mod is client-only (no server tick logic)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ImmersiveStormsClient.init(modBus));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
