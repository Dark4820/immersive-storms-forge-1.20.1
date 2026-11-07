package com.thedeathlycow.immersive.storms;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ImmersiveStorms.MOD_ID)
public class ImmersiveStorms {
    public static final String MOD_ID = "immersive_storms";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ImmersiveStorms() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
