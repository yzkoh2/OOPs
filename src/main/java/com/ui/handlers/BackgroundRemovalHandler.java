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
import com.ui.panels.ForegroundRefinementPanel;
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
    private ForegroundRefinementPanel refinementPanel;
    
    private BackgroundRemovalCallback callback;
    
    // Refinement tracking
    private int refinementAttempts = 0;
    private static final int MAX_REFINEMENT_ATTEMPTS = 5;
    
    /**
     * Interface for background removal callbacks
     */
    public interface BackgroundRemovalCallback {
        void onBackgroundRemovalStarted();
        void onBackgroundRemovalCompleted(Photo processedPhoto);
        void onBackgroundRemovalFailed(String errorMessage);
        void onRefinementProgress(int attempts, boolean isSuccessful);
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
        // Reset refinement attempts
        refinementAttempts = 0;
        
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
                    // Don't call completion callback yet - go to refinement stage
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
                    
                    // Note: we don't set the processed frame back to the photo yet
                    // That will happen in the refinement stage
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
/**
     * Start the refinement stage after initial segmentation
     * 
     * @param photo Photo with initial segmentation
     */
    private void startRefinementStage(Photo photo) {
        try {
            // Create visualization frame for the refinement panel
            Frame visualizationFrame = backgroundRemover.createMaskVisualization();
            
            // Create refinement dialog
            refinementDialog = new JDialog(parentFrame, "Refine Foreground Selection", true);
            refinementDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            // Create refinement panel with enhanced callback
            refinementPanel = new ForegroundRefinementPanel(visualizationFrame, 
                    new ForegroundRefinementPanel.RefinementCallback() {
                @Override
                public void onApplyRefinement(
                    List<Point> foregroundPoints, 
                    List<Point> backgroundPoints, 
                    List<Integer> foregroundBrushSizes, 
                    List<Integer> backgroundBrushSizes
                ) {
                    // If previous methods passed a single brush size, we'll use the first size for all points
                    int brushSize = foregroundBrushSizes.isEmpty() ? 10 : foregroundBrushSizes.get(0);
                    applyRefinement(foregroundPoints, backgroundPoints, brushSize);
                }

                @Override
                public void onResetRefinement() {
                    resetRefinement();
                }

                @Override
                public void onUpdateVisualization() {
                    updateVisualization();
                }
            });
            
            // Set dialog size and content
            refinementDialog.getContentPane().add(refinementPanel);
            refinementDialog.pack();
            refinementDialog.setLocationRelativeTo(parentFrame);
            
            // Add window listener to handle dialog closing
            refinementDialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    handleRefinementDialogClosed();
                }
            });
            
            // Show refinement dialog
            refinementDialog.setVisible(true);
            
            // When the dialog becomes invisible (after it's closed), finalize the process
            // This is needed because setVisible(true) blocks until dialog is closed
            handleRefinementDialogClosed();
            
        } catch (Exception ex) {
            // Handle errors
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, 
                    "Error starting refinement: " + ex.getMessage(),
                    "Refinement Error", 
                    JOptionPane.ERROR_MESSAGE);
            
            if (callback != null) {
                callback.onBackgroundRemovalFailed("Error in refinement stage: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Apply refinement markings and update the segmentation
     * 
     * @param foregroundPoints Foreground marking points
     * @param backgroundPoints Background marking points
     * @param brushSize Brush size used for markings
     */
    private void applyRefinement(List<Point> foregroundPoints, List<Point> backgroundPoints, int brushSize) {
        // Increment refinement attempts
        refinementAttempts++;
        
        // Update status
        if (statusLabel != null) {
            statusLabel.setText("Applying refinements (Attempt " + refinementAttempts + ")...");
        }
        
        // Check maximum refinement attempts
        if (refinementAttempts > MAX_REFINEMENT_ATTEMPTS) {
            JOptionPane.showMessageDialog(parentFrame, 
                    "Maximum refinement attempts reached. Finalizing segmentation.",
                    "Refinement Limit", 
                    JOptionPane.INFORMATION_MESSAGE);
            handleRefinementDialogClosed();
            return;
        }
        
        // Use SwingWorker to perform refinement in background
        new SwingWorker<Frame, Void>() {
            @Override
            protected Frame doInBackground() throws Exception {
                // Apply refinement and get updated result
                return backgroundRemover.refineSegmentation(foregroundPoints, backgroundPoints, brushSize);
            }
            
            @Override
            protected void done() {
                try {
                    // Get the result
                    Frame result = get();
                    
                    // Update visualization
                    Frame visualizationFrame = backgroundRemover.createMaskVisualization();
                    refinementPanel.updateVisualization(visualizationFrame);
                    
                    // Notify callback about refinement progress
                    if (callback != null) {
                        callback.onRefinementProgress(refinementAttempts, true);
                    }
                    
                    // Update status
                    if (statusLabel != null) {
                        statusLabel.setText("Refinement applied. Continue refining or close dialog to finish.");
                    }
                    
                } catch (Exception ex) {
                    // Handle errors
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(parentFrame, 
                            "Error applying refinement: " + ex.getMessage(),
                            "Refinement Error", 
                            JOptionPane.ERROR_MESSAGE);
                    
                    // Notify callback about refinement failure
                    if (callback != null) {
                        callback.onRefinementProgress(refinementAttempts, false);
                    }
                    
                    if (statusLabel != null) {
                        statusLabel.setText("Error applying refinement.");
                    }
                }
            }
        }.execute();
    }
    
    /**
     * Reset refinement markings
     */
    private void resetRefinement() {
        try {
            // Clear markings in panel
            refinementPanel.clearMarkings();
            
            // Update visualization with original segmentation
            Frame visualizationFrame = backgroundRemover.createMaskVisualization();
            refinementPanel.updateVisualization(visualizationFrame);
            
            // Update status
            if (statusLabel != null) {
                statusLabel.setText("Refinement reset.");
            }
            
        } catch (Exception ex) {
            // Handle errors
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, 
                    "Error resetting refinement: " + ex.getMessage(),
                    "Refinement Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Update the visualization in the refinement panel
     */
    private void updateVisualization() {
        try {
            // Create new visualization
            Frame visualizationFrame = backgroundRemover.createMaskVisualization();
            
            // Update panel
            refinementPanel.updateVisualization(visualizationFrame);
            
        } catch (Exception ex) {
            // Handle errors
            ex.printStackTrace();
        }
    }
    
    /**
     * Complete the background removal process
     * Called when refinement dialog is closed
     */
    private void finalizeBackgroundRemoval() {
        try {
            // Update status
            if (statusLabel != null) {
                statusLabel.setText("Applying final background replacement...");
            }
            
            // Apply final mask to the photo
            Photo processedPhoto = backgroundRemover.applyFinalMask(currentPhoto);
            
            // Notify completion
            if (callback != null) {
                callback.onBackgroundRemovalCompleted(processedPhoto);
            }
            
            // Update status
            if (statusLabel != null) {
                statusLabel.setText("Background removal complete.");
            }
            
        } catch (Exception ex) {
            // Handle errors
            ex.printStackTrace();
            
            if (callback != null) {
                callback.onBackgroundRemovalFailed("Error finalizing background removal: " + ex.getMessage());
            }
            
            if (statusLabel != null) {
                statusLabel.setText("Error finalizing background removal.");
            }
        } finally {
            // Clean up resources
            if (backgroundRemover != null) {
                backgroundRemover.release();
            }
        }
    }
    
    /**
     * Window listener to handle when refinement dialog is closed
     */
    private void handleRefinementDialogClosed() {
        // Finalize the background removal when dialog is closed
        finalizeBackgroundRemoval();
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