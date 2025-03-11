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
 * Image preview component with zoom and pan capabilities
 */
public class ImagePreviewPane extends VBox {
    
    private javafx.scene.image.ImageView imageView;
    private Slider zoomSlider;
    
    public ImagePreviewPane() {
        setPadding(new Insets(10));
        setSpacing(10);
        
        // Create the image view
        imageView = new javafx.scene.image.ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        
        // Create zoom controls
        HBox zoomBox = new HBox(10);
        zoomSlider = new Slider(0.5, 2.0, 1.0);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(0.5);
        zoomBox.getChildren().addAll(new Label("Zoom:"), zoomSlider);
        
        // Add components
        getChildren().addAll(imageView, zoomBox);
    }
    
    public javafx.scene.image.ImageView getImageView() {
        return imageView;
    }
    
    public Slider getZoomSlider() {
        return zoomSlider;
    }
    
    public void setImage(javafx.scene.image.Image image) {
        imageView.setImage(image);
    }
}