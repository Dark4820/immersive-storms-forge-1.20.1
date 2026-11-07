package com.thedeathlycow.immersive.storms.client;

import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import com.thedeathlycow.immersive.storms.client.particle.BlackWaterDropParticle;
import com.thedeathlycow.immersive.storms.client.particle.DustGrainParticle;
import com.thedeathlycow.immersive.storms.client.world.BiomeWindEffects;
import com.thedeathlycow.immersive.storms.client.world.FogEffects;
import com.thedeathlycow.immersive.storms.client.world.SandstormParticles;
import com.thedeathlycow.immersive.storms.client.world.SandstormSounds;
import com.thedeathlycow.immersive.storms.config.ImmersiveStormsConfig;
import com.thedeathlycow.immersive.storms.registry.ISParticleTypes;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client entrypoint. Replaces the Fabric ClientModInitializer: everything here runs only on the physical client.
 */
public final class ImmersiveStormsClient {
    private static ConfigHolder<ImmersiveStormsConfig> configHolder = null;

    private static final List<Consumer<ClientLevel>> LEVEL_TICK_HANDLERS = new ArrayList<>();

    public static void init(ModLoadingContext context, IEventBus modBus) {
        registerConfig();

        // Config button in the Forge mod list (replaces the Mod Menu integration)
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> AutoConfig.getConfigScreen(ImmersiveStormsConfig.class, parent).get()
                )
        );

        boolean disableSandstormEffects = getConfig().getSandstorm().isDetectParticleRain()
                && ModList.get().isLoaded("particlerain");

        if (!disableSandstormEffects) {
            LEVEL_TICK_HANDLERS.add(new SandstormParticles());
            LEVEL_TICK_HANDLERS.add(new SandstormSounds());
        } else {
            ImmersiveStorms.LOGGER.info("Particle Rain has been detected, disabling Immersive Storms sandstorm particle and sound effects");
        }

        LEVEL_TICK_HANDLERS.add(new BiomeWindEffects());

        modBus.addListener(ImmersiveStormsClient::registerParticleProviders);

        MinecraftForge.EVENT_BUS.addListener(ImmersiveStormsClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ImmersiveStormsClient::onComputeFogColor);
        MinecraftForge.EVENT_BUS.addListener(ImmersiveStormsClient::onRenderFog);
    }

    public static ImmersiveStormsConfig getConfig() {
        if (configHolder == null) {
            registerConfig();
        }

        return configHolder.getConfig();
    }

    private static void registerConfig() {
        if (configHolder == null) {
            configHolder = AutoConfig.register(ImmersiveStormsConfig.class, JanksonConfigSerializer::new);
        }
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ISParticleTypes.DUST_GRAIN.get(), DustGrainParticle.Provider::new);
        event.registerSpriteSet(ISParticleTypes.BLACK_RAIN.get(), BlackWaterDropParticle.Provider::new);
    }

    /**
     * Equivalent of Fabric's ClientTickEvents.END_WORLD_TICK: only while a level is loaded and the game is not paused.
     */
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.isPaused()) {
            return;
        }

        for (Consumer<ClientLevel> handler : LEVEL_TICK_HANDLERS) {
            handler.accept(level);
        }
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        FogEffects.onComputeFogColor(event, getConfig());
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        FogEffects.onRenderFog(event, getConfig());
    }

    private ImmersiveStormsClient() {

    }
}
