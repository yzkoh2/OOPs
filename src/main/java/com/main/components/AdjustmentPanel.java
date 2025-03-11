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
 * Panel for controlling image adjustments like brightness, contrast, etc.
 */
public class AdjustmentPanel extends SettingsPanel {
    
    private Slider brightnessSlider;
    private Slider contrastSlider;
    private Slider saturationSlider;
    private Button resetButton;
    private Button applyButton;
    
    public AdjustmentPanel() {
        super("Image Adjustments");
        
        // Create sliders
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        brightnessSlider = createAdjustmentSlider(-100, 100, 0);
        contrastSlider = createAdjustmentSlider(-100, 100, 0);
        saturationSlider = createAdjustmentSlider(-100, 100, 0);
        
        grid.add(new Label("Brightness:"), 0, 0);
        grid.add(brightnessSlider, 1, 0);
        grid.add(new Label("Contrast:"), 0, 1);
        grid.add(contrastSlider, 1, 1);
        grid.add(new Label("Saturation:"), 0, 2);
        grid.add(saturationSlider, 1, 2);
        
        // Create buttons
        HBox buttonBox = new HBox(10);
        resetButton = new Button("Reset");
        applyButton = new Button("Apply Changes");
        buttonBox.getChildren().addAll(resetButton, applyButton);
        
        getChildren().addAll(grid, buttonBox);
    }
    
    private Slider createAdjustmentSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit((max - min) / 4);
        GridPane.setHgrow(slider, Priority.ALWAYS);
        return slider;
    }
    
    public Slider getBrightnessSlider() {
        return brightnessSlider;
    }
    
    public Slider getContrastSlider() {
        return contrastSlider;
    }
    
    public Slider getSaturationSlider() {
        return saturationSlider;
    }
    
    public Button getResetButton() {
        return resetButton;
    }
    
    public Button getApplyButton() {
        return applyButton;
    }
}