package com.main.controllers;

import com.entities.BackgroundSettings;
import com.entities.ExportSettings;
import com.entities.Photo;
import com.editor.BackgroundRemover;
import com.editor.ImageExporter;
import com.editor.ImageResizer;
import com.main.MainApplication;
import com.main.components.AdjustmentPanel;
import com.main.components.BackgroundPanel;
import com.main.components.ExportPanel;
import com.main.components.ImagePreviewPane;
import com.main.components.SizePanel;
import com.util.Constants;
import com.util.FileUtils;
import com.util.ImageUtils;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Controller for the import view
 */
public class ImportController {
    
    @FXML
    private BorderPane rootPane;
    
    @FXML
    private VBox mainContainer;
    
    @FXML
    private Button browseButton;
    
    @FXML
    private Button webcamButton;
    
    @FXML
    private Button recentButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private VBox recentPhotosContainer;
    
    private MainApplication mainApplication;
    private Preferences prefs;
    private List<String> recentFilePaths = new ArrayList<>();
    
    /**
     * Initializes the controller class. This method is automatically called
     * after the FXML file has been loaded.
     */
    @FXML
    private void initialize() {
        // Set up UI components
        setupUI();
        
        // Load preferences
        prefs = Preferences.userNodeForPackage(ImportController.class);
        
        // Load recent files
        loadRecentFiles();
    }
    
    /**
     * Set up the UI components and event handlers
     */
    private void setupUI() {
        // Style the main container
        mainContainer.setSpacing(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.CENTER);
        
        // Set up import buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        browseButton = new Button("Browse Files");
        browseButton.setPrefWidth(150);
        browseButton.getStyleClass().add("primary-button");
        browseButton.setOnAction(this::handleBrowseAction);
        
        webcamButton = new Button("Use Webcam");
        webcamButton.setPrefWidth(150);
        webcamButton.getStyleClass().add("secondary-button");
        webcamButton.setOnAction(this::handleWebcamAction);
        
        buttonBox.getChildren().addAll(browseButton, webcamButton);
        
        // Status label
        statusLabel = new Label("Select or capture a photo to begin");
        statusLabel.getStyleClass().add("status-label");
        
        // Recent photos section
        VBox recentSection = new VBox(10);
        recentSection.setAlignment(Pos.CENTER);
        
        recentButton = new Button("View Recent Photos");
        recentButton.getStyleClass().add("text-button");
        recentButton.setOnAction(this::handleRecentAction);
        
        recentPhotosContainer = new VBox(10);
        recentPhotosContainer.setVisible(false);
        recentPhotosContainer.setPrefHeight(200);
        recentPhotosContainer.getStyleClass().add("recent-photos-container");
        
        recentSection.getChildren().addAll(recentButton, recentPhotosContainer);
        
        // Add components to main container
        mainContainer.getChildren().addAll(
                createHeaderLabel("Import Photo"),
                buttonBox,
                statusLabel,
                recentSection
        );
        
        // Add main container to root pane
        rootPane.setCenter(mainContainer);
    }
    
