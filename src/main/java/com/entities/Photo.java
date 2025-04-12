package com.entities;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;

public class Photo implements Cloneable {
    // Store original image as BufferedImage for stability
    private BufferedImage originalBufferedImage;
    private Frame processedFrame;
    private String fileName;
    private LocalDateTime uploadTime;
    private int width;
    private int height;
    private File sourceFile;

    private static final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private static final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

    public Photo(Frame frame, String fileName, File sourceFile) {
        // Store original as BufferedImage to prevent corruption
        this.originalBufferedImage = java2DConverter.convert(frame);
        // Keep processed frame as Frame for editing
        this.processedFrame = frame.clone();
        this.fileName = fileName;
        this.uploadTime = LocalDateTime.now();
        this.sourceFile = sourceFile;
        updateDimensions();
    }

    private void updateDimensions() {
        if (processedFrame != null) {
            width = processedFrame.imageWidth;
            height = processedFrame.imageHeight;
        }
    }

    public Frame getOriginalFrame() {
        // Convert the stored BufferedImage back to a Frame when needed
        return java2DConverter.convert(originalBufferedImage);
    }

    public Frame getProcessedFrame() {
        return processedFrame;
    }

    public void setProcessedFrame(Frame processedFrame) {
        this.processedFrame = processedFrame;
        updateDimensions();
    }

    public BufferedImage getProcessedBufferedImage() {
        return java2DConverter.convert(processedFrame);
    }

    public BufferedImage getOriginalBufferedImage() {
        return originalBufferedImage;
    }

    public Mat getProcessedMat() {
        return matConverter.convert(processedFrame);
    }

    public String getFileName() {
        return fileName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    /**
     * Creates a deep copy of the Photo object
     * 
     * @return A cloned Photo object with copies of all frames
     */
    @Override
    public Photo clone() {
        try {
            Photo clone = (Photo) super.clone();

            // Deep copy the original BufferedImage
            if (this.originalBufferedImage != null) {
                // Create a new BufferedImage with the same properties
                BufferedImage copy = new BufferedImage(
                        this.originalBufferedImage.getWidth(),
                        this.originalBufferedImage.getHeight(),
                        this.originalBufferedImage.getType());

                // Copy the pixel data
                copy.getGraphics().drawImage(this.originalBufferedImage, 0, 0, null);
                clone.originalBufferedImage = copy;
            }

            // Deep copy the processed frame
            if (this.processedFrame != null) {
                // Convert to BufferedImage and back for clean copy
                BufferedImage temp = java2DConverter.convert(this.processedFrame);
                clone.processedFrame = java2DConverter.convert(temp);
            }

            return clone;
        } catch (CloneNotSupportedException e) {
            // This should not happen since we implement Cloneable
            throw new RuntimeException("Failed to clone Photo", e);
        }
    }

    /**
     * Primary implementation: Reset using the stored BufferedImage
     */
    public void resetToOriginal() {
        if (originalBufferedImage != null) {
            // Convert the original BufferedImage back to a Frame
            try {
                // Clean conversion through BufferedImage
                this.processedFrame = java2DConverter.convert(originalBufferedImage);
                updateDimensions();
                System.out.println("Reset to original using BufferedImage conversion");
            } catch (Exception e) {
                System.err.println("Error in primary reset method: " + e.getMessage());
                // Fall back to secondary methods
                resetToOriginalFromFile();
            }
        } else {
            // If original BufferedImage is null, try to reset from file
            resetToOriginalFromFile();
        }
    }

    /**
     * Fallback implementation: Reload from source file
     */
    private void resetToOriginalFromFile() {
        if (sourceFile != null && sourceFile.exists()) {
            try {
                // Reload the image from its source file
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFile);
                grabber.start();
                this.processedFrame = grabber.grabImage();
                grabber.stop();

                // Also update the original BufferedImage
                this.originalBufferedImage = java2DConverter.convert(this.processedFrame);

                updateDimensions();
                System.out.println("Reset to original by reloading from file");
            } catch (Exception e) {
                System.err.println("Error reloading original image: " + e.getMessage());
            }
        } else {
            System.err.println("Cannot reset - source file not available");
        }
    }
}