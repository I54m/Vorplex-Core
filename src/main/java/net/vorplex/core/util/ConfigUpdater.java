package net.vorplex.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.vorplex.core.VorplexCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;

/**
 * Class to handle all configuration updates and versioning
 */
public class ConfigUpdater {

    private static final VorplexCore plugin = VorplexCore.getInstance();
    /**
     * Hard-coded current config version used in this version of the plugin
     */
    private static final String CURRENT_CONFIG_VERSION = "1.1";

    /**
     * Main usage for ConfigUpdater.
     * Checks if the config is outdated and then updates it if it is
     */
    public static void checkAndUpdate() {
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.getComponentLogger().info(Component.text("[ConfigUpdater] No config.yml detected, saving default...").color(NamedTextColor.GREEN));
            plugin.saveResource("config.yml", false);
            plugin.reloadConfig();
            return;
        }
        plugin.getComponentLogger().info(Component.text("[ConfigUpdater] Checking config version...").color(NamedTextColor.GREEN));
        if (isConfigOutDated()) {
            plugin.getComponentLogger().info(Component.text("[ConfigUpdater] Config is out of date, updating...").color(NamedTextColor.YELLOW));
            try {
                updateConfig();
            } catch (Exception e) {
                plugin.getComponentLogger().error("[ConfigUpdater] An error occurred while trying to update config.yml: ");
                plugin.getComponentLogger().error(e.getMessage());
            }
            plugin.reloadConfig();
        } else
            plugin.getComponentLogger().info(Component.text("[ConfigUpdater] Config is up-to-date!").color(NamedTextColor.GREEN));
    }

    /**
     * Handles the renaming of old config, creation of new config and merging of old config
     *
     * @throws IllegalStateException if the current config could not be renamed
     */
    private static void updateConfig() throws IllegalStateException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        File oldConfigFile = new File(plugin.getDataFolder(), "old-config.yml");

        // Overwrite old-config.yml if it exists
        if (oldConfigFile.exists())
            oldConfigFile.delete();

        // Rename current config - throw error on failure
        if (!configFile.renameTo(oldConfigFile))
            throw new IllegalStateException("Failed to rename config.yml to old-config.yml");

        // Force Save fresh default config
        plugin.saveResource("config.yml", true);
        plugin.reloadConfig();

        // Merge old values into new config
        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile);
        FileConfiguration newConfig = plugin.getConfig();

        mergeConfigs(oldConfig, newConfig, "");

        newConfig.set("config-version", CURRENT_CONFIG_VERSION);
        plugin.saveConfig();

        plugin.getComponentLogger().info(Component.text("[ConfigUpdater] Config update complete!").color(NamedTextColor.GREEN));
        plugin.getComponentLogger().info(Component.text("[ConfigUpdater] Please check config.yml to ensure settings are as expected!").color(NamedTextColor.RED));
    }


    /**
     * Method to merge the old config into the new config
     * @param oldConfig the config to copy valid options from
     * @param newConfig the config to copy the valid options to
     * @param path the config section path - only used in logging, set to "" if top level section
     */
    public static void mergeConfigs(ConfigurationSection oldConfig, ConfigurationSection newConfig, String path) {
        for (String key : oldConfig.getKeys(false)) {
            if (key.equalsIgnoreCase("config-version")) {
                continue;
            }

            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (!newConfig.contains(key)) {
                if (plugin != null)
                    plugin.getComponentLogger().warn("[ConfigUpdater] Not copying removed key: {}", fullPath);
                continue; // removed in new config
            }

            Object oldValue = oldConfig.get(key);

            if (oldValue instanceof ConfigurationSection oldSection) {
                ConfigurationSection newSection = newConfig.getConfigurationSection(key);
                if (newSection != null) {
                    mergeConfigs(oldSection, newSection, fullPath);
                }
            } else {
                newConfig.set(key, oldValue);
                if (plugin != null)
                    plugin.getComponentLogger().info(Component.text("[ConfigUpdater] Migrating Key: " + fullPath).color(NamedTextColor.GREEN));
            }
        }
    }


    /**
     * Method to check if the currently used config is outdated
     * @return true if config is outdated else false
     */
    public static boolean isConfigOutDated() {
        int[] currentParts = parseVersion(CURRENT_CONFIG_VERSION);
        int[] storedParts = parseVersion(plugin.getConfig().getString("config-version", "0.0"));

        int maxLength = Math.max(currentParts.length, storedParts.length);

        for (int i = 0; i < maxLength; i++) {
            int current = i < currentParts.length ? currentParts[i] : 0;
            int stored = i < storedParts.length ? storedParts[i] : 0;

            if (stored < current) return true;
            if (stored > current) return false;
        }

        return false; // equal
    }

    /**
     * Method to parse version string to an array of ints. I.E "1.23.4" becomes {1,23,4}
     * @param version the version string to parse
     * @return an int array containing an int for each part of a version string
     */
    private static int[] parseVersion(String version) {
        try {
            return Arrays.stream(version.split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (NumberFormatException e) {
            plugin.getComponentLogger().warn("[ConfigUpdater] Invalid config-version format, forcing config update.");
            return new int[]{0, 0};
        }
    }
}
