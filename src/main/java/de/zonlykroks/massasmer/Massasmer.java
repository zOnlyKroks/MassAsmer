package de.zonlykroks.massasmer;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Massasmer implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("Massasmer");

    @Override
    public void onInitialize() {
        LOGGER.info("If you got this far, you are a true gamer.");
    }
}
