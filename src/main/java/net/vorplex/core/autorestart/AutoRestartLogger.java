package net.vorplex.core.autorestart;

import net.vorplex.core.VorplexCore;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AutoRestartLogger {
    private static final Logger logger = Logger.getLogger(AutoRestartLogger.class.getName());
    private static final VorplexCore plugin = VorplexCore.getInstance();

    private static FileHandler fileHandler = null;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        try {
            File logFile = new File(plugin.getDataFolder(), "AutoRestart.log");
            if (!logFile.exists()) logFile.createNewFile();

            fileHandler = new FileHandler(logFile.getPath(), true);

            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            // Disable logging to console
            logger.setUseParentHandlers(false);

            initialized = true;
        } catch (IOException e) {
            plugin.getComponentLogger().error("Failed to initialize auto restart log file handler: {}", e.getMessage());
        }
    }

    public static void close() {
        if (initialized && fileHandler != null)
            fileHandler.close();
    }

    public static void log(Level level, String message) {
        if (!initialized) init();
        logger.log(level, message);
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warning(String message) {
        log(Level.WARNING, message);
    }

    public static void severe(String message) {
        log(Level.SEVERE, message);
    }

}
