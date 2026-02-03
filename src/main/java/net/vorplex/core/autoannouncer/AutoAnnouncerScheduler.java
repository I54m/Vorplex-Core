package net.vorplex.core.autoannouncer;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class AutoAnnouncerScheduler {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    private static ArrayList<String> messages = new ArrayList<>();
    private static int previousMessageNumber;
    private static BukkitTask announcerTask;
    private static String prefix;
    private static boolean playSound;

    public static void start() {
        prefix = plugin.getConfig().getString("AutoAnnouncer.Prefix", "<dark_purple>[<light_purple><b>Tip</b></light_purple>]</dark_purple>");
        messages = (ArrayList<String>) plugin.getConfig().getList("AutoAnnouncer.Messages");
        if (messages == null || messages.isEmpty()) {
            plugin.getComponentLogger().error("No messages were loaded for Auto Announcer!");
            plugin.getComponentLogger().error("Module will not be enabled!");
            return;
        }
        playSound = plugin.getConfig().getBoolean("AutoAnnouncer.PlaySound", true);
        announcerTask = Bukkit.getScheduler().runTaskTimer(plugin,
                AutoAnnouncerScheduler::runAnnouncement,
                720L,
                (plugin.getConfig().getInt("AutoAnnouncer.Interval", 120) * 20L));
    }

    public static void stop() {
        if (announcerTask != null) {
            announcerTask.cancel();
            announcerTask = null;
        }
    }

    private static void runAnnouncement() {
        if (!messages.isEmpty()) {
            int messageNumber;
            if (plugin.getConfig().getBoolean("AutoAnnouncer.Random", true))
                messageNumber = ThreadLocalRandom.current().nextInt(messages.size());
            else {
                messageNumber = previousMessageNumber + 1;
                if (previousMessageNumber >= messages.size())
                    previousMessageNumber = 0;
                else previousMessageNumber++;
            }
            Audience players = Audience.audience(Bukkit.getOnlinePlayers());
            players.sendMessage(MiniMessage.miniMessage().deserialize(prefix + " " + messages.get(messageNumber)));
            if (playSound)
                players.playSound(Sound.sound(Key.key("entity.chicken.egg"), Sound.Source.MASTER, 1f, 1));
        }
    }
}
