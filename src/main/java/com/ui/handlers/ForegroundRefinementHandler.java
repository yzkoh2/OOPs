package com.ui.handlers;

import java.awt.Point;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.bytedeco.javacv.Frame;
import javax.swing.JDialog;
import com.editor.BackgroundRemover;
import com.entities.BackgroundSettings;
import com.entities.Photo;
import com.ui.panels.ForegroundRefinementPanel;


/**
 * Handles the foreground refinement process for background removal.
 */
public class ForegroundRefinementHandler {
    // Parent components
    private JFrame parentFrame;
    private JLabel statusLabel;

    // Processing components
    private Photo currentPhoto;
    private BackgroundRemover backgroundRemover;
    private BackgroundSettings backgroundSettings;
    private Frame visualizationFrame;

    // Refinement dialog
    private JDialog refinementDialog;
    private ForegroundRefinementPanel refinementPanel;

    // Callback for refinement process
    private RefinementCallback callback;

    // Refinement tracking
    private int refinementAttempts = 0;
    private static final int MAX_REFINEMENT_ATTEMPTS = 5;

    /**
     * Callback interface for refinement process.
     */
    public interface RefinementCallback {
        void onRefinementStarted();
        void onRefinementCompleted(Photo processedPhoto);
        void onRefinementFailed(String errorMessage);
        void onRefinementProgress(int attempts, boolean isSuccessful);
    }

    /**
     * Constructor for ForegroundRefinementHandler.
     * 
     * @param parentFrame Parent frame for dialogs
     * @param statusLabel Status label for updates
     * @param currentPhoto Photo to refine
     * @param backgroundSettings Background removal settings
     * @param callback Callback for refinement process
     */


    public ForegroundRefinementHandler(
        JFrame parentFrame,
        JLabel statusLabel,
        Photo currentPhoto,
        BackgroundSettings backgroundSettings,
        Frame visualizationFrame,
        BackgroundRemover backgroundRemover, // Add this parameter
        RefinementCallback callback
    ) {
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.backgroundSettings = backgroundSettings;
        this.visualizationFrame = visualizationFrame;
        this.callback = callback;
        
        // Use the passed backgroundRemover instead of creating a new one
        this.backgroundRemover = backgroundRemover;
    }

