package net.vorplex.core.autorestart;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.vorplex.core.VorplexCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Class used to maintain the auto restart module's configuration
 */
public class AutoRestartConfig {
    private final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * True if the auto restart config section is valid
     */
    public boolean valid;
    /**
     * The cron schedule to use for auto restarts
     */
    public List<String> schedule;
    /**
     * The sound to play when an auto restart notification is sent
     */
    public Sound notifySound;
    /**
     * True if the notification sound is enabled
     */
    public boolean notifySoundEnabled;
    /**
     * True if the chat notifications are enabled
     */
    public boolean notifyChatEnabled;
    /**
     * True if the boss bar countdown is enabled
     */
    public boolean bossBarCountdownEnabled;
    /**
     * The chat notification messages and their periods
     */
    public Map<Integer, String> notifyChatPeriods;
    /**
     * True if the Title notifications are enabled
     */
    public boolean notifyTitleEnabled;
    /**
     * The title notification messages and their periods
     */
    public Map<Integer, TitleMessage> notifyTitlePeriods;

    /**
     * Load the config and ensure it is valid
     */
    public AutoRestartConfig() {
        valid = loadConfig();
        // check if cron time format valid
        for (String cronTime : schedule) {
            try {
                CronExpression.validateExpression(cronTime);
            } catch (ParseException ex) {
                valid = false;
                plugin.getLogger().warning("Cron time format is invalid: " + cronTime);
                plugin.getLogger().warning("Error: " + ex.getMessage());
            }
        }
    }

    /**
     * Load the config, checking if it is valid
     *
     * @return true if validation passes, else false
     */
    private boolean loadConfig() {
        try {
            FileConfiguration config = plugin.getConfig();
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

            notifySoundEnabled = config.getBoolean("AutoRestart.notify.sound.enabled", true);
            notifySound = Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 1f, 1f);

            bossBarCountdownEnabled = config.getBoolean("AutoRestart.notify.bossBarCountdown.enabled", true);
        } catch (Exception e) {
            plugin.getComponentLogger().error("Error loading AutoRestart config: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Class to decode the title message from teh config
     */
    public static class TitleMessage {
        final Component title, subtitle;
        final int fadeIn, stay, fadeOut;

        /**
         * Get title message from the provided config value
         * @param description the config value to decode
         */
        TitleMessage(String description) {
            String[] descriptionArray = description.split(" :: ");
            title = MiniMessage.miniMessage().deserialize(descriptionArray[0]);
            subtitle = MiniMessage.miniMessage().deserialize(descriptionArray[1]);

            String[] times = descriptionArray[2].split(" ");
            fadeIn = Integer.parseInt(times[0]);
            stay = Integer.parseInt(times[1]);
            fadeOut = Integer.parseInt(times[2]);
        }
    }
}

