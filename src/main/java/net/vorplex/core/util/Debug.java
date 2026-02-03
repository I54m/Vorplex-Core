package net.vorplex.core.util;

import net.vorplex.core.VorplexCore;

public class Debug {

    private static final VorplexCore plugin = VorplexCore.getInstance();

    public static void log(String message) {
        if (plugin.getConfig().getBoolean("debug", false))
            plugin.getLogger().info("Debug: " + message);
    }
}
