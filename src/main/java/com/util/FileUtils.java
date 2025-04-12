package com.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.entities.Photo;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FileUtils {
    private static final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    
    public static void ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public static Photo loadPhoto(File file) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            Frame frame = grabber.grabImage();
            if (frame == null) {
                throw new IOException("Failed to grab frame from file: " + file.getAbsolutePath());
            }
            
            // Fix orientation if this is a JPEG file
            if (isJpegFile(file)) {
                frame = correctOrientation(frame, file);
            }
            
            return new Photo(frame, file.getName(), file);
        }
    }
    
    private static Frame correctOrientation(Frame frame, File imageFile) {
        try {
            // Read EXIF metadata
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            
            if (exifIFD0 != null && exifIFD0.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                int orientation = exifIFD0.getInt(ExifDirectoryBase.TAG_ORIENTATION);
                
                // Convert frame to BufferedImage for rotation
                BufferedImage bufferedImage = java2DConverter.convert(frame);
                
                // Apply rotation based on orientation tag
                switch (orientation) {
                    case 1: // Normal orientation - no change needed
                        return frame; 
                    case 2: // Flipped horizontally
                        bufferedImage = flipHorizontally(bufferedImage);
                        break;
                    case 3: // Rotated 180 degrees
                        bufferedImage = rotate(bufferedImage, 180);
                        break;
                    case 4: // Flipped vertically
                        bufferedImage = flipVertically(bufferedImage);
                        break;
                    case 5: // Rotated 90 degrees CW and flipped horizontally
                        bufferedImage = rotate(bufferedImage, 90);
                        bufferedImage = flipHorizontally(bufferedImage);
                        break;
                    case 6: // Rotated 90 degrees CW
                        bufferedImage = rotate(bufferedImage, 90);
                        break;
                    case 7: // Rotated 90 degrees CW and flipped vertically
                        bufferedImage = rotate(bufferedImage, 90);
                        bufferedImage = flipVertically(bufferedImage);
                        break;
                    case 8: // Rotated 270 degrees CW
                        bufferedImage = rotate(bufferedImage, 270);
                        break;
                }
                
                // Convert corrected BufferedImage back to Frame
                return java2DConverter.convert(bufferedImage);
            }
        } catch (ImageProcessingException | IOException | com.drew.metadata.MetadataException e) {
            System.err.println("Error reading EXIF data: " + e.getMessage());
            // If any error occurs, return the original frame
        }
        
        return frame;
    }
    private static BufferedImage rotate(BufferedImage image, int degrees) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage rotated;
        if (degrees == 90 || degrees == 270) {
            rotated = new BufferedImage(height, width, image.getType());
        } else {
            rotated = new BufferedImage(width, height, image.getType());
        }
        
        double radians = Math.toRadians(degrees);
        AffineTransform transform = new AffineTransform();
        
        if (degrees == 90) {
            transform.translate(height, 0);
        } else if (degrees == 180) {
            transform.translate(width, height);
        } else if (degrees == 270) {
            transform.translate(0, width);
        }
        
        transform.rotate(radians);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        op.filter(image, rotated);
        
        return rotated;
    }
    
    private static BufferedImage flipHorizontally(BufferedImage image) {
        AffineTransform transform = AffineTransform.getScaleInstance(-1, 1);
        transform.translate(-image.getWidth(), 0);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(image, null);
    }
    
    private static BufferedImage flipVertically(BufferedImage image) {
        AffineTransform transform = AffineTransform.getScaleInstance(1, -1);
        transform.translate(0, -image.getHeight());
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(image, null);
    }
    
    private static boolean isJpegFile(File file) {
        String extension = getFileExtension(file).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg");
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public static boolean isImageFile(File file) {
        if (!file.isFile()) {
            return false;
        }

        String extension = getFileExtension(file);
        return extension.equals("jpg") ||
                extension.equals("jpeg") ||
                extension.equals("png") ||
                extension.equals("bmp") ||
                extension.equals("gif");
    }
}