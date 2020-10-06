package io.github.f2bb.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * For logging and debugging, helps because it shows the recent stack trace so u can see where the print statement was
 *
 * @author devan
 */
public class DLogger {

	/**
	 * Utility class
	 */
	private DLogger() {}

	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static PrintWriter errout;

	public static void writeError(Exception e) {
		e.printStackTrace(errout);
		errout.flush();
	}

	static {
		try {
			errout = new PrintWriter(new BufferedWriter(new FileWriter("errors.txt")));
		} catch (IOException e) {
			errout = null;
			e.printStackTrace();
		}
	}

	static {
		LOGGER.setLevel(Level.FINEST);
		LOGGER.setUseParentHandlers(false);
		LOGGER.addHandler(new Handler() {

			@Override
			public void close() {
				/* dunno what this does */
			}

			@Override
			public void flush() {
				/* dunno what this does */
			}

			@Override
			public void publish(LogRecord r) {
				Level l = r.getLevel();
				if (l.intValue() != Level.SEVERE.intValue()) {
					System.out.printf("%s -> %s : %s\n", r.getParameters()[0], l.toString(), r.getMessage());
				} else {
					System.err.printf("(%s:%d) -> %s : %s\n", r.getParameters()[0], (Integer) r.getParameters()[1], l.toString(), r.getMessage());
				}
			}
		});

	}

	/**
	 * Puts an error message on the consol with the stack trace
	 */
	public static void error(Object msg) {
		LOGGER.log(Level.SEVERE, toString(msg), Thread.currentThread().getStackTrace()[2]);
	}

	/**
	 * Puts an warn message on the consol with the stack trace
	 */
	public static void warn(Object msg) {
		LOGGER.log(Level.WARNING, toString(msg), Thread.currentThread().getStackTrace()[2]);
	}

	/**
	 * Puts an info message on the consol with the stack trace
	 */
	public static void info(Object msg) {
		LOGGER.log(Level.INFO, toString(msg), Thread.currentThread().getStackTrace()[2]);
	}

	/**
	 * Puts an debug message on the consol with the stack trace
	 */
	public static void debug(Object msg) {
		LOGGER.log(Level.CONFIG, toString(msg), Thread.currentThread().getStackTrace()[2]);
	}

	/**
	 * Puts an relief message on the consol with the stack trace
	 */
	public static void relief(Object msg) {
		LOGGER.log(Level.FINE, toString(msg), Thread.currentThread().getStackTrace()[2]);
	}

	private static String toString(Object obj) {
		if(obj instanceof Object[]) {
			return Arrays.deepToString((Object[]) obj);
		}
		return String.valueOf(obj);
	}
}