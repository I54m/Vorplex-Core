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

/**
 * Class used to control the auto restart functionality
 */
public class AutoRestartScheduler {
    @Getter
    private ZonedDateTime restartTime;

    private static final long BOSS_BAR_DURATION_SECONDS = 60;

    private final VorplexCore plugin;

    private Scheduler quartzScheduler;
    private BossBar bossBarCountdown;
    private BukkitTask bossBarCountdownTask;

    /**
     * Create an instance of the AutoRestartScheduler
     *
     * @param plugin the instance of the VorplexCore plugin class
     */
    public AutoRestartScheduler(VorplexCore plugin) {
        this.plugin = plugin;
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

    /**
     * Initialize the Scheduler and start the auto restart countdown
     * @param autoRestartConfig the config instance to use for the scheduler
     */
    public void init(AutoRestartConfig autoRestartConfig) {
        if (!autoRestartConfig.valid) return;

        plugin.setAutoRestartConfig(autoRestartConfig);

        try {
            quartzScheduler = getQuartzSchedulerFactory().getScheduler();
            quartzScheduler.start();
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("Failed to initialize AutoRestart Quartz scheduler", e);
        }

        start();
    }

    /**
     * Start the auto restart scheduler from the plugin's autoRestartConfig
     */
    public void start() {
        if (restartTime != null) return;

        Date now = new Date();
        ZonedDateTime nextRestart = null;

        try {
            for (String cron : plugin.getAutoRestartConfig().schedule) {
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
        } catch (ParseException e) {
            plugin.getComponentLogger().error("Failed to schedule AutoRestart from config", e);
        }
    }

    /**
     * Cancel the running restart tasks
     */
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

    /**
     * Cancel the running quartz jobs
     */
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

    /**
     * Shutdown the Auto Restart Scheduler
     */
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

    /**
     * Schedule a restart for a specific time (Does not cancel currently running tasks)
     * @param restartTime The ZonedDateTime to restart at
     */
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

    /**
     * Cancel the running restart tasks and reschedule them amount of timeunits later
     * @param chronoUnit the time unit to use (e.g. Seconds, Minutes etc.)
     * @param amount the amount of time until the new restart
     */
    public void rescheduleRestart(ChronoUnit chronoUnit, long amount) {
        cancelRestart();
        scheduleRestart(ZonedDateTime.now().plus(amount, chronoUnit));
    }

    /**
     * Schedule the notification jobs
     * @param autoRestartConfig config to use for the notifications and times
     * @param restartTime the time to restart at
     * @throws SchedulerException thrown if an exception was encountered while scheduling tasks
     */
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

    /**
     * Send a Notification
     * @param autoRestartConfig the config to get the message from
     * @param seconds the seconds until the restart
     */
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

    /**
     * Start the boss bar countdown
     * @param autoRestartConfig the config to use for the boss bar countdown
     * @param restartTime the time when the restart will occur
     */
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

    /**
     * Update the boss bar with the remaining seconds until the restart
     * @param autoRestartConfig the config to use for the boss bar
     */
    private void updateBossBarCountdown(AutoRestartConfig autoRestartConfig) {
        if (autoRestartConfig.bossBarCountdownEnabled && bossBarCountdown != null) {
            long seconds = Duration.between(ZonedDateTime.now(), restartTime).getSeconds() + 1;

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

    /**
     * Schedule the server shutdown task
     * @param restartTime the time the restart will happen
     * @throws SchedulerException thrown if an exception was encountered during scheduling
     */
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

    /**
     * Shutdown the server via the auto restart module
     */
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
