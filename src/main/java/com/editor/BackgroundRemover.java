package com.editor;

import java.awt.Color;
import java.awt.Point;
import java.util.List;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import static org.bytedeco.opencv.global.opencv_imgproc.grabCut;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;

import com.entities.BackgroundSettings;
import com.entities.GrabCutMask;
import com.entities.Photo;

/**
 * Implements background removal using OpenCV's GrabCut algorithm with a two-stage approach:
 * 1. Initial segmentation with rectangle selection
 * 2. Refinement using user markings for definite foreground/background
 */
public class BackgroundRemover implements ImageProcessor {

    private final BackgroundSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
    
    // Selection rectangle for initial segmentation
    private Rect selectionRect = null;
    
    // Store mask between stages
    private GrabCutMask grabCutMask = null;
    
    // Original image for two-stage processing
    private Mat originalImage = null;
    
    // Initial segmentation result
    private Mat initialResult = null;

    public BackgroundRemover(BackgroundSettings settings) {
        this.settings = settings;
    }

    /**
     * Set the manually drawn selection rectangle
     *
     * @param startX X coordinate of starting point
     * @param startY Y coordinate of starting point
     * @param endX X coordinate of ending point
     * @param endY Y coordinate of ending point
     */
    public void setSelectionRect(int startX, int startY, int endX, int endY) {
        // Ensure correct rectangle even if drawn from bottom-right to top-left
        int x = Math.min(startX, endX);
        int y = Math.min(startY, endY);
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);

