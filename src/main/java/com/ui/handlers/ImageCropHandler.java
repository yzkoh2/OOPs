package com.ui.handlers;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.bytedeco.opencv.opencv_core.Rect;

import com.editor.ImageResizer;
import com.entities.Photo;
import com.ui.panels.CropPanel;

public class ImageCropHandler {
    private GenericImageHandler genericHandler;

    public interface ImageCropCallback {
        void onCropStarted();
        void onCropCompleted(Photo croppedPhoto);
        void onCropFailed(String errorMessage);
    }

    public ImageCropHandler(
            JFrame parentFrame, 
            JLabel statusLabel, 
            Photo currentPhoto,
            ImageCropCallback callback
    ) {
        genericHandler = new GenericImageHandler(
            parentFrame, 
            statusLabel, 
            currentPhoto,
            new GenericImageHandler.ImageProcessCallback() {
                @Override
                public void onProcessStarted() {
                    if (callback != null) callback.onCropStarted();
                }

                @Override
                public void onProcessCompleted(Photo processedPhoto) {
                    if (callback != null) callback.onCropCompleted(processedPhoto);
                }

                @Override
                public void onProcessFailed(String errorMessage) {
                    if (callback != null) callback.onCropFailed(errorMessage);
                }
            },
            new GenericImageHandler.ImageProcessor() {
                @Override
                public void process(Photo photo, Rect rect) throws Exception {
                    // Create resizer with crop rectangle
                    ImageResizer resizer = new ImageResizer(
                            rect,
                            rect.width(),
                            rect.height(),
                            true
                    );
                    
                    // Process the photo
                    resizer.process(photo);
                }
            },
            CropPanel.class,
            "Crop Image - Click and drag to select area",
            "Cropping image..."
        );
    }

    public void handleCrop(ActionEvent e) {
        genericHandler.handleProcess(e);
    }

    // Setter for current photo to allow updating the photo to be cropped
    public void setCurrentPhoto(Photo photo) {
        genericHandler.setCurrentPhoto(photo);
    }
}