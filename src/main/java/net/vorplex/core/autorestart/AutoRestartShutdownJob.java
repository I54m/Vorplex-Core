package net.vorplex.core.autorestart;

import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

public class AutoRestartShutdownJob implements Job {

    private final VorplexCore plugin = VorplexCore.getInstance();

    @Override
    public void execute(JobExecutionContext context) {
        try {
            AutoRestartScheduler scheduler = (AutoRestartScheduler) context.getScheduler().getContext().get("autoRestartScheduler");

            Bukkit.getScheduler().runTask(plugin, scheduler::shutdownServer);
        } catch (SchedulerException e) {
            plugin.getComponentLogger().error("An Exception was encountered during autorestart shutdown job execution!", e);
        }
    }
}
