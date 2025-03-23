package com.entities;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.awt.Point;
import java.util.List;

/**
 * Encapsulates the GrabCut mask and related data between processing stages.
 * This allows persistence of mask information between initial segmentation
 * and user refinement stages.
 */
public class GrabCutMask {
    // GrabCut segmentation constants
    public static final int GC_BGD = 0;      // Definite background
    public static final int GC_FGD = 1;      // Definite foreground
    public static final int GC_PR_BGD = 2;   // Probable background
    public static final int GC_PR_FGD = 3;   // Probable foreground
    
    private Mat mask;            // The actual GrabCut mask
    private Mat bgModel;         // Background model
    private Mat fgModel;         // Foreground model
    private Rect selectionRect;  // User's initial selection rectangle
    private boolean isInitialized = false;
    
    /**
     * Creates a new GrabCutMask for the given image dimensions
     * 
     * @param imgWidth Width of the image
     * @param imgHeight Height of the image
     */
    public GrabCutMask(int imgWidth, int imgHeight) {
        // Initialize mask as background
        mask = new Mat(imgHeight, imgWidth, opencv_core.CV_8UC1);
        mask.setTo(new Mat(1, 1, opencv_core.CV_8UC1, new org.bytedeco.opencv.opencv_core.Scalar(GC_BGD)));
        
        // Initialize models (will be filled by GrabCut algorithm)
        bgModel = new Mat();
        fgModel = new Mat();
    }
    
    /**
     * Sets the initial selection rectangle
     * 
     * @param x X coordinate of top-left
     * @param y Y coordinate of top-left
     * @param width Width of rectangle
     * @param height Height of rectangle
     */
    public void setSelectionRect(int x, int y, int width, int height) {
        selectionRect = new Rect(x, y, width, height);
        
        // Mark selection rectangle as probable foreground
        markRectAsProbableForeground(selectionRect);
        isInitialized = true;
    }
    
    /**
     * Mark a rectangle area in the mask as probable foreground
     * 
     * @param rectangle Rectangle area to mark
     */
    private void markRectAsProbableForeground(Rect rectangle) {
        // Ensure rectangle is within image bounds
        rectangle.x(Math.max(0, rectangle.x()));
        rectangle.y(Math.max(0, rectangle.y()));
        rectangle.width(Math.min(mask.cols() - rectangle.x(), rectangle.width()));
        rectangle.height(Math.min(mask.rows() - rectangle.y(), rectangle.height()));
        
        // Draw filled rectangle of GC_PR_FGD in the mask
        opencv_imgproc.rectangle(
            mask,
            new org.bytedeco.opencv.opencv_core.Point(rectangle.x(), rectangle.y()),
            new org.bytedeco.opencv.opencv_core.Point(rectangle.x() + rectangle.width(), rectangle.y() + rectangle.height()),
            new org.bytedeco.opencv.opencv_core.Scalar(GC_PR_FGD),
            -1,  // Fill the rectangle
            8, // LINE_8
            0
        );
    }
    
    /**
     * Apply user's foreground markings to the mask
     * 
     * @param foregroundPoints List of points marked as definite foreground
     * @param brushSize Size of the brush used for marking
     */
    public void markForeground(List<Point> foregroundPoints, int brushSize) {
        if (foregroundPoints == null || foregroundPoints.isEmpty()) {
            return;
        }
        
        for (Point p : foregroundPoints) {
            // Draw filled circle of GC_FGD in the mask
            opencv_imgproc.circle(
                mask,
                new org.bytedeco.opencv.opencv_core.Point(p.x, p.y),
                brushSize / 2,
                new org.bytedeco.opencv.opencv_core.Scalar(GC_FGD),
                -1,  // Fill the circle
                8, // LINE_8
                0
            );
        }
    }
    
    /**
     * Apply user's background markings to the mask
     * 
     * @param backgroundPoints List of points marked as definite background
     * @param brushSize Size of the brush used for marking
     */
    public void markBackground(List<Point> backgroundPoints, int brushSize) {
        if (backgroundPoints == null || backgroundPoints.isEmpty()) {
            return;
        }
        
        for (Point p : backgroundPoints) {
            // Draw filled circle of GC_BGD in the mask
            opencv_imgproc.circle(
                mask,
                new org.bytedeco.opencv.opencv_core.Point(p.x, p.y),
                brushSize / 2,
                new org.bytedeco.opencv.opencv_core.Scalar(GC_BGD),
                -1,  // Fill the circle
                8,
                0
            );
        }
    }
    
    /**
     * Get the current mask
     * 
     * @return The GrabCut mask
     */
    public Mat getMask() {
        return mask;
    }
    
    /**
     * Get the background model
     * 
     * @return The background model Mat
     */
    public Mat getBgModel() {
        return bgModel;
    }
    
    /**
     * Get the foreground model
     * 
     * @return The foreground model Mat
     */
    public Mat getFgModel() {
        return fgModel;
    }
    
    /**
     * Get the selection rectangle
     * 
     * @return The selection rectangle
     */
    public Rect getSelectionRect() {
        return selectionRect;
    }
    
    /**
     * Check if the mask has been initialized with a selection rectangle
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Create a foreground mask for displaying or final processing
     * 
     * @return A binary mask with 255 for foreground pixels and 0 for background
     */
    public Mat createForegroundMask() {
        Mat foregroundMask = new Mat();
        
        // Create a mask for probable foreground pixels (GC_PR_FGD)
        Mat prFgdMask = new Mat();
        Mat prFgdValue = new Mat(1, 1, opencv_core.CV_8UC1, 
                new org.bytedeco.opencv.opencv_core.Scalar(GC_PR_FGD));
        opencv_core.compare(mask, prFgdValue, prFgdMask, opencv_core.CMP_EQ);
        
        // Create a mask for definite foreground pixels (GC_FGD)
        Mat fgdMask = new Mat();
        Mat fgdValue = new Mat(1, 1, opencv_core.CV_8UC1,
                new org.bytedeco.opencv.opencv_core.Scalar(GC_FGD));
        opencv_core.compare(mask, fgdValue, fgdMask, opencv_core.CMP_EQ);
        
        // Combine both masks
        opencv_core.bitwise_or(prFgdMask, fgdMask, foregroundMask);
        
        // Convert to binary mask (0 or 255)
        Mat binaryMask = new Mat();
        
        // Use thresholding instead of matrix multiplication to convert to binary
        opencv_imgproc.threshold(foregroundMask, binaryMask, 0, 255, opencv_imgproc.THRESH_BINARY);
        
        // Release temporary matrices
        prFgdMask.release();
        prFgdValue.release();
        fgdMask.release();
        fgdValue.release();
        foregroundMask.release();
        
        return binaryMask;
    }
    
    /**
     * Clean up resources
     */
    public void release() {
        if (mask != null) mask.release();
        if (bgModel != null) bgModel.release();
        if (fgModel != null) fgModel.release();
    }
    
    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}