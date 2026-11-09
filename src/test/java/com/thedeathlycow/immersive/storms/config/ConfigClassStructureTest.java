package com.thedeathlycow.immersive.storms.config;

import me.shedaniel.autoconfig.ConfigData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the "IllegalAccessException: Can not set static final ..." crash when saving the
 * config screen: AutoConfig's GUI makes an entry for every declared field of a ConfigData class (and of
 * nested objects) and writes all of them back on save. Such classes must only contain plain, writable
 * instance fields.
 */
class ConfigClassStructureTest {
    @Test
    void configClassesOnlyHaveWritableInstanceFields() {
        List<String> problems = new ArrayList<>();
        for (Class<?> type : configClasses()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                    problems.add(type.getSimpleName() + "." + field.getName() + " is " + Modifier.toString(mod));
                }
            }
        }
        assertEquals(List.of(), problems, "Config classes must not contain static or final fields");
    }

    @Test
    void walksNestedSandstormConfig() {
        Set<Class<?>> classes = configClasses();
        assertTrue(classes.contains(ImmersiveStormsConfig.class));
        assertTrue(classes.contains(SandstormConfig.class));
    }

    /**
     * The root config class plus every nested object field it contains (what AutoConfig shows as sub-categories).
     */
    private static Set<Class<?>> configClasses() {
        Set<Class<?>> out = new LinkedHashSet<>();
        collect(ImmersiveStormsConfig.class, out);
        return out;
    }

    private static void collect(Class<?> type, Set<Class<?>> out) {
        if (!out.add(type)) {
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (ConfigData.class.isAssignableFrom(fieldType)) {
                collect(fieldType, out);
            }
        }
    }
}
