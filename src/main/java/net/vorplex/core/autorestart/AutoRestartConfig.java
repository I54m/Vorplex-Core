package net.vorplex.core.autorestart;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class AutoRestartConfig {
    public boolean valid;
    public List<String> schedule;
    public Sound notifySound;

    public boolean notifyChatEnabled;
    Map<Integer, String> notifyChatPeriods;
    public boolean notifyTitleEnabled;
    Map<Integer, TitleMessage> notifyTitlePeriods;

    public AutoRestartConfig() {
        valid = loadConfig();
        // check if cron time format valid
        for (String cronTime : schedule) {
            try {
                CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                new CronParser(definition).parse(cronTime);
            } catch (IllegalArgumentException ex) {
                valid = false;
                Bukkit.getLogger().warning("Cron time format is invalid: " + cronTime);
                Bukkit.getLogger().warning("Error: " + ex.getMessage());
            }
        }
    }


    private boolean loadConfig() {
        try {
            FileConfiguration config = VorplexCore.getInstance().getConfig();
            schedule = config.getStringList("AutoRestart.schedule");

            notifyChatEnabled = config.getBoolean("AutoRestart.notify.chat.enabled");
            notifyChatPeriods = new TreeMap<>();
            for (String timeKey : Objects.requireNonNull(config.getConfigurationSection("AutoRestart.notify.chat.periods")).getKeys(false)) {
                Integer time = Integer.valueOf(timeKey);
                String message = Objects.requireNonNull(config.getString("AutoRestart.notify.chat.periods." + timeKey));
                notifyChatPeriods.put(time, message);
            }

            notifyTitleEnabled = config.getBoolean("AutoRestart.notify.title.enabled");
            notifyTitlePeriods = new TreeMap<>();
            for (String timeKey : Objects.requireNonNull(config.getConfigurationSection("AutoRestart.notify.title.periods")).getKeys(false)) {
                Integer time = Integer.valueOf(timeKey);
                TitleMessage message = new TitleMessage(Objects.requireNonNull(config.getString("AutoRestart.notify.title.periods." + timeKey)));
                notifyTitlePeriods.put(time, message);
            }

            String notifySoundName = config.getString("AutoRestart.notify.sound", "ui.button.click");
            notifySound = Sound.sound(Key.key(notifySoundName), Sound.Source.MASTER, 1f, 1f);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    static class TitleMessage {
        Component title, subtitle;
        long fadeIn, stay, fadeOut;

        TitleMessage(String description) {
            String[] descriptionArray = description.split(" :: ");
            title = MiniMessage.miniMessage().deserialize(descriptionArray[0]);
            subtitle = MiniMessage.miniMessage().deserialize(descriptionArray[1]);

            String[] times = descriptionArray[2].split(" ");
            fadeIn = Long.parseLong(times[0]);
            stay = Long.parseLong(times[1]);
            fadeOut = Long.parseLong(times[2]);
        }
    }
}

