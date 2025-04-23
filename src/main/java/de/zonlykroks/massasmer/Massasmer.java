package de.zonlykroks.massasmer;

import de.zonlykroks.massasmer.util.LoggerWrapper;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;

public class Massasmer implements ModInitializer {

    private static final LoggerWrapper LOGGER = new LoggerWrapper(LogManager.getLogger("Massasmer"), MassasmerPreLaunch.configManager.isLogEnabled());

    @Override
    public void onInitialize() {
        LOGGER.info("If you got this far, you are a true gamer.");
    }
}
