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

import org.bytedeco.opencv.opencv_core.Rect;

import com.config.ApplicationConfig;
import com.editor.BackgroundRemover;
import com.editor.ImageExporter;
import com.editor.ImageResizer;
import com.entities.BackgroundSettings;
import com.entities.ExportSettings;
import com.entities.Photo;
import com.util.Constants;
import com.util.FileUtils;

public class MainApplication {
    // UI components
    private JFrame mainFrame;
    private JPanel mainPanel;
    private JLabel previewLabel;
    private JPanel controlPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    
    // Application state
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private ExportSettings exportSettings;
    private Rectangle cropRect;
    private boolean isCropping = false;
    
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            new MainApplication().initialize();
        });
    }
    
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
    
    private void handleOpenImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image files", Constants.SUPPORTED_INPUT_FORMATS
        ));
        
        int result = fileChooser.showOpenDialog(mainFrame);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImage(selectedFile);
        }
    }
    
    private void loadImage(File file) {
        if (!FileUtils.isImageFile(file)) {
            JOptionPane.showMessageDialog(
                mainFrame,
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
                    currentPhoto = get();
                    updatePreview();
                    statusLabel.setText("Image loaded: " + file.getName());
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error loading image: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Failed to load image");
                }
            }
        };
        
        worker.execute();
    }
    
    private void handleSaveImage(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");
        fileChooser.setSelectedFile(new File(exportSettings.getFileNamePrefix() + "output." + 
                                             exportSettings.getFormat().getExtension()));
        
        int result = fileChooser.showSaveDialog(mainFrame);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            String outputPath = fileChooser.getSelectedFile().getAbsolutePath();
            exportSettings.setOutputDirectory(fileChooser.getSelectedFile().getParent());
            
            statusLabel.setText("Saving image...");
            
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
                    } catch (InterruptedException | ExecutionException ex) {
                        JOptionPane.showMessageDialog(
                            mainFrame,
                            "Error saving image: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        statusLabel.setText("Failed to save image");
                    }
                }
            };
            
            worker.execute();
        }
    }
    
private void handleCrop(ActionEvent e) {
    if (currentPhoto == null) {
        JOptionPane.showMessageDialog(
            mainFrame,
            "No image loaded.",
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
        return;
    }
    
    // Create a custom panel for interactive cropping
    BufferedImage image = currentPhoto.getProcessedBufferedImage();
    CropPanel cropPanel = new CropPanel(image);
    
    // Create a frame to hold the cropping panel
    JFrame cropFrame = new JFrame("Crop Image - Click and drag to select area");
    cropFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    cropFrame.setLayout(new BorderLayout());
    
    // Add control buttons
    JPanel buttonPanel = new JPanel();
    JButton confirmButton = new JButton("Confirm Crop");
    JButton cancelButton = new JButton("Cancel");
    
    buttonPanel.add(confirmButton);
    buttonPanel.add(cancelButton);
    
    // Add components to frame
    cropFrame.add(new JScrollPane(cropPanel), BorderLayout.CENTER);
    cropFrame.add(buttonPanel, BorderLayout.SOUTH);
    
    // Set frame size and make visible
    cropFrame.setSize(Math.min(image.getWidth() + 50, 800), 
                      Math.min(image.getHeight() + 100, 600));
    cropFrame.setLocationRelativeTo(mainFrame);
    cropFrame.setVisible(true);
    
    // Handle confirm button click
    confirmButton.addActionListener(confirmEvent -> {
        Rectangle cropRect = cropPanel.getSelectionRectangle();
        
        if (cropRect != null && cropRect.width > 10 && cropRect.height > 10) {
            statusLabel.setText("Cropping image...");
            
            // Use SwingWorker to process in background
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Convert Java Rectangle to OpenCV Rect
                    Rect rect = new Rect(
                        cropRect.x, 
                        cropRect.y, 
                        cropRect.width, 
                        cropRect.height
                    );
                    
                    // Create resizer with crop rectangle
                    ImageResizer resizer = new ImageResizer(
                        rect, 
                        rect.width(), 
                        rect.height(), 
                        true
                    );
                    
                    // Process the photo
                    resizer.process(currentPhoto);
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        updatePreview();
                        statusLabel.setText("Image cropped successfully");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            mainFrame,
                            "Error cropping image: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        statusLabel.setText("Failed to crop image");
                    }
                }
            };
            
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
    
    // Handle cancel button click
    cancelButton.addActionListener(cancelEvent -> cropFrame.dispose());
}

// Custom panel for interactive cropping
private class CropPanel extends JPanel {
    private BufferedImage image;
    private Rectangle selectionRect;
    private Point startPoint;

    public CropPanel(BufferedImage image) {
        this.image = image;
        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        
        // Add mouse listeners for interactive selection
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                selectionRect = null;
                repaint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint != null) {
                    int x = Math.min(startPoint.x, e.getX());
                    int y = Math.min(startPoint.y, e.getY());
                    int width = Math.abs(e.getX() - startPoint.x);
                    int height = Math.abs(e.getY() - startPoint.y);
                    
                    selectionRect = new Rectangle(x, y, width, height);
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                // Selection is complete when mouse is released
            }
        };
        
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw the image
        g.drawImage(image, 0, 0, null);
        
        // Draw selection rectangle if it exists
        if (selectionRect != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(0, 120, 215, 128));
            g2d.fillRect(selectionRect.x, selectionRect.y, 
                        selectionRect.width, selectionRect.height);
            
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(selectionRect.x, selectionRect.y, 
                        selectionRect.width, selectionRect.height);
        }
    }
    
    public Rectangle getSelectionRectangle() {
        return selectionRect;
    }
}

    
    private void handleRemoveBackground(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        statusLabel.setText("Removing background...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                BackgroundRemover remover = new BackgroundRemover(backgroundSettings);
                remover.process(currentPhoto);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    updatePreview();
                    statusLabel.setText("Background removed");
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error removing background: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Failed to remove background");
                }
            }
        };
        
        worker.execute();
    }
    
    private void handleResize(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "No image loaded.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        statusLabel.setText("Resizing image...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Get dimensions from config 
                ApplicationConfig config = ApplicationConfig.getInstance();
                int widthMm = config.getIntProperty("photo.width.mm", Constants.PASSPORT_PHOTO_WIDTH_MM);
                int heightMm = config.getIntProperty("photo.height.mm", Constants.PASSPORT_PHOTO_HEIGHT_MM);
                int dpi = config.getIntProperty("photo.dpi", 300);
                
                // Calculate dimensions in pixels
                int pixelsPerMm = dpi / 25; // 25.4mm per inch, simplified to 25
                int widthPx = widthMm * pixelsPerMm;
                int heightPx = heightMm * pixelsPerMm;
                
                ImageResizer resizer = new ImageResizer(widthPx, heightPx, true);
                resizer.process(currentPhoto);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    updatePreview();
                    statusLabel.setText("Image resized to ID photo dimensions");
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error resizing image: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Failed to resize image");
                }
            }
        };
        
        worker.execute();
    }
    
    private void handleChooseBackgroundColor(ActionEvent e) {
        Color initialColor = backgroundSettings.getBackgroundColor();
        Color selectedColor = JColorChooser.showDialog(
            mainFrame,
            "Choose Background Color",
            initialColor
        );
        
        if (selectedColor != null) {
            backgroundSettings.setBackgroundColor(selectedColor);
            
            // If we have an image and already removed background, apply the new color
            if (currentPhoto != null) {
                handleRemoveBackground(e);
            }
        }
    }
    
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
}
