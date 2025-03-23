package com.ui.handlers;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.bytedeco.opencv.opencv_core.Rect;

import com.editor.ImageResizer;
import com.entities.Photo;
import com.ui.panels.CropPanel;

public class ImageCropHandler {
    private JFrame parentFrame;
    private JLabel statusLabel;
    private Photo currentPhoto;
    private ImageCropCallback callback;

    // Interface for callback to handle crop events
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
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.callback = callback;
    }

    public void handleCrop(ActionEvent e) {
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

        // Prepare the image for cropping
        BufferedImage image = currentPhoto.getProcessedBufferedImage();

        // Calculate scaling for crop panel
        int maxPreviewSize = 800;
        int maxHeight = 600;
        double aspectRatio = (double) image.getWidth() / image.getHeight();
        
        int panelWidth = maxPreviewSize;
        int panelHeight = (int) (maxPreviewSize / aspectRatio);

        if (panelHeight > maxHeight) {
            panelHeight = maxHeight;
            panelWidth = (int) (maxHeight * aspectRatio);
        }

        // Scale the image for preview
        Image scaledImage = image.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        // Calculate scaling factors
        final double scaleX = (double) image.getWidth() / resizedImage.getWidth();
        final double scaleY = (double) image.getHeight() / resizedImage.getHeight();

        // Create crop panel
        CropPanel cropPanel = new CropPanel(resizedImage);

        // Create crop frame
        JFrame cropFrame = new JFrame("Crop Image - Click and drag to select area");
        cropFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        cropFrame.add(new JScrollPane(cropPanel));
        cropFrame.setSize(
            Math.min(image.getWidth() + 50, 800),
            Math.min(image.getHeight() + 100, 600)
        );
        cropFrame.setLocationRelativeTo(parentFrame);

        // Add confirmation button
        javax.swing.JButton confirmButton = new javax.swing.JButton("Confirm Crop");
        confirmButton.addActionListener(confirmEvent -> {
            Rectangle cropRect = cropPanel.getSelectionRectangle();

            if (cropRect != null && cropRect.width > 10 && cropRect.height > 10) {
                // Map crop area back to original image size
                int originalX = (int) (cropRect.x * scaleX);
                int originalY = (int) (cropRect.y * scaleY);
                int originalWidth = (int) (cropRect.width * scaleX);
                int originalHeight = (int) (cropRect.height * scaleY);

                Rectangle originalRect = new Rectangle(originalX, originalY, originalWidth, originalHeight);

                // Notify crop started
                if (callback != null) {
                    callback.onCropStarted();
                }

                statusLabel.setText("Cropping image...");

                // Background worker for cropping
                SwingWorker<Photo, Void> worker = new SwingWorker<Photo, Void>() {
                    @Override
                    protected Photo doInBackground() throws Exception {
                        // Create rect for OpenCV
                        Rect rect = new Rect(originalRect.x, originalRect.y, 
                                             originalRect.width, originalRect.height);

                        // Create resizer with crop rectangle
                        ImageResizer resizer = new ImageResizer(
                                rect,
                                rect.width(),
                                rect.height(),
                                true
                        );

                        // Create a copy of the current photo to avoid modifying the original
                        Photo croppedPhoto = new Photo(currentPhoto);
                        
                        // Process the photo
                        resizer.process(croppedPhoto);
                        return croppedPhoto;
                    }

                    @Override
                    protected void done() {
                        try {
                            Photo croppedPhoto = get();
                            statusLabel.setText("Image cropped successfully");
                            
                            // Notify callback
                            if (callback != null) {
                                callback.onCropCompleted(croppedPhoto);
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            String errorMessage = "Error cropping image: " + ex.getMessage();
                            
                            JOptionPane.showMessageDialog(
                                    parentFrame,
                                    errorMessage,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            
                            statusLabel.setText("Failed to crop image");
                            
                            // Notify callback of failure
                            if (callback != null) {
                                callback.onCropFailed(errorMessage);
                            }
                        }
                    }
                };

                // Execute the crop worker
                worker.execute();
                cropFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(
                        cropFrame,
                        "Please select a valid crop area (minimum 10x10 pixels).",
                        "Invalid Selection",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        // Add cancel button
        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
        cancelButton.addActionListener(cancelEvent -> cropFrame.dispose());

        // Create button panel
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // Add button panel to frame
        cropFrame.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        // Make crop frame visible
        cropFrame.setVisible(true);
    }

    // Setter for current photo to allow updating the photo to be cropped
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
}