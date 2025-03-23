package com.ui.handlers;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.editor.ImageExporter;
import com.entities.ExportSettings;
import com.entities.Photo;

public class ImageSaveHandler {
    private JFrame parentFrame;
    private JLabel statusLabel;
    private Photo currentPhoto;
    private ExportSettings exportSettings;
    private ImageSaveCallback callback;

    // Interface for callback to handle save events
    public interface ImageSaveCallback {
        void onImageSaveStarted();
        void onImageSaved(File savedFile);
        void onImageSaveFailed(String errorMessage);
    }

    public ImageSaveHandler(
            JFrame parentFrame, 
            JLabel statusLabel, 
            Photo currentPhoto, 
            ExportSettings exportSettings,
            ImageSaveCallback callback
    ) {
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.exportSettings = exportSettings;
        this.callback = callback;
    }

    public void handleSaveImage(ActionEvent e) {
        // Check if an image is loaded
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                    parentFrame,
                    "No image loaded.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Prepare file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");
        fileChooser.setSelectedFile(new File(
            exportSettings.getFileNamePrefix() + "output." + 
            exportSettings.getFormat().getExtension()
        ));

        // Show save dialog
        int result = fileChooser.showSaveDialog(parentFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String outputPath = selectedFile.getAbsolutePath();
            
            // Update export settings with output directory
            exportSettings.setOutputDirectory(selectedFile.getParent());

            // Notify callback that save is starting
            if (callback != null) {
                callback.onImageSaveStarted();
            }

            // Status update
            statusLabel.setText("Saving image...");

            // Background worker for saving
            SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {
                @Override
                protected File doInBackground() throws Exception {
                    ImageExporter exporter = new ImageExporter(exportSettings);
                    return exporter.export(currentPhoto, outputPath);
                }

                @Override
                protected void done() {
                    try {
                        File savedFile = get();
                        statusLabel.setText("Image saved to: " + savedFile.getAbsolutePath());
                        
                        // Notify callback of successful save
                        if (callback != null) {
                            callback.onImageSaved(savedFile);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        String errorMessage = "Error saving image: " + ex.getMessage();
                        
                        // Show error dialog
                        JOptionPane.showMessageDialog(
                                parentFrame,
                                errorMessage,
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        
                        // Update status
                        statusLabel.setText("Failed to save image");
                        
                        // Notify callback of save failure
                        if (callback != null) {
                            callback.onImageSaveFailed(errorMessage);
                        }
                    }
                }
            };

            // Execute the save worker
            worker.execute();
        }
    }

    // Setter for current photo to allow updating the photo to be saved
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
}