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
 * Panel for export settings
 */
public class ExportPanel extends SettingsPanel {
    
    private Button formatJpgButton;
    private Button formatPngButton;
    private Slider qualitySlider;
    private Button printButton;
    private Button saveButton;
    
    public ExportPanel() {
        super("Export Settings");
        
        // Create format buttons
        HBox formatBox = new HBox(10);
        formatJpgButton = new Button("JPG");
        formatPngButton = new Button("PNG");
        formatBox.getChildren().addAll(new Label("Format:"), formatJpgButton, formatPngButton);
        
        // Create quality slider
        VBox qualityBox = new VBox(5);
        qualitySlider = new Slider(0, 100, 85);
        qualitySlider.setShowTickLabels(true);
        qualitySlider.setShowTickMarks(true);
        qualitySlider.setMajorTickUnit(20);
        qualityBox.getChildren().addAll(new Label("Quality:"), qualitySlider);
        
        // Create action buttons
        HBox actionBox = new HBox(10);
        printButton = new Button("Print");
        saveButton = new Button("Save");
        actionBox.getChildren().addAll(printButton, saveButton);
        
        getChildren().addAll(formatBox, qualityBox, actionBox);
    }
    
    public Button getFormatJpgButton() {
        return formatJpgButton;
    }
    
    public Button getFormatPngButton() {
        return formatPngButton;
    }
    
    public Slider getQualitySlider() {
        return qualitySlider;
    }
    
    public Button getPrintButton() {
        return printButton;
    }
    
    public Button getSaveButton() {
        return saveButton;
    }
}