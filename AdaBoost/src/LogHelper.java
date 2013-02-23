import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class LogHelper {
	public static final String DEBUG = "DEBUG";
	static Logger logger = Logger.getAnonymousLogger();

	public static void initialize(String name, boolean isDebug) {
		logger = Logger.getLogger(name);
		setDebug(isDebug);
		logger.setUseParentHandlers(false);
		StreamHandler handler = new StreamHandler(System.out, new SimpleFormatter() {
			public String format(LogRecord record) {
				return record.getMessage();
			}
		});
		logger.addHandler(handler);
	}

	/**
	 * Set the system to log all messages if isDebug is true;
	 * otherwise log only non-debug messages.
	 * 
	 * @param isDebug
	 */
	public static void setDebug(boolean isDebug) {
		if (isDebug) {
			logger.setLevel(Level.ALL);
		}
		else {
			logger.setLevel(Level.INFO);
		}
	}

	/**
	 * Log a message as a specified log level
	 * @param lvl DEBUG for debug messages, any other level will always be logged
	 * @param msg message to log
	 */
	public static void log(String lvl, String msg) {
		if ("DEBUG".equals(lvl)) {
			if (logger.getLevel() == Level.ALL) {
				// not sure why this does not work, so use System.out instead
				//logger.fine(msg);
				System.out.print(msg);
			}
		}
		else {
			// not sure why this buffers in eclipse sometimes, so use System.out instead
			//logger.info(msg);
			System.out.print(msg);
		}
	}

	public static void logln(String lvl, String msg) {
		log(lvl, msg + "\n");
	}

	/**
	 * Log a message at non-debug level
	 * @param msg
	 */
	public static void log(String msg) {
		log("INFO", msg);
	}

	public static void logln(String msg) {
		log(msg + "\n");
	}
}
