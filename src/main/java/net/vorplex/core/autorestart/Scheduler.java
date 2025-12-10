package net.vorplex.core.autorestart;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Scheduler {
    public static List<BukkitTask> tasks = new ArrayList<>();
    public static ZonedDateTime nextTime;
    private static VorplexCore plugin = VorplexCore.getInstance();

    public static void start(Config config) {
        if (!config.valid) return;
        long delayTicks = getDelayTicks(config.schedule);
        tasks = new ArrayList<>();

        scheduleNotify(config, delayTicks);
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

    @SuppressWarnings("deprecation")
    public static void scheduleNotify(Config config, long initDelayTicks) {
        if (config.notifyChatEnabled) {
            config.notifyChatPeriods.entrySet().forEach(entry -> {
                String message = entry.getValue();
                boolean sound = config.notifySound != null;
                long notifyDelay = initDelayTicks - entry.getKey() * 20;
                if (notifyDelay < 1) return;

                tasks.add(Bukkit.getScheduler().runTaskLater(VorplexCore.instance, () -> {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendMessage(message);
                        if (sound) player.playSound(player.getLocation(), config.notifySound, 1.0f, 1.0f);
                    });
                }, notifyDelay));
            });
        }

        if (config.notifyTitleEnabled) {
            config.notifyTitlePeriods.entrySet().forEach(entry -> {
                Config.TitleMessage titleMessage = entry.getValue();
                boolean sound = config.notifySound != null;
                long notifyDelay = initDelayTicks - entry.getKey() * 20;
                if (notifyDelay < 1) return;

                tasks.add(Bukkit.getScheduler().runTaskLater(VorplexCore.instance, () -> {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (plugin.old) player.sendTitle(titleMessage.title, titleMessage.subtitle);
                        else player.sendTitle(
                                titleMessage.title, titleMessage.subtitle,
                                titleMessage.fadeIn, titleMessage.stay, titleMessage.fadeOut);
                        // if no sound from chat notify at same time, then play from title notify
                        if (!config.notifyChatEnabled || !config.notifyChatPeriods.containsKey(entry.getKey())) {
                            if (sound) player.playSound(player.getLocation(), config.notifySound, 1.0f, 1.0f);
                        }
                    });
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
            ZonedDateTime runTime = ExecutionTime.forCron(cron).nextExecution(timeNow).get();
            long delayTicks = (runTime.toEpochSecond() - currentSecond) * 20;
            if (delayTicks < nextDelayTicks) {
                nextDelayTicks = delayTicks;
                nextTime = runTime;
            }
        }
        return nextDelayTicks;
    }
}
