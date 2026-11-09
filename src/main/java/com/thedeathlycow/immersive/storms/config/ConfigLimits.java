package com.thedeathlycow.immersive.storms.config;

/**
 * Port addition: upper caps for config values, so a config value cannot flood the particle engine.
 * <p>
 * These must NOT live in a {@link me.shedaniel.autoconfig.ConfigData} class: AutoConfig's GUI turns every
 * declared field into an entry and tries to write it back on save, which fails for static final fields.
 * <p>
 * Sandstorm worst case per client tick: (2 * 64)^2 columns * (1/60) * 4 ~= 1092 spawn attempts, vs ~27 with defaults.
 */
public final class ConfigLimits {
    public static final int MAX_SANDSTORM_RENDER_DISTANCE = 64;
    public static final float MAX_SANDSTORM_DENSITY_MULTIPLIER = 4.0f;

    private ConfigLimits() {

    }
}
