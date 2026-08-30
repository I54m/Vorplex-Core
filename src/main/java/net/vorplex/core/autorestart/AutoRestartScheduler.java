package net.vorplex.core.autorestart;

import lombok.Getter;
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
import org.jspecify.annotations.NonNull;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.text.ParseException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AutoRestartScheduler {
    @Getter
    private ZonedDateTime restartTime;

    private static final long BOSS_BAR_DURATION_SECONDS = 60;

    private final VorplexCore plugin;

    private Scheduler quartzScheduler;
    private BossBar bossBarCountdown;
    private BukkitTask bossBarCountdownTask;

    public AutoRestartScheduler(VorplexCore plugin) {
        this.plugin = plugin;
    }

    public void start(AutoRestartConfig autoRestartConfig) {
        if (!autoRestartConfig.valid) return;

        plugin.autoRestartConfig = autoRestartConfig;

        try {
            quartzScheduler = getQuartzSchedulerFactory().getScheduler();
            quartzScheduler.start();

            Date now = new Date();
            ZonedDateTime nextRestart = null;

            for (String cron : autoRestartConfig.schedule) {
                CronExpression cronExpression = new CronExpression(cron);
                Date nextFireTime = cronExpression.getNextValidTimeAfter(now);

                if (nextFireTime == null)
                    continue;

                ZonedDateTime candidate = nextFireTime.toInstant().atZone(ZoneId.systemDefault());

                if (nextRestart == null || candidate.isBefore(nextRestart))
                    nextRestart = candidate;
            }

            if (nextRestart != null)
                scheduleRestart(nextRestart);

        } catch (ParseException | SchedulerException e) {
            plugin.getComponentLogger().error("Failed to start AutoRestart Quartz scheduler", e);
        }
    }

    private @NonNull StdSchedulerFactory getQuartzSchedulerFactory() throws SchedulerException {
        Properties quartzProperties = new Properties();
        quartzProperties.setProperty("org.quartz.scheduler.instanceName", "VorplexCore-AutoRestart");
        quartzProperties.setProperty("org.quartz.threadPool.class", "net.vorplex.core.lib.quartz.simpl.SimpleThreadPool");
        quartzProperties.setProperty("org.quartz.threadPool.threadCount", "1");
        quartzProperties.setProperty("org.quartz.threadPool.threadPriority", "5");
        quartzProperties.setProperty("org.quartz.jobStore.class", "net.vorplex.core.lib.quartz.simpl.RAMJobStore");

        return new StdSchedulerFactory(quartzProperties);
    }

    public void cancelRestart() {
        AutoRestartLogger.info("Cancelling Auto restart...");
        restartTime = null;
        if (bossBarCountdownTask != null) {
            bossBarCountdownTask.cancel();
            bossBarCountdownTask = null;
        }
        if (bossBarCountdown != null) {
            Audience.audience(Bukkit.getOnlinePlayers()).hideBossBar(bossBarCountdown);
            bossBarCountdown = null;
        }
        cancelQuartzJobs();
    }

    private void cancelQuartzJobs() {
        try {
            if (quartzScheduler == null || quartzScheduler.isShutdown())
                return;
            Set<JobKey> jobKeys = quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals("autorestart"));
            quartzScheduler.deleteJobs(new ArrayList<>(jobKeys));

            AutoRestartLogger.info("Cancelled Auto restart!");
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("Failed to cancel AutoRestart Quartz jobs", e);
        }
    }


    public void shutdown() {
        try {
            restartTime = null;
            if (quartzScheduler != null && !quartzScheduler.isShutdown()) {
                try {
                    quartzScheduler.shutdown(false);
                } catch (SchedulerException e) {
                    plugin.getComponentLogger().error("An Exception was encountered while trying to shutdown the quartz scheduler for autorestart", e);
                }
            }
            if (bossBarCountdownTask != null) {
                bossBarCountdownTask.cancel();
                bossBarCountdownTask = null;
            }
            if (bossBarCountdown != null) {
                Audience.audience(Bukkit.getOnlinePlayers()).hideBossBar(bossBarCountdown);
                bossBarCountdown = null;
            }
            AutoRestartLogger.info("Shutting down Auto restart Scheduler");
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("Failed to shutdown AutoRestart scheduler", e);
        }
    }

    private void scheduleRestart(ZonedDateTime restartTime) {
        try {
            if (quartzScheduler == null)
                throw new IllegalStateException("AutoRestart Quartz scheduler has not been initialized");
            if (quartzScheduler.isShutdown())
                throw new IllegalStateException("AutoRestart Quartz scheduler has been shut down");

            this.restartTime = restartTime;
            quartzScheduler.getContext().put("autoRestartScheduler", this);
            scheduleShutdown(restartTime);
            scheduleNotifications(plugin.getAutoRestartConfig(), restartTime);
            AutoRestartLogger.info("Scheduled restart for: " + restartTime);
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("An Exception was encountered while trying to schedule a restart for: {}", restartTime);
            plugin.getComponentLogger().error("Error:", e);
        }
    }

    public void rescheduleRestart(ChronoUnit chronoUnit, long amount) {
        cancelRestart();
        scheduleRestart(ZonedDateTime.now().plus(amount, chronoUnit));
    }

    private void scheduleNotifications(AutoRestartConfig autoRestartConfig, ZonedDateTime restartTime) throws SchedulerException {
        Set<Integer> notificationPeriods = new TreeSet<>();

        notificationPeriods.addAll(autoRestartConfig.notifyChatPeriods.keySet());
        notificationPeriods.addAll(autoRestartConfig.notifyTitlePeriods.keySet());

        for (Integer secondsBefore : notificationPeriods) {
            ZonedDateTime notificationTime = restartTime.minusSeconds(secondsBefore);

            if (!notificationTime.isAfter(ZonedDateTime.now())) continue;

            TriggerKey triggerKey = new TriggerKey("autorestart-notification-trigger-" + secondsBefore, "autorestart");
            JobKey jobKey = new JobKey("autorestart-notification-" + secondsBefore, "autorestart");

            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).startAt(Date.from(notificationTime.toInstant())).build();
            JobDetail job = JobBuilder.newJob(AutoRestartNotifyJob.class).withIdentity(jobKey).usingJobData("seconds", secondsBefore).build();
            quartzScheduler.scheduleJob(job, trigger);
        }

        if (autoRestartConfig.bossBarCountdownEnabled) {
            ZonedDateTime bossBarStartTime = restartTime.minusSeconds(BOSS_BAR_DURATION_SECONDS);

            if (bossBarStartTime.isAfter(ZonedDateTime.now())) {

                JobKey jobKey = new JobKey("autorestart-bossbar", "autorestart");

                TriggerKey triggerKey = new TriggerKey("autorestart-bossbar-trigger", "autorestart");

                JobDetail job = JobBuilder.newJob(AutoRestartBossBarJob.class)
                        .withIdentity(jobKey)
                        .usingJobData("restartTime", restartTime.toInstant().toEpochMilli())
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .startAt(Date.from(bossBarStartTime.toInstant()))
                        .build();

                quartzScheduler.scheduleJob(job, trigger);
            }
        }
    }

    protected void sendNotification(AutoRestartConfig autoRestartConfig, int seconds) {
        if (autoRestartConfig.notifyChatEnabled) {
            if (autoRestartConfig.notifyChatPeriods.containsKey(seconds)) {
                String message = autoRestartConfig.notifyChatPeriods.get(seconds);
                Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());
                audienceLater.sendMessage(MiniMessage.miniMessage().deserialize(message));
                if (autoRestartConfig.notifySoundEnabled) audienceLater.playSound(autoRestartConfig.notifySound);
            }
        }

        if (autoRestartConfig.notifyTitleEnabled) {
            if (autoRestartConfig.notifyTitlePeriods.containsKey(seconds)) {
                AutoRestartConfig.TitleMessage titleMessage = autoRestartConfig.notifyTitlePeriods.get(seconds);
                Audience audienceLater = Audience.audience(Bukkit.getServer().getOnlinePlayers());

                audienceLater.showTitle(Title.title(
                        titleMessage.title,
                        titleMessage.subtitle,
                        titleMessage.fadeIn,
                        titleMessage.stay,
                        titleMessage.fadeOut
                ));
                // if no sound from chat notify at same time, then play from title notify
                if (!autoRestartConfig.notifyChatEnabled || !autoRestartConfig.notifyChatPeriods.containsKey(seconds))
                    if (autoRestartConfig.notifySoundEnabled)
                        audienceLater.playSound(autoRestartConfig.notifySound);
            }
        }
    }

    protected void beginBossBarCountdown(AutoRestartConfig autoRestartConfig, ZonedDateTime restartTime) {
        if (autoRestartConfig.bossBarCountdownEnabled) {
            long seconds = Duration.between(ZonedDateTime.now(), restartTime).getSeconds();

            bossBarCountdown = BossBar.bossBar(
                    Component.text("Server Rebooting in ").append(Component.text(seconds).color(NamedTextColor.RED), Component.text(" seconds!")),
                    1.0f,
                    BossBar.Color.PINK,
                    BossBar.Overlay.NOTCHED_6
            );

            bossBarCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateBossBarCountdown(autoRestartConfig), 0L, 20L);
        }
    }

    private void updateBossBarCountdown(AutoRestartConfig autoRestartConfig) {
        if (autoRestartConfig.bossBarCountdownEnabled && bossBarCountdown != null) {
            long seconds = Duration.between(ZonedDateTime.now(), restartTime).getSeconds();

            if (seconds <= 0) {
                Audience.audience(Bukkit.getOnlinePlayers()).hideBossBar(bossBarCountdown);
                return;
            }
            float progress = Math.clamp(seconds / (float) BOSS_BAR_DURATION_SECONDS, 0.0f, 1.0f);

            bossBarCountdown.progress(progress);
            bossBarCountdown.name(
                    Component.text("Server Rebooting in ").append(Component.text(seconds).color(NamedTextColor.RED), Component.text(" seconds!"))
            );
            if (seconds <= 5) bossBarCountdown.color(BossBar.Color.RED);
            else if (seconds <= 15) bossBarCountdown.color(BossBar.Color.YELLOW);

            Audience.audience(Bukkit.getOnlinePlayers()).showBossBar(bossBarCountdown);
        }
    }

    private void scheduleShutdown(ZonedDateTime restartTime) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(AutoRestartShutdownJob.class)
                .withIdentity("autorestart-shutdownjob", "autorestart")
                .usingJobData("plugin", plugin.getName())
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("autorestart-shutdowntrigger", "autorestart")
                .startAt(Date.from(restartTime.toInstant()))
                .forJob(job)
                .build();

        quartzScheduler.scheduleJob(job, trigger);
    }

    protected void shutdownServer() {
        plugin.getComponentLogger().info("Server Shutdown requested via autorestart module");
        Bukkit.getServer().savePlayers();
        for (World world : Bukkit.getServer().getWorlds()) {
            world.save();
        }
        plugin.getComponentLogger().info("Player and world data saved!");
        plugin.getComponentLogger().info("Shutting down server!");
        AutoRestartLogger.info("Server Shutting down due to Auto Restart");
        Bukkit.getServer().shutdown();
    }
}
