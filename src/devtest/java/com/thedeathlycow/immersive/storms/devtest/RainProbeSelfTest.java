package com.thedeathlycow.immersive.storms.devtest;

import com.mojang.datafixers.util.Pair;
import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * DEV ONLY (src/devtest, never packaged). Run with: {@code gradlew runClient -Pselftest=rain[:label]}
 * <p>
 * Loads the "Terralith" test world, moves the player to plains, sets daytime rain, logs which
 * DimensionSpecialEffects the overworld uses (and which mod it comes from) and what its rain/sky hooks return,
 * then takes two screenshots (horizon and sky) into run/screenshots and closes the game.
 */
@Mod.EventBusSubscriber(modid = ImmersiveStorms.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RainProbeSelfTest {
    private static final String WORLD = "Terralith";

    private enum Stage { WAIT_TITLE, LOADING, SETUP, SETTLE, SHOT_HORIZON, SHOT_SKY, GO_OTHER, SETTLE_OTHER, DONE }

    /** Other dimensions visited after the overworld, to check the effects guard leaves them alone. */
    private static final String[] OTHER_DIMENSIONS = {"mofus_better_end_:broken_constellation", "minecraft:the_end"};
    private static int otherIndex = 0;
    private static long[] guardBefore;

    private static Stage stage = Stage.WAIT_TITLE;
    private static int timer = 0;
    private static String label = "";
    private static final List<String> report = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || stage == Stage.DONE) {
            return;
        }
        String prop = System.getProperty("immersive_storms.selftest", "off");
        if (!prop.startsWith("rain")) {
            stage = Stage.DONE;
            return;
        }
        label = prop.contains(":") ? prop.substring(prop.indexOf(':') + 1) : "run";
        Minecraft mc = Minecraft.getInstance();
        try {
            tick(mc);
        } catch (Throwable t) {
            report.add("FAIL exception: " + t);
            ImmersiveStorms.LOGGER.error("[RAINPROBE] failed", t);
            finish(mc);
        }
    }

    private static void tick(Minecraft mc) {
        switch (stage) {
            case WAIT_TITLE -> {
                if (mc.screen instanceof TitleScreen title) {
                    setGuardOption(!label.contains("guardoff"));
                    // the game must keep ticking even if the window loses focus (otherwise tickRain stops while frames still render)
                    mc.options.pauseOnLostFocus = false;
                    stage = Stage.LOADING;
                    mc.createWorldOpenFlows().loadLevel(title, WORLD);
                }
            }
            case LOADING -> {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && mc.screen == null) {
                    stage = Stage.SETUP;
                }
            }
            case SETUP -> {
                setup(mc);
                stage = Stage.SETTLE;
                timer = 0;
            }
            case SETTLE -> {
                // let rain ramp up (rain level +0.01/tick) and chunks render
                if (++timer >= 260) {
                    probe(mc);
                    mc.options.hideGui = true;
                    look(mc.player, -90f, 5f); // east, roughly horizontal: rain and clouds
                    stage = Stage.SHOT_HORIZON;
                    timer = 0;
                }
            }
            case SHOT_HORIZON -> {
                if (++timer >= 20) {
                    shot(mc, "horizon");
                    look(mc.player, -90f, -55f); // up towards the morning sun: sky
                    stage = Stage.SHOT_SKY;
                    timer = 0;
                }
            }
            case SHOT_SKY -> {
                if (++timer >= 20) {
                    shot(mc, "sky");
                    stage = Stage.GO_OTHER;
                }
            }
            case GO_OTHER -> {
                if (otherIndex >= OTHER_DIMENSIONS.length || mc.getSingleplayerServer() == null) {
                    finish(mc);
                    return;
                }
                String dim = OTHER_DIMENSIONS[otherIndex];
                guardBefore = guardCounts();
                var server = mc.getSingleplayerServer();
                var source = server.createCommandSourceStack().withSuppressedOutput();
                run(server, source, "execute in " + dim + " run tp " + mc.player.getGameProfile().getName() + " 0.5 140 0.5 -90 5");
                stage = Stage.SETTLE_OTHER;
                timer = 0;
            }
            case SETTLE_OTHER -> {
                String target = OTHER_DIMENSIONS[otherIndex];
                if (mc.level == null || !mc.level.dimension().location().toString().equals(target)) {
                    // still in the previous dimension (frames after /tp): keep moving the baseline
                    guardBefore = guardCounts();
                    timer = 0;
                    return;
                }
                if (++timer == 20) {
                    // baseline only once the client has been in the target dimension for a while
                    guardBefore = guardCounts();
                }
                if (timer >= 180) {
                    String dim = OTHER_DIMENSIONS[otherIndex];
                    ClientLevel level = mc.level;
                    long[] after = guardCounts();
                    long delta = 0;
                    for (int i = 0; i < after.length; i++) {
                        delta += after[i] - guardBefore[i];
                    }
                    report.add("INFO other dimension " + dim + ": now in " + level.dimension().location()
                            + ", effects=" + level.effects().getClass().getName() + " (from mod: " + ownerMod(level.effects().getClass()) + ")"
                            + ", guard activations while here=" + delta);
                    report.add((delta == 0 ? "PASS" : "FAIL") + " guard did not act in " + dim);
                    shot(mc, dim.replace(':', '_'));
                    otherIndex++;
                    stage = Stage.GO_OTHER;
                }
            }
            default -> {
            }
        }
    }

    private static void setup(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        ServerLevel overworld = server.overworld();
        LocalPlayer player = mc.player;
        BlockPos origin = player.blockPosition();
        Pair<BlockPos, Holder<Biome>> found = overworld.findClosestBiome3d(
                h -> h.is(Biomes.PLAINS) || h.is(Biomes.SUNFLOWER_PLAINS), origin, 6400, 32, 64);
        BlockPos target = found != null ? found.getFirst() : origin;
        report.add("INFO biome target: " + (found != null ? found.getSecond().unwrapKey().map(ResourceKey::location).orElse(null) + " at " + target : "none, staying at " + origin));

        var source = server.createCommandSourceStack().withSuppressedOutput();
        String name = player.getGameProfile().getName();
        run(server, source, "gamemode spectator " + name);
        run(server, source, "gamerule doWeatherCycle false");
        run(server, source, "gamerule doDaylightCycle false");
        run(server, source, "time set 1500");
        run(server, source, "weather rain");
        // load the chunk, then put the player a few blocks above the surface
        overworld.getChunk(target);
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ()) + 6;
        run(server, source, "tp " + name + " " + target.getX() + " " + y + " " + target.getZ() + " -90 5");
    }

    private static void probe(Minecraft mc) {
        ClientLevel level = mc.level;
        DimensionSpecialEffects effects = level.effects();
        Camera camera = mc.gameRenderer.getMainCamera();
        report.add("INFO label: " + label);
        report.add("INFO dimension: " + level.dimension().location() + ", effectsLocation: " + level.dimensionType().effectsLocation());
        report.add("INFO effects class: " + effects.getClass().getName() + " (from mod: " + ownerMod(effects.getClass()) + ")");
        report.add("INFO vanilla OverworldEffects: " + (effects.getClass() == DimensionSpecialEffects.OverworldEffects.class));
        report.add("INFO cloudHeight=" + effects.getCloudHeight() + " hasGround=" + effects.hasGround()
                + " skyType=" + effects.skyType() + " forceBrightLightmap=" + effects.forceBrightLightmap()
                + " constantAmbientLight=" + effects.constantAmbientLight());
        report.add("INFO rainLevel=" + level.getRainLevel(1f) + " isRaining=" + level.isRaining() + " player at " + mc.player.blockPosition()
                + " biome=" + level.getBiome(mc.player.blockPosition()).unwrapKey().map(ResourceKey::location).orElse(null));
        Vec3Pos cam = new Vec3Pos(camera);
        boolean rainHandled = effects.renderSnowAndRain(level, 0, 0f, mc.gameRenderer.lightTexture(), cam.x, cam.y, cam.z);
        boolean tickHandled = effects.tickRain(level, 0, camera);
        report.add("INFO effects.renderSnowAndRain() returned " + rainHandled + (rainHandled ? " -> vanilla rain is SKIPPED" : " -> vanilla rain is drawn"));
        report.add("INFO effects.tickRain() returned " + tickHandled + (tickHandled ? " -> vanilla splashes/rain sound SKIPPED" : " -> vanilla splashes/rain sound run"));
        dumpCustomSets(effects);
        long[] g = guardCounts();
        report.add("INFO restore option=" + com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient.getConfig().isRestoreVanillaOverworldWeatherAndSky()
                + " guard activations: rain=" + g[0] + " rainTick=" + g[1] + " clouds=" + g[2] + " sky=" + g[3]);
        for (var hook : com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard.Hook.values()) {
            report.add("INFO   hook " + hook + ": calls=" + com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard.getCalls(hook)
                    + " handledInOverworld=" + com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard.getHandledInOverworld(hook)
                    + " activations=" + com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard.getActivations(hook));
        }
    }

    private static long[] guardCounts() {
        var hooks = com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard.Hook.values();
        long[] out = new long[hooks.length];
        for (int i = 0; i < hooks.length; i++) {
            out[i] = com.thedeathlycow.immersive.storms.client.world.OverworldEffectsGuard.getActivations(hooks[i]);
        }
        return out;
    }

    /** Sets the restore option in memory only (the config file is not written). */
    private static void setGuardOption(boolean value) {
        try {
            Object config = com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient.getConfig();
            java.lang.reflect.Field f = config.getClass().getDeclaredField("restoreVanillaOverworldWeatherAndSky");
            f.setAccessible(true);
            f.setBoolean(config, value);
            report.add("INFO restore option set to " + value + " for this run (in memory only)");
        } catch (ReflectiveOperationException e) {
            report.add("WARN could not set the restore option: " + e);
        }
    }

    /**
     * MCreator-style effects keep static sets of hooks (CUSTOM_WEATHER, CUSTOM_SKY, ...): list who filled them.
     */
    private static void dumpCustomSets(DimensionSpecialEffects effects) {
        for (Class<?> c = effects.getClass(); c != null && c != DimensionSpecialEffects.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !java.util.Collection.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    java.util.Collection<?> set = (java.util.Collection<?>) f.get(null);
                    report.add("INFO " + c.getSimpleName() + "." + f.getName() + " size=" + (set == null ? "null" : set.size()));
                    if (set != null) {
                        for (Object hook : set) {
                            report.add("INFO   - " + hook.getClass().getName() + " (from mod: " + ownerMod(hook.getClass()) + ")");
                        }
                    }
                } catch (Exception e) {
                    report.add("WARN could not read " + f.getName() + ": " + e);
                }
            }
        }
    }

    private record Vec3Pos(double x, double y, double z) {
        Vec3Pos(Camera c) {
            this(c.getPosition().x, c.getPosition().y, c.getPosition().z);
        }
    }

    private static String ownerMod(Class<?> type) {
        String path = type.getName().replace('.', '/') + ".class";
        if (type.getName().startsWith("net.minecraft.")) {
            return "minecraft";
        }
        for (IModInfo info : ModList.get().getMods()) {
            try {
                if (Files.exists(info.getOwningFile().getFile().findResource(path))) {
                    return info.getModId();
                }
            } catch (Exception ignored) {
            }
        }
        return "?";
    }

    private static void look(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    private static void shot(Minecraft mc, String what) {
        String file = "rainprobe_" + label + "_" + what + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> {
        });
        report.add("INFO screenshot: screenshots/" + file);
    }

    private static void run(MinecraftServer server, net.minecraft.commands.CommandSourceStack source, String command) {
        server.getCommands().performPrefixedCommand(source, command);
    }

    private static void finish(Minecraft mc) {
        stage = Stage.DONE;
        for (String line : report) {
            ImmersiveStorms.LOGGER.info("[RAINPROBE] {}", line);
        }
        try {
            Path out = mc.gameDirectory.toPath().resolve("selftest").resolve("rain-probe-" + label + ".txt");
            Files.createDirectories(out.getParent());
            Files.write(out, report, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ImmersiveStorms.LOGGER.error("[RAINPROBE] could not write result", e);
        }
        mc.options.hideGui = false;
        mc.stop();
    }

    private RainProbeSelfTest() {

    }
}
