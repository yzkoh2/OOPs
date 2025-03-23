package com.ui.handlers;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.bytedeco.opencv.opencv_core.Rect;

import com.editor.BackgroundRemover;
import com.entities.BackgroundSettings;
import com.entities.Photo;
import com.ui.panels.SelectionPanel;

public class BackgroundRemovalHandler {
    private GenericImageHandler genericHandler;

    public interface BackgroundRemovalCallback {
        void onBackgroundRemovalStarted();
        void onBackgroundRemovalCompleted(Photo processedPhoto);
        void onBackgroundRemovalFailed(String errorMessage);
    }

    public BackgroundRemovalHandler(
            JFrame parentFrame, 
            JLabel statusLabel, 
            Photo currentPhoto,
            BackgroundSettings backgroundSettings,
            BackgroundRemovalCallback callback
    ) {
        genericHandler = new GenericImageHandler(
            parentFrame, 
            statusLabel, 
            currentPhoto,
            new GenericImageHandler.ImageProcessCallback() {
                @Override
                public void onProcessStarted() {
                    if (callback != null) callback.onBackgroundRemovalStarted();
                }

                @Override
                public void onProcessCompleted(Photo processedPhoto) {
                    if (callback != null) callback.onBackgroundRemovalCompleted(processedPhoto);
                }

                @Override
                public void onProcessFailed(String errorMessage) {
                    if (callback != null) callback.onBackgroundRemovalFailed(errorMessage);
                }
            },
            new GenericImageHandler.ImageProcessor() {
                @Override
                public void process(Photo photo, Rect rect) throws Exception {
                    // Create background remover
                    BackgroundRemover remover = new BackgroundRemover(backgroundSettings);
                    
                    // Set selection rectangle
                    remover.setSelectionRect(
                        rect.x(), 
                        rect.y(),
                        rect.x() + rect.width(),
                        rect.y() + rect.height()
                    );
                    
                    // Process the photo
                    remover.process(photo);
                }
            },
            SelectionPanel.class,
            "Select Foreground - Click and drag to select area",
            "Removing background..."
        );
    }

    public void handleRemoveBackground(ActionEvent e) {
        genericHandler.handleProcess(e);
    }

    // Setter for current photo to allow updating the photo for background removal
    public void setCurrentPhoto(Photo photo) {
        genericHandler.setCurrentPhoto(photo);
    }
}