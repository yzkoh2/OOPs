package com.ui.handlers;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.bytedeco.javacv.Frame;

import com.config.ApplicationConfig;
import com.editor.ImageResizer;
import com.entities.Photo;
import com.util.Constants;

public class ImageResizeHandler {
    private JFrame parentFrame;
    private JLabel statusLabel;
    private Photo currentPhoto;
    private ImageResizeCallback callback;

    // Interface for callback to handle resize events
    public interface ImageResizeCallback {
        void onResizeStarted();
        void onResizeCompleted(Photo resizedPhoto);
        void onResizeFailed(String errorMessage);
    }

    public ImageResizeHandler(
            JFrame parentFrame, 
            JLabel statusLabel, 
            Photo currentPhoto,
            ImageResizeCallback callback
    ) {
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.callback = callback;
    }

    public void handleResize(ActionEvent e) {
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

        // Notify resize started
        if (callback != null) {
            callback.onResizeStarted();
        }

        statusLabel.setText("Resizing image...");

        // Background worker for resizing
        SwingWorker<Photo, Void> worker = new SwingWorker<Photo, Void>() {
            @Override
            protected Photo doInBackground() throws Exception {
                // Get dimensions from config 
                ApplicationConfig config = ApplicationConfig.getInstance();
                int widthMm = config.getIntProperty("photo.width.mm", Constants.PASSPORT_PHOTO_WIDTH_MM);
                int heightMm = config.getIntProperty("photo.height.mm", Constants.PASSPORT_PHOTO_HEIGHT_MM);
                int dpi = config.getIntProperty("photo.dpi", 300);

                // Calculate dimensions in pixels
                int pixelsPerMm = dpi / 25; // 25.4mm per inch, simplified to 25
                int widthPx = widthMm * pixelsPerMm;
                int heightPx = heightMm * pixelsPerMm;

                // Create a copy of the current photo to avoid modifying the original
                Photo resizedPhoto = new Photo(currentPhoto);

                // Get the current processed frame
                Frame photoFrame = resizedPhoto.getProcessedFrame();

                // Create resizer
                ImageResizer resizer = new ImageResizer(widthPx, heightPx, true);
                
                // Resize frame to ID photo size
                Frame resizedFrame = resizer.resizeToIDPhotoSize(photoFrame);
            
                // Update photo with resized frame
                resizedPhoto.setProcessedFrame(resizedFrame);
                
                return resizedPhoto;
            }

            @Override
            protected void done() {
                try {
                    Photo resizedPhoto = get();
                    statusLabel.setText("Image resized to ID photo dimensions");
                    
                    // Notify callback
                    if (callback != null) {
                        callback.onResizeCompleted(resizedPhoto);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    String errorMessage = "Error resizing image: " + ex.getMessage();
                    
                    JOptionPane.showMessageDialog(
                            parentFrame,
                            errorMessage,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    
                    statusLabel.setText("Failed to resize image");
                    
                    // Notify callback of failure
                    if (callback != null) {
                        callback.onResizeFailed(errorMessage);
                    }
                }
            }
        };

        // Execute the resize worker
        worker.execute();
    }

    // Setter for current photo to allow updating the photo to be resized
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
}