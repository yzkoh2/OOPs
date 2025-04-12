package com.editor;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import java.awt.Color;

public class ImageResizer implements ImageProcessor {

    private final int targetWidth;
    private final int targetHeight;
    private final boolean maintainAspectRatio;
    private Rect cropRect;
    private final Color backgroundColor;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    /**
     * Constructor for simple resizing (scaling) with default white background
     */
    public ImageResizer(int targetWidth, int targetHeight, boolean maintainAspectRatio) {
        this(targetWidth, targetHeight, maintainAspectRatio, Color.WHITE);
    }
    
    /**
     * Constructor for simple resizing (scaling) with custom background color
     */
    public ImageResizer(int targetWidth, int targetHeight, boolean maintainAspectRatio, Color backgroundColor) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maintainAspectRatio = maintainAspectRatio;
        this.cropRect = null; // No cropping, just resize
        this.backgroundColor = backgroundColor;
    }

    /**
     * Constructor for crop + resize operations with default white background
     */
    public ImageResizer(Rect cropRect, int targetWidth, int targetHeight, boolean maintainAspectRatio) {
        this(cropRect, targetWidth, targetHeight, maintainAspectRatio, Color.WHITE);
    }
    
    /**
     * Constructor for crop + resize operations with custom background color
     */
    public ImageResizer(Rect cropRect, int targetWidth, int targetHeight, boolean maintainAspectRatio, Color backgroundColor) {
        this.cropRect = cropRect;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maintainAspectRatio = maintainAspectRatio;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public Frame process(Frame frame) {
        Mat image = converter.convert(frame);
        Mat result;

        // Step 1: Apply cropping if a crop rectangle is specified
        if (cropRect != null) {
            // Ensure crop rectangle is within image bounds
            Rect validRect = new Rect(
                    Math.max(0, cropRect.x()),
                    Math.max(0, cropRect.y()),
                    Math.min(cropRect.width(), image.cols() - cropRect.x()),
                    Math.min(cropRect.height(), image.rows() - cropRect.y())
            );
            Mat croppedImage = new Mat(image, validRect);
            result = croppedImage.clone();
            croppedImage.release();
        } else {
            result = image.clone();
        }

        // Step 2: Resize (scale) the image
        Mat resizedImage = new Mat();
        if (maintainAspectRatio) {
            // Calculate the scaling factor to fit within the target dimensions
            // while preserving aspect ratio
            double widthRatio = (double) targetWidth / result.cols();
            double heightRatio = (double) targetHeight / result.rows();
            double scaleFactor = Math.min(widthRatio, heightRatio);
            
            // Calculate new dimensions
            int newWidth = (int) (result.cols() * scaleFactor);
            int newHeight = (int) (result.rows() * scaleFactor);
            
            // Resize to the calculated dimensions
            opencv_imgproc.resize(result, resizedImage, new Size(newWidth, newHeight), 
                                 0, 0, opencv_imgproc.INTER_AREA);
            
            // If the scaled image is smaller than the target, create a padded image
            // with the user-selected background color
            if (newWidth != targetWidth || newHeight != targetHeight) {
                // Convert Java Color to OpenCV Scalar (BGR format in OpenCV)
                Scalar bgColorScalar = new Scalar(
                    backgroundColor.getBlue(),
                    backgroundColor.getGreen(),
                    backgroundColor.getRed(),
                    255
                );
                
                Mat paddedImage = new Mat(targetHeight, targetWidth, result.type(), bgColorScalar);
                
                // Position the image centered horizontally, but aligned to the bottom vertically
                // (This is common for ID photos where we need less space at the bottom)
                int x = (targetWidth - newWidth) / 2;  // Center horizontally
                int y = targetHeight - newHeight;      // Align to bottom
                
                // Create a region of interest and copy the resized image there
                Mat roi = new Mat(paddedImage, new Rect(x, y, newWidth, newHeight));
                resizedImage.copyTo(roi);
                roi.release();
                
                // Use the padded image as the result
                Mat temp = resizedImage;
                resizedImage = paddedImage;
                temp.release();
            }
        } else {
            // Just resize to target dimensions without preserving aspect ratio
            // This will stretch/compress the image to fit exactly
            opencv_imgproc.resize(result, resizedImage, new Size(targetWidth, targetHeight), 
                                 0, 0, opencv_imgproc.INTER_AREA);
        }
        
        // Clean up
        result.release();
        
        // Convert back to Frame
        Frame outputFrame = converter.convert(resizedImage);
        resizedImage.release();
        
        return outputFrame;
    }

}