        // Create the rectangle object
        this.selectionRect = new Rect(x, y, width, height);
    }

    /**
     * Clear the selection rectangle
     */
    public void clearSelection() {
        this.selectionRect = null;
    }

    /**
     * Check if a selection has been made
     */
    public boolean hasSelection() {
        return this.selectionRect != null;
    }

    /**
     * Process a frame (implements ImageProcessor interface)
     * 
     * @param frame Input frame to process
     * @return Processed frame
     */
    @Override
    public Frame process(Frame frame) {
        // For direct frame processing, just perform initial segmentation
        return performInitialSegmentation(frame);
    }
    
    // We don't need to override the default process(Photo) method from the interface
    // as it will call our process(Frame) method automatically
    
    /**
     * Perform the initial segmentation using the rectangle selection
     * 
     * @param frame Input frame to process
     * @return Processed frame with background replaced
     */
    public Frame performInitialSegmentation(Frame frame) {
        // Convert frame to OpenCV Mat
        originalImage = converter.convert(frame);
        
        // Initialize GrabCut mask
        grabCutMask = new GrabCutMask(originalImage.cols(), originalImage.rows());
        
        // Define rectangle for GrabCut - use manual selection if available, otherwise default
        Rect rectangle;
        if (selectionRect != null) {
            rectangle = selectionRect;
        } else {
            // Fall back to automatic rectangle with margin
            int margin = Math.min(originalImage.rows(), originalImage.cols()) / 10;
            rectangle = new Rect(
                    margin,
                    margin,
                    originalImage.cols() - 2 * margin,
                    originalImage.rows() - 2 * margin
            );
        }
        
        // Set the selection rectangle in the GrabCut mask
        grabCutMask.setSelectionRect(
                rectangle.x(),
                rectangle.y(),
                rectangle.width(),
                rectangle.height()
        );
        
        // Apply GrabCut algorithm
        grabCut(originalImage, 
                grabCutMask.getMask(), 
                rectangle, 
                grabCutMask.getBgModel(), 
                grabCutMask.getFgModel(),
                settings.getIterations(), 
                opencv_imgproc.GC_INIT_WITH_RECT);
        
        // Create foreground mask for visualization
        Mat foregroundMask = grabCutMask.createForegroundMask();
        
        // Apply the mask to get the segmented result
        initialResult = applyMaskToImage(originalImage, foregroundMask);
        
        return converter.convert(initialResult);
    }
    
    /**
     * Refine the segmentation using foreground and background markings
     * 
     * @param foregroundPoints Points marked as definite foreground
     * @param backgroundPoints Points marked as definite background
     * @param brushSize Size of brush used for markings
     * @return Processed frame with refined segmentation
     */
    public Frame refineSegmentation(List<Point> foregroundPoints, List<Point> backgroundPoints, int brushSize) {
        // Ensure we have original image and mask
        if (originalImage == null || grabCutMask == null) {
            throw new IllegalStateException("Must perform initial segmentation before refinement");
        }
        
        // Apply user markings to the mask
        grabCutMask.markForeground(foregroundPoints, brushSize);
        grabCutMask.markBackground(backgroundPoints, brushSize);
        
        // Apply GrabCut algorithm with the updated mask
        grabCut(originalImage, 
                grabCutMask.getMask(), 
                new Rect(), // Empty rect as we're using mask initialization
                grabCutMask.getBgModel(), 
                grabCutMask.getFgModel(),
                settings.getIterations(), 
                opencv_imgproc.GC_INIT_WITH_MASK);
        
        // Create foreground mask
        Mat foregroundMask = grabCutMask.createForegroundMask();
        
        // Apply mask to original image
        Mat refinedResult = applyMaskToImage(originalImage, foregroundMask);
        
        return converter.convert(refinedResult);
    }
    
    /**
     * Apply the foreground mask to an image and replace the background
     * 
     * @param image Original image
     * @param foregroundMask Binary mask (255 for foreground, 0 for background)
     * @return Image with background replaced
     */
    private Mat applyMaskToImage(Mat image, Mat foregroundMask) {
        // Create foreground image
        Mat foreground = new Mat(image.size(), image.type(), new Scalar(0, 0, 0, 0));
        image.copyTo(foreground, foregroundMask);

        // Create background based on settings
        Mat background;
        if (settings.getType() == BackgroundSettings.BackgroundType.SOLID_COLOR) {
            Color color = settings.getBackgroundColor();
            background = new Mat(image.size(), image.type(),
                    new Scalar(color.getBlue(), color.getGreen(), color.getRed(), 255));
        } else {
            // Load custom background image
            // This is simplified - in a real implementation you'd need to load and resize the image
            background = new Mat(image.size(), image.type(),
                    new Scalar(255, 255, 255, 255)); // Default to white if loading fails
        }

        // Create inverse mask for background
        Mat backgroundMask = new Mat();
        opencv_core.bitwise_not(foregroundMask, backgroundMask);

        // Apply background to original image
        Mat result = new Mat(image.size(), image.type());
        background.copyTo(result, backgroundMask);
        foreground.copyTo(result, foregroundMask);

        // Clean up resources
        foregroundMask.release();
        background.release();
        foreground.release();
        backgroundMask.release();

        return result;
    }
    
    /**
     * Creates a visualization of the current mask for the refinement UI
     * Shows overlay of original image with colored mask
     * 
     * @return Visualization frame
     */
    public Frame createMaskVisualization() {
        if (originalImage == null || grabCutMask == null) {
            throw new IllegalStateException("Must perform initial segmentation before visualization");
        }
       
        // Create a copy of the original image
        Mat visualization = originalImage.clone();
       
        // Get the foreground mask
        Mat foregroundMask = grabCutMask.createForegroundMask();
       
        // Create a striped red overlay for background areas
        Mat stripeOverlay = new Mat(originalImage.size(), originalImage.type(), new Scalar(0, 0, 0, 0));
        
        try {
            // Create striped pattern
            for (int y = 0; y < stripeOverlay.rows(); y++) {
                for (int x = 0; x < stripeOverlay.cols(); x++) {
                    // Create diagonal stripe pattern
                    if ((x + y) % 10 < 5) {
                        stripeOverlay.ptr(y, x).put(
                            (byte)82,   // Blue
                            (byte)3,     // Green
                            (byte)82,     // Red
                            (byte)100    // Alpha (transparency)
                        );
                    }
                }
            }
       
            // Create inverse of foreground mask (background mask)
            Mat backgroundMask = new Mat();
            try {
                opencv_core.bitwise_not(foregroundMask, backgroundMask);
       
                // Apply striped overlay to background areas
                Mat overlay = new Mat(originalImage.size(), originalImage.type());
                try {
                    visualization.copyTo(overlay);
                    stripeOverlay.copyTo(overlay, backgroundMask);
       
                    // Blend with original (50% transparency)
                    opencv_core.addWeighted(visualization, 0.7, overlay, 0.3, 0, visualization);
                } finally {
                    overlay.release();
                }
            } finally {
                backgroundMask.release();
            }
        } finally {
            stripeOverlay.release();
        }
       
        // Clean up
        foregroundMask.release();
       
        return converter.convert(visualization);
    }
    /**
     * Get the current GrabCut mask
     * 
     * @return The GrabCut mask object
     */
    public GrabCutMask getGrabCutMask() {
        return grabCutMask;
    }
    
    /**
     * Get the original image
     * 
     * @return The original image Mat
     */
    public Mat getOriginalImage() {
        return originalImage;
    }
    
    /**
     * Get the initial segmentation result
     * 
     * @return The initial result Mat
     */
    public Mat getInitialResult() {
        return initialResult;
    }
    
    /**
     * Apply background replacement to a photo using a final mask
     * 
     * @param photo Photo to process
     * @return Processed photo
     */
    public Photo applyFinalMask(Photo photo) {
        // Get the processed frame
        Frame frame = photo.getProcessedFrame().clone();
        
        // Convert to Mat
        Mat image = converter.convert(frame);
        
        // Create foreground mask
        Mat foregroundMask = grabCutMask.createForegroundMask();
        
        // Apply mask
        Mat result = applyMaskToImage(image, foregroundMask);
        
        // Convert back to frame
        Frame processedFrame = converter.convert(result);
        
        // Set the processed frame back to the photo
        photo.setProcessedFrame(processedFrame);
        
        // Release resources
        image.release();
        result.release();
        
        return photo;
    }
    
    /**
     * Clean up resources
     */
    public void release() {
        if (grabCutMask != null) {
            grabCutMask.release();
        }
        
        if (originalImage != null) {
            originalImage.release();
        }
        
        if (initialResult != null) {
            initialResult.release();
        }
    }
}