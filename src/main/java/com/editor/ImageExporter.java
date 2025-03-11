package com.editor;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.*;
// import org.w3c.dom.css.Rect;
import org.bytedeco.javacpp.IntPointer;


import com.entities.ExportSettings;

/**
 * Handles exporting processed images to files.
 */
public class ImageExporter implements ImageProcessor {
    
    private ExportSettings settings;
    
    public ImageExporter() {
        this.settings = new ExportSettings();
    }
    
    public ImageExporter(ExportSettings settings) {
        this.settings = settings;
    }
    
    @Override
    public Mat process(Mat inputImage) {
        // Default implementation just returns the input
        return inputImage.clone();
    }
    
    /**
     * Exports image to file with current settings.
     */
    public boolean exportImage(Mat image) {
        try {
            // Ensure output directory exists
            File outputDir = new File(settings.getOutputFolder());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // Set compression parameters
            int[] compressionArray = new int[2]; // Only max 2 values needed
            boolean hasParams = false;

            if (settings.getFormat().equalsIgnoreCase("jpg") || 
                settings.getFormat().equalsIgnoreCase("jpeg")) {
                compressionArray[0] = IMWRITE_JPEG_QUALITY;
                compressionArray[1] = settings.getQuality();
                hasParams = true;
            } else if (settings.getFormat().equalsIgnoreCase("png")) {
                compressionArray[0] = IMWRITE_PNG_COMPRESSION;
                compressionArray[1] = 9; // Maximum compression for PNG
                hasParams = true;
            }

            // Convert to IntPointer
            IntPointer params = hasParams ? new IntPointer(compressionArray) : new IntPointer();

            // Save the image
            String outputPath = settings.getFullOutputPath();
            return imwrite(outputPath, image, params);
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Creates a sheet with multiple copies of the ID photo.
     */
    public Mat createMultipleLayout(Mat image) {
        int rows = settings.getRows();
        int cols = settings.getColumns();
        
        // Calculate dimensions for the sheet
        int sheetWidth = image.cols() * cols;
        int sheetHeight = image.rows() * rows;
        
        // Create a white sheet
        Mat sheet = new Mat(sheetHeight, sheetWidth, image.type(), new Scalar(255, 255, 255, 255));
        
        // Copy the image multiple times onto the sheet
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * image.cols();
                int y = r * image.rows();
                
                Rect roi = new Rect(x, y, image.cols(), image.rows());
                Mat destination = new Mat(sheet, roi);
                image.copyTo(destination);
            }
        }
        
        return sheet;
    }
    
    /**
     * Exports multiple layout sheet to file.
     */
    public boolean exportMultipleLayout(Mat image) {
        Mat sheet = createMultipleLayout(image);
        
        // Modify filename to indicate it's a sheet
        String originalFilename = settings.getOutputFilename();
        settings.setOutputFilename(originalFilename + "_sheet");
        
        // Export the sheet
        boolean result = exportImage(sheet);
        
        // Restore original filename
        settings.setOutputFilename(originalFilename);
        
        return result;
    }
    
    // Getters and setters
    public ExportSettings getSettings() {
        return settings;
    }
    
    public void setSettings(ExportSettings settings) {
        this.settings = settings;
    }
}