    /**
     * Start the refinement process.
     */
    public void startRefinement() {
        // Reset refinement attempts
        refinementAttempts = 0;

        try {
            // Create visualization frame
            Frame visualizationFrame = backgroundRemover.createMaskVisualization();

            // Create refinement dialog
            refinementDialog = new JDialog(parentFrame, "Refine Foreground Selection", true);
            refinementDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Create refinement panel
            refinementPanel = new ForegroundRefinementPanel(
                visualizationFrame, 
                new ForegroundRefinementPanel.RefinementCallback() {
                    @Override
                    public void onApplyRefinement(
                        List<Point> foregroundPoints, 
                        List<Point> backgroundPoints, 
                        List<Integer> foregroundBrushSizes, 
                        List<Integer> backgroundBrushSizes
                    ) {
                        applyRefinement(foregroundPoints, backgroundPoints, 
                            foregroundBrushSizes.isEmpty() ? 10 : foregroundBrushSizes.get(0));
                    }
            
                    @Override
                    public void onResetRefinement() {
                        resetRefinement();
                    }
                    
                    @Override
                    public void onFinalizeRefinement() {
                        finalizeRefinement(); // Directly call finalizeRefinement
                    }
                }
            );

            // Configure dialog
            refinementDialog.getContentPane().add(refinementPanel);
            refinementDialog.pack();
            refinementDialog.setLocationRelativeTo(parentFrame);

            // Add window listener for cleanup
            refinementDialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    finalizeRefinement();
                }
            });

            // Show refinement dialog
            refinementDialog.setVisible(true);

        } catch (Exception ex) {
            handleRefinementError(ex);
        }
    }

    /**
     * Apply refinement markings.
     * 
     * @param foregroundPoints Foreground marking points
     * @param backgroundPoints Background marking points
     * @param brushSize Brush size used for markings
     */
    private void applyRefinement(
            List<Point> foregroundPoints, 
            List<Point> backgroundPoints, 
            int brushSize
    ) {
        // Increment refinement attempts
        refinementAttempts++;

        // Update status
        updateStatus("Applying refinements (Attempt " + refinementAttempts + ")...");

        // Check maximum refinement attempts
        if (refinementAttempts > MAX_REFINEMENT_ATTEMPTS) {
            JOptionPane.showMessageDialog(
                parentFrame, 
                "Maximum refinement attempts reached. Finalizing segmentation.",
                "Refinement Limit", 
                JOptionPane.INFORMATION_MESSAGE
            );
            finalizeRefinement();
            return;
        }

        // Background worker for refinement
        new SwingWorker<Frame, Void>() {
            @Override
            protected Frame doInBackground() throws Exception {
                return backgroundRemover.refineSegmentation(
                    foregroundPoints, 
                    backgroundPoints, 
                    brushSize
                );
            }

            @Override
            protected void done() {
                try {
                    // Get refined result
                    Frame result = get();

                    // Update visualization
                    Frame visualizationFrame = backgroundRemover.createMaskVisualization();
                    refinementPanel.updateVisualization(visualizationFrame);

                    // Notify callback
                    notifyRefinementProgress(true);
                    updateStatus("Refinement applied. Continue refining or close dialog to finish.");

                } catch (Exception ex) {
                    handleRefinementError(ex);
                }
            }
        }.execute();
    }

    /**
     * Reset refinement markings.
     */
    private void resetRefinement() {
        try {
            // Clear panel markings
            refinementPanel.clearMarkings();

            // Recreate visualization
            Frame visualizationFrame = backgroundRemover.createMaskVisualization();
            refinementPanel.updateVisualization(visualizationFrame);

            updateStatus("Refinement reset.");

        } catch (Exception ex) {
            handleRefinementError(ex);
        }
    }

    /**
     * Finalize the refinement process.
     */
    public void finalizeRefinement() {
        System.out.println("FINALIZE REFINEMENT START:");
        System.out.println("Current Photo Details:");
        System.out.println("  Current Photo: " + (currentPhoto != null));
        if (currentPhoto != null) {
            System.out.println("  Original Frame: " + (currentPhoto.getOriginalFrame() != null));
            System.out.println("  Processed Frame: " + (currentPhoto.getProcessedFrame() != null));
        }
        try {
            updateStatus("Applying final background replacement...");

            // Make sure we have a valid backgroundRemover and photo
            if (backgroundRemover == null) {
                throw new IllegalStateException("Background remover is null");
            }
            if (currentPhoto == null) {
                throw new IllegalStateException("Current photo is null");
            }

            // Apply final mask and store the result in a separate variable
            // This creates a new Photo object via the copy constructor
            System.out.println("Applying final mask to photo...");
            Photo processedPhoto = backgroundRemover.applyFinalMask(currentPhoto);
            
            // Verify we have a valid processed photo
            if (processedPhoto == null) {
                throw new IllegalStateException("Processed photo is null after applying mask");
            }
            
            // Make sure the processed frame is not null
            if (processedPhoto.getProcessedFrame() == null) {
                throw new IllegalStateException("Processed frame is null in the processed photo");
            }

            // Log success for debugging
            System.out.println("Background removal process completed successfully");
            System.out.println("Original photo hash: " + System.identityHashCode(currentPhoto));
            System.out.println("Processed photo hash: " + System.identityHashCode(processedPhoto));
            
            // Store the reference to pass to the callback
            final Photo finalProcessedPhoto = processedPhoto;
            
            // Close the dialog first to prevent any interference with the callback
            if (refinementDialog != null && refinementDialog.isVisible()) {
                refinementDialog.dispose();
                refinementDialog = null;
            }
            
            // Use SwingUtilities.invokeLater to make sure UI updates happen on EDT
            SwingUtilities.invokeLater(() -> {
                // Notify callback with the processed photo
                notifyRefinementCompleted(finalProcessedPhoto);
                updateStatus("Background removal complete.");
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            handleRefinementError(ex);
        }
        // DO NOT cleanup resources here, move it to after callback completes
    }

    /**
     * Handle refinement errors.
     * 
     * @param ex Exception that occurred
     */
    private void handleRefinementError(Exception ex) {
        ex.printStackTrace();
        
        // Show error dialog
        JOptionPane.showMessageDialog(
            parentFrame,
            "Error in refinement: " + ex.getMessage(),
            "Refinement Error", 
            JOptionPane.ERROR_MESSAGE
        );

        // Notify callback
        notifyRefinementFailed(ex.getMessage());
        updateStatus("Refinement failed.");

        // Cleanup resources
        cleanup();
    }

    /**
     * Update status label.
     * 
     * @param message Status message
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * Notify refinement progress.
     * 
     * @param isSuccessful Whether refinement was successful
     */
    private void notifyRefinementProgress(boolean isSuccessful) {
        if (callback != null) {
            callback.onRefinementProgress(refinementAttempts, isSuccessful);
        }
    }

    /**
     * Notify refinement completion.
     * 
     * @param processedPhoto Processed photo
     */
    private void notifyRefinementCompleted(Photo processedPhoto) {
        if (callback != null) {
            try {
                System.out.println("REFINEMENT CALLBACK: Notifying of completion");
                
                // Detailed logging of photo states
                System.out.println("Photo State Details:");
                System.out.println("  Processed Photo null: " + (processedPhoto == null));
                System.out.println("  Current Photo null: " + (currentPhoto == null));
                
                if (processedPhoto == null) {
                    System.err.println("WARNING: Processed photo is null. Using current photo as fallback.");
                    
                    if (currentPhoto == null) {
                        System.err.println("CRITICAL: Both processed and current photos are null!");
                        return; // Prevent further processing
                    }
                    
                    processedPhoto = currentPhoto;
                }
                
                // Extensive frame logging
                Frame processedFrame = processedPhoto.getProcessedFrame();
                System.out.println("Processed Frame Details:");
                System.out.println("  Frame null: " + (processedFrame == null));
                
                if (processedFrame != null) {
                    System.out.println("  Width: " + processedFrame.imageWidth);
                    System.out.println("  Height: " + processedFrame.imageHeight);
                    System.out.println("  Depth: " + processedFrame.imageDepth);
                    System.out.println("  Channels: " + processedFrame.imageChannels);
                }
                
                // Object identity check
                if (processedPhoto == currentPhoto) {
                    System.err.println("WARNING: Processed photo is the same object as current photo!");
                }
                
                // Invoke callback
                callback.onRefinementCompleted(processedPhoto);
                
                // Cleanup with logging
                System.out.println("Scheduling cleanup after callback");
                new Timer(1000, e -> {
                    ((Timer) e.getSource()).stop();
                    System.out.println("Performing delayed cleanup");
                    cleanup();
                }).start();
                
            } catch (Exception e) {
                System.err.println("CRITICAL: Error in refinement completion");
                System.err.println("Exception Details:");
                e.printStackTrace();
                
                // Ensure cleanup happens even on error
                cleanup();
            }
        } else {
            System.err.println("CRITICAL: Callback is null in notifyRefinementCompleted");
            cleanup();
        }
    }

    /**
     * Notify refinement failure.
     * 
     * @param errorMessage Error message
     */

     private void notifyRefinementFailed(String errorMessage) {
        if (callback != null) {
            try {
                callback.onRefinementFailed(errorMessage);
            } catch (Exception e) {
                System.err.println("Error in refinement failure callback: " + e.getMessage());
            }
        }
    }
    /**
     * Cleanup resources.
     */
    private void cleanup() {    
        // Cleanup dialog and UI resources
        if (refinementDialog != null) {
            refinementDialog.dispose();
            refinementDialog = null;
        }
    
        // Note: We're NOT releasing the backgroundRemover here
        // because BackgroundRemovalHandler is responsible for that
        // This prevents premature release of resources needed for mask application
        
        // Set references to null
        visualizationFrame = null;
        
        // Don't clear these until we're sure we're done with the photo processing
        // parentFrame = null;
        // statusLabel = null;
        // currentPhoto = null;
        // callback = null;
        // backgroundRemover = null;
    }
}