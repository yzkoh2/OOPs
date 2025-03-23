package com.ui.handlers;

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

import com.entities.Photo;
import com.ui.panels.ImageSelectionPanel;

public class GenericImageHandler {
    // Generic callback interface
    public interface ImageProcessCallback {
        void onProcessStarted();
        void onProcessCompleted(Photo processedPhoto);
        void onProcessFailed(String errorMessage);
    }

    // Interface for actual processing logic
    public interface ImageProcessor {
        void process(Photo photo, Rect selectionRect) throws Exception;
    }

    private JFrame parentFrame;
    private JLabel statusLabel;
    private Photo currentPhoto;
    private ImageProcessCallback callback;
    private ImageProcessor processor;
    private String processingTitle;
    private String processingMessage;
    private Class<? extends ImageSelectionPanel> selectionPanelClass;
    
    // Track temporary resources that need cleanup
    private BufferedImage previewImage = null;

    public GenericImageHandler(
            JFrame parentFrame, 
            JLabel statusLabel, 
            Photo currentPhoto,
            ImageProcessCallback callback,
            ImageProcessor processor,
            Class<? extends ImageSelectionPanel> selectionPanelClass,
            String processingTitle,
            String processingMessage
    ) {
        this.parentFrame = parentFrame;
        this.statusLabel = statusLabel;
        this.currentPhoto = currentPhoto;
        this.callback = callback;
        this.processor = processor;
        this.selectionPanelClass = selectionPanelClass;
        this.processingTitle = processingTitle;
        this.processingMessage = processingMessage;
    }

