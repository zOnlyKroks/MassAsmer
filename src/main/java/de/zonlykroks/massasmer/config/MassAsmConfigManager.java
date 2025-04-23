package de.zonlykroks.massasmer.config;

import de.zonlykroks.massasmer.MassasmerPreLaunch;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MassAsmConfigManager {

    private final String KEY = "allow-attach-non-fail-hard";
    private final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("massasm.properties");
    private final Properties PROPS = new Properties();

    public MassAsmConfigManager() {
        load();
    }

    private void load() {
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
                PROPS.load(is);
            } catch (IOException e) {
                System.err.println("[MassASM] Failed to load config: " + e.getMessage());
            }
        } else {
            PROPS.setProperty(KEY, Boolean.toString(false));
            save();
        }
    }

    private void save() {
        try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
            PROPS.store(os, "MassASM Configuration");
        } catch (IOException e) {
            MassasmerPreLaunch.LOGGER.error("[MassASM] Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Check if non-failing attach is allowed.
     * @return true if allowed, false otherwise
     */
    public boolean isAllowAttachNonFailHard() {
        return Boolean.parseBoolean(PROPS.getProperty(KEY, "false"));
    }
}
