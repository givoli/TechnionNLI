package ofergivoli.olib.io.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper for SLF4J. An all-static-methods class for simple logging with a single logger.
 */
public class Log {

	public enum MessageType {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

	private final static Logger logger = LoggerFactory.getLogger(Log.class);

	
	
	public static void trace(String message) {
		logger.trace(message);
	}
	
	public static void debug(String message) {
		logger.debug(message);
	}
	
	public static void info(String message) {
		logger.info(message);
	}

	/**
	 * Also write the message to standard error channel.
	 */
	public static void warn(String message) {
		logger.warn(message);
	}
	
	/**
	 * Also write the message to standard error channel.
	 */
	public static void error(String message) {
		logger.error(message);
	}

    public static void log(MessageType type, String message) {
        switch (type) {
            case TRACE:
                trace(message);
                break;
            case DEBUG:
                debug(message);
                break;
            case INFO:
                info(message);
                break;
            case WARN:
                warn(message);
                break;
            case ERROR:
                error(message);
                break;
        }
    }

}
