package net.vorplex.core.autorestart;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class AutoRestartScheduler {
    public static List<BukkitTask> tasks = new ArrayList<>();
    public static ZonedDateTime nextTime;
    private static final VorplexCore plugin = VorplexCore.getInstance();

    public static void start(AutoRestartConfig autoRestartConfig) {
        if (!autoRestartConfig.valid) return;
        long delayTicks = getDelayTicks(autoRestartConfig.schedule);
        tasks = new ArrayList<>();

        scheduleNotify(autoRestartConfig, delayTicks);
        tasks.add(Bukkit.getScheduler().runTaskLater(VorplexCore.instance, () -> {
            Bukkit.getServer().savePlayers();
            for (World world : Bukkit.getServer().getWorlds()) {
                world.save();
            }
            Bukkit.getServer().shutdown();
        }, delayTicks));
    }

    public static void stop() {
        for (BukkitTask task : tasks){
            task.cancel();
        }
        tasks.clear();
        nextTime = null;
    }

    //@SuppressWarnings("deprecation")
    public static void scheduleNotify(AutoRestartConfig autoRestartConfig, long initDelayTicks) {
        if (autoRestartConfig.notifyChatEnabled) {
            autoRestartConfig.notifyChatPeriods.forEach((key, message) -> {
                boolean sound = autoRestartConfig.notifySound != null;
                long notifyDelay = initDelayTicks - key * 20;
                if (notifyDelay < 1) return;

                tasks.add(Bukkit.getScheduler().runTaskLater(VorplexCore.instance, () -> {
                    Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());
                    audienceLater.sendMessage(MiniMessage.miniMessage().deserialize(message));
                    if (sound) audienceLater.playSound(autoRestartConfig.notifySound);
                }, notifyDelay));
            });
        }

        if (autoRestartConfig.notifyTitleEnabled) {
            autoRestartConfig.notifyTitlePeriods.forEach((key, titleMessage) -> {
                boolean sound = autoRestartConfig.notifySound != null;
                long notifyDelay = initDelayTicks - key * 20;
                if (notifyDelay < 1) return;

                tasks.add(Bukkit.getScheduler().runTaskLater(VorplexCore.instance, () -> {
                    Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());

                    if (autoRestartConfig.notifyTitleEnabled)
                        audienceLater.showTitle(Title.title(titleMessage.title, titleMessage.subtitle,
                                Title.Times.times(Duration.ofMillis(titleMessage.fadeIn), Duration.ofMillis(titleMessage.stay), Duration.ofMillis(titleMessage.fadeOut))));
                    // if no sound from chat notify at same time, then play from title notify
                    if (!autoRestartConfig.notifyChatEnabled || !autoRestartConfig.notifyChatPeriods.containsKey(key))
                        if (sound) audienceLater.playSound(autoRestartConfig.notifySound);
                }, notifyDelay));
            });
        }
    }

    private static long getDelayTicks(List<String> schedule) {
        long nextDelayTicks = Long.MAX_VALUE;
        long currentSecond = System.currentTimeMillis() / 1000;
        ZonedDateTime timeNow = ZonedDateTime.now();

        for (String cronTime : schedule) {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            Cron cron = new CronParser(definition).parse(cronTime);
            ZonedDateTime runTime = ExecutionTime.forCron(cron).nextExecution(timeNow).orElseThrow();
            long delayTicks = (runTime.toEpochSecond() - currentSecond) * 20;
            if (delayTicks < nextDelayTicks) {
                nextDelayTicks = delayTicks;
                nextTime = runTime;
            }
        }
        return nextDelayTicks;
    }
}
