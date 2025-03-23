package com.ui.handlers;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.entities.Photo;
import com.util.Constants;
import com.util.FileUtils;

public class ImageOpenHandler {
    private JFrame parentFrame;
    private JLabel statusLabel;
    private ImageOpenCallback callback;

    // Interface for callback to update UI after image is loaded
    public interface ImageOpenCallback {
        void onImageLoaded(Photo photo);
        void onImageLoadFailed(String errorMessage);
    }

    public ImageOpenHandler(JFrame parentFrame, JLabel statusLabel, ImageOpenCallback callback) {
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.callback = callback;
    }

    public void handleOpenImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Image files", Constants.SUPPORTED_INPUT_FORMATS
        ));

        int result = fileChooser.showOpenDialog(parentFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImage(selectedFile);
        }
    }

    private void loadImage(File file) {
        if (!FileUtils.isImageFile(file)) {
            JOptionPane.showMessageDialog(
                    parentFrame,
                    "Selected file is not a supported image format.",
                    "Invalid File",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        statusLabel.setText("Loading image...");

        SwingWorker<Photo, Void> worker = new SwingWorker<Photo, Void>() {
            @Override
            protected Photo doInBackground() throws Exception {
                return FileUtils.loadPhoto(file);
            }

            @Override
            protected void done() {
                try {
                    Photo loadedPhoto = get();
                    statusLabel.setText("Image loaded: " + file.getName());
                    
                    // Use callback to update UI
                    if (callback != null) {
                        callback.onImageLoaded(loadedPhoto);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    String errorMessage = "Error loading image: " + ex.getMessage();
                    JOptionPane.showMessageDialog(
                            parentFrame,
                            errorMessage,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Failed to load image");
                    
                    // Use callback to handle load failure
                    if (callback != null) {
                        callback.onImageLoadFailed(errorMessage);
                    }
                }
            }
        };

        worker.execute();
    }
}