package com.editor;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

import org.bytedeco.opencv.opencv_core.*;
// import org.w3c.dom.css.Rect;

// import apple.laf.JRSUIConstants.Size;

/**
 * Handles image cropping and resizing operations.
 */
public class ImageResizer implements ImageProcessor {
    
    private Size targetSize;
    
    public ImageResizer() {
        // Default ID photo size 35x45mm at 300 DPI
        this.targetSize = new Size(413, 531); // Approximately 35x45mm at 300 DPI
    }
    
    public ImageResizer(Size targetSize) {
        this.targetSize = targetSize;
    }
    
    @Override
    public Mat process(Mat inputImage) {
        // Default implementation resizes to target size while maintaining aspect ratio
        return resizeWithAspectRatio(inputImage, targetSize.width(), targetSize.height());
    }
    
    /**
     * Crops image to specified rectangle.
     */
    public Mat cropImage(Mat inputImage, Rect cropRect) {
        // Ensure the crop rectangle is within image bounds
        Rect safeRect = new Rect(
            Math.max(0, cropRect.x()),
            Math.max(0, cropRect.y()),
            Math.min(cropRect.width(), inputImage.cols() - cropRect.x()),
            Math.min(cropRect.height(), inputImage.rows() - cropRect.y())
        );
        
        // Extract the subregion defined by cropRect
        return new Mat(inputImage, safeRect);
    }
    
    /**
     * Resizes image to exact dimensions.
     */
    public Mat resizeImage(Mat inputImage, Size targetSize) {
        Mat resized = new Mat();
        resize(inputImage, resized, targetSize, 0, 0, INTER_AREA);
        return resized;
    }
    
    /**
     * Resizes image while maintaining aspect ratio.
     */
    public Mat resizeWithAspectRatio(Mat inputImage, double targetWidth, double targetHeight) {
        double aspectRatio = (double) inputImage.cols() / inputImage.rows();
        double targetRatio = targetWidth / targetHeight;
        
        int newWidth, newHeight;
        if (aspectRatio > targetRatio) {
            // Width is the limiting factor
            newWidth = (int) targetWidth;
            newHeight = (int) (targetWidth / aspectRatio);
        } else {
            // Height is the limiting factor
            newWidth = (int) (targetHeight * aspectRatio);
            newHeight = (int) targetHeight;
        }
        
        Size newSize = new Size(newWidth, newHeight);
        
        Mat resized = new Mat();
        resize(inputImage, resized, newSize, 0, 0, INTER_AREA);
        return resized;
    }
    
    /**
     * Creates a canvas of target size and centers the image on it.
     */
    public Mat centerOnCanvas(Mat inputImage, Size canvasSize, Scalar backgroundColor) {
        Mat canvas = new Mat(canvasSize, inputImage.type(), backgroundColor);
        
        // Calculate position to center the image
        int x = (int)((canvasSize.width() - inputImage.cols()) / 2);
        int y = (int)((canvasSize.height() - inputImage.rows()) / 2);
        
        // Create region of interest on the canvas
        Rect roi = new Rect(x, y, inputImage.cols(), inputImage.rows());
        
        // Copy the image to the ROI
        Mat roiMat = new Mat(canvas, roi);
        inputImage.copyTo(roiMat);
        
        return canvas;
    }
    
    // Getters and setters
    public Size getTargetSize() {
        return targetSize;
    }
    
    public void setTargetSize(Size targetSize) {
        this.targetSize = targetSize;
    }
}