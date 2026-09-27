package net.vorplex.core.util;

import net.vorplex.core.VorplexCore;

/**
 * Simple class to check debug var and log if true
 */
public class Debug {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    /**
     * Log a debug message if debug more is on
     *
     * @param message the message to log
     */
    public static void log(String message) {
        if (plugin.getConfig().getBoolean("debug", false))
            plugin.getLogger().info("Debug: " + message);
    }
}
