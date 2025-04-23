package de.zonlykroks.massasmer.util;

import org.apache.logging.log4j.Logger;

public class LoggerWrapper {
    private final Logger logger;
    private final boolean debug;

    public LoggerWrapper(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }

    // INFO
    public void info(String message) {
        if (debug) logger.info(message);
    }

    public void info(String format, Object... args) {
        if (debug) logger.info(format, args);
    }

    // DEBUG
    public void debug(String message) {
        if (debug) logger.debug(message);
    }

    public void debug(String format, Object... args) {
        if (debug) logger.debug(format, args);
    }

    // WARN
    public void warn(String message) {
        if (debug) logger.warn(message);
    }

    public void warn(String format, Object... args) {
        if (debug) logger.warn(format, args);
    }

    // ERROR
    public void error(String message) {
        if (debug) logger.error(message);
    }

    public void error(String format, Object... args) {
        if (debug) logger.error(format, args);
    }

    // TRACE
    public void trace(String message) {
        if (debug) logger.trace(message);
    }

    public void trace(String format, Object... args) {
        if (debug) logger.trace(format, args);
    }

    // Fatal (optional depending on your logger)
    public void fatal(String message) {
        if (debug && logger instanceof org.apache.logging.log4j.spi.ExtendedLogger) {
            logger.fatal(message);
        }
    }

    public void fatal(String format, Object... args) {
        if (debug && logger instanceof org.apache.logging.log4j.spi.ExtendedLogger) {
            logger.fatal(format, args);
        }
    }

    // Getter if needed externally
    public Logger getUnderlyingLogger() {
        return logger;
    }
}

