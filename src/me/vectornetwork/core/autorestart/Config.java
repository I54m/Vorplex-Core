package me.vectornetwork.core.autorestart;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import me.vectornetwork.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Config {
    boolean valid;
    public List<String> schedule;
    public Sound notifySound;

    public boolean notifyChatEnabled;
    Map<Integer, String> notifyChatPeriods;
    public boolean notifyTitleEnabled;
    Map<Integer, TitleMessage> notifyTitlePeriods;

    public Config() {
        valid = true;

        FileConfiguration config = loadCFG("config.yml");
        schedule = config.getStringList("AutoRestart.schedule");

        notifyChatEnabled = config.getBoolean("AutoRestart.notify.chat.enabled");
        notifyChatPeriods = new TreeMap<>();
        for (String timeKey : config.getConfigurationSection("AutoRestart.notify.chat.periods").getKeys(false)) {
            Integer time = Integer.valueOf(timeKey);
            String message = ChatColor.translateAlternateColorCodes('&', config.getString("AutoRestart.notify.chat.periods." + timeKey));
            notifyChatPeriods.put(time, message);
        }

        notifyTitleEnabled = config.getBoolean("AutoRestart.notify.title.enabled");
        notifyTitlePeriods = new TreeMap<>();
        for (String timeKey : config.getConfigurationSection("AutoRestart.notify.title.periods").getKeys(false)) {
            Integer time = Integer.valueOf(timeKey);
            TitleMessage message = new TitleMessage(config.getString("AutoRestart.notify.title.periods." + timeKey));
            notifyTitlePeriods.put(time, message);
        }

        String notifySoundName = config.getString("AutoRestart.notify.sound", "UI_BUTTON_CLICK");
        notifySound = Sound.valueOf(notifySoundName);

        // check if cron time format valid
        for (String cronTime : schedule) {
            try {
                CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                new CronParser(definition).parse(cronTime);
            } catch (IllegalArgumentException ex) {
                valid = false;
                Bukkit.getLogger().warning(Main.PREFIX_NO_COLOR + "Cron time format is invalid: " + cronTime);
                Bukkit.getLogger().warning(Main.PREFIX_NO_COLOR + "Error: " + ex.getMessage());
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static FileConfiguration loadCFG(String fileName) {
        File file = new File(Main.instance.getDataFolder(), fileName);
        File dir = file.getParentFile(); if (!dir.isDirectory()) dir.mkdirs();
        if (!file.isFile())
            try {
                InputStream is = Main.class.getResourceAsStream("/" + file.getName());
                OutputStream os = new FileOutputStream(file);
                byte[] data = new byte[is.available()];
                is.read(data); os.write(data);
                is.close(); os.close();
            } catch (IOException ex) {ex.printStackTrace();}
        return YamlConfiguration.loadConfiguration(file);
    }

    static class TitleMessage {
        String title, subtitle;
        int fadeIn, stay, fadeOut;

        TitleMessage(String description) {
            String[] descriptionArray = description.split(" :: ");
            title = ChatColor.translateAlternateColorCodes('&', descriptionArray[0]);
            subtitle = ChatColor.translateAlternateColorCodes('&', descriptionArray[1]);

            String[] times = descriptionArray[2].split(" ");
            fadeIn = Integer.parseInt(times[0]);
            stay = Integer.parseInt(times[1]);
            fadeOut = Integer.parseInt(times[2]);
        }
    }
}

