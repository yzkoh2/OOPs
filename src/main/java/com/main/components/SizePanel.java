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
 * Panel for controlling photo size and cropping
 */
public class SizePanel extends SettingsPanel {
    
    private TextField widthField;
    private TextField heightField;
    private Button applySizeButton;
    private Button standardSizeButton;
    private Button cropButton;
    
    public SizePanel() {
        super("Photo Size Settings");
        
        // Create size input fields
        GridPane sizeGrid = new GridPane();
        sizeGrid.setHgap(10);
        sizeGrid.setVgap(10);
        
        widthField = new TextField();
        widthField.setPromptText("Width (px)");
        heightField = new TextField();
        heightField.setPromptText("Height (px)");
        
        sizeGrid.add(new Label("Width:"), 0, 0);
        sizeGrid.add(widthField, 1, 0);
        sizeGrid.add(new Label("px"), 2, 0);
        sizeGrid.add(new Label("Height:"), 0, 1);
        sizeGrid.add(heightField, 1, 1);
        sizeGrid.add(new Label("px"), 2, 1);
        
        // Create buttons
        applySizeButton = new Button("Apply Custom Size");
        standardSizeButton = new Button("Standard ID Sizes");
        cropButton = new Button("Crop Photo");
        
        applySizeButton.setMaxWidth(Double.MAX_VALUE);
        standardSizeButton.setMaxWidth(Double.MAX_VALUE);
        cropButton.setMaxWidth(Double.MAX_VALUE);
        
        getChildren().addAll(sizeGrid, applySizeButton, standardSizeButton, cropButton);
    }
    
    public TextField getWidthField() {
        return widthField;
    }
    
    public TextField getHeightField() {
        return heightField;
    }
    
    public Button getApplySizeButton() {
        return applySizeButton;
    }
    
    public Button getStandardSizeButton() {
        return standardSizeButton;
    }
    
    public Button getCropButton() {
        return cropButton;
    }
}