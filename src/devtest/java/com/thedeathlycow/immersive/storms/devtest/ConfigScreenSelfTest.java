package com.thedeathlycow.immersive.storms.devtest;

import com.thedeathlycow.immersive.storms.ImmersiveStorms;
import com.thedeathlycow.immersive.storms.client.ImmersiveStormsClient;
import com.thedeathlycow.immersive.storms.client.world.BlackRainEffect;
import com.thedeathlycow.immersive.storms.config.ImmersiveStormsConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DEV ONLY (src/devtest, never packaged). Run with: {@code gradlew runClient -Pselftest=config}
 * <p>
 * Opens the real Cloth Config screen of the mod, changes one option in each category (root + sandstorm
 * sub-category), saves through the screen like the "Save" button does, and checks that:
 * <ul>
 *     <li>saving does not throw (regression: static final fields made AutoConfig crash on save);</li>
 *     <li>disabling black rain is applied in game (config object and BlackRainEffect);</li>
 *     <li>the values are written to config/immersive_storms.json5 and survive a reload from disk.</li>
 * </ul>
 * The original config file is restored afterwards. Results go to the log and to run/selftest/config-screen.txt,
 * then the game closes.
 */
@Mod.EventBusSubscriber(modid = ImmersiveStorms.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConfigScreenSelfTest {
    private static final String KEY = "text.autoconfig." + ImmersiveStorms.MOD_ID + ".option.";
    private static boolean done = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!"config".equals(System.getProperty("immersive_storms.selftest"))) {
            done = true;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen title)) {
            return; // wait until the game has fully started
        }
        done = true;

        List<String> report = new ArrayList<>();
        boolean passed;
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(ImmersiveStorms.MOD_ID + ".json5");
        String backup = null;
        try {
            backup = Files.exists(configFile) ? Files.readString(configFile, StandardCharsets.UTF_8) : null;
            passed = run(mc, title, configFile, report);
        } catch (Throwable t) {
            passed = false;
            report.add("FAIL unexpected exception: " + t);
            ImmersiveStorms.LOGGER.error("[SELFTEST] config screen self test threw", t);
        } finally {
            restore(configFile, backup, report);
        }

        report.add(0, passed ? "RESULT: PASS" : "RESULT: FAIL");
        for (String line : report) {
            ImmersiveStorms.LOGGER.info("[SELFTEST] {}", line);
        }
        try {
            Path out = mc.gameDirectory.toPath().resolve("selftest").resolve("config-screen.txt");
            Files.createDirectories(out.getParent());
            Files.write(out, report, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ImmersiveStorms.LOGGER.error("[SELFTEST] could not write result file", e);
        }
        mc.stop();
    }

    private static boolean run(Minecraft mc, TitleScreen title, Path configFile, List<String> report) throws IOException {
        boolean ok = true;
        ImmersiveStormsConfig config = ImmersiveStormsClient.getConfig();
        ok &= check(report, config.isEnableBlackRain(), "precondition: black rain enabled before the test");

        // Open the real config screen exactly like the mod list does
        Screen screen = AutoConfig.getConfigScreen(ImmersiveStormsConfig.class, title).get();
        if (!(screen instanceof AbstractConfigScreen configScreen)) {
            report.add("FAIL config screen is " + screen.getClass().getName() + ", expected a Cloth Config screen");
            return false;
        }
        mc.setScreen(configScreen);

        Map<String, AbstractConfigEntry<?>> entries = new LinkedHashMap<>();
        Map<String, String> categoryOf = new LinkedHashMap<>();
        for (Map.Entry<Component, List<AbstractConfigEntry<?>>> category : configScreen.getCategorizedEntries().entrySet()) {
            for (AbstractConfigEntry<?> entry : category.getValue()) {
                collect(entry, category.getKey().getString(), entries, categoryOf);
            }
        }
        report.add("INFO found " + entries.size() + " entries: " + entries.keySet());

        // Every option of both config classes must be on screen, and nothing else (no constants)
        String[] expected = {
                "enableFogChanges", "enableSandstormFog", "enableBlizzardFog", "enableDenseFog",
                "enableAmbientWindParticles", "enableAmbientWindSounds", "enableBlackRain",
                "fogDistanceMultiplier", "windParticleChanceMultiplier", "sandstorm",
                "sandstorm.enableSandstormParticles", "sandstorm.enableSandstormSounds", "sandstorm.detectParticleRain",
                "sandstorm.sandstormParticleRenderDistance", "sandstorm.sandstormParticleDensityMultiplier"
        };
        ok &= check(report, entries.keySet().equals(new java.util.LinkedHashSet<>(List.of(expected))),
                "screen shows exactly the expected options");

        // Root category: one boolean (black rain off) and one number
        ok &= toggle(report, entries.get("enableBlackRain"), "enableBlackRain");
        ok &= setText(report, entries.get("windParticleChanceMultiplier"), "1.5", "windParticleChanceMultiplier");
        // Sandstorm sub-category: one boolean and one number
        ok &= toggle(report, entries.get("sandstorm.enableSandstormSounds"), "sandstorm.enableSandstormSounds");
        ok &= setText(report, entries.get("sandstorm.sandstormParticleRenderDistance"), "32", "sandstorm.sandstormParticleRenderDistance");

        ok &= check(report, configScreen.isEdited(), "screen reports unsaved edits");

        // Same call as the "Save" / "Save & Quit" button
        try {
            configScreen.saveAll(false);
            report.add("PASS saveAll() did not throw");
        } catch (Throwable t) {
            report.add("FAIL saveAll() threw: " + t);
            ImmersiveStorms.LOGGER.error("[SELFTEST] saveAll failed", t);
            return false;
        }
        mc.setScreen(title);

        // Applied in game
        config = ImmersiveStormsClient.getConfig();
        ok &= check(report, !config.isEnableBlackRain(), "in game: config.isEnableBlackRain() == false");
        // With black rain disabled BlackRainEffect returns before touching the level/biome
        ok &= check(report, !BlackRainEffect.isBlackRain(null, null), "in game: BlackRainEffect.isBlackRain() == false");
        ok &= check(report, Math.abs(config.getWindParticleChanceMultiplier() - 1.5f) < 1e-6, "in game: wind multiplier == 1.5");
        ok &= check(report, !config.getSandstorm().isEnableSandstormSounds(), "in game: sandstorm sounds == false");
        ok &= check(report, config.getSandstorm().getSandstormParticleRenderDistance() == 32, "in game: sandstorm render distance == 32");

        // Written to the file
        String json = Files.readString(configFile, StandardCharsets.UTF_8);
        ok &= check(report, json.contains("\"enableBlackRain\": false"), "file: \"enableBlackRain\": false");
        ok &= check(report, json.contains("\"windParticleChanceMultiplier\": 1.5"), "file: \"windParticleChanceMultiplier\": 1.5");
        ok &= check(report, json.contains("\"enableSandstormSounds\": false"), "file: \"enableSandstormSounds\": false");
        ok &= check(report, json.contains("\"sandstormParticleRenderDistance\": 32"), "file: \"sandstormParticleRenderDistance\": 32");
        ok &= check(report, !json.contains("MAX_"), "file: no constants serialized");

        // Survives a reload from disk
        ConfigHolder<ImmersiveStormsConfig> holder = AutoConfig.getConfigHolder(ImmersiveStormsConfig.class);
        ok &= check(report, holder.load(), "reload from disk succeeded");
        ok &= check(report, !holder.getConfig().isEnableBlackRain(), "after reload: black rain still disabled");
        ok &= check(report, holder.getConfig().getSandstorm().getSandstormParticleRenderDistance() == 32, "after reload: render distance still 32");
        return ok;
    }

    private static void collect(AbstractConfigEntry<?> entry, String category,
                                Map<String, AbstractConfigEntry<?>> out, Map<String, String> categoryOf) {
        String name = optionName(entry.getFieldName());
        out.put(name, entry);
        categoryOf.put(name, category);
        if (entry instanceof SubCategoryListEntry sub) {
            for (Object child : sub.getValue()) {
                collect((AbstractConfigEntry<?>) child, category + " > " + name, out, categoryOf);
            }
        }
    }

    private static String optionName(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(KEY)) {
            return translatable.getKey().substring(KEY.length());
        }
        return "?" + component.getString();
    }

    private static boolean toggle(List<String> report, AbstractConfigEntry<?> entry, String name) {
        if (!(entry instanceof BooleanListEntry bool)) {
            report.add("FAIL " + name + " is not a boolean entry: " + entry);
            return false;
        }
        boolean before = bool.getValue();
        // Press the toggle button (not the reset button), like a player click
        for (GuiEventListener child : bool.children()) {
            if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents t
                    && t.getKey().contains("reset")) {
                continue;
            }
            if (child instanceof Button button) {
                button.onPress();
                break;
            }
        }
        return check(report, bool.getValue() == !before, "screen: toggled " + name + " " + before + " -> " + bool.getValue());
    }

    private static boolean setText(List<String> report, AbstractConfigEntry<?> entry, String value, String name) {
        if (!(entry instanceof TextFieldListEntry<?> text)) {
            report.add("FAIL " + name + " is not a text entry: " + entry);
            return false;
        }
        text.setValue(value);
        return check(report, text.isEdited() && String.valueOf(text.getValue()).startsWith(value.replace(".0", "")),
                "screen: set " + name + " = " + text.getValue());
    }

    private static boolean check(List<String> report, boolean condition, String what) {
        report.add((condition ? "PASS " : "FAIL ") + what);
        return condition;
    }

    private static void restore(Path configFile, String backup, List<String> report) {
        try {
            if (backup != null) {
                Files.writeString(configFile, backup, StandardCharsets.UTF_8);
                AutoConfig.getConfigHolder(ImmersiveStormsConfig.class).load();
                report.add("INFO original config file restored");
            }
        } catch (Throwable t) {
            report.add("WARN could not restore the config file: " + t);
        }
    }

    private ConfigScreenSelfTest() {

    }
}
