package net.vorplex.core.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUpdaterTest {

    @Test
    void mergeConfigs_UserValues_ShouldOverwriteNewDefaults() {
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
    void mergeConfigs_RemovedKeys_ShouldNotBeCopied() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("old.setting", true);

        YamlConfiguration newConfig = new YamlConfiguration();
        // key intentionally missing

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertFalse(newConfig.contains("old.setting"));
    }

    @Test
    void mergeConfigs_ConfigVersion_ShouldKeepNewVersion() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("config-version", "0.5");

        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("config-version", "1.0");

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertEquals("1.0", newConfig.getString("config-version"));
    }

    @Test
    void mergeConfigs_NestedSections_ShouldMergeCorrectly() {
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


    @Test
    void mergeConfigs_SectionsWithCustomKeys_ShouldAlwaysCopyOld() {
        YamlConfiguration oldConfig = new YamlConfiguration();
        oldConfig.set("JoinMessages.PermissionJoinMessages.enabled", true);
        oldConfig.set("JoinMessages.PermissionJoinMessages.messages.customvalue1", "oldvalue1");
        oldConfig.set("JoinMessages.PermissionJoinMessages.messages.customvalue2", "oldvalue2");
        oldConfig.set("JoinMessages.PermissionJoinMessages.messages.customvalue3", "oldvalue3");

        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.set("JoinMessages.PermissionJoinMessages.enabled", true);
        newConfig.set("JoinMessages.PermissionJoinMessages.messages.value1", "newvalue1");
        newConfig.set("JoinMessages.PermissionJoinMessages.messages.value2", "newvalue2");
        newConfig.set("JoinMessages.PermissionJoinMessages.messages.value3", "newvalue3");

        ConfigUpdater.mergeConfigs(oldConfig, newConfig, "");

        assertTrue(newConfig.getBoolean("JoinMessages.PermissionJoinMessages.enabled"));
        assertEquals("oldvalue1", newConfig.getString("JoinMessages.PermissionJoinMessages.messages.customvalue1"));
        assertEquals("oldvalue2", newConfig.getString("JoinMessages.PermissionJoinMessages.messages.customvalue2"));
        assertEquals("oldvalue3", newConfig.getString("JoinMessages.PermissionJoinMessages.messages.customvalue3"));
        //Assert default/new keys do not get copied into config
        assertNull(newConfig.getString("JoinMessages.PermissionJoinMessages.messages.value1"));
        assertNull(newConfig.getString("JoinMessages.PermissionJoinMessages.messages.value2"));
        assertNull(newConfig.getString("JoinMessages.PermissionJoinMessages.messages.value3"));
    }
}