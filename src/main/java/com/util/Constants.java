package com.util;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants used throughout the application
 */
public class Constants {
    
    // Application info
    public static final String APP_NAME = "ID Photo Generator";
    public static final String APP_VERSION = "1.0.0";
    
    // File formats
    public static final String FORMAT_JPG = "jpg";
    public static final String FORMAT_PNG = "png";
    
    // Standard ID photo dimensions in pixels (at 300 DPI)
    public static final Map<String, Dimension> STANDARD_PHOTO_DIMENSIONS = new HashMap<String, Dimension>() {{
        // 35mm x 45mm - Passport photos (most countries)
        put("Passport (35x45mm)", new Dimension(413, 531));
        
        // 2x2 inches - US Passport/Visa
        put("US Passport (2x2\")", new Dimension(600, 600));
        
        // 33mm x 48mm - ID Card
        put("ID Card (33x48mm)", new Dimension(390, 567));
        
        // 25mm x 35mm - Driving License
        put("Driving License (25x35mm)", new Dimension(295, 413));
        
        // Custom size - Will be configured by the user
        put("Custom", new Dimension(0, 0));
    }};
    
    // Standard background colors for ID photos
    public static final Map<String, Color> STANDARD_BACKGROUND_COLORS = new HashMap<String, Color>() {{
        put("White", Color.WHITE);
        put("Light Blue", new Color(215, 235, 250));
        put("Light Gray", new Color(230, 230, 230));
        put("Dark Blue", new Color(0, 51, 102));
        put("Red", new Color(200, 16, 46));
    }};
    
    // File dialog filter descriptions
    public static final String FILTER_IMAGES = "Image Files (*.jpg, *.jpeg, *.png)";
    
    // UI constants
    public static final int PREVIEW_WIDTH = 300;
    public static final int PREVIEW_HEIGHT = 400;
    public static final int THUMBNAIL_SIZE = 100;
    public static final int DEFAULT_BORDER_SIZE = 5;
    public static final int DEFAULT_SPACING = 10;
    
    // Background removal constants
    public static final int GRABCUT_ITERATIONS = 5;
    public static final int FOREGROUND_MARKER = 1;
    public static final int BACKGROUND_MARKER = 0;
    public static final int POSSIBLE_FOREGROUND_MARKER = 2;
    public static final int POSSIBLE_BACKGROUND_MARKER = 3;
    
    // Default settings
    public static final String DEFAULT_PHOTO_TYPE = "Passport (35x45mm)";
    public static final String DEFAULT_BACKGROUND_COLOR = "White";
    public static final String DEFAULT_OUTPUT_FORMAT = FORMAT_JPG;
    public static final int DEFAULT_QUALITY = 90;
    
    // Error messages
    public static final String ERROR_LOADING_IMAGE = "Error loading image. Please try another file.";
    public static final String ERROR_PROCESSING_IMAGE = "Error processing image. Please try again.";
    public static final String ERROR_SAVING_IMAGE = "Error saving image. Please check the output location.";
    public static final String ERROR_NO_IMAGE_LOADED = "No image loaded. Please upload an image first.";
    
    // Success messages
    public static final String SUCCESS_IMAGE_SAVED = "Image saved successfully!";
    
    // Configuration file
    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String CONFIG_LAST_DIRECTORY = "last.directory";
    public static final String CONFIG_LAST_PHOTO_TYPE = "last.photo.type";
    public static final String CONFIG_LAST_BACKGROUND_COLOR = "last.background.color";
    public static final String CONFIG_LAST_OUTPUT_FORMAT = "last.output.format";
    public static final String CONFIG_LAST_QUALITY = "last.quality";
    
    // Face detection parameters
    public static final double FACE_RELATIVE_SIZE = 0.7; // Face should occupy 70% of the photo height
    public static final double FACE_TOP_POSITION = 0.3; // The top of the face should be 30% from the top of the photo
}