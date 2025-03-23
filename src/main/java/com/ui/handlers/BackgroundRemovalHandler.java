package com.ui.handlers;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Rect;

import com.editor.BackgroundRemover;
import com.entities.BackgroundSettings;
import com.entities.Photo;
import com.ui.panels.SelectionPanel;

/**
 * Handles the two-stage background removal process:
 * 1. Initial segmentation with rectangle selection
 * 2. Refinement with user markings
 */
public class BackgroundRemovalHandler {
    private JFrame parentFrame;
    private JLabel statusLabel;
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private BackgroundRemover backgroundRemover;
    private JDialog refinementDialog;
    private ForegroundRefinementHandler foregroundRefinementHandler;
    
    private BackgroundRemovalCallback callback;
    
    /**
     * Interface for background removal callbacks
     */
    public interface BackgroundRemovalCallback {
        void onBackgroundRemovalStarted();
        void onBackgroundRemovalCompleted(Photo processedPhoto);
        void onBackgroundRemovalFailed(String errorMessage);
    }
    
    /**
     * Constructor
     * 
     * @param parentFrame Parent frame for dialogs
     * @param statusLabel Status label for updates
     * @param currentPhoto Photo to process
     * @param backgroundSettings Background settings
     * @param callback Callback for process events
     */
    public BackgroundRemovalHandler(
            JFrame parentFrame, 
            JLabel statusLabel, 
            Photo currentPhoto,
            BackgroundSettings backgroundSettings,
            BackgroundRemovalCallback callback
    ) {
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.backgroundSettings = backgroundSettings;
        this.callback = callback;
        
        // Initialize the background remover
        this.backgroundRemover = new BackgroundRemover(backgroundSettings);
    }
    
    /**
     * Handle the background removal action
     * 
     * @param e Action event
     */
    public void handleRemoveBackground(ActionEvent e) {
        // Start with first stage - rectangle selection
        startInitialSegmentation(e);
    }
    
    /**
     * Start the initial segmentation process with rectangle selection
     * 
     * @param e Action event
     */
    private void startInitialSegmentation(ActionEvent e) {
        // Create generic handler for selection phase
        GenericImageHandler genericHandler = new GenericImageHandler(
            parentFrame, 
            statusLabel, 
            currentPhoto,
            new GenericImageHandler.ImageProcessCallback() {
                @Override
                public void onProcessStarted() {
                    if (callback != null) callback.onBackgroundRemovalStarted();
                }

                @Override
                public void onProcessCompleted(Photo processedPhoto) {
                    // Proceed to refinement stage
                    startRefinementStage(processedPhoto);
                }

                @Override
                public void onProcessFailed(String errorMessage) {
                    if (callback != null) callback.onBackgroundRemovalFailed(errorMessage);
                }
            },
            new GenericImageHandler.ImageProcessor() {
                @Override
                public void process(Photo photo, Rect rect) throws Exception {
                    // Set selection rectangle in our background remover
                    backgroundRemover.setSelectionRect(
                        rect.x(), 
                        rect.y(),
                        rect.x() + rect.width(),
                        rect.y() + rect.height()
                    );
                    
                    // Perform initial segmentation
                    backgroundRemover.performInitialSegmentation(photo.getProcessedFrame());
                }
            },
            SelectionPanel.class,
            "Select Foreground - Click and drag to select area",
            "Performing initial segmentation..."
        );
        
        // Start the selection process
        genericHandler.handleProcess(e);
    }
    
    /**
     * Start the refinement stage after initial segmentation
     * 
     * @param photo Photo with initial segmentation
     */
    private void startRefinementStage(Photo photo) {
        try {
            // Perform initial segmentation if not already done
            if (backgroundRemover.getOriginalImage() == null) {
                backgroundRemover.performInitialSegmentation(photo.getProcessedFrame());
            }

            // Create visualization frame for the refinement panel
            Frame visualizationFrame = backgroundRemover.createMaskVisualization();
            
            // Create refinement dialog
            refinementDialog = new JDialog(parentFrame, "Refine Foreground Selection", true);
            refinementDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            // Create foreground refinement handler
            foregroundRefinementHandler = new ForegroundRefinementHandler(
                parentFrame, 
                statusLabel, 
                currentPhoto, 
                backgroundSettings,
                visualizationFrame,
                backgroundRemover, // Pass the existing instance
                new ForegroundRefinementHandler.RefinementCallback() {
                    @Override
                    public void onRefinementStarted() {
                        // Optional: Add specific handling if needed
                    }

                    @Override
                    public void onRefinementCompleted(Photo processedPhoto) {
                        // Clean up and notify main callback
                        cleanup();
                        if (callback != null) {
                            callback.onBackgroundRemovalCompleted(processedPhoto);
                        }
                    }

                    @Override
                    public void onRefinementFailed(String errorMessage) {
                        // Clean up and notify main callback
                        cleanup();
                        if (callback != null) {
                            callback.onBackgroundRemovalFailed(errorMessage);
                        }
                    }

                    @Override
                    public void onRefinementProgress(int attempts, boolean isSuccessful) {
                        // Optional: Add progress tracking or logging
                    }
                }
            );

            // Start refinement process
            foregroundRefinementHandler.startRefinement();
            
        } catch (Exception ex) {
            // Handle errors
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                parentFrame, 
                "Error starting refinement: " + ex.getMessage(),
                "Refinement Error", 
                JOptionPane.ERROR_MESSAGE
            );
            
            // Notify callback of failure
            if (callback != null) {
                callback.onBackgroundRemovalFailed("Error in refinement stage: " + ex.getMessage());
            }
            
            // Ensure cleanup
            cleanup();
        }
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        // Release background remover
        if (backgroundRemover != null) {
            backgroundRemover.release();
            backgroundRemover = null;
        }

        // Dispose refinement dialog
        if (refinementDialog != null) {
            refinementDialog.dispose();
            refinementDialog = null;
        }

        // Release foreground refinement handler
        if (foregroundRefinementHandler != null) {
            foregroundRefinementHandler = null;
        }

        // Clear references
        parentFrame = null;
        statusLabel = null;
        currentPhoto = null;
        callback = null;

        // Hint to garbage collector
        System.gc();
    }

    /**
     * Setter for current photo to allow updating the photo for background removal
     * 
     * @param photo New photo to process
     */
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
}