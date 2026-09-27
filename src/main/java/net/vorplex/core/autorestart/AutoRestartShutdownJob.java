package net.vorplex.core.autorestart;

import net.vorplex.core.VorplexCore;
import org.bukkit.Bukkit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

/**
 * Quartz job to manage the server shutdown
 */
public class AutoRestartShutdownJob implements Job {

    private final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * Execute the shutdown job to shut down the server after countdown is complete
     *
     * @param context the job context that quartz provides
     */
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
