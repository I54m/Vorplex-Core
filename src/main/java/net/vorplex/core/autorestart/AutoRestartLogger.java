package net.vorplex.core.autorestart;

import net.vorplex.core.VorplexCore;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

/**
 * Custom logger for the auto restart module
 */
public class AutoRestartLogger {
    private static final Logger logger = Logger.getLogger(AutoRestartLogger.class.getName());
    private static final VorplexCore plugin = VorplexCore.getInstance();

    private static FileHandler fileHandler = null;
    private static boolean initialized = false;

    /**
     * Initialize the logger and prepare for logging
     */
    public static void init() {
        if (initialized) return;
        try {
            File logFile = new File(plugin.getDataFolder(), "AutoRestart.log");
            if (!logFile.exists()) logFile.createNewFile();

            fileHandler = new FileHandler(logFile.getPath(), true);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("%1$tb %1$td, %1$tY %1$tr %2$s: %3$s%n",
                            new Date(record.getMillis()),
                            record.getLevel().getLocalizedName(),
                            formatMessage(record)
                    );
                }
            });

            logger.addHandler(fileHandler);

            // Disable logging to console
            logger.setUseParentHandlers(false);

            initialized = true;
        } catch (IOException e) {
            plugin.getComponentLogger().error("Failed to initialize auto restart log file handler: {}", e.getMessage());
        }
    }

    /**
     * Stop all logging and save the log file
     */
    public static void close() {
        if (initialized && fileHandler != null)
            fileHandler.close();
    }

    /**
     * Log message at the specified level
     *
     * @param level   the level to log the message at
     * @param message the message to log
     */
    public static void log(Level level, String message) {
        if (!initialized) init();
        logger.log(level, message);
    }

    /**
     * Log a message at the info level
     * @param message the message to log
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Log a message at the warning level
     * @param message the message to log
     */
    public static void warning(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Log a message at the severe level
     * @param message the message to log
     */
    public static void severe(String message) {
        log(Level.SEVERE, message);
    }

}
