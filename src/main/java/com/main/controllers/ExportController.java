package com.main.controllers;

import com.entities.ExportSettings;
import com.entities.Photo;
import com.main.MainApplication;
import com.util.Constants;
import com.util.FileUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;

/**
 * Controller for the Export View where users can configure export settings and save photos
 */
public class ExportController {
    
    @FXML private ImageView previewImageView;
    @FXML private ComboBox<String> formatComboBox;
    @FXML private Slider qualitySlider;
    @FXML private TextField fileNameTextField;
    @FXML private TextField outputPathTextField;
    @FXML private Button browseButton;
    @FXML private Button exportButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;
    
    private MainApplication mainApplication;
    private Photo currentPhoto;
    private ExportSettings exportSettings;
    
    /**
     * Initialize the controller
     */
    public void initialize() {
        // Initialize UI components
        formatComboBox.getItems().addAll(Constants.OUTPUT_FORMATS);
        
        // Set up event handlers
        browseButton.setOnAction(event -> browseOutputDirectory());
        exportButton.setOnAction(event -> exportPhoto());
        backButton.setOnAction(event -> goBack());
        
        // Set up quality slider
        qualitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (exportSettings != null) {
                exportSettings.setQuality(newVal.intValue());
            }
        });
        
        // Set up format combo box
        formatComboBox.setOnAction(event -> {
            if (exportSettings != null) {
                exportSettings.setOutputFormat(formatComboBox.getValue());
            }
        });
    }
    
    /**
     * Initialize the view with current data
     */
    public void initializeView() {
        if (currentPhoto == null || 
            (currentPhoto.getProcessedImage() == null && currentPhoto.getOriginalImage() == null)) {
            mainApplication.showErrorAlert("Error", "No photo to export", 
                    "Please go back and process a photo first.");
            return;
        }
        
        // Display the processed image or original if no processing was done
        previewImageView.setImage(currentPhoto.getProcessedImage() != null ? 
                currentPhoto.getProcessedImage() : currentPhoto.getOriginalImage());
        
        // Set initial values from settings
        if (exportSettings != null) {
            formatComboBox.setValue(exportSettings.getOutputFormat());
            qualitySlider.setValue(exportSettings.getQuality());
            outputPathTextField.setText(exportSettings.getOutputDirectory());
            
            // Generate a default filename based on the original filename
            if (currentPhoto.getFileName() != null) {
                String baseName = FileUtils.getFileNameWithoutExtension(currentPhoto.getFileName());
                fileNameTextField.setText(baseName + "_ID");
            } else {
                fileNameTextField.setText("ID_Photo");
            }
        }
        
        // Update status
        statusLabel.setText("Ready to export");
    }
    
    /**
     * Open a directory chooser to select the output directory
     */
    private void browseOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");
        
        // Set initial directory if available
        if (exportSettings != null && exportSettings.getOutputDirectory() != null) {
            File initialDir = new File(exportSettings.getOutputDirectory());
            if (initialDir.exists()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(mainApplication.getPrimaryStage());
        
        if (selectedDirectory != null) {
            String path = selectedDirectory.getAbsolutePath();
            outputPathTextField.setText(path);
            
            if (exportSettings != null) {
                exportSettings.setOutputDirectory(path);
            }
        }
    }
    
    /**
     * Export the photo with the current settings
     */
    private void exportPhoto() {
        if (currentPhoto == null || 
            (currentPhoto.getProcessedImage() == null && currentPhoto.getOriginalImage() == null)) {
            showError("No photo to export");
            return;
        }
        
        // Validate input
        String fileName = fileNameTextField.getText().trim();
        if (fileName.isEmpty()) {
            showError("Please enter a file name");
            return;
        }
        
        String outputPath = outputPathTextField.getText().trim();
        if (outputPath.isEmpty()) {
            showError("Please select an output directory");
            return;
        }
        
        // Update status
        statusLabel.setText("Exporting photo...");
        
        try {
            // Update export settings
            if (exportSettings != null) {
                exportSettings.setFileName(fileName);
                exportSettings.setOutputDirectory(outputPath);
                exportSettings.setOutputFormat(formatComboBox.getValue());
                exportSettings.setQuality((int) qualitySlider.getValue());
            }
            
            // Export the photo
            File outputFile = mainApplication.getImageExporter().exportImage(
                    currentPhoto.getProcessedImage() != null ? 
                            currentPhoto.getProcessedImage() : 
                            currentPhoto.getOriginalImage(),
                    exportSettings);
            
            // Show success message
            statusLabel.setText("Photo exported successfully");
            mainApplication.showInfoAlert("Export Successful", 
                    "Photo exported successfully", 
                    "The photo has been saved to: " + outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            showError("Failed to export photo: " + e.getMessage());
        }
    }
    
    /**
     * Navigate back to the editor view
     */
    private void goBack() {
        mainApplication.showEditorView();
    }
    
    /**
     * Display an error message
     * @param message the error message
     */
    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        mainApplication.showErrorAlert("Export Error", "An error occurred", message);
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
     * @param photo the photo to export
     */
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
    
    /**
     * Set the export settings
     * @param settings the export settings
     */
    public void setExportSettings(ExportSettings settings) {
        this.exportSettings = settings;
    }
}
