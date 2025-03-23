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

import com.editor.BackgroundRemover;
import com.entities.BackgroundSettings;
import com.entities.Photo;
import com.ui.panels.SelectionPanel;

public class BackgroundRemovalHandler {
    private JFrame parentFrame;
    private JLabel statusLabel;
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private BackgroundRemovalCallback callback;

    // Interface for callback to handle background removal events
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
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.backgroundSettings = backgroundSettings;
        this.callback = callback;
    }

    public void handleRemoveBackground(ActionEvent e) {
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

        // Prepare the image for background removal
        BufferedImage image = currentPhoto.getProcessedBufferedImage();

        // Calculate scaling for selection panel
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

        // Create selection panel
        SelectionPanel selectionPanel = new SelectionPanel(resizedImage);

        // Create selection frame
        JFrame selectionFrame = new JFrame("Select Foreground - Click and drag to select area");
        selectionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        selectionFrame.add(new JScrollPane(selectionPanel));
        selectionFrame.setSize(
            Math.min(image.getWidth() + 50, 800),
            Math.min(image.getHeight() + 100, 600)
        );
        selectionFrame.setLocationRelativeTo(parentFrame);

        // Add confirmation button
        javax.swing.JButton processButton = new javax.swing.JButton("Process Background Removal");
        processButton.addActionListener(processEvent -> {
            Rectangle selectionRect = selectionPanel.getSelectionRectangle();

            if (selectionRect != null && selectionRect.width > 10 && selectionRect.height > 10) {
                // Map selection area back to original image size
                int originalX = (int) (selectionRect.x * scaleX);
                int originalY = (int) (selectionRect.y * scaleY);
                int originalWidth = (int) (selectionRect.width * scaleX);
                int originalHeight = (int) (selectionRect.height * scaleY);

                Rectangle originalRect = new Rectangle(originalX, originalY, originalWidth, originalHeight);

                // Notify background removal started
                if (callback != null) {
                    callback.onBackgroundRemovalStarted();
                }

                statusLabel.setText("Removing background...");

                // Background worker for background removal
                SwingWorker<Photo, Void> worker = new SwingWorker<Photo, Void>() {
                    @Override
                    protected Photo doInBackground() throws Exception {
                        // Create rect for OpenCV
                        Rect rect = new Rect(
                            originalRect.x, 
                            originalRect.y, 
                            originalRect.width, 
                            originalRect.height
                        );

                        // Create background remover
                        BackgroundRemover remover = new BackgroundRemover(backgroundSettings);
                        
                        // Set selection rectangle
                        remover.setSelectionRect(
                            rect.x(), 
                            rect.y(),
                            rect.x() + rect.width(),
                            rect.y() + rect.height()
                        );

                        // Create a copy of the current photo to avoid modifying the original
                        Photo processedPhoto = new Photo(currentPhoto);
                        
                        // Process the photo
                        remover.process(processedPhoto);
                        return processedPhoto;
                    }

                    @Override
                    protected void done() {
                        try {
                            Photo processedPhoto = get();
                            statusLabel.setText("Background removed successfully");
                            
                            // Notify callback
                            if (callback != null) {
                                callback.onBackgroundRemovalCompleted(processedPhoto);
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            String errorMessage = "Error removing background: " + ex.getMessage();
                            
                            JOptionPane.showMessageDialog(
                                    parentFrame,
                                    errorMessage,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            
                            statusLabel.setText("Failed to remove background");
                            
                            // Notify callback of failure
                            if (callback != null) {
                                callback.onBackgroundRemovalFailed(errorMessage);
                            }
                        }
                    }
                };

                // Execute the background removal worker
                worker.execute();
                selectionFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(
                        selectionFrame,
                        "Please select a valid area (minimum 10x10 pixels).",
                        "Invalid Selection",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        // Add cancel button
        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
        cancelButton.addActionListener(cancelEvent -> selectionFrame.dispose());

        // Create button panel
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        buttonPanel.add(processButton);
        buttonPanel.add(cancelButton);

        // Add button panel to frame
        selectionFrame.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        // Make selection frame visible
        selectionFrame.setVisible(true);
    }

    // Setter for current photo to allow updating the photo for background removal
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
}