    public void handleProcess(ActionEvent e) {
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

        try {
            statusLabel.setText("Preparing image for processing...");
            
            // Get the image carefully
            BufferedImage image = null;
            try {
                image = currentPhoto.getProcessedBufferedImage();
                if (image == null) {
                    throw new NullPointerException("Failed to get processed image");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        parentFrame,
                        "Error preparing image: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                statusLabel.setText("Failed to prepare image");
                return;
            }
    
            // Calculate scaling for selection panel
            int maxPreviewSize = 800;
            int maxHeight = 600;
            
            int panelWidth = image.getWidth();
            int panelHeight = image.getHeight();
            if (panelWidth > maxPreviewSize || panelHeight > maxHeight) {
                double aspectRatio = (double) image.getWidth() / image.getHeight();
               
                if (image.getWidth() > maxPreviewSize) {
                    panelWidth = maxPreviewSize;
                    panelHeight = (int) (panelWidth / aspectRatio);
                }
               
                if (panelHeight > maxHeight) {
                    panelHeight = maxHeight;
                    panelWidth = (int) (panelHeight * aspectRatio);
                }
            }
    
            // Scale the image for preview - use memory-efficient approach
            try {
                // Create a small preview image
                Image scaledImage = image.getScaledInstance(panelWidth, panelHeight, Image.SCALE_FAST);
                previewImage = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = previewImage.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();
                
                // Help gc by nulling references we don't need anymore
                scaledImage.flush();
                scaledImage = null;
                
                // We don't need the full image anymore for UI
                image.flush();
                image = null;
                
                // Force GC after creating preview image
                System.gc();
            } catch (OutOfMemoryError oom) {
                cleanupTemporaryResources();
                JOptionPane.showMessageDialog(
                        parentFrame,
                        "Not enough memory to create image preview.",
                        "Memory Error",
                        JOptionPane.ERROR_MESSAGE
                );
                statusLabel.setText("Memory error: Operation cancelled");
                return;
            }
    
            // Calculate scaling factors
            final double scaleX = (double) currentPhoto.getWidth() / previewImage.getWidth();
            final double scaleY = (double) currentPhoto.getHeight() / previewImage.getHeight();
    
            // Create selection panel using reflection to support different panel types
            ImageSelectionPanel selectionPanel;
            try {
                selectionPanel = selectionPanelClass.getConstructor(BufferedImage.class)
                        .newInstance(previewImage);
            } catch (Exception ex) {
                cleanupTemporaryResources();
                JOptionPane.showMessageDialog(
                        parentFrame,
                        "Error creating selection panel: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
    
            // Create selection frame
            JFrame selectionFrame = new JFrame(processingTitle);
            selectionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            selectionFrame.add(new JScrollPane(selectionPanel));
            selectionFrame.setSize(
                Math.min(panelWidth + 50, 800),
                Math.min(panelHeight + 100, 600)
            );
            selectionFrame.setLocationRelativeTo(parentFrame);
            
            // Add window listener to clean up resources when frame is closed
            selectionFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    cleanupTemporaryResources();
                }
            });
    
            // Add confirmation button
            javax.swing.JButton processButton = new javax.swing.JButton("Process");
            processButton.addActionListener(processEvent -> {
                Rectangle selectionRect = selectionPanel.getSelectionRectangle();
    
                if (selectionRect != null && selectionRect.width > 10 && selectionRect.height > 10) {
                    // Map selection area back to original image size
                    int originalX = (int) (selectionRect.x * scaleX);
                    int originalY = (int) (selectionRect.y * scaleY);
                    int originalWidth = (int) (selectionRect.width * scaleX);
                    int originalHeight = (int) (selectionRect.height * scaleY);
    
                    Rectangle originalRect = new Rectangle(originalX, originalY, originalWidth, originalHeight);
    
                    // Notify process started
                    if (callback != null) {
                        callback.onProcessStarted();
                    }
    
                    statusLabel.setText(processingMessage);
                    
                    // Clean up temporary UI resources before heavy processing
                    cleanupTemporaryResources();
                    selectionFrame.dispose();
    
                    // Background worker for processing
                    SwingWorker<Photo, Void> worker = new SwingWorker<Photo, Void>() {
                        private Photo processedPhoto = null;
                        
                        @Override
                        protected Photo doInBackground() throws Exception {
                            try {
                                // Create rect for OpenCV
                                Rect rect = new Rect(
                                    originalRect.x, 
                                    originalRect.y, 
                                    originalRect.width, 
                                    originalRect.height
                                );
        
                                // Create a copy of the current photo to avoid modifying the original
                                processedPhoto = new Photo(currentPhoto);
                                
                                // Process the photo using provided processor
                                processor.process(processedPhoto, rect);
                                return processedPhoto;
                            } catch (Exception e) {
                                // Ensure cleanup happens if an exception occurs
                                cleanupWorkerResources();
                                throw e;
                            } catch (OutOfMemoryError oom) {
                                // Explicitly handle out of memory
                                cleanupWorkerResources();
                                System.gc();
                                throw new Exception("Out of memory during processing. Try using a smaller image or closing other applications.", oom);
                            } catch (Error err) {
                                // Handle other serious errors
                                cleanupWorkerResources();
                                System.gc();
                                throw new Exception("Critical error during processing: " + err.getMessage(), err);
                            }
                        }
    
                        @Override
                        protected void done() {
                            try {
                                Photo result = get();
                                statusLabel.setText("Process completed successfully");
                                
                                // Notify callback
                                if (callback != null) {
                                    callback.onProcessCompleted(result);
                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                String errorMessage = "Error processing image: " + ex.getMessage();
                                
                                JOptionPane.showMessageDialog(
                                        parentFrame,
                                        errorMessage,
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE
                                );
                                
                                statusLabel.setText("Process failed");
                                
                                // Notify callback of failure
                                if (callback != null) {
                                    callback.onProcessFailed(errorMessage);
                                }
                                
                                // Free memory in case of error
                                cleanupWorkerResources();
                            }
                        }
                        
                        private void cleanupWorkerResources() {
                            // Free the processedPhoto if it exists and process failed
                            if (processedPhoto != null) {
                                try {
                                    processedPhoto.freeResources();
                                    processedPhoto = null;
                                } catch (Exception e) {
                                    System.err.println("Error cleaning up worker resources: " + e.getMessage());
                                }
                            }
                            
                            // Request garbage collection
                            System.gc();
                        }
                    };
    
                    // Execute the processing worker
                    worker.execute();
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
            
        } catch (OutOfMemoryError oom) {
            // Handle out of memory errors during the UI creation
            cleanupTemporaryResources();
            System.gc(); // Request immediate garbage collection
            JOptionPane.showMessageDialog(
                parentFrame,
                "Not enough memory to process this image. Try closing other applications or using a smaller image.",
                "Out of Memory Error",
                JOptionPane.ERROR_MESSAGE
            );
            statusLabel.setText("Memory error: Operation cancelled");
        } catch (Exception ex) {
            // Handle other exceptions
            cleanupTemporaryResources();
            JOptionPane.showMessageDialog(
                parentFrame,
                "Error: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            statusLabel.setText("Error: Operation cancelled");
        }
    }
    
    private void cleanupTemporaryResources() {
        try {
            if (previewImage != null) {
                previewImage.flush();
                previewImage = null;
            }
            System.gc();
        } catch (Exception e) {
            System.err.println("Error cleaning temporary resources: " + e.getMessage());
        }
    }

    // Setter for current photo to allow updating the photo for processing
    public void setCurrentPhoto(Photo photo) {
        this.currentPhoto = photo;
    }
}