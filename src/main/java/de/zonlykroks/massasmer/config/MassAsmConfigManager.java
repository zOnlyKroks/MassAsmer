package de.zonlykroks.massasmer.config;

import de.zonlykroks.massasmer.MassasmerPreLaunch;
import de.zonlykroks.massasmer.filter.Filters;
import de.zonlykroks.massasmer.filter.api.TransformerFilter;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MassAsmConfigManager {
    private final String KEY_ATTACH = "allow-attach-non-fail-hard";
    private final String KEY_LOG = "enable-log";
    private final String KEY_EXCLUSIONS = "transformer-exclusions";

    // Default exclusions that will be used only when creating the config file for the first time
    private static final String DEFAULT_EXCLUSIONS =
            "java.,javax.,sun.,com.sun.,jdk.," +         // Java core
                    "org.objectweb.asm," +                        // ASM library
                    "org.apache.logging," +                       // Log4j and other Apache logging
                    "net.fabricmc.loader," +                      // Fabric Loader
                    "org.slf4j," +                                // SLF4J logging
                    "com.google," +                               // Google libraries (like Guava)
                    "io.netty," +                                 // Netty networking
                    "it.unimi.dsi.fastutil," +                    // FastUtil
                    "org.apache.commons," +                       // Apache Commons
                    "org.apache.http," +                          // Apache HTTP
                    "lombok";                                     // Lombok

    private final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("massasm.properties");
    private final Properties PROPS = new Properties();
    private final TransformerFilter exclusionFilter;

    public MassAsmConfigManager() {
        load();
        this.exclusionFilter = buildExclusionFilter();
    }

    private void load() {
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
                PROPS.load(is);
            } catch (IOException e) {
                MassasmerPreLaunch.LOGGER.error("[MassASM] Failed to load config {}", e.getMessage());
            }
        } else {
            // Set default properties only when creating the file for the first time
            PROPS.setProperty(KEY_ATTACH, Boolean.toString(false));
            PROPS.setProperty(KEY_LOG, Boolean.toString(true));
            PROPS.setProperty(KEY_EXCLUSIONS, DEFAULT_EXCLUSIONS);
            save();
        }
    }

    private void save() {
        try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
            PROPS.store(os, "MassASM Configuration");
        } catch (IOException e) {
            MassasmerPreLaunch.LOGGER.error("[MassASM] Failed to save config: {}", e.getMessage());
        }
    }

    /**
     * Builds a transformer filter from the configured exclusions.
     *
     * @return A TransformerFilter that returns false for classes that should not be transformed
     */
    private TransformerFilter buildExclusionFilter() {
        // Use whatever is in the properties file, empty string if property doesn't exist
        String exclusionsStr = PROPS.getProperty(KEY_EXCLUSIONS, "");
        String[] exclusions = exclusionsStr.split(",");

        // Build the exclusion filter from the loaded exclusions
        TransformerFilter filter = null;
        for (String exclusion : exclusions) {
            if (exclusion == null || exclusion.trim().isEmpty()) {
                continue;
            }

            if (filter == null) {
                filter = Filters.startsWith(exclusion.trim());
            } else {
                filter = filter.or(Filters.startsWith(exclusion.trim()));
            }
        }

        // Negate the filter so it returns false for excluded packages
        return filter != null ? filter.negate() : Filters.all();
    }

    /**
     * Check if non-failing attach is allowed.
     * @return true if allowed, false otherwise
     */
    public boolean isAllowAttachNonFailHard() {
        return Boolean.parseBoolean(PROPS.getProperty(KEY_ATTACH, "false"));
    }

    /**
     * Check if logging is enabled.
     * @return true if enabled, false otherwise
     */
    public boolean isLogEnabled() {
        return Boolean.parseBoolean(PROPS.getProperty(KEY_LOG, "true"));
    }

    /**
     * Gets the transformer exclusion filter that prevents transformation of
     * core Java classes, the transformer API itself, and other critical systems.
     *
     * @return A TransformerFilter that returns false for classes that should not be transformed
     */
    public TransformerFilter getTransformerExclusionFilter() {
        return exclusionFilter;
    }

    /**
     * Gets the raw exclusion string from the configuration.
     *
     * @return A comma-separated list of package prefixes that should be excluded from transformation
     */
    public String getExclusionsString() {
        return PROPS.getProperty(KEY_EXCLUSIONS, "");
    }
}