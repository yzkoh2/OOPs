// File: OOP/src/main/java/com/ui/MainApplication.java
package com.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout; // Added for color preview
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.*; // Consolidated imports
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.Rect;

import com.config.ApplicationConfig;
import com.editor.BackgroundRemover;
import com.editor.ImageExporter;
import com.editor.ImageResizer;
import com.editor.PhotoHistory;
import com.entities.BackgroundSettings;
import com.entities.ExportSettings;
import com.entities.Photo;
import com.util.Constants;
import com.util.FileUtils;


public class MainApplication {

    // UI components
    private JFrame mainFrame;
    private JFrame cropFrame;
    private JPanel mainPanel;
    private JLabel previewLabel;
    private JPanel controlPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JButton undoButton;
    private JButton redoButton;
    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;
    // New UI components for dimensions and color preview
    private JTextField widthField;
    private JTextField heightField;
    private JLabel colorPreviewLabel; // To show selected background color

    // Application state
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private ExportSettings exportSettings;
    private Rectangle cropRect;
    private boolean isCropping = false;

    // History manager for undo/redo operations
    private PhotoHistory photoHistory;

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
        backgroundSettings = new BackgroundSettings(); // BackgroundSettings stores the chosen color
        exportSettings = new ExportSettings();

        // Initialize photo history
        photoHistory = new PhotoHistory();

