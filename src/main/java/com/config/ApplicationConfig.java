package com.config;

import com.util.Constants;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ApplicationConfig {
    private static final String CONFIG_FILE = "idphoto_config.properties";
    private Properties properties;
    private static ApplicationConfig instance;
    
    private ApplicationConfig() {
        properties = new Properties();
        loadConfig();
    }
    
    public static synchronized ApplicationConfig getInstance() {
        if (instance == null) {
            instance = new ApplicationConfig();
        }
        return instance;
    }
    
    private void loadConfig() {
        Path configPath = Paths.get(System.getProperty("user.home"), CONFIG_FILE);
        
        if (Files.exists(configPath)) {
            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading configuration: " + e.getMessage());
                setDefaultProperties();
            }
        } else {
            setDefaultProperties();
            saveConfig();
        }
    }
    
    private void setDefaultProperties() {
        // UI settings
        properties.setProperty("ui.theme", "light");
        properties.setProperty("ui.window.width", String.valueOf(Constants.DEFAULT_WINDOW_WIDTH));
        properties.setProperty("ui.window.height", String.valueOf(Constants.DEFAULT_WINDOW_HEIGHT));
        
        // Export settings
        properties.setProperty("export.directory", System.getProperty("user.home"));
        properties.setProperty("export.format", "jpg");
        properties.setProperty("export.quality", String.valueOf(Constants.DEFAULT_JPEG_QUALITY));
        properties.setProperty("export.prefix", Constants.DEFAULT_EXPORT_PREFIX);
        
        // Background settings
        properties.setProperty("background.color.red", "255");
        properties.setProperty("background.color.green", "255");
        properties.setProperty("background.color.blue", "255");
        properties.setProperty("background.blur.radius", String.valueOf(Constants.DEFAULT_BLUR_RADIUS));
        properties.setProperty("background.edge.threshold", String.valueOf(Constants.DEFAULT_EDGE_THRESHOLD));
        properties.setProperty("background.grabcut.iterations", String.valueOf(Constants.DEFAULT_GRABCUT_ITERATIONS));
        
        // Photo dimensions
        properties.setProperty("photo.width.mm", String.valueOf(Constants.PASSPORT_PHOTO_WIDTH_MM));
        properties.setProperty("photo.height.mm", String.valueOf(Constants.PASSPORT_PHOTO_HEIGHT_MM));
        properties.setProperty("photo.dpi", "300");
    }
    
    public void saveConfig() {
        Path configPath = Paths.get(System.getProperty("user.home"), CONFIG_FILE);
        
        try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
            properties.store(fos, "ID Photo Generator Configuration");
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double getDoubleProperty(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
}
