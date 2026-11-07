package com.thedeathlycow.immersive.storms.registry;

import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ISSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ImmersiveStorms.MOD_ID);

    public static final RegistryObject<SoundEvent> WEATHER_STRONG_WIND = register("weather.strong_wind");

    /**
     * Port addition: ambient wind sound (replaces the vanilla 1.21.5 BLOCK_SAND_WIND sound, which does not exist in 1.20.1).
     */
    public static final RegistryObject<SoundEvent> WEATHER_SOFT_WIND = register("weather.soft_wind");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ImmersiveStorms.id(name)));
    }

    private ISSoundEvents() {

    }
}
