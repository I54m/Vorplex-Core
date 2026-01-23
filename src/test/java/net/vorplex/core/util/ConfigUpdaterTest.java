package net.vorplex.core.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUpdaterTest {

    @Test
    void mergeCopiesUserValuesButKeepsNewDefaults() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("database.host", "localhost");
        oldConfig.set("database.port", 3307);
        oldConfig.set("config-version", "0.9");

        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("database.host", "127.0.0.1");
        newConfig.set("database.port", 3306);
        newConfig.set("database.ssl", false);
        newConfig.set("config-version", "1.0");

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertEquals("localhost", newConfig.getString("database.host"));
        assertEquals(3307, newConfig.getInt("database.port"));
        assertFalse(newConfig.getBoolean("database.ssl")); // new option preserved
    }

    @Test
    void removedKeysAreNotCopied() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("old.setting", true);

        YamlConfiguration newConfig = new YamlConfiguration();
        // key intentionally missing

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertFalse(newConfig.contains("old.setting"));
    }

    @Test
    void configVersionIsNeverCopied() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("config-version", "0.5");

        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("config-version", "1.0");

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertEquals("1.0", newConfig.getString("config-version"));
    }

    @Test
    void nestedSectionsMergeCorrectly() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("features.auto-save.enabled", true);
        oldConfig.set("features.auto-save.interval", 10);

        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("features.auto-save.enabled", false);
        newConfig.set("features.auto-save.interval", 5);
        newConfig.set("features.auto-save.async", false);

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertTrue(newConfig.getBoolean("features.auto-save.enabled"));
        assertEquals(10, newConfig.getInt("features.auto-save.interval"));
        assertFalse(newConfig.getBoolean("features.auto-save.async"));
    }
}