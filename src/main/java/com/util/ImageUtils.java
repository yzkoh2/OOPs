package com.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.RescaleOp;


/**
 * Utility class for image manipulation operations
 */
public class ImageUtils {
    
    /**
     * Resizes an image to the specified width and height
     * 
     * @param originalImage The original image to resize
     * @param targetWidth The target width
     * @param targetHeight The target height
     * @param preserveRatio Whether to preserve the aspect ratio
     * @return The resized image
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight, boolean preserveRatio) {
        if (originalImage == null) {
            return null;
        }
        
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Calculate dimensions that preserve the aspect ratio if needed
        if (preserveRatio) {
            double ratio = (double) originalWidth / (double) originalHeight;
            
            if (originalWidth > originalHeight) {
                targetHeight = (int) (targetWidth / ratio);
            } else {
                targetWidth = (int) (targetHeight * ratio);
            }
        }
        
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D g = resizedImage.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        
        return resizedImage;
    }
    
    /**
     * Crops an image to the specified dimensions
     * 
     * @param originalImage The original image to crop
     * @param x The x-coordinate of the top-left corner of the crop area
     * @param y The y-coordinate of the top-left corner of the crop area
     * @param width The width of the crop area
     * @param height The height of the crop area
     * @return The cropped image
     */
    public static BufferedImage cropImage(BufferedImage originalImage, int x, int y, int width, int height) {
        if (originalImage == null) {
            return null;
        }
        
        // Ensure crop region is within image bounds
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > originalImage.getWidth()) width = originalImage.getWidth() - x;
        if (y + height > originalImage.getHeight()) height = originalImage.getHeight() - y;
        
        return originalImage.getSubimage(x, y, width, height);
    }
    
    /**
     * Changes the background color of an image
     * 
     * @param image The image to modify
     * @param maskImage The mask image indicating background pixels (white indicates background)
     * @param backgroundColor The new background color
     * @return The image with the changed background
     */
    public static BufferedImage changeBackgroundColor(BufferedImage image, BufferedImage maskImage, Color backgroundColor) {
        if (image == null || maskImage == null) {
            return image;
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get mask pixel (white means background)
                int maskRGB = maskImage.getRGB(x, y) & 0xFF;
                
                // If pixel is part of background (white in mask), use background color
                if (maskRGB > 240) {
                    result.setRGB(x, y, backgroundColor.getRGB());
                } else {
                    // Otherwise, use original pixel
                    result.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Converts a BufferedImage to an ImageIcon with the specified dimensions
     * 
     * @param image The image to convert
     * @param width The width of the icon
     * @param height The height of the icon
     * @return The ImageIcon
     */
    public static ImageIcon createImageIcon(BufferedImage image, int width, int height) {
        if (image == null) {
            return null;
        }
        
        BufferedImage resized = resizeImage(image, width, height, true);
        return new ImageIcon(resized);
    }
    
    /**
     * Rotates an image by the specified angle in degrees
     * 
     * @param image The image to rotate
     * @param degrees The rotation angle in degrees
     * @return The rotated image
     */
    public static BufferedImage rotateImage(BufferedImage image, double degrees) {
        if (image == null) {
            return null;
        }
        
        double radians = Math.toRadians(degrees);
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Calculate dimensions of the rotated image
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth = (int) Math.round(width * cos + height * sin);
        int newHeight = (int) Math.round(height * cos + width * sin);
        
        // Create a new buffered image
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = rotated.createGraphics();
        
        // Set rendering hints for better quality
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Create an AffineTransform for rotation
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - width) / 2.0, (newHeight - height) / 2.0);
        at.rotate(radians, width / 2.0, height / 2.0);
        
        // Draw the image with the transform
        g.drawImage(image, at, null);
        g.dispose();
        
        return rotated;
    }
    
    /**
     * Adjusts the brightness of an image
     * 
     * @param image The image to adjust
     * @param factor Brightness adjustment factor (1.0 is original, > 1.0 is brighter, < 1.0 is darker)
     * @return The brightness-adjusted image
     */
    public static BufferedImage adjustBrightness(BufferedImage image, float factor) {
        if (image == null) {
            return null;
        }
        
        BufferedImage adjusted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        
        // Create a rescale operation with the brightness factor
        float[] scales = {factor, factor, factor, 1.0f}; // RGB scales, Alpha unchanged
        float[] offsets = {0, 0, 0, 0}; // No offsets
        
        RescaleOp op = new RescaleOp(scales, offsets, null);
        
        // Apply the operation to create the adjusted image
        op.filter(image, adjusted);
        
        return adjusted;
    }
    
    /**
     * Converts a BufferedImage to grayscale
     * 
     * @param image The image to convert
     * @return The grayscale image
     */
    public static BufferedImage convertToGrayscale(BufferedImage image) {
        if (image == null) {
            return null;
        }
        
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = result.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        return result;
    }
}