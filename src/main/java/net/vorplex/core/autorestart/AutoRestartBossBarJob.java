package net.vorplex.core.autorestart;

import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Quartz job to manage starting the boss bar countdown
 */
public class AutoRestartBossBarJob implements Job {

    private final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * Execute the boss bar countdown job to begin the boss bar countdown
     *
     * @param context the job context that quartz provides
     * @throws JobExecutionException upon encountering an exception running the job
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            long restartTimeMillis = context.getJobDetail()
                    .getJobDataMap()
                    .getLong("restartTime");
            AutoRestartScheduler scheduler = (AutoRestartScheduler) context.getScheduler().getContext().get("autoRestartScheduler");

            ZonedDateTime restartTime = Instant
                    .ofEpochMilli(restartTimeMillis)
                    .atZone(ZoneId.systemDefault());

            Bukkit.getScheduler().runTask(plugin, () -> scheduler.beginBossBarCountdown(plugin.getAutoRestartConfig(), restartTime));
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("An Exception was encountered during autorestart shutdown job execution!", e);
        }
    }
}
