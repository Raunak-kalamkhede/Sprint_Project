package com.capstone.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader — Reads configuration from config.properties.
 *
 * WHY: No hardcoding. All URLs, credentials, timeouts, and flags live in one file.
 * If the environment changes (e.g., URL changes), only config.properties needs editing.
 *
 * Uses Singleton pattern — loads properties ONCE, reuses the same Properties object.
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static final String CONFIG_FILE = "config/config.properties";

    // Singleton Properties instance — loaded once when class is first used
    private static Properties properties;

    static {
        loadProperties();
    }

    private ConfigReader() {}

    /**
     * Loads config.properties from the classpath (src/test/resources/config/).
     * Called once via static initialiser block.
     */
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream inputStream =
                     ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new RuntimeException(
                    "config.properties NOT found at: " + CONFIG_FILE +
                    ". Ensure it exists under src/test/resources/config/"
                );
            }
            properties.load(inputStream);
            log.info("Configuration loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to load config.properties: {}", e.getMessage());
            throw new RuntimeException("Config load failure: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a String property value.
     *
     * @param key Property key (e.g., "base.url")
     * @return Property value
     * @throws RuntimeException if key is not found
     */
    public static String get(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Property '" + key + "' not found in config.properties");
        }
        return value.trim();
    }

    /**
     * Gets a String property with a default fallback.
     */
    public static String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    /**
     * Gets an integer property value.
     */
    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    /**
     * Gets a boolean property value.
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Gets a long property value.
     */
    public static long getLong(String key) {
        return Long.parseLong(get(key));
    }

    // === Convenience getters for commonly used config values ===

    public static String getBaseUrl()        { return get("base.url"); }
    public static String getApiBaseUrl()     { return get("api.base.url"); }
    public static String getEmail()          { return get("email"); }
    public static String getPassword()       { return get("password"); }
    public static String getBrowser()        { return get("browser", "chrome"); }
    public static boolean isHeadless()       { return getBoolean("headless"); }
    public static int getExplicitWait()      { return getInt("explicit.wait"); }
    public static int getImplicitWait()      { return getInt("implicit.wait"); }
    public static long getApiThresholdMs()   { return getLong("api.response.threshold.ms"); }
    public static long getUiThresholdMs()    { return getLong("ui.page.load.threshold.ms"); }
    public static String getTestDataFile()   { return get("test.data.file"); }
    public static String getScreenshotDir()  { return get("screenshot.dir"); }
}
