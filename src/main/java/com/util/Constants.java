package com.util;

import java.awt.Color;

public class Constants {
    // Application info
    public static final String APP_NAME = "ID Photo Generator";
    public static final String APP_VERSION = "1.0.0";
    
    // UI constants
    public static final int DEFAULT_WINDOW_WIDTH = 1024;
    public static final int DEFAULT_WINDOW_HEIGHT = 768;
    public static final int PREVIEW_PANEL_WIDTH = 400;
    public static final int CONTROL_PANEL_WIDTH = 250;
    
    // ID photo standard dimensions (in pixels at 300 DPI)
    public static final int PASSPORT_PHOTO_WIDTH_MM = 35;  // 35mm
    public static final int PASSPORT_PHOTO_HEIGHT_MM = 45; // 45mm
    public static final int PIXELS_PER_MM = 12;            // at 300 DPI
    
    public static final int PASSPORT_PHOTO_WIDTH_PX = PASSPORT_PHOTO_WIDTH_MM * PIXELS_PER_MM;
    public static final int PASSPORT_PHOTO_HEIGHT_PX = PASSPORT_PHOTO_HEIGHT_MM * PIXELS_PER_MM;
    
    // Background colors
    public static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
    public static final Color[] STANDARD_BACKGROUND_COLORS = {
        Color.WHITE,
        new Color(215, 215, 255),  // Light blue
        new Color(230, 230, 230)   // Light gray
    };
    
    // File formats
    public static final String[] SUPPORTED_INPUT_FORMATS = {
        "jpg", "jpeg", "png", "bmp", "gif"
    };
    
    public static final String[] SUPPORTED_OUTPUT_FORMATS = {
        "jpg", "png", "bmp"
    };
    
    // Processing constants
    public static final int DEFAULT_GRABCUT_ITERATIONS = 5;
    public static final double DEFAULT_EDGE_THRESHOLD = 0.5;
    public static final int DEFAULT_BLUR_RADIUS = 3;
    
    // Export constants
    public static final int DEFAULT_JPEG_QUALITY = 90;
    public static final String DEFAULT_EXPORT_PREFIX = "ID_Photo_";
}