        // Set up the main window
        mainFrame = new JFrame(Constants.APP_NAME);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(
                config.getIntProperty("ui.window.width", Constants.DEFAULT_WINDOW_WIDTH),
                config.getIntProperty("ui.window.height", Constants.DEFAULT_WINDOW_HEIGHT)
        );
        mainFrame.setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = createMenuBar();
        mainFrame.setJMenuBar(menuBar);

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

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(this::handleOpenImage);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(this::handleSaveImage);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> mainFrame.dispose());

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");

        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoMenuItem.addActionListener(this::handleUndo);
        undoMenuItem.setEnabled(false);

        redoMenuItem = new JMenuItem("Redo");
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        redoMenuItem.addActionListener(this::handleRedo);
        redoMenuItem.setEnabled(false);

        JMenuItem cropItem = new JMenuItem("Crop");
        cropItem.addActionListener(this::handleCrop);

        // Renamed for clarity
        JMenuItem processBackgroundItem = new JMenuItem("Process Background & Resize");
        processBackgroundItem.addActionListener(this::handleProcessBackgroundAndResize); // Changed handler

        JMenuItem resetItem = new JMenuItem("Reset to Original");
        resetItem.addActionListener(e -> handleReset());

        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        editMenu.addSeparator();
        editMenu.add(cropItem);
        // editMenu.add(resizeItem); // Removed old resize item
        editMenu.add(processBackgroundItem); // Added combined item
        // editMenu.add(removeBackgroundItem); // Removed old background item
        editMenu.addSeparator();
        editMenu.add(resetItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        return menuBar;
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

        // --- File Controls ---
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

        // --- History Controls ---
        JPanel historyPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        historyPanel.setBorder(BorderFactory.createTitledBorder("History"));

        undoButton = new JButton("Undo");
        undoButton.setToolTipText("Undo the last action (Ctrl+Z)");
        undoButton.addActionListener(this::handleUndo);
        undoButton.setEnabled(false);
        historyPanel.add(undoButton);

        redoButton = new JButton("Redo");
        redoButton.setToolTipText("Redo the last undone action (Ctrl+Y)");
        redoButton.addActionListener(this::handleRedo);
        redoButton.setEnabled(false);
        historyPanel.add(redoButton);

        panel.add(historyPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- Edit Controls ---
        JPanel editPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        editPanel.setBorder(BorderFactory.createTitledBorder("Edit"));

        JButton cropButton = new JButton("Crop");
        cropButton.addActionListener(this::handleCrop);
        editPanel.add(cropButton);

        // Combined button
        JButton processBackgroundButton = new JButton("Process Background & Resize");
        processBackgroundButton.setToolTipText("Remove background, apply color, and resize using settings below");
        processBackgroundButton.addActionListener(this::handleProcessBackgroundAndResize); // Changed handler
        editPanel.add(processBackgroundButton);

        // Removed old individual buttons
        // JButton removeBackgroundButton = new JButton("Remove Background");
        // removeBackgroundButton.addActionListener(this::handleRemoveBackground);
        // editPanel.add(removeBackgroundButton);
        // JButton resizeButton = new JButton("Resize to ID Format");
        // resizeButton.addActionListener(this::handleResize);
        // editPanel.add(resizeButton);

        JButton resetButton = new JButton("Reset to Original");
        resetButton.addActionListener(e -> handleReset());
        editPanel.add(resetButton);

        panel.add(editPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- Background & Resize Settings ---
        JPanel processSettingsPanel = new JPanel();
        processSettingsPanel.setLayout(new BoxLayout(processSettingsPanel, BoxLayout.Y_AXIS));
        processSettingsPanel.setBorder(BorderFactory.createTitledBorder("Background & Resize Settings"));

        // Dimension Inputs
        JPanel dimensionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        widthField = new JTextField(5); // Field for width
        heightField = new JTextField(5); // Field for height
        // Pre-fill with default values or leave empty
        ApplicationConfig config = ApplicationConfig.getInstance();
        widthField.setText(String.valueOf(config.getIntProperty("photo.width.px", 600))); // Example default
        heightField.setText(String.valueOf(config.getIntProperty("photo.height.px", 600))); // Example default

        dimensionPanel.add(new JLabel("Width (px):"));
        dimensionPanel.add(widthField);
        dimensionPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        dimensionPanel.add(new JLabel("Height (px):"));
        dimensionPanel.add(heightField);
        processSettingsPanel.add(dimensionPanel);

        // Background Color Chooser and Preview
        JPanel bgColorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bgColorButton = new JButton("Choose Background Color");
        bgColorButton.addActionListener(this::handleChooseBackgroundColor); // Now only sets the color

        colorPreviewLabel = new JLabel("  "); // Small label for color preview
        colorPreviewLabel.setOpaque(true);
        colorPreviewLabel.setBackground(backgroundSettings.getBackgroundColor()); // Set initial color
        colorPreviewLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorPreviewLabel.setPreferredSize(new Dimension(20, 20)); // Size of the preview box

        bgColorPanel.add(bgColorButton);
        bgColorPanel.add(colorPreviewLabel); // Add preview next to button
        processSettingsPanel.add(bgColorPanel);

        panel.add(processSettingsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));


        // --- Export Settings ---
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

    // --- Event Handlers ---

    private void handleUndo(ActionEvent e) {
        if (!photoHistory.canUndo()) { return; }
        Photo previousState = photoHistory.undo(currentPhoto);
        if (previousState != null) {
            currentPhoto = previousState;
            updatePreview();
            updateUndoRedoButtons();
            statusLabel.setText("Undo completed");
        }
    }

    private void handleRedo(ActionEvent e) {
        if (!photoHistory.canRedo()) { return; }
        Photo nextState = photoHistory.redo(currentPhoto);
        if (nextState != null) {
            currentPhoto = nextState;
            updatePreview();
            updateUndoRedoButtons();
            statusLabel.setText("Redo completed");
        }
    }

    private void updateUndoRedoButtons() {
        boolean canUndo = photoHistory.canUndo();
        boolean canRedo = photoHistory.canRedo();
        undoButton.setEnabled(canUndo);
        redoButton.setEnabled(canRedo);
        undoMenuItem.setEnabled(canUndo);
        redoMenuItem.setEnabled(canRedo);
    }

    private void handleOpenImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", Constants.SUPPORTED_INPUT_FORMATS));
        if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            loadImage(fileChooser.getSelectedFile());
        }
    }

    private void loadImage(File file) {
        if (!FileUtils.isImageFile(file)) {
            JOptionPane.showMessageDialog(mainFrame, "Selected file is not a supported image format.", "Invalid File", JOptionPane.ERROR_MESSAGE);
            return;
        }
        statusLabel.setText("Loading image...");
        new SwingWorker<Photo, Void>() {
            @Override protected Photo doInBackground() throws Exception { return FileUtils.loadPhoto(file); }
            @Override protected void done() {
                try {
                    currentPhoto = get();
                    photoHistory.clear(); // Clear history for new image
                    updateUndoRedoButtons();
                    updatePreview();
                    statusLabel.setText("Image loaded: " + file.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Failed to load image");
                }
            }
        }.execute();
    }

    private void handleSaveImage(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");
        fileChooser.setSelectedFile(new File(exportSettings.getFileNamePrefix() + "output." + exportSettings.getFormat().getExtension()));
        if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            String outputPath = fileChooser.getSelectedFile().getAbsolutePath();
            exportSettings.setOutputDirectory(fileChooser.getSelectedFile().getParent());
            statusLabel.setText("Saving image...");
            new SwingWorker<File, Void>() {
                @Override protected File doInBackground() throws Exception {
                    ImageExporter exporter = new ImageExporter(exportSettings);
                    return exporter.export(currentPhoto, outputPath);
                }
                @Override protected void done() {
                    try { statusLabel.setText("Image saved to: " + get().getAbsolutePath()); }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainFrame, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Failed to save image");
                    }
                }
            }.execute();
        }
    }

    private void handleCrop(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        photoHistory.saveState(currentPhoto); // Save state before action

        BufferedImage image = currentPhoto.getProcessedBufferedImage();
        int maxWidth = 800, maxHeight = 600;
        double aspectRatio = (double) image.getWidth() / image.getHeight();
        int panelWidth = maxWidth;
        int panelHeight = (int) (maxWidth / aspectRatio);
        if (panelHeight > maxHeight) {
            panelHeight = maxHeight; panelWidth = (int) (maxHeight * aspectRatio);
        }
        Image scaledImage = image.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics(); g2d.drawImage(scaledImage, 0, 0, null); g2d.dispose();

        final double scaleX = (double) image.getWidth() / resizedImage.getWidth();
        final double scaleY = (double) image.getHeight() / resizedImage.getHeight();

        CropPanel cropPanel = new CropPanel(resizedImage);
        JFrame cropFrame = new JFrame("Crop Image - Click and drag to select area");
        cropFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        cropFrame.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JButton confirmButton = new JButton("Confirm Crop");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(confirmButton); buttonPanel.add(cancelButton);
        cropFrame.add(new JScrollPane(cropPanel), BorderLayout.CENTER);
        cropFrame.add(buttonPanel, BorderLayout.SOUTH);
        cropFrame.pack(); // Use pack for better sizing
        cropFrame.setLocationRelativeTo(mainFrame);
        cropFrame.setVisible(true);

        confirmButton.addActionListener(confirmEvent -> {
            Rectangle cropRect = cropPanel.getSelectionRectangle();
            if (cropRect != null && cropRect.width > 10 && cropRect.height > 10) {
                int originalX = (int) (cropRect.x * scaleX); int originalY = (int) (cropRect.y * scaleY);
                int originalWidth = (int) (cropRect.width * scaleX); int originalHeight = (int) (cropRect.height * scaleY);
                Rectangle originalRect = new Rectangle(originalX, originalY, originalWidth, originalHeight);
                statusLabel.setText("Cropping image...");
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        // Cropping is essentially resizing with a source rectangle
                        ImageResizer resizer = new ImageResizer(
                            new Rect(originalRect.x, originalRect.y, originalRect.width, originalRect.height),
                            originalRect.width, // Target is the cropped size
                            originalRect.height,
                            false // Don't maintain aspect ratio for crop, use exact rect
                        );
                        resizer.process(currentPhoto); // Process the photo using ImageResizer's crop constructor
                        return null;
                    }
                    @Override protected void done() {
                        try { get(); updatePreview(); updateUndoRedoButtons(); statusLabel.setText("Image cropped successfully"); }
                        catch (Exception ex) {
                            JOptionPane.showMessageDialog(mainFrame, "Error cropping image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            statusLabel.setText("Failed to crop image");
                        }
                    }
                }.execute();
                cropFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(cropFrame, "Please select a valid crop area.", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        cancelButton.addActionListener(cancelEvent -> cropFrame.dispose());
    }

    // --- NEW Combined Handler for Background Removal and Resizing ---
    private void handleProcessBackgroundAndResize(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- Get User Inputs ---
        int targetWidth, targetHeight;
        try {
            targetWidth = Integer.parseInt(widthField.getText().trim());
            targetHeight = Integer.parseInt(heightField.getText().trim());
            if (targetWidth <= 0 || targetHeight <= 0) {
                throw new NumberFormatException("Dimensions must be positive.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(mainFrame, "Invalid dimensions entered. Please enter positive numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Color is already set in backgroundSettings by handleChooseBackgroundColor

        // --- Process in Background ---
        photoHistory.saveState(currentPhoto); // Save state before combined action
        statusLabel.setText("Processing background and resizing...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Step 1: Remove Background (using the selected color from backgroundSettings)
                // Ensure BackgroundRemover uses the model path correctly
                BackgroundRemover remover = new BackgroundRemover(backgroundSettings, "model/modnet.onnx"); // Pass settings
                remover.process(currentPhoto); // This now ONLY removes background + applies color

                // Step 2: Resize the result (using user dimensions)
                ImageResizer resizer = new ImageResizer(targetWidth, targetHeight, true); // Maintain aspect ratio is often desired
                resizer.process(currentPhoto); // Process the photo *after* background removal

                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions from either step
                    updatePreview();
                    updateUndoRedoButtons(); // Update history buttons
                    statusLabel.setText("Background processed and image resized");
                } catch (Exception ex) {
                    // Rollback on failure? Optional, depends on desired behaviour.
                    // For now, show error and potentially inconsistent state.
                    // Photo previousState = photoHistory.undo(currentPhoto); // Attempt to rollback
                    // if (previousState != null) currentPhoto = previousState;

                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Error during processing: " + ex.getMessage(),
                            "Processing Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Processing failed");
                    updatePreview(); // Show the potentially intermediate state
                    updateUndoRedoButtons();
                }
            }
        };

        worker.execute();
    }


    // --- Modified: Only chooses and stores color ---
    private void handleChooseBackgroundColor(ActionEvent e) {
        Color initialColor = backgroundSettings.getBackgroundColor();
        Color selectedColor = JColorChooser.showDialog(mainFrame, "Choose Background Color", initialColor);

        if (selectedColor != null) {
            backgroundSettings.setBackgroundColor(selectedColor); // Update the settings object
            colorPreviewLabel.setBackground(selectedColor); // Update the visual preview
            statusLabel.setText("Background color selected. Press 'Process Background' to apply.");
            // DO NOT trigger background removal here anymore.
            // if (currentPhoto != null) {
            //     handleRemoveBackground(e); // Removed this line
            // }
        }
    }

    private void handleReset() {
        if (currentPhoto == null) { return; }
        photoHistory.saveState(currentPhoto);
        currentPhoto.resetToOriginal();
        updatePreview();
        updateUndoRedoButtons();
        statusLabel.setText("Image reset to original");
    }

    private void updatePreview() {
        if (currentPhoto != null) {
            BufferedImage image = currentPhoto.getProcessedBufferedImage();
            int maxPreviewSize = Constants.PREVIEW_PANEL_WIDTH; // Use constant
            int width = image.getWidth(); int height = image.getHeight();
            Image scaledImage = image; // Default to original if small enough
            if (width > maxPreviewSize || height > maxPreviewSize) {
                double scale = Math.min((double) maxPreviewSize / width, (double) maxPreviewSize / height);
                scaledImage = image.getScaledInstance((int) (width * scale), (int) (height * scale), Image.SCALE_SMOOTH);
            }
            previewLabel.setIcon(new ImageIcon(scaledImage));
            previewLabel.setText(""); // Clear "No image" text
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("No image loaded");
        }
        previewLabel.revalidate();
        previewLabel.repaint();
    }

    // --- Inner Class: CropPanel ---
    private class CropPanel extends JPanel {
        private BufferedImage image;
        private Rectangle selectionRect;
        private Point startPoint;

        public CropPanel(BufferedImage image) {
            this.image = image;
            this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { startPoint = e.getPoint(); selectionRect = null; repaint(); }
                @Override public void mouseDragged(MouseEvent e) {
                    if (startPoint != null) {
                        int x = Math.min(startPoint.x, e.getX()); int y = Math.min(startPoint.y, e.getY());
                        int w = Math.abs(e.getX() - startPoint.x); int h = Math.abs(e.getY() - startPoint.y);
                        selectionRect = new Rectangle(x, y, w, h); repaint();
                    }
                }
            };
            addMouseListener(mouseAdapter); addMouseMotionListener(mouseAdapter);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
            if (selectionRect != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(0, 120, 215, 128)); g2d.fillRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
                g2d.setColor(Color.BLUE); g2d.setStroke(new BasicStroke(2)); g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
            }
        }
        public Rectangle getSelectionRectangle() { return selectionRect; }
    }

    // Removed unused SelectionPanel class if it existed
}