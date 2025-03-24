package com.ui.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.models.RefinementModel;
import com.ui.components.DrawingBoard;
import com.ui.components.RefinementToolbar;

/**
 * Panel for refinement stage of image segmentation.
 */
public class ForegroundRefinementPanel extends JPanel {
    // Converters
    private final Java2DFrameConverter frameConverter = new Java2DFrameConverter();
    
    // UI Components
    private DrawingBoard drawingBoard;
    private RefinementToolbar refinementToolbar;
    
    // Model
    private RefinementModel refinementModel;
    
    // Callback for refinement actions
    private RefinementCallback callback;
    
    /**
     * Callback interface for refinement actions.
     */
    public interface RefinementCallback {
        void onApplyRefinement(
            List<Point> foregroundPoints, 
            List<Point> backgroundPoints, 
            List<Integer> foregroundBrushSizes, 
            List<Integer> backgroundBrushSizes
        );
        void onResetRefinement();
        void onFinalizeRefinement();
    }
    
    /**
     * Constructor for the refinement panel.
     * 
     * @param visualizationFrame Initial visualization frame
     * @param callback Callback for refinement actions
     */
    public ForegroundRefinementPanel(Frame visualizationFrame, RefinementCallback callback) {
        this.callback = callback;
        this.refinementModel = new RefinementModel();
        
        // Setup panel layout
        setLayout(new BorderLayout());
        
        // Create and add components
        initializeComponents(visualizationFrame);
        
        // Add action buttons
        add(createActionPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Initialize panel components.
     * 
     * @param visualizationFrame Frame for initial visualization
     */
    private void initializeComponents(Frame visualizationFrame) {
        // Convert frame to BufferedImage
        BufferedImage visualizationImage = frameConverter.convert(visualizationFrame);
        
        // Create refinement toolbar and add it to NORTH position
        refinementToolbar = new RefinementToolbar(refinementModel);
        add(refinementToolbar, BorderLayout.NORTH);
        
        // Calculate scaled dimensions to fit within maximum bounds
        int maxPreviewSize = 800;
        int maxHeight = 600;
        
        int panelWidth = visualizationImage.getWidth();
        int panelHeight = visualizationImage.getHeight();
        
        if (panelWidth > maxPreviewSize || panelHeight > maxHeight) {
            double aspectRatio = (double) visualizationImage.getWidth() / visualizationImage.getHeight();
            
            if (visualizationImage.getWidth() > maxPreviewSize) {
                panelWidth = maxPreviewSize;
                panelHeight = (int) (panelWidth / aspectRatio);
            }
            
            if (panelHeight > maxHeight) {
                panelHeight = maxHeight;
                panelWidth = (int) (panelHeight * aspectRatio);
            }
        }
        
        // Create drawing board
        drawingBoard = new DrawingBoard(refinementModel, visualizationImage);
        
        // Set preferred size based on the calculated dimensions
        drawingBoard.setPreferredSize(new Dimension(panelWidth, panelHeight));
        
        // Add drawing board directly to the panel without JScrollPane
        add(drawingBoard, BorderLayout.CENTER);
    }
    
    /**
     * Create action panel with Apply and Reset buttons.
     * 
     * @return JPanel with action buttons
     */
    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel();
        
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            if (callback != null) {
                callback.onApplyRefinement(
                    refinementModel.extractForegroundPoints(),
                    refinementModel.extractBackgroundPoints(),
                    refinementModel.extractForegroundBrushSizes(),
                    refinementModel.extractBackgroundBrushSizes()
                );
            }
            
            // Clear the model after applying
            refinementModel.clearMarkings();
            drawingBoard.repaint();
        });
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            refinementModel.clearMarkings();
            drawingBoard.repaint();
            
            if (callback != null) {
                callback.onResetRefinement();
            }
        });
        
        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(e -> {
            // Apply any remaining markings
            if (callback != null) {
                callback.onApplyRefinement(
                    refinementModel.extractForegroundPoints(),
                    refinementModel.extractBackgroundPoints(),
                    refinementModel.extractForegroundBrushSizes(),
                    refinementModel.extractBackgroundBrushSizes()
                );
                
                // Call the new finalize method
                callback.onFinalizeRefinement();
            }
            
            // Close the dialog
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        });
        
        actionPanel.add(applyButton);
        actionPanel.add(resetButton);
        actionPanel.add(confirmButton);
        
        return actionPanel;
    }
    
    /**
     * Update the visualization image.
     * 
     * @param visualizationFrame New visualization frame
     */
    public void updateVisualization(Frame visualizationFrame) {
        if (visualizationFrame != null) {
            BufferedImage newImage = frameConverter.convert(visualizationFrame);
            drawingBoard.updateBackgroundImage(newImage);
        }
    }
    
    /**
     * Clear all markings.
     */
    public void clearMarkings() {
        refinementModel.clearMarkings();
        drawingBoard.repaint();
    }
    
    /**
     * Release resources when panel is no longer needed.
     */
    public void releaseResources() {
        // Remove callback reference
        callback = null;
        
        // Clear the refinement model
        refinementModel.clearMarkings();
        
        // Remove and cleanup components
        if (drawingBoard != null) {
            remove(drawingBoard);
            drawingBoard = null;
        }
        
        if (refinementToolbar != null) {
            remove(refinementToolbar);
            refinementToolbar = null;
        }
        
        // Force garbage collection hint
        System.gc();
    }
}