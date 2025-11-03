package de.zonlykroks.massasmer.util;

import org.apache.logging.log4j.Logger;

public record LoggerWrapper(Logger logger, boolean debug) {

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

