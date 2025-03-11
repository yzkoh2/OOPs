package com.main;

import com.config.ApplicationConfig;
import com.entities.BackgroundSettings;
import com.entities.ExportSettings;
import com.entities.Photo;
import com.editor.BackgroundRemover;
import com.editor.ImageExporter;
import com.editor.ImageProcessor;
import com.editor.ImageResizer;
import com.main.controllers.EditorController;
import com.main.controllers.ExportController;
import com.main.controllers.ImportController;
import com.util.Constants;
import com.util.FileUtils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main entry point for the ID Photo Generator application.
 * This class initializes the JavaFX UI and manages the primary application flow.
 */
public class MainApplication extends Application {
    
    private Stage primaryStage;
    private BorderPane rootLayout;
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private ExportSettings exportSettings;
    
    // Service components
    private ImageProcessor imageProcessor;
    private BackgroundRemover backgroundRemover;
    private ImageResizer imageResizer;
    private ImageExporter imageExporter;
    
    // Controllers
    private ImportController importController;
    private EditorController editorController;
    private ExportController exportController;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(Constants.APP_NAME);
        this.primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_icon.png")));
        
        // Initialize settings
        backgroundSettings = new BackgroundSettings();
        exportSettings = new ExportSettings();
        
        // Initialize services
        initializeServices();
        
        // Set up the root layout
        initRootLayout();
        
        // Show the import view initially
        showImportView();
        
        // Set minimum window size
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        primaryStage.show();
    }
    
    /**
     * Initialize the service components
     */
    private void initializeServices() {
        backgroundRemover = new BackgroundRemover();
        imageResizer = new ImageResizer();
        imageExporter = new ImageExporter();
        imageProcessor = new ImageProcessor(); // This was missing
    }
    
    /**
     * Initialize the root layout which will contain all other UI components
     */
    private void initRootLayout() {
        try {
            // Load root layout from fxml file
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApplication.class.getResource("/views/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();
            
            // Set the scene containing the root layout
            Scene scene = new Scene(rootLayout);
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Application Error", "Failed to load the application layout.", 
                    "Please restart the application. If the problem persists, please reinstall.");
        }
    }
    
    /**
     * Shows the import view where users can upload or select photos
     */
    public void showImportView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApplication.class.getResource("/views/ImportView.fxml"));
            
            rootLayout.setCenter(loader.load());
            
            // Give the controller access to the main app
            importController = loader.getController();
            importController.setMainApplication(this);
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to load the import view.", 
                    "Please restart the application.");
        }
    }
    
    /**
     * Shows the editor view where users can adjust and process photos
     */
    public void showEditorView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApplication.class.getResource("/views/EditorView.fxml"));
            
            rootLayout.setCenter(loader.load());
            
            // Give the controller access to the main app
            editorController = loader.getController();
            editorController.setMainApplication(this);
            editorController.setCurrentPhoto(currentPhoto);
            editorController.setBackgroundSettings(backgroundSettings);
            editorController.initializeView();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to load the editor view.", 
                    "Please restart the application.");
        }
    }
    
    /**
     * Shows the export view where users can configure export settings and save photos
     */
    public void showExportView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApplication.class.getResource("/views/ExportView.fxml"));
            
            rootLayout.setCenter(loader.load());
            
            // Give the controller access to the main app
            exportController = loader.getController();
            exportController.setMainApplication(this);
            exportController.setCurrentPhoto(currentPhoto);
            exportController.setExportSettings(exportSettings);
            exportController.initializeView();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to load the export view.", 
                    "Please restart the application.");
        }
    }
    
    /**
     * Set the current photo and update relevant UI components
     * @param photo the new photo object
     */
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
    
    /**
     * Get the image processor service
     * @return the image processor
     */
    public ImageProcessor getImageProcessor() {
        return imageProcessor;
    }
    
    /**
     * Get the background remover service
     * @return the background remover
     */
    public BackgroundRemover getBackgroundRemover() {
        return backgroundRemover;
    }
    
    /**
     * Get the image resizer service
     * @return the image resizer
     */
    public ImageResizer getImageResizer() {
        return imageResizer;
    }
    
    /**
     * Get the image exporter service
     * @return the image exporter
     */
    public ImageExporter getImageExporter() {
        return imageExporter;
    }
    
    /**
     * Display an error alert dialog
     * @param title the title of the alert
     * @param header the header text
     * @param content the content text
     */
    public void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Display an information alert dialog
     * @param title the title of the alert
     * @param header the header text
     * @param content the content text
     */
    public void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Application entry point
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Load application configuration
        ApplicationConfig.getInstance();
        
        // Launch the JavaFX application
        launch(args);
    }
}