package com.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.util.Constants;

/**
 * Manages application configuration and settings
 */
public class ApplicationConfig {
    
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String CONFIG_DIR = ".idphotogenerator";
    private static final String CONFIG_PATH = USER_HOME + "/" + CONFIG_DIR + "/" + Constants.CONFIG_FILE_NAME;
    
    private static Properties properties;
    private static ApplicationConfig instance;
    
    private ApplicationConfig() {
        initConfig();
    }
    
    /**
     * Returns the singleton instance of ApplicationConfig
     * 
     * @return ApplicationConfig instance
     */
    public static synchronized ApplicationConfig getInstance() {
        if (instance == null) {
            instance = new ApplicationConfig();
        }
        return instance;
    }
    
    /**
     * Initializes the configuration
     */
    private void initConfig() {
        properties = new Properties();
        
        // Create configuration directory if it doesn't exist
        Path configDir = Paths.get(USER_HOME, CONFIG_DIR);
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            Path configFile = Paths.get(CONFIG_PATH);
            
            // Create the config file if it doesn't exist
            if (!Files.exists(configFile)) {
                setDefaultProperties();
                saveConfig();
            } else {
                loadConfig();
            }
        } catch (IOException e) {
            System.err.println("Error initializing configuration: " + e.getMessage());
            setDefaultProperties();
        }
    }
    
    /**
     * Loads configuration from file
     */
    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_PATH)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            setDefaultProperties();
        }
    }
    
    /**
     * Saves configuration to file
     */
    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_PATH)) {
            properties.store(output, "ID Photo Generator Configuration");
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }
    
    /**
     * Sets default property values
     */
    private void setDefaultProperties() {
        properties.setProperty(Constants.CONFIG_LAST_DIRECTORY, USER_HOME);
        properties.setProperty(Constants.CONFIG_LAST_PHOTO_TYPE, Constants.DEFAULT_PHOTO_TYPE);
        properties.setProperty(Constants.CONFIG_LAST_BACKGROUND_COLOR, Constants.DEFAULT_BACKGROUND_COLOR);
        properties.setProperty(Constants.CONFIG_LAST_OUTPUT_FORMAT, Constants.DEFAULT_OUTPUT_FORMAT);
        properties.setProperty(Constants.CONFIG_LAST_QUALITY, String.valueOf(Constants.DEFAULT_QUALITY));
    }
    
    /**
     * Gets a property value
     * 
     * @param key Property key
     * @return Property value
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Gets a property value with a default
     * 
     * @param key Property key
     * @param defaultValue Default value if property doesn't exist
     * @return Property value or default
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Sets a property value
     * 
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Gets the last used directory for file operations
     * 
     * @return Path to the last used directory
     */
    public String getLastDirectory() {
        return getProperty(Constants.CONFIG_LAST_DIRECTORY, USER_HOME);
    }
    
    /**
     * Sets the last used directory for file operations
     * 
     * @param directory Path to the directory
     */
    public void setLastDirectory(String directory) {
        setProperty(Constants.CONFIG_LAST_DIRECTORY, directory);
    }
    
    /**
     * Gets the last used photo type
     * 
     * @return Last used photo type
     */
    public String getLastPhotoType() {
        return getProperty(Constants.CONFIG_LAST_PHOTO_TYPE, Constants.DEFAULT_PHOTO_TYPE);
    }
    
    /**
     * Sets the last used photo type
     * 
     * @param photoType Photo type
     */
    public void setLastPhotoType(String photoType) {
        setProperty(Constants.CONFIG_LAST_PHOTO_TYPE, photoType);
    }
    
    /**
     * Gets the last used background color
     * 
     * @return Last used background color
     */
    public String getLastBackgroundColor() {
        return getProperty(Constants.CONFIG_LAST_BACKGROUND_COLOR, Constants.DEFAULT_BACKGROUND_COLOR);
    }
    
    /**
     * Sets the last used background color
     * 
     * @param colorName Background color name
     */
    public void setLastBackgroundColor(String colorName) {
        setProperty(Constants.CONFIG_LAST_BACKGROUND_COLOR, colorName);
    }
    
    /**
     * Gets the last used output format
     * 
     * @return Last used output format
     */
    public String getLastOutputFormat() {
        return getProperty(Constants.CONFIG_LAST_OUTPUT_FORMAT, Constants.DEFAULT_OUTPUT_FORMAT);
    }
    
    /**
     * Sets the last used output format
     * 
     * @param format Output format
     */
    public void setLastOutputFormat(String format) {
        setProperty(Constants.CONFIG_LAST_OUTPUT_FORMAT, format);
    }
    
    /**
     * Gets the last used quality setting
     * 
     * @return Last used quality setting
     */
    public int getLastQuality() {
        try {
            return Integer.parseInt(getProperty(Constants.CONFIG_LAST_QUALITY, 
                                                String.valueOf(Constants.DEFAULT_QUALITY)));
        } catch (NumberFormatException e) {
            return Constants.DEFAULT_QUALITY;
        }
    }
    
    /**
     * Sets the last used quality setting
     * 
     * @param quality Quality setting
     */
    public void setLastQuality(int quality) {
        setProperty(Constants.CONFIG_LAST_QUALITY, String.valueOf(quality));
    }
}