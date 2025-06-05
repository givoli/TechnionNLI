package ofergivoli.olib.io.log;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;

import java.io.File;
import java.nio.file.Path;


/**
 * Represents a directory to which {@link Logger}s write, and also provides easy access to the created loggers by
 * associating a human readable id string with each logger (that id is also part of the log filename).
 */
public class LogDirectory {

    /**
     * Files will be written in this directory, overwriting when relevant.
     * It is created during construction of this object if missing.
     */
    private final File outputLogDir;
    private final String logFilenamesPrefix;

    /**
     * holds all the loggers associated with this object. Each logger has a human-readable id.
     */
    private final SafeMap<String, Logger> logIdToLogger = new SafeHashMap<>();
    private Object path;

    /**
     * @param logDirectory Log files will be written in this directory, overwriting when relevant. Path doesn't have to
     *                     already exist (all missing directories will be created).
     *
     */
    @SuppressWarnings("unused")
    public LogDirectory(File logDirectory) {
        this(logDirectory, "");
    }

    public LogDirectory(Path logDirectory) {
        this(logDirectory.toFile());
    }

    /**
     * This constructor creates 'logDirectory' if not already existing.
     * @param logDirectory Log files will be written in this directory, overwriting when relevant.
     * @param logFilenamesPrefix should probably end with some visual separator such as "__".
     */
    public LogDirectory(File logDirectory, String logFilenamesPrefix) {
        this.outputLogDir = logDirectory;
        this.logFilenamesPrefix = logFilenamesPrefix;
        if (!logDirectory.exists()) {
            boolean ok = logDirectory.mkdirs();
            Verify.verify(ok);
        }
    }


    public Logger getOrCreateLogger(String logId) {
        String filename = logFilenamesPrefix + logId + ".log";
        File outLogFile = new File(outputLogDir,filename);
        if (!logIdToLogger.safeContainsKey(logId))
            logIdToLogger.put(logId, new Logger(outLogFile,false));
        return logIdToLogger.safeGet(logId);
    }


    public boolean isLoggerExist(String logId) {
        return logIdToLogger.safeContainsKey(logId);
    }

    public Logger getWarningLogger() {
        return getOrCreateLogger("WARNINGS");
    }

    public Logger getGeneralLogger() {
        return getOrCreateLogger("general");
    }

    public Path getOutputDirectory() {
        return outputLogDir.toPath();
    }
}
