package com.main.controllers;

import com.entities.BackgroundSettings;
import com.entities.Photo;
import com.main.MainApplication;
import com.util.Constants;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;

/**
 * Controller for the Editor View where users can adjust photos and change backgrounds
 */
public class EditorController {
    
    @FXML private ImageView photoImageView;
    @FXML private ComboBox<String> photoTypeComboBox;
    @FXML private ColorPicker backgroundColorPicker;
    @FXML private Button removeBackgroundButton;
    @FXML private Button applyChangesButton;
    @FXML private Button nextButton;
    @FXML private Button backButton;
    @FXML private Slider brightnessSlider;
    @FXML private Slider contrastSlider;
    @FXML private Label statusLabel;
    @FXML private VBox editorControlsPane;
    
    private MainApplication mainApplication;
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private Image processedImage;
    
    /**
     * Initialize the controller
     */
    public void initialize() {
        // Initialize UI components
        photoTypeComboBox.getItems().addAll(Constants.PHOTO_TYPES);
        
        // Set up event handlers
        removeBackgroundButton.setOnAction(event -> removeBackground());
        applyChangesButton.setOnAction(event -> applyChanges());
        nextButton.setOnAction(event -> goToExport());
        backButton.setOnAction(event -> goBack());
        
        // Set up sliders
        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        contrastSlider.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        
        // Set up color picker
        backgroundColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (backgroundSettings != null) {
                backgroundSettings.setBackgroundColor(colorToHex(newVal));
                updatePreview();
            }
        });
        
        // Set up photo type combo box
        photoTypeComboBox.setOnAction(event -> {
            if (backgroundSettings != null) {
                backgroundSettings.setPhotoType(photoTypeComboBox.getValue());
                updatePreview();
            }
        });
    }
    
    /**
     * Initialize the view with current data
     */
    public void initializeView() {
        if (currentPhoto == null || currentPhoto.getOriginalImage() == null) {
            mainApplication.showErrorAlert("Error", "No photo loaded", 
                    "Please go back and select a photo first.");
            return;
        }
        
        // Display the original image
        photoImageView.setImage(currentPhoto.getOriginalImage());
        
        // Set initial values from settings
        if (backgroundSettings != null) {
            photoTypeComboBox.setValue(backgroundSettings.getPhotoType());
            backgroundColorPicker.setValue(hexToColor(backgroundSettings.getBackgroundColor()));
        }
        
        // Reset sliders
        brightnessSlider.setValue(0);
        contrastSlider.setValue(0);
        
        // Update status
        statusLabel.setText("Ready to edit");
    }
    
    /**
     * Remove background from the photo
     */
    private void removeBackground() {
        if (currentPhoto == null || currentPhoto.getOriginalImage() == null) {
            showError("No photo loaded");
            return;
        }
        
        statusLabel.setText("Removing background...");
        
        try {
            // Use the background remover service
            processedImage = mainApplication.getBackgroundRemover().removeBackground(
                    currentPhoto.getOriginalImage(), 
                    backgroundColorPicker.getValue());
            
            // Update the image view
            photoImageView.setImage(processedImage);
            
            // Update the current photo
            currentPhoto.setProcessedImage(processedImage);
            
            statusLabel.setText("Background removed successfully");
        } catch (Exception e) {
            showError("Failed to remove background: " + e.getMessage());
        }
    }
    
    /**
     * Apply changes to the photo (brightness, contrast, etc.)
     */
    private void applyChanges() {
        if (currentPhoto == null || currentPhoto.getOriginalImage() == null) {
            showError("No photo loaded");
            return;
        }
        
        statusLabel.setText("Applying changes...");
        
        try {
            // Get values from UI controls
            double brightness = brightnessSlider.getValue();
            double contrast = contrastSlider.getValue();
            
            // Apply changes using image processor
            Image adjustedImage = mainApplication.getImageProcessor().adjustImage(
                    currentPhoto.getProcessedImage() != null ? 
                            currentPhoto.getProcessedImage() : 
                            currentPhoto.getOriginalImage(),
                    brightness, 
                    contrast);
            
            // Update the image view
            photoImageView.setImage(adjustedImage);
            
            // Update the current photo
            currentPhoto.setProcessedImage(adjustedImage);
            
            statusLabel.setText("Changes applied successfully");
        } catch (Exception e) {
            showError("Failed to apply changes: " + e.getMessage());
        }
    }
    
    /**
     * Update the preview based on current settings
     */
    private void updatePreview() {
        if (currentPhoto == null || currentPhoto.getOriginalImage() == null) {
            return;
        }
        
        // This would be called when sliders or settings change
        // For performance reasons, you might want to debounce this
        // or only apply changes when the user clicks a button
    }
    
    /**
     * Navigate to the export view
     */
    private void goToExport() {
        if (currentPhoto == null || 
            (currentPhoto.getProcessedImage() == null && currentPhoto.getOriginalImage() == null)) {
            showError("No photo to export");
            return;
        }
        
        mainApplication.showExportView();
    }
    
    /**
     * Navigate back to the import view
     */
    private void goBack() {
        mainApplication.showImportView();
    }
    
    /**
     * Display an error message
     * @param message the error message
     */
    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        mainApplication.showErrorAlert("Editor Error", "An error occurred", message);
    }
    
    /**
     * Convert a JavaFX Color to a hex string
     * @param color the color to convert
     * @return the hex string representation
     */
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }
    
    /**
     * Convert a hex string to a JavaFX Color
     * @param hex the hex string
     * @return the Color object
     */
    private Color hexToColor(String hex) {
        return Color.web(hex);
    }
    
    /**
     * Set the main application reference
     * @param mainApplication the main application
     */
    public void setMainApplication(MainApplication mainApplication) {
        this.mainApplication = mainApplication;
    }
    
    /**
     * Set the current photo
     * @param photo the photo to edit
     */
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
    
    /**
     * Set the background settings
     * @param settings the background settings
     */
    public void setBackgroundSettings(BackgroundSettings settings) {
        this.backgroundSettings = settings;
    }
}
