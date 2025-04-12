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

            // Deep copy the original BufferedImage (remains the same)
            if (this.originalBufferedImage != null) {
                BufferedImage copy = new BufferedImage(
                        this.originalBufferedImage.getWidth(),
                        this.originalBufferedImage.getHeight(),
                        this.originalBufferedImage.getType());
                copy.getGraphics().drawImage(this.originalBufferedImage, 0, 0, null);
                clone.originalBufferedImage = copy;
            }

            // Deep copy the processed frame (Corrected Frame -> BufferedImage -> Frame -> Mat -> Clone -> Frame strategy)
            if (this.processedFrame != null &&
                this.processedFrame.image != null &&
                this.processedFrame.image.length > 0 &&
                this.processedFrame.image[0] != null &&
                this.processedFrame.image[0].limit() > 0) {

                Mat matToClone = null;
                Mat clonedMat = null;
                Frame intermediateFrame = null; // Added for clarity
                Frame finalClonedFrame = null;

                try {
                    System.out.println("Photo.clone: Attempting Frame->BufferedImage->Frame->Mat strategy...");

                    // 1. Frame to BufferedImage
                    BufferedImage tempBufferedImage = java2DConverter.convert(this.processedFrame);

                    if (tempBufferedImage != null) {
                        // --- START CORRECTION ---
                        // 2a. Convert BufferedImage back to an intermediate Frame using Java2D converter
                        intermediateFrame = java2DConverter.convert(tempBufferedImage);

                        // 2b. Convert the intermediate Frame to Mat using the Mat converter
                        if (intermediateFrame != null) {
                            matToClone = matConverter.convert(intermediateFrame); // Use matConverter HERE
                            System.out.println("Photo.clone: Converted intermediate Frame to Mat.");
                        } else {
                            System.err.println("Photo.clone: Failed to convert BufferedImage back to intermediate Frame.");
                            matToClone = null; // Ensure matToClone is null if intermediateFrame is null
                        }
                        // --- END CORRECTION ---

                        if (matToClone != null && !matToClone.isNull() && matToClone.cols() > 0 && matToClone.rows() > 0) {
                            // 3. Clone the Mat derived from BufferedImage path
                            clonedMat = matToClone.clone();
                            System.out.println("Photo.clone: Cloned Mat successfully.");

                            // 4. Convert Cloned Mat back to Frame
                            finalClonedFrame = matConverter.convert(clonedMat);
                            System.out.println("Photo.clone: Converted cloned Mat back to Frame.");

                        } else {
                            // This path is taken if matToClone is null or invalid
                            System.err.println("Photo.clone: Failed to get valid Mat from intermediate Frame.");
                        }
                    } else {
                        System.err.println("Photo.clone: Failed to convert Frame to BufferedImage.");
                    }

                } catch (Exception e) {
                     System.err.println("Error during Frame->BufferedImage->Frame->Mat cloning in Photo.clone: " + e.getMessage());
                     finalClonedFrame = null; // Ensure null on error
                } finally {
                   // Release intermediate Mats that were definitely created
                   if (clonedMat != null && !clonedMat.isNull()) clonedMat.release();
                   if (matToClone != null && !matToClone.isNull()) matToClone.release();
                   // intermediateFrame typically doesn't need manual release when created via converter
                }

                // If the primary strategy failed, fall back to just BufferedImage copy
                if (finalClonedFrame == null) {
                    System.err.println("Photo.clone: Falling back to Frame->BufferedImage->Frame cloning.");
                    try {
                       // Fallback: Frame -> BufferedImage -> Frame
                       BufferedImage temp = java2DConverter.convert(this.processedFrame);
                       if (temp != null) {
                           finalClonedFrame = java2DConverter.convert(temp);
                           System.out.println("Photo.clone: Fallback cloning successful.");
                       } else {
                           System.err.println("Photo.clone: Fallback Frame->BufferedImage failed.");
                           finalClonedFrame = null;
                       }
                    } catch (Exception fallbackEx) {
                       System.err.println("Error during fallback BufferedImage cloning: " + fallbackEx.getMessage());
                       finalClonedFrame = null;
                    }
                }

                clone.processedFrame = finalClonedFrame; // Assign the result (or null if all failed)

            } else {
                System.err.println("Warning: Source processedFrame is null or invalid in Photo.clone.");
                clone.processedFrame = null;
            }

            // Copy other fields
            clone.fileName = this.fileName;
            clone.uploadTime = this.uploadTime;
            clone.width = this.width;
            clone.height = this.height;
            clone.sourceFile = this.sourceFile;

            return clone;
        } catch (CloneNotSupportedException e) {
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