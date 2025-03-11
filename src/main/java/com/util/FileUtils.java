package com.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Utility class for file operations related to image handling
 */
public class FileUtils {
    
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png");
    
    /**
     * Validates if the file is an image with supported format
     * 
     * @param file File to validate
     * @return true if file is a valid image with supported format
     */
    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        
        return SUPPORTED_FORMATS.contains(extension);
    }
    
    /**
     * Saves an image to the specified location
     * 
     * @param image BufferedImage to save
     * @param format Format of the image (jpg, png)
     * @param outputPath Path where the image will be saved
     * @return true if the image was saved successfully
     */
    public static boolean saveImage(BufferedImage image, String format, String outputPath) {
        try {
            File outputFile = new File(outputPath);
            // Create parent directories if they don't exist
            outputFile.getParentFile().mkdirs();
            return ImageIO.write(image, format, outputFile);
        } catch (IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a temporary file for image processing
     * 
     * @param prefix File prefix
     * @param suffix File suffix (extension)
     * @return File object of the created temporary file
     * @throws IOException if temporary file creation fails
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix);
    }
    
    /**
     * Copies a file to a new location
     * 
     * @param source Source file
     * @param destination Destination file
     * @return true if the file was copied successfully
     */
    public static boolean copyFile(File source, File destination) {
        try {
            Path sourcePath = Paths.get(source.getAbsolutePath());
            Path destinationPath = Paths.get(destination.getAbsolutePath());
            
            // Create parent directories if they don't exist
            destination.getParentFile().mkdirs();
            
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Error copying file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extracts the file extension from a file path
     * 
     * @param filePath Path of the file
     * @return File extension without the dot
     */
    public static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * Checks if a directory exists and creates it if it doesn't
     * 
     * @param directoryPath Path of the directory
     * @return true if the directory exists or was created successfully
     */
    public static boolean ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return directory.mkdirs();
        }
        return directory.isDirectory();
    }
}