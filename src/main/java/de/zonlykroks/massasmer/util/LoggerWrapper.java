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

    public void warn(String format, Object... args) {
        if (debug) logger.warn(format, args);
    }


    public void error(String format, Object... args) {
        if (debug) logger.error(format, args);
    }
}

