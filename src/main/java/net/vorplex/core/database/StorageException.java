package net.vorplex.core.database;

/**
 * Exception used to wrap any Exception encountered in a StorageProvider
 */
public class StorageException extends RuntimeException {

    /**
     * Basic constructor for a StorageException
     *
     * @param message The error message for the Exception
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Constructor for a StorageException
     * @param message The error message for the Exception
     * @param cause The cause of the Exception (usually an SQLException)
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
