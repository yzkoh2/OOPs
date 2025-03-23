package com.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Rect;

import com.config.ApplicationConfig;
import com.editor.BackgroundRemover;
import com.editor.ImageExporter;
import com.editor.ImageResizer;
import com.entities.BackgroundSettings;
import com.entities.ExportSettings;
import com.entities.Photo;
import com.ui.handlers.BackgroundRemovalHandler;
import com.ui.handlers.ImageCropHandler;
import com.ui.handlers.ImageOpenHandler;
import com.ui.handlers.ImageResizeHandler;
import com.ui.handlers.ImageSaveHandler;
import com.util.Constants;
import com.util.FileUtils;
import com.util.ImageUtils;

public class MainApplication {
    // UI Components
    private JFrame mainFrame;
    private JPanel mainPanel;
    private JLabel previewLabel;
    private JPanel controlPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;

    // Application State
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private ExportSettings exportSettings;

    
    // Main entry point
    public static void main(String[] args) {
        // Set system look and feel
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Architecture: " + System.getProperty("os.arch"));
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            new MainApplication().initialize();
        });
    }

    // Initialize the application
    private void initialize() {
        // Load configuration
        ApplicationConfig config = ApplicationConfig.getInstance();

        // Initialize settings
        backgroundSettings = new BackgroundSettings();
        exportSettings = new ExportSettings();

        // Set up the main window
        mainFrame = new JFrame(Constants.APP_NAME);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(
                config.getIntProperty("ui.window.width", Constants.DEFAULT_WINDOW_WIDTH),
                config.getIntProperty("ui.window.height", Constants.DEFAULT_WINDOW_HEIGHT)
        );
        mainFrame.setLocationRelativeTo(null);

        // Add window close listener to save configuration
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                config.saveConfig();
            }
        });

        // Create main panel with border layout
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create preview panel
        JPanel previewPanel = createPreviewPanel();
        mainPanel.add(previewPanel, BorderLayout.CENTER);

        // Create control panel
        controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.EAST);

        // Create status panel
        statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        mainFrame.setContentPane(mainPanel);
        mainFrame.setVisible(true);
    }

    // Create preview panel
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Preview",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        // Create a scrollable image preview
        previewLabel = new JLabel("No image loaded", JLabel.CENTER);
        previewLabel.setPreferredSize(new Dimension(
                Constants.PREVIEW_PANEL_WIDTH,
                Constants.PREVIEW_PANEL_WIDTH
        ));

        JScrollPane scrollPane = new JScrollPane(previewLabel);
        scrollPane.setPreferredSize(new Dimension(
                Constants.PREVIEW_PANEL_WIDTH,
                Constants.PREVIEW_PANEL_WIDTH
        ));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // Create control panel with various action buttons
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Controls",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        panel.setPreferredSize(new Dimension(Constants.CONTROL_PANEL_WIDTH, -1));

        // Add file controls
        JPanel filePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        filePanel.setBorder(BorderFactory.createTitledBorder("File"));

        JButton openButton = new JButton("Open Image");
        openButton.addActionListener(this::handleOpenImage);
        filePanel.add(openButton);

        JButton saveButton = new JButton("Save Image");
        saveButton.addActionListener(this::handleSaveImage);
        filePanel.add(saveButton);

        panel.add(filePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add edit controls
        JPanel editPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        editPanel.setBorder(BorderFactory.createTitledBorder("Edit"));

        JButton cropButton = new JButton("Crop");
        cropButton.addActionListener(this::handleCrop);
        editPanel.add(cropButton);

        JButton removeBackgroundButton = new JButton("Remove Background");
        removeBackgroundButton.addActionListener(this::handleRemoveBackground);
        editPanel.add(removeBackgroundButton);

        JButton resizeButton = new JButton("Resize to ID Format");
        resizeButton.addActionListener(this::handleResize);
        editPanel.add(resizeButton);

        panel.add(editPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add background settings
        JPanel bgPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        bgPanel.setBorder(BorderFactory.createTitledBorder("Background Settings"));

        JButton bgColorButton = new JButton("Choose Background Color");
        bgColorButton.addActionListener(this::handleChooseBackgroundColor);
        bgPanel.add(bgColorButton);

        panel.add(bgPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add export settings
        JPanel exportPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        exportPanel.setBorder(BorderFactory.createTitledBorder("Export Settings"));

        JComboBox<String> formatComboBox = new JComboBox<>(Constants.SUPPORTED_OUTPUT_FORMATS);
        formatComboBox.setSelectedItem(exportSettings.getFormat().getExtension());
        formatComboBox.addActionListener(e -> {
            String format = (String) formatComboBox.getSelectedItem();
            if ("jpg".equals(format) || "jpeg".equals(format)) {
                exportSettings.setFormat(ExportSettings.ImageFormat.JPEG);
            } else if ("png".equals(format)) {
                exportSettings.setFormat(ExportSettings.ImageFormat.PNG);
            } else if ("bmp".equals(format)) {
                exportSettings.setFormat(ExportSettings.ImageFormat.BMP);
            }
        });

        JPanel formatPanel = new JPanel(new BorderLayout());
        formatPanel.add(new JLabel("Format:"), BorderLayout.WEST);
        formatPanel.add(formatComboBox, BorderLayout.CENTER);
        exportPanel.add(formatPanel);

        JCheckBox multipleCheckBox = new JCheckBox("Generate multiple copies", exportSettings.isGenerateMultiples());
        multipleCheckBox.addActionListener(e -> {
            exportSettings.setGenerateMultiples(multipleCheckBox.isSelected());
        });
        exportPanel.add(multipleCheckBox);

        panel.add(exportPanel);

        return panel;
    }
    

    // Update preview with current photo
    private void updatePreview() {
        if (currentPhoto != null) {
            BufferedImage image = currentPhoto.getProcessedBufferedImage();

            // Scale the image for preview if too large
            int maxPreviewSize = Constants.PREVIEW_PANEL_WIDTH;
            int width = image.getWidth();
            int height = image.getHeight();

            if (width > maxPreviewSize || height > maxPreviewSize) {
                double scale = Math.min(
                        (double) maxPreviewSize / width,
                        (double) maxPreviewSize / height
                );

                int scaledWidth = (int) (width * scale);
                int scaledHeight = (int) (height * scale);

                Image scaledImage = image.getScaledInstance(
                        scaledWidth, scaledHeight, Image.SCALE_SMOOTH
                );

                ImageIcon icon = new ImageIcon(scaledImage);
                previewLabel.setIcon(icon);
                previewLabel.setText("");
            } else {
                previewLabel.setIcon(new ImageIcon(image));
                previewLabel.setText("");
            }
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("No image loaded");
        }
    }

    // Placeholder methods for event handlers (to be implemented in next iterations)
    private void handleOpenImage(ActionEvent e) {
        ImageOpenHandler openHandler = new ImageOpenHandler(
            mainFrame, 
            statusLabel, 
            new ImageOpenHandler.ImageOpenCallback() {
                @Override
                public void onImageLoaded(Photo photo) {
                    // Update the current photo
                    currentPhoto = photo;
                    
                    // Update the preview
                    updatePreview();
                }

                @Override
                public void onImageLoadFailed(String errorMessage) {
                    // Show error message to user
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        errorMessage,
                        "Image Load Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    
                    // Clear the current photo
                    currentPhoto = null;
                    updatePreview();
                }
            }
        );

        // Delegate image opening to the handler
        openHandler.handleOpenImage(e);
    }

    // Updated method to handle image saving
    private void handleSaveImage(ActionEvent e) {
        // Check if an image is loaded
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ImageSaveHandler saveHandler = new ImageSaveHandler(
            mainFrame, 
            statusLabel, 
            currentPhoto, 
            exportSettings,
            new ImageSaveHandler.ImageSaveCallback() {
                @Override
                public void onImageSaveStarted() {
                    // Optional: Disable save button or show loading indicator
                }

                @Override
                public void onImageSaved(File savedFile) {
                    // Optional: Refresh UI or show success message
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        "Image saved successfully: " + savedFile.getAbsolutePath(),
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }

                @Override
                public void onImageSaveFailed(String errorMessage) {
                    // Error handling is already done in the handler
                }
            }
        );

        // Delegate image saving to the handler
        saveHandler.handleSaveImage(e);
    }

    // Updated method to handle background removal
    private void handleRemoveBackground(ActionEvent e) {
        // Check if an image is loaded
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        BackgroundRemovalHandler bgRemovalHandler = new BackgroundRemovalHandler(
            mainFrame,
            statusLabel,
            currentPhoto,
            backgroundSettings,
            new BackgroundRemovalHandler.BackgroundRemovalCallback() {
                @Override
                public void onBackgroundRemovalStarted() {
                    // Optional: Show loading state
                }
                @Override
                public void onBackgroundRemovalCompleted(Photo processedPhoto) {
                    // Update current photo and preview
                    currentPhoto = processedPhoto;
                    updatePreview();
                }
                @Override
                public void onBackgroundRemovalFailed(String errorMessage) {
                    // Error handling is already done in the handler
                }
                @Override
                public void onRefinementProgress(int attempts, boolean isSuccessful) {
                    // Optional: Add progress tracking or user feedback
                    if (!isSuccessful) {
                        JOptionPane.showMessageDialog(
                            mainFrame,
                            "Refinement attempt " + attempts + " was unsuccessful.",
                            "Refinement Warning",
                            JOptionPane.WARNING_MESSAGE
                        );
                    }
                }
            }
        );

        // Delegate background removal to the handler
        bgRemovalHandler.handleRemoveBackground(e);
    }

    // Updated method to handle image resizing
    private void handleResize(ActionEvent e) {
        // Check if an image is loaded
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ImageResizeHandler resizeHandler = new ImageResizeHandler(
            mainFrame, 
            statusLabel, 
            currentPhoto,
            new ImageResizeHandler.ImageResizeCallback() {
                @Override
                public void onResizeStarted() {
                    // Optional: Show loading state
                }

                @Override
                public void onResizeCompleted(Photo resizedPhoto) {
                    // Update current photo and preview
                    currentPhoto = resizedPhoto;
                    updatePreview();
                }

                @Override
                public void onResizeFailed(String errorMessage) {
                    // Error handling is already done in the handler
                }
            }
        );

        // Delegate resizing to the handler
        resizeHandler.handleResize(e);
    }

    // Updated method to handle image cropping
    private void handleCrop(ActionEvent e) {
        // Check if an image is loaded
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ImageCropHandler cropHandler = new ImageCropHandler(
            mainFrame, 
            statusLabel, 
            currentPhoto,
            new ImageCropHandler.ImageCropCallback() {
                @Override
                public void onCropStarted() {
                    // Optional: Show loading state
                }

                @Override
                public void onCropCompleted(Photo croppedPhoto) {
                    // Update current photo and preview
                    currentPhoto = croppedPhoto;
                    updatePreview();
                }

                @Override
                public void onCropFailed(String errorMessage) {
                    // Error handling is already done in the handler
                }
            }
        );

        // Delegate cropping to the handler
        cropHandler.handleCrop(e);
    }

    // Updated method to handle background color selection
    private void handleChooseBackgroundColor(ActionEvent e) {
        Color initialColor = backgroundSettings.getBackgroundColor();
        Color selectedColor = JColorChooser.showDialog(
            mainFrame, 
            "Choose Background Color", 
            initialColor != null ? initialColor : Color.WHITE
        );

        if (selectedColor != null) {
            // Update background settings
            backgroundSettings.setBackgroundColor(selectedColor);
            
            // Optional: Provide visual feedback
            statusLabel.setText("Background color updated");
        }
    }
}