    /**
     * Create a styled header label
     */
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("header-label");
        return label;
    }
    
    /**
     * Load recent files from preferences
     */
    private void loadRecentFiles() {
        // Clear existing list
        recentFilePaths.clear();
        
        // Load up to 5 recent files
        for (int i = 0; i < 5; i++) {
            String path = prefs.get("recent_file_" + i, null);
            if (path != null && new File(path).exists()) {
                recentFilePaths.add(path);
            }
        }
        
        // Update UI
        updateRecentFilesUI();
    }
    
    /**
     * Update the UI to show recent files
     */
    private void updateRecentFilesUI() {
        recentPhotosContainer.getChildren().clear();
        
        if (recentFilePaths.isEmpty()) {
            Label noRecentLabel = new Label("No recent photos found");
            noRecentLabel.getStyleClass().add("info-label");
            recentPhotosContainer.getChildren().add(noRecentLabel);
        } else {
            // Create thumbnail for each recent file
            for (String path : recentFilePaths) {
                File file = new File(path);
                HBox itemBox = createRecentFileItem(file);
                recentPhotosContainer.getChildren().add(itemBox);
            }
        }
    }
    
    /**
     * Create a UI component for a recent file item
     */
    private HBox createRecentFileItem(File file) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5));
        
        // Try to load a thumbnail
        ImageView thumbnailView = new ImageView();
        thumbnailView.setFitHeight(40);
        thumbnailView.setFitWidth(40);
        thumbnailView.setPreserveRatio(true);
        
        try {
            Image thumbnail = new Image(file.toURI().toString(), 40, 40, true, true);
            thumbnailView.setImage(thumbnail);
        } catch (Exception e) {
            // Use a placeholder if image loading fails
            Rectangle placeholder = new Rectangle(40, 40, Color.LIGHTGRAY);
            StackPane placeholderPane = new StackPane(placeholder);
            box.getChildren().add(placeholderPane);
        }
        
        // File name label
        Label nameLabel = new Label(file.getName());
        nameLabel.getStyleClass().add("recent-file-label");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        
        // Open button
        Button openButton = new Button("Open");
        openButton.getStyleClass().add("small-button");
        openButton.setOnAction(e -> openFile(file));
        
        box.getChildren().addAll(thumbnailView, nameLabel, openButton);
        return box;
    }
    
    /**
     * Handle clicking the browse button to select a file
     */
    private void handleBrowseAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Photo");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new ExtensionFilter("All Files", "*.*"));
        
        // Try to set initial directory to last used folder
        String lastDir = prefs.get("last_directory", null);
        if (lastDir != null) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setInitialDirectory(dir);
            }
        }
        
        File selectedFile = fileChooser.showOpenDialog(mainApplication.getPrimaryStage());
        if (selectedFile != null) {
            openFile(selectedFile);
            
            // Save directory for next time
            prefs.put("last_directory", selectedFile.getParent());
        }
    }
    
    /**
     * Open a selected file
     */
    private void openFile(File file) {
        try {
            // Load the image
            Image image = new Image(file.toURI().toString());
            
            if (image.isError()) {
                throw new IOException("Failed to load image");
            }
            
            // Create a new Photo object
            Photo photo = new Photo();
            photo.setImage(image);
            photo.setFileName(file.getName());
            photo.setFilePath(file.getAbsolutePath());
            
            // Add to recent files
            addToRecentFiles(file.getAbsolutePath());
            
            // Set the current photo in the main application
            mainApplication.setCurrentPhoto(photo);
            
            // Move to editor view
            mainApplication.showEditorView();
            
        } catch (Exception e) {
            e.printStackTrace();
            mainApplication.showErrorAlert("Import Error", "Failed to load the selected image",
                    "The file may be corrupted or in an unsupported format.");
            statusLabel.setText("Error loading image. Please try again.");
        }
    }
    
    /**
     * Add a file path to recent files list
     */
    private void addToRecentFiles(String filePath) {
        // Remove if already exists
        recentFilePaths.remove(filePath);
        
        // Add to beginning of list
        recentFilePaths.add(0, filePath);
        
        // Trim list to max 5 items
        while (recentFilePaths.size() > 5) {
            recentFilePaths.remove(recentFilePaths.size() - 1);
        }
        
        // Save to preferences
        for (int i = 0; i < recentFilePaths.size(); i++) {
            prefs.put("recent_file_" + i, recentFilePaths.get(i));
        }
    }
    
    /**
     * Handle clicking the webcam button to take a photo
     */
    private void handleWebcamAction(ActionEvent event) {
        // TODO: Implement webcam capture functionality
        statusLabel.setText("Webcam functionality coming soon");
        
        mainApplication.showInfoAlert("Coming Soon", "Webcam Capture",
                "This feature will be available in the next update.");
    }
    
    /**
     * Handle clicking the recent button to show recent photos
     */
    private void handleRecentAction(ActionEvent event) {
        // Toggle visibility of recent photos container
        boolean isVisible = !recentPhotosContainer.isVisible();
        recentPhotosContainer.setVisible(isVisible);
        recentButton.setText(isVisible ? "Hide Recent Photos" : "View Recent Photos");
    }
    
    /**
     * Set reference to the main application
     * @param mainApplication the main application instance
     */
    public void setMainApplication(MainApplication mainApplication) {
        this.mainApplication = mainApplication;
    }
}
