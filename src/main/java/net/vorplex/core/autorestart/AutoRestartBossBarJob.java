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

public class AutoRestartBossBarJob implements Job {

    private final VorplexCore plugin = VorplexCore.getInstance();

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

            Bukkit.getScheduler().runTask(plugin, () -> scheduler.beginBossBarCountdown(plugin.autoRestartConfig, restartTime));
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("An Exception was encountered during autorestart shutdown job execution!", e);
        }
    }
}
