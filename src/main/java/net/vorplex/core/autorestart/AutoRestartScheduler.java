package net.vorplex.core.autorestart;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AutoRestartScheduler {
    public static List<BukkitTask> tasks = new ArrayList<>();
    public static ZonedDateTime nextTime;

    private static final VorplexCore plugin = VorplexCore.getInstance();

    private static BossBar bossBarCountdown;

    public static void start(AutoRestartConfig autoRestartConfig) {
        if (!autoRestartConfig.valid) return;
        long delayTicks = getDelayTicks(autoRestartConfig.schedule);
        tasks = new ArrayList<>();

        scheduleNotify(autoRestartConfig, delayTicks);
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, AutoRestartScheduler::shutdownServer, delayTicks));
    }

    public static void stop() {
        for (BukkitTask task : tasks){
            task.cancel();
        }
        tasks.clear();
        Audience.audience(Bukkit.getOnlinePlayers()).hideBossBar(bossBarCountdown);
    }

    public static void scheduleReboot(TimeUnit timeUnit, long amount) {
        scheduleReboot(timeUnit.toSeconds(amount) * 20);
    }

    public static void scheduleReboot(long initDelayTicks) {
        //stop current reboot tasks
        stop();
        //set nextTime variable
        long delaySeconds = initDelayTicks / 20;
        nextTime = ZonedDateTime.now().plusSeconds(delaySeconds);
        //schedule requested reboot
        scheduleNotify(new AutoRestartConfig(), initDelayTicks);
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, AutoRestartScheduler::shutdownServer, initDelayTicks));
    }

    private static void scheduleNotify(AutoRestartConfig autoRestartConfig, long initDelayTicks) {
        if (autoRestartConfig.notifyChatEnabled) {
            autoRestartConfig.notifyChatPeriods.forEach((key, message) -> {
                long notifyDelay = initDelayTicks - key * 20;
                if (notifyDelay < 1) return;

                tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());
                    audienceLater.sendMessage(MiniMessage.miniMessage().deserialize(message));
                    if (autoRestartConfig.notifySoundEnabled) audienceLater.playSound(autoRestartConfig.notifySound);
                }, notifyDelay));
            });
        }

        if (autoRestartConfig.notifyTitleEnabled) {
            autoRestartConfig.notifyTitlePeriods.forEach((key, titleMessage) -> {
                long notifyDelay = initDelayTicks - key * 20;
                if (notifyDelay < 1) return;

                tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());

                    audienceLater.showTitle(Title.title(
                            titleMessage.title,
                            titleMessage.subtitle,
                            titleMessage.fadeIn,
                            titleMessage.stay,
                            titleMessage.fadeOut
                    ));
                    // if no sound from chat notify at same time, then play from title notify
                    if (!autoRestartConfig.notifyChatEnabled || !autoRestartConfig.notifyChatPeriods.containsKey(key))
                        if (autoRestartConfig.notifySoundEnabled)
                            audienceLater.playSound(autoRestartConfig.notifySound);
                }, notifyDelay));
            });
        }

        if (autoRestartConfig.bossBarCountdownEnabled) {

            long bossbarTicks = Math.min(1200, initDelayTicks);

            bossBarCountdown = BossBar.bossBar(
                    Component.text("Server Rebooting in ").append(Component.text(bossbarTicks / 20).color(NamedTextColor.RED), Component.text(" seconds!")),
                    1.0f,
                    BossBar.Color.PINK,
                    BossBar.Overlay.NOTCHED_6
            );

            AtomicLong remainingTicks = new AtomicLong(Math.min(1200, initDelayTicks));

            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {

                long ticksLeft = remainingTicks.addAndGet(-20);

                long secondsLeft = ticksLeft / 20;
                if (secondsLeft < 0) {
                    Audience.audience(Bukkit.getOnlinePlayers()).hideBossBar(bossBarCountdown);
                    return;
                }
                float progress = Math.min(1.0f, secondsLeft / (float) (bossbarTicks / 20));

                bossBarCountdown.progress(progress);
                bossBarCountdown.name(
                        Component.text("Server Rebooting in ").append(Component.text(secondsLeft).color(NamedTextColor.RED), Component.text(" seconds!"))
                );
                if (secondsLeft <= 5) bossBarCountdown.color(BossBar.Color.RED);
                else if (secondsLeft <= 10) bossBarCountdown.color(BossBar.Color.YELLOW);

                Audience.audience(Bukkit.getOnlinePlayers()).showBossBar(bossBarCountdown);
            }, Math.max(20, initDelayTicks - 1200), 20L));
        }
    }

    private static long getDelayTicks(List<String> schedule) {
        long nextDelayTicks = Long.MAX_VALUE;

        for (String cronTime : schedule) {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
            Cron cron = new CronParser(definition).parse(cronTime);
            ZonedDateTime runTime = ExecutionTime.forCron(cron)
                    .nextExecution(ZonedDateTime.now())
                    .orElseThrow();
            long delayTicks = Duration.between(ZonedDateTime.now(), runTime).toSeconds() * 20;
            if (delayTicks < nextDelayTicks) {
                nextDelayTicks = delayTicks;
                nextTime = runTime;
            }
        }
        return nextDelayTicks;
    }

    private static void shutdownServer() {
        plugin.getComponentLogger().info("Server Shutdown requested via autorestart module");
        Bukkit.getServer().savePlayers();
        for (World world : Bukkit.getServer().getWorlds()) {
            world.save();
        }
        plugin.getComponentLogger().info("Player and world data saved!");
        plugin.getComponentLogger().info("Shutting down server!");
        Bukkit.getServer().shutdown();
    }
}
