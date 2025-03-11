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
 * Provides a panel with common controls for adjusting photo settings
 */
public class SettingsPanel extends VBox {
    
    private Label titleLabel;
    
    public SettingsPanel(String title) {
        setPadding(new Insets(10));
        setSpacing(10);
        
        // Create title label with larger, bold font
        titleLabel = new Label(title);
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
        
        getChildren().add(titleLabel);
    }
    
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}



