package com.main.components;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
/**
 * Panel for controlling background settings like color, blur, etc.
 */
public class BackgroundPanel extends SettingsPanel {
    
    private Button removeBackgroundButton;
    private Button chooseColorButton;
    private Button applyBlurButton;
    private Slider blurSlider;
    
    public BackgroundPanel() {
        super("Background Settings");
        
        // Create background removal button
        removeBackgroundButton = new Button("Remove Background");
        removeBackgroundButton.setMaxWidth(Double.MAX_VALUE);
        
        // Create color chooser button
        chooseColorButton = new Button("Choose Background Color");
        chooseColorButton.setMaxWidth(Double.MAX_VALUE);
        
        // Create blur controls
        HBox blurBox = new HBox(10);
        blurSlider = new Slider(0, 20, 0);
        blurSlider.setShowTickLabels(true);
        blurSlider.setShowTickMarks(true);
        blurSlider.setMajorTickUnit(5);
        HBox.setHgrow(blurSlider, Priority.ALWAYS);
        
        applyBlurButton = new Button("Apply Blur");
        blurBox.getChildren().addAll(blurSlider, applyBlurButton);
        
        // Add all controls to the panel
        getChildren().addAll(removeBackgroundButton, chooseColorButton, 
                new Label("Background Blur"), blurBox);
    }
    
    public Button getRemoveBackgroundButton() {
        return removeBackgroundButton;
    }
    
    public Button getChooseColorButton() {
        return chooseColorButton;
    }
    
    public Button getApplyBlurButton() {
        return applyBlurButton;
    }
    
    public Slider getBlurSlider() {
        return blurSlider;
    }
}
