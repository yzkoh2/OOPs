package com.ui.components;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import com.models.RefinementModel;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Toolbar component for image refinement controls.
 */
public class RefinementToolbar extends JPanel {
    private final RefinementModel refinementModel;
    
    // UI Components
    private final JRadioButton foregroundButton;
    private final JRadioButton backgroundButton;
    private final JSlider brushSizeSlider;
    private final JLabel brushSizeLabel;
    
    /**
     * Creates a new RefinementToolbar.
     * 
     * @param refinementModel The model to manage refinement state
     */
    public RefinementToolbar(RefinementModel refinementModel) {
        this.refinementModel = refinementModel;
        
        // Configure panel layout
        setLayout(new FlowLayout(FlowLayout.LEFT));
        
        // Create marking mode buttons
        ButtonGroup modeGroup = new ButtonGroup();
        foregroundButton = new JRadioButton("Mark Foreground", true);
        backgroundButton = new JRadioButton("Mark Background", false);
        
        modeGroup.add(foregroundButton);
        modeGroup.add(backgroundButton);
        
        // Set up mode change listeners
        foregroundButton.addActionListener(e -> 
            refinementModel.setCurrentMode(RefinementModel.MarkingMode.FOREGROUND)
        );
        backgroundButton.addActionListener(e -> 
            refinementModel.setCurrentMode(RefinementModel.MarkingMode.BACKGROUND)
        );
        
        // Brush size slider
        brushSizeLabel = new JLabel("Brush Size: ");
        brushSizeSlider = new JSlider(JSlider.HORIZONTAL, 2, 50, 10);
        brushSizeSlider.setMajorTickSpacing(10);
        brushSizeSlider.setMinorTickSpacing(2);
        brushSizeSlider.setPaintTicks(true);
        
        // Update model when brush size changes
        brushSizeSlider.addChangeListener(e -> 
            refinementModel.setBrushSize(brushSizeSlider.getValue())
        );
        
        // Add components to toolbar
        add(foregroundButton);
        add(backgroundButton);
        add(brushSizeLabel);
        add(brushSizeSlider);
    }
    
    /**
     * Get the foreground marking button.
     * 
     * @return Foreground radio button
     */
    public JRadioButton getForegroundButton() {
        return foregroundButton;
    }
    
    /**
     * Get the background marking button.
     * 
     * @return Background radio button
     */
    public JRadioButton getBackgroundButton() {
        return backgroundButton;
    }
    
    /**
     * Get the brush size slider.
     * 
     * @return Brush size slider
     */
    public JSlider getBrushSizeSlider() {
        return brushSizeSlider;
    }
}