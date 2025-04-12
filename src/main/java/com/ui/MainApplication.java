package com.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
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

import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.SwingWorker;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.Toolkit;
import com.cloud.CloudServiceFactory;
import com.cloud.CloudStorageService;
import com.cloud.impl.GoogleDriveCloudStorageService;

import java.util.List;
import java.util.ArrayList;
import com.editor.BatchProcessor;
import com.gui.BatchProcessingPanel;

public class MainApplication {

    // UI components
    private JFrame mainFrame;
    private JFrame cropFrame;
    private JPanel mainPanel;
    private JLabel previewLabel;
    private JScrollPane controlPanel;
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
    private Frame layoutSourceFrame;

    // New UI components for background image feature
    private JRadioButton solidColorRadio;
    private JRadioButton imageBackgroundRadio;
    private JButton backgroundImageButton;
    private JLabel backgroundImagePreview;
    private String selectedBackgroundImagePath;

    // Application state
    private Photo currentPhoto;
    private BackgroundSettings backgroundSettings;
    private ExportSettings exportSettings;
    private Rectangle cropRect;
    private boolean isCropping = false;

    // History manager for undo/redo operations
    private PhotoHistory photoHistory;

    // Add to the class-level fields:
    private JFrame batchProcessingFrame;
    private BatchProcessingPanel batchPanel;
    private List<File> batchInputFiles = new ArrayList<>();

    // Cloud Integration
    private List<CloudStorageService> cloudServices;
    private JComboBox<String> cloudServiceComboBox;
    private JLabel authStatusLabel;
    private JButton authButton;

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
        backgroundSettings = new BackgroundSettings(); // BackgroundSettings stores the chosen color/image
        exportSettings = new ExportSettings();

        // Initialize photo history
        photoHistory = new PhotoHistory();

        // Set up the main window
        mainFrame = new JFrame(Constants.APP_NAME);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(
                config.getIntProperty("ui.window.width", Constants.DEFAULT_WINDOW_WIDTH),
                config.getIntProperty("ui.window.height", Constants.DEFAULT_WINDOW_HEIGHT));
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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

        // Add new item for batch processing
        JMenuItem batchItem = new JMenuItem("Batch Process");
        batchItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK));
        batchItem.addActionListener(this::handleBatchProcess);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(this::handleSaveImage);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> mainFrame.dispose());

        fileMenu.add(openItem);
        fileMenu.add(batchItem);
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
        processBackgroundItem.addActionListener(this::handleProcessBackgroundAndResize);

        JMenuItem resetItem = new JMenuItem("Reset to Original");
        resetItem.addActionListener(e -> handleReset());

        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        editMenu.addSeparator();
        editMenu.add(cropItem);
        editMenu.add(processBackgroundItem);
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
                TitledBorder.TOP));

        // Create a scrollable image preview
        previewLabel = new JLabel("No image loaded", JLabel.CENTER);
        previewLabel.setPreferredSize(new Dimension(
                Constants.PREVIEW_PANEL_WIDTH,
                Constants.PREVIEW_PANEL_WIDTH));

        JScrollPane scrollPane = new JScrollPane(previewLabel);
        scrollPane.setPreferredSize(new Dimension(
                Constants.PREVIEW_PANEL_WIDTH,
                Constants.PREVIEW_PANEL_WIDTH));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Controls",
                TitledBorder.LEFT,
                TitledBorder.TOP));

        // Optional: restrict max width, let height grow naturally
        panel.setMaximumSize(new Dimension(Constants.CONTROL_PANEL_WIDTH, Integer.MAX_VALUE));

        // --- File Controls ---
        JPanel filePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        filePanel.setBorder(BorderFactory.createTitledBorder("File"));

        JButton openButton = new JButton("Open Image");
        openButton.addActionListener(this::handleOpenImage);
        filePanel.add(openButton);

        JButton batchButton = new JButton("Batch Process");
        batchButton.addActionListener(this::handleBatchProcess);
        filePanel.add(batchButton);

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

        // --- Step 1: Crop ---
        JPanel cropPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        cropPanel.setBorder(BorderFactory.createTitledBorder("Step 1: Crop (Optional)"));

        JButton cropButton = new JButton("Crop Image");
        cropButton.setToolTipText("Select a portion of the image to keep");
        cropButton.addActionListener(this::handleCrop);
        cropPanel.add(cropButton);

        panel.add(cropPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- Step 2: Configure Settings ---
        JPanel processSettingsPanel = new JPanel();
        processSettingsPanel.setLayout(new BoxLayout(processSettingsPanel, BoxLayout.Y_AXIS));
        processSettingsPanel.setBorder(BorderFactory.createTitledBorder("Step 2: Configure Settings"));

        JPanel dimensionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        widthField = new JTextField(5);
        heightField = new JTextField(5);

        ApplicationConfig config = ApplicationConfig.getInstance();
        widthField.setText(String.valueOf(Constants.PASSPORT_PHOTO_WIDTH_MM));
        heightField.setText(String.valueOf(Constants.PASSPORT_PHOTO_HEIGHT_MM));

        dimensionPanel.add(new JLabel("Width (mm):"));
        dimensionPanel.add(widthField);
        dimensionPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        dimensionPanel.add(new JLabel("Height (mm):"));
        dimensionPanel.add(heightField);
        processSettingsPanel.add(dimensionPanel);

        JPanel backgroundPanel = createBackgroundPanel();
        processSettingsPanel.add(backgroundPanel);

        panel.add(processSettingsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- Step 3: Process ---
        JPanel processPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        processPanel.setBorder(BorderFactory.createTitledBorder("Step 3: Process Image"));

        JButton processButton = new JButton("Process Background & Resize");
        processButton.setFont(processButton.getFont().deriveFont(Font.BOLD));
        processButton
                .setToolTipText("Remove background, apply selected color/image, and resize to specified dimensions");
        processButton.addActionListener(this::handleProcessBackgroundAndResize);
        processPanel.add(processButton);

        JButton resetButton = new JButton("Reset to Original");
        resetButton.setToolTipText("Discard all changes and restore the original image");
        resetButton.addActionListener(e -> handleReset());
        processPanel.add(resetButton);

        panel.add(processPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- Step 4: Export ---
        JPanel exportPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        exportPanel.setBorder(BorderFactory.createTitledBorder("Step 4: Export"));

        JComboBox<String> formatComboBox = new JComboBox<>(Constants.SUPPORTED_OUTPUT_FORMATS);
        formatComboBox.setSelectedItem(exportSettings.getFormat().getExtension());
        formatComboBox.addActionListener(e -> {
            String format = (String) formatComboBox.getSelectedItem();
            switch (format) {
                case "jpg":
                case "jpeg":
                    exportSettings.setFormat(ExportSettings.ImageFormat.JPEG);
                    break;
                case "png":
                    exportSettings.setFormat(ExportSettings.ImageFormat.PNG);
                    break;
                case "bmp":
                    exportSettings.setFormat(ExportSettings.ImageFormat.BMP);
                    break;
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

        // Add Cloud Export panel
        JPanel cloudExportPanel = createCloudExportPanel();
        panel.add(cloudExportPanel);

        // Layout sheet options
        String[] layoutOptions = { "1x1 (1 copy)", "2x2 (4 copies)", "4x6 (8 copies)", "3x4 (6 copies)" };
        JComboBox<String> layoutDropdown = new JComboBox<>(layoutOptions);

        JButton generateSheetButton = new JButton("Generate ID Photo Sheet");
        generateSheetButton.addActionListener(e -> {
            if (currentPhoto == null) {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "No image loaded.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedLayout = (String) layoutDropdown.getSelectedItem();
            generateLayoutSheet(selectedLayout);
        });

        exportPanel.add(new JLabel("Layout:"));
        exportPanel.add(layoutDropdown);
        exportPanel.add(generateSheetButton);

        JButton saveButton = new JButton("Save Image");
        saveButton.addActionListener(this::handleSaveImage);
        exportPanel.add(saveButton);

        // Wrap everything in a scroll pane
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling

        return scrollPane;
    }

    // New method for creating the background panel with options for both color and
    // image
    private JPanel createBackgroundPanel() {
        JPanel bgPanel = new JPanel();
        bgPanel.setLayout(new BoxLayout(bgPanel, BoxLayout.Y_AXIS));
        bgPanel.setBorder(BorderFactory.createTitledBorder("Background Settings"));

        // Background type selection
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup bgTypeGroup = new ButtonGroup();

        solidColorRadio = new JRadioButton("Solid Color");
        imageBackgroundRadio = new JRadioButton("Image Background");

        // Set initial selection based on current settings
        if (backgroundSettings.getType() == BackgroundSettings.BackgroundType.SOLID_COLOR) {
            solidColorRadio.setSelected(true);
        } else {
            imageBackgroundRadio.setSelected(true);
        }

        // Add listeners to update the background type
        solidColorRadio.addActionListener(e -> {
            backgroundSettings.setType(BackgroundSettings.BackgroundType.SOLID_COLOR);
            updateBackgroundControlsState();
        });

        imageBackgroundRadio.addActionListener(e -> {
            backgroundSettings.setType(BackgroundSettings.BackgroundType.CUSTOM_IMAGE);
            updateBackgroundControlsState();
        });

        bgTypeGroup.add(solidColorRadio);
        bgTypeGroup.add(imageBackgroundRadio);

        typePanel.add(solidColorRadio);
        typePanel.add(imageBackgroundRadio);
        bgPanel.add(typePanel);

        // Solid color controls
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bgColorButton = new JButton("Choose Background Color");
        bgColorButton.addActionListener(this::handleChooseBackgroundColor);

        colorPreviewLabel = new JLabel("  ");
        colorPreviewLabel.setOpaque(true);
        colorPreviewLabel.setBackground(backgroundSettings.getBackgroundColor());
        colorPreviewLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorPreviewLabel.setPreferredSize(new Dimension(20, 20));

        colorPanel.add(bgColorButton);
        colorPanel.add(colorPreviewLabel);
        bgPanel.add(colorPanel);

        // Background image controls
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backgroundImageButton = new JButton("Select Background Image");
        backgroundImageButton.addActionListener(this::handleChooseBackgroundImage);

        // Preview thumbnail
        backgroundImagePreview = new JLabel("No image selected");
        backgroundImagePreview.setPreferredSize(new Dimension(100, 60));
        backgroundImagePreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // If image path is already set, try to load and show a preview
        if (backgroundSettings.getBackgroundImagePath() != null &&
                !backgroundSettings.getBackgroundImagePath().isEmpty()) {
            try {
                File imgFile = new File(backgroundSettings.getBackgroundImagePath());
                if (imgFile.exists()) {
                    selectedBackgroundImagePath = backgroundSettings.getBackgroundImagePath();
                    updateBackgroundImagePreview();
                }
            } catch (Exception ex) {
                // Ignore errors and keep default "No image selected" text
            }
        }

        imagePanel.add(backgroundImageButton);
        imagePanel.add(backgroundImagePreview);
        bgPanel.add(imagePanel);

        // Update which controls are enabled based on current type
        updateBackgroundControlsState();

        return bgPanel;
    }

    // Update control states based on background type
    private void updateBackgroundControlsState() {
        boolean isColorType = backgroundSettings.getType() == BackgroundSettings.BackgroundType.SOLID_COLOR;

        // Update UI controls based on selected type
        colorPreviewLabel.setEnabled(isColorType);
        backgroundImageButton.setEnabled(!isColorType);
        backgroundImagePreview.setEnabled(!isColorType);
    }

    // Add this method to handle choosing a background image
    private void handleChooseBackgroundImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Background Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Image files", Constants.SUPPORTED_INPUT_FORMATS));

        if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (FileUtils.isImageFile(selectedFile)) {
                selectedBackgroundImagePath = selectedFile.getAbsolutePath();
                backgroundSettings.setBackgroundImagePath(selectedBackgroundImagePath);
                updateBackgroundImagePreview();
                statusLabel.setText("Background image selected: " + selectedFile.getName());
            } else {
                JOptionPane.showMessageDialog(mainFrame,
                        "Selected file is not a supported image format.",
                        "Invalid File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Add this method to update the background image preview
    private void updateBackgroundImagePreview() {
        if (selectedBackgroundImagePath != null && !selectedBackgroundImagePath.isEmpty()) {
            try {
                // Load the image
                File imgFile = new File(selectedBackgroundImagePath);
                if (imgFile.exists()) {
                    // Load and scale the image for preview
                    BufferedImage originalImg = javax.imageio.ImageIO.read(imgFile);

                    // Create scaled version
                    int maxPreviewWidth = 80;
                    int maxPreviewHeight = 50;
                    double aspectRatio = (double) originalImg.getWidth() / originalImg.getHeight();

                    int previewWidth = maxPreviewWidth;
                    int previewHeight = (int) (previewWidth / aspectRatio);

                    if (previewHeight > maxPreviewHeight) {
                        previewHeight = maxPreviewHeight;
                        previewWidth = (int) (previewHeight * aspectRatio);
                    }

                    // Create a scaled preview
                    Image scaledImg = originalImg.getScaledInstance(
                            previewWidth, previewHeight, Image.SCALE_SMOOTH);
                    backgroundImagePreview.setIcon(new ImageIcon(scaledImg));
                    backgroundImagePreview.setText("");

                    // Update settings
                    backgroundSettings.setBackgroundImagePath(selectedBackgroundImagePath);
                }
            } catch (Exception ex) {
                backgroundImagePreview.setIcon(null);
                backgroundImagePreview.setText("Error loading image");
                System.err.println("Error creating preview: " + ex.getMessage());
            }
        } else {
            backgroundImagePreview.setIcon(null);
            backgroundImagePreview.setText("No image selected");
        }
        backgroundImagePreview.revalidate();
        backgroundImagePreview.repaint();
    }

    // --- Event Handlers ---

    private void handleUndo(ActionEvent e) {
        if (!photoHistory.canUndo()) {
            return;
        }
        Photo previousState = photoHistory.undo(currentPhoto);
        if (previousState != null) {
            currentPhoto = previousState;
            updatePreview();
            updateUndoRedoButtons();
            statusLabel.setText("Undo completed");
        }
    }

    private void handleRedo(ActionEvent e) {
        if (!photoHistory.canRedo()) {
            return;
        }
        Photo nextState = photoHistory.redo(currentPhoto);
        if (nextState != null) {
            currentPhoto = nextState;
            updatePreview();
            updateUndoRedoButtons();
            layoutSourceFrame = currentPhoto.getProcessedFrame().clone();

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
            JOptionPane.showMessageDialog(mainFrame, "Selected file is not a supported image format.", "Invalid File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        statusLabel.setText("Loading image...");
        new SwingWorker<Photo, Void>() {
            @Override
            protected Photo doInBackground() throws Exception {
                return FileUtils.loadPhoto(file);
            }

            @Override
            protected void done() {
                try {
                    currentPhoto = get();
                    photoHistory.clear(); // Clear history for new image
                    updateUndoRedoButtons();
                    updatePreview();
                    layoutSourceFrame = currentPhoto.getProcessedFrame().clone();

                    statusLabel.setText("Image loaded: " + file.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "Error loading image: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
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
        fileChooser.setSelectedFile(
                new File(exportSettings.getFileNamePrefix() + "output." + exportSettings.getFormat().getExtension()));
        if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            String outputPath = fileChooser.getSelectedFile().getAbsolutePath();
            exportSettings.setOutputDirectory(fileChooser.getSelectedFile().getParent());
            statusLabel.setText("Saving image...");
            new SwingWorker<File, Void>() {
                @Override
                protected File doInBackground() throws Exception {
                    ImageExporter exporter = new ImageExporter(exportSettings);
                    return exporter.export(currentPhoto, outputPath);
                }

                @Override
                protected void done() {
                    try {
                        statusLabel.setText("Image saved to: " + get().getAbsolutePath());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainFrame, "Error saving image: " + ex.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
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

        // Save state before cropping for undo capability
        photoHistory.saveState(currentPhoto);

        // Get the current image
        BufferedImage image = currentPhoto.getProcessedBufferedImage();

        // Calculate dimensions for the preview panel
        int maxWidth = 800, maxHeight = 600;
        double aspectRatio = (double) image.getWidth() / image.getHeight();
        int panelWidth = maxWidth;
        int panelHeight = (int) (maxWidth / aspectRatio);

        if (panelHeight > maxHeight) {
            panelHeight = maxHeight;
            panelWidth = (int) (maxHeight * aspectRatio);
        }

        // Create a scaled version for the UI
        Image scaledImage = image.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        // Calculate scale factors to map UI coordinates back to original image
        final double scaleX = (double) image.getWidth() / resizedImage.getWidth();
        final double scaleY = (double) image.getHeight() / resizedImage.getHeight();

        // Create the crop selection panel
        CropPanel cropPanel = new CropPanel(resizedImage);

        // Create a frame for the crop UI
        JFrame cropFrame = new JFrame("Crop Image - Click and drag to select area");
        cropFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        cropFrame.setLayout(new BorderLayout());

        // Add buttons
        JPanel buttonPanel = new JPanel();
        JButton confirmButton = new JButton("Confirm Crop");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // Assemble the frame
        cropFrame.add(new JScrollPane(cropPanel), BorderLayout.CENTER);
        cropFrame.add(buttonPanel, BorderLayout.SOUTH);
        cropFrame.pack();
        cropFrame.setLocationRelativeTo(mainFrame);
        cropFrame.setVisible(true);

        // Handle the confirm button
        confirmButton.addActionListener(confirmEvent -> {
            Rectangle cropRect = cropPanel.getSelectionRectangle();
            if (cropRect != null && cropRect.width > 10 && cropRect.height > 10) {
                // Map selection coordinates back to original image
                int originalX = (int) (cropRect.x * scaleX);
                int originalY = (int) (cropRect.y * scaleY);
                int originalWidth = (int) (cropRect.width * scaleX);
                int originalHeight = (int) (cropRect.height * scaleY);

                // Create a rectangle in original image coordinates
                Rect originalRect = new Rect(originalX, originalY, originalWidth, originalHeight);

                statusLabel.setText("Cropping image...");

                // Process the crop in a background thread
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            // Create an ImageResizer specifically for cropping
                            ImageResizer resizer = new ImageResizer(
                                    originalRect, // Crop rectangle
                                    originalWidth, // Target is the cropped size
                                    originalHeight,
                                    false // Exact crop (don't maintain aspect ratio)
                            );

                            // Apply the crop
                            resizer.process(currentPhoto);
                            return null;
                        } catch (Exception ex) {
                            throw new Exception("Cropping failed: " + ex.getMessage(), ex);
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            get(); // Check for exceptions
                            updatePreview();
                            updateUndoRedoButtons();
                            layoutSourceFrame = currentPhoto.getProcessedFrame().clone();

                            statusLabel.setText("Image cropped successfully");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    "Error cropping image: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            statusLabel.setText("Failed to crop image");
                        }
                    }
                }.execute();

                cropFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(
                        cropFrame,
                        "Please select a valid crop area.",
                        "Invalid Selection",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        // Handle the cancel button
        cancelButton.addActionListener(cancelEvent -> cropFrame.dispose());
    }

    // Updated handler for the Process Background and Resize button
    // The issue is in MainApplication.java's handleProcessBackgroundAndResize
    // method
    // Here's the corrected version:

    // Updated handleProcessBackgroundAndResize method in MainApplication.java
    // This creates a two-step processing approach for background images

    // Updated handleProcessBackgroundAndResize method in MainApplication.java
    // With fixes for final variables referenced from inner classes

    private void handleProcessBackgroundAndResize(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- Get User Inputs (in millimeters) ---
        int targetWidthMM, targetHeightMM;
        try {
            targetWidthMM = Integer.parseInt(widthField.getText().trim());
            targetHeightMM = Integer.parseInt(heightField.getText().trim());
            if (targetWidthMM <= 0 || targetHeightMM <= 0) {
                throw new NumberFormatException("Dimensions must be positive.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Invalid dimensions entered. Please enter positive numbers.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Convert MM to pixels using the PIXELS_PER_MM constant
        final int targetWidth = targetWidthMM * Constants.PIXELS_PER_MM;
        final int targetHeight = targetHeightMM * Constants.PIXELS_PER_MM;

        // Validate background image if that type is selected
        final boolean useBackgroundImage = backgroundSettings
                .getType() == BackgroundSettings.BackgroundType.CUSTOM_IMAGE;
        final String originalBackgroundImagePath;
        final BackgroundSettings.BackgroundType originalBackgroundType = backgroundSettings.getType();

        // Initialize to null, then conditionally set if using background image
        if (useBackgroundImage) {
            String bgImagePath = backgroundSettings.getBackgroundImagePath();
            if (bgImagePath == null || bgImagePath.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Please select a background image.",
                        "Missing Background Image",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if the file exists
            File bgImageFile = new File(bgImagePath);
            if (!bgImageFile.exists() || !bgImageFile.isFile()) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Background image file not found: " + bgImagePath,
                        "File Not Found",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Store the original background image path
            originalBackgroundImagePath = bgImagePath;
        } else {
            // Must initialize even when not used
            originalBackgroundImagePath = null;
        }

        // --- Process in Background ---
        photoHistory.saveState(currentPhoto); // Save state before combined action
        statusLabel.setText("Processing background and resizing...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // STEP 1: If using background image, first apply with solid color background
                    if (useBackgroundImage) {
                        // Temporarily change settings to use a solid color
                        backgroundSettings.setType(BackgroundSettings.BackgroundType.SOLID_COLOR);

                        // Process with solid white background first
                        BackgroundRemover solidColorRemover = new BackgroundRemover(
                                backgroundSettings,
                                "model/modnet.onnx");
                        solidColorRemover.process(currentPhoto);

                        // Scale the result first
                        ImageResizer resizer = new ImageResizer(
                                targetWidth,
                                targetHeight,
                                true, // maintain aspect ratio
                                backgroundSettings.getBackgroundColor());
                        resizer.process(currentPhoto);

                        // Now restore the settings to use the background image
                        backgroundSettings.setType(BackgroundSettings.BackgroundType.CUSTOM_IMAGE);
                        backgroundSettings.setBackgroundImagePath(originalBackgroundImagePath);

                        // Apply the background image as a second pass
                        BackgroundRemover imageRemover = new BackgroundRemover(
                                backgroundSettings,
                                "model/modnet.onnx");
                        imageRemover.process(currentPhoto);
                    } else {
                        // Normal single-pass processing for solid color background
                        BackgroundRemover remover = new BackgroundRemover(
                                backgroundSettings,
                                "model/modnet.onnx");
                        remover.process(currentPhoto);

                        // Scale the result
                        ImageResizer resizer = new ImageResizer(
                                targetWidth,
                                targetHeight,
                                true, // maintain aspect ratio
                                backgroundSettings.getBackgroundColor());
                        resizer.process(currentPhoto);
                    }

                    return null;
                } catch (Exception ex) {
                    // Make sure to restore original settings even if processing fails
                    if (useBackgroundImage) {
                        backgroundSettings.setType(originalBackgroundType);
                        backgroundSettings.setBackgroundImagePath(originalBackgroundImagePath);
                    }
                    throw new Exception("Processing failed: " + ex.getMessage(), ex);
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    updatePreview();
                    updateUndoRedoButtons();
                    layoutSourceFrame = currentPhoto.getProcessedFrame().clone();
                    statusLabel.setText("Background processed and image resized successfully");

                    // Make sure UI reflects correct settings if we've changed them
                    if (useBackgroundImage) {
                        solidColorRadio.setSelected(false);
                        imageBackgroundRadio.setSelected(true);
                        updateBackgroundControlsState();
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Error during processing: " + ex.getCause().getMessage(),
                            "Processing Error",
                            JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Processing failed");
                    updatePreview();
                    updateUndoRedoButtons();
                }
            }
        };

        worker.execute();
    }

    private void generateLayoutSheet(String layoutOption) {
        statusLabel.setText("Generating layout sheet...");

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                try {

                    OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                    Java2DFrameConverter java2DConverter = new Java2DFrameConverter();

                    Mat bgrMat = matConverter.convert(layoutSourceFrame);
                    Mat rgbMat = new Mat();

                    org.bytedeco.opencv.global.opencv_imgproc.cvtColor(
                            bgrMat, rgbMat, org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB);

                    Frame rgbFrame = matConverter.convert(rgbMat);
                    BufferedImage base = java2DConverter.convert(rgbFrame);

                    int idWidth = 300, idHeight = 400;
                    int cols = 2, rows = 2;

                    switch (layoutOption) {
                        case "1x1 (1 copy)":
                            cols = 1;
                            rows = 1;
                            break;
                        case "4x6 (8 copies)":
                            cols = 4;
                            rows = 2;
                            break;
                        case "3x4 (6 copies)":
                            cols = 3;
                            rows = 2;
                            break;
                        default:
                            // Default is 2x2 (4 copies)
                            break;
                    }

                    int spacing = 20;
                    int sheetWidth = cols * idWidth + (cols + 1) * spacing;
                    int sheetHeight = rows * idHeight + (rows + 1) * spacing;

                    BufferedImage sheet = new BufferedImage(sheetWidth, sheetHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = sheet.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, sheetWidth, sheetHeight);

                    double imgAspect = (double) base.getWidth() / base.getHeight();
                    double cellAspect = (double) idWidth / idHeight;

                    int drawWidth, drawHeight;
                    if (imgAspect < cellAspect) {
                        // Image is narrower — fit height, crop width
                        drawHeight = idHeight;
                        drawWidth = (int) (idHeight * imgAspect);
                    } else {
                        // Image is wider — fit width, crop height
                        drawWidth = idWidth;
                        drawHeight = (int) (idWidth / imgAspect);
                    }

                    Image scaled = base.getScaledInstance(drawWidth, drawHeight, Image.SCALE_SMOOTH);

                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            int x = spacing + c * (idWidth + spacing);
                            int y = spacing + r * (idHeight + spacing);

                            int drawX = x + (idWidth - drawWidth) / 2;
                            int drawY = y + (idHeight - drawHeight) / 2;

                            g.drawImage(scaled, drawX, drawY, null);
                        }
                    }

                    g.dispose();
                    return sheet;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    BufferedImage layout = get();
                    if (layout != null) {
                        Java2DFrameConverter converter = new Java2DFrameConverter();
                        Frame sheetFrame = converter.convert(layout);
                        currentPhoto.setProcessedFrame(sheetFrame);

                        updatePreview();
                        updateUndoRedoButtons();
                        statusLabel.setText("Layout sheet generated.");
                    } else {
                        statusLabel.setText("Failed to generate sheet.");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error creating layout.");
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    // Modified to only choose and store color
    private void handleChooseBackgroundColor(ActionEvent e) {
        Color initialColor = backgroundSettings.getBackgroundColor();
        Color selectedColor = JColorChooser.showDialog(mainFrame, "Choose Background Color", initialColor);

        if (selectedColor != null) {
            backgroundSettings.setBackgroundColor(selectedColor); // Update the settings object
            colorPreviewLabel.setBackground(selectedColor); // Update the visual preview
            statusLabel.setText("Background color selected. Press 'Process Background' to apply.");
        }
    }

    private void handleReset() {
        if (currentPhoto == null) {
            return;
        }
        photoHistory.saveState(currentPhoto);
        currentPhoto.resetToOriginal();
        updatePreview();
        updateUndoRedoButtons();
        layoutSourceFrame = currentPhoto.getProcessedFrame().clone();

        statusLabel.setText("Image reset to original");
    }

    private void updatePreview() {
        if (currentPhoto != null) {
            BufferedImage image = currentPhoto.getProcessedBufferedImage();
            int maxPreviewSize = Constants.PREVIEW_PANEL_WIDTH; // Use constant
            int width = image.getWidth();
            int height = image.getHeight();
            Image scaledImage = image; // Default to original if small enough
            if (width > maxPreviewSize || height > maxPreviewSize) {
                double scale = Math.min((double) maxPreviewSize / width, (double) maxPreviewSize / height);
                scaledImage = image.getScaledInstance((int) (width * scale), (int) (height * scale),
                        Image.SCALE_SMOOTH);
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
                        int w = Math.abs(e.getX() - startPoint.x);
                        int h = Math.abs(e.getY() - startPoint.y);
                        selectionRect = new Rectangle(x, y, w, h);
                        repaint();
                    }
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
            if (selectionRect != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(0, 120, 215, 128));
                g2d.fillRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
            }
        }

        public Rectangle getSelectionRectangle() {
            return selectionRect;
        }
    }

    // Batch processing handler with background image support
    private void handleBatchProcess(ActionEvent e) {
        // Create a file chooser that allows multiple file selection
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Photos for Batch Processing");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", Constants.SUPPORTED_INPUT_FORMATS));

        if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();

            // Validate we have at least one file
            if (selectedFiles.length == 0) {
                JOptionPane.showMessageDialog(mainFrame,
                        "No files selected for batch processing.",
                        "No Files Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Store the selected files
            batchInputFiles = new ArrayList<>();
            for (File file : selectedFiles) {
                if (FileUtils.isImageFile(file)) {
                    batchInputFiles.add(file);
                } else {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Skipping unsupported file: " + file.getName(),
                            "Unsupported File", JOptionPane.WARNING_MESSAGE);
                }
            }

            // If no valid files, exit early
            if (batchInputFiles.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame,
                        "No valid image files were selected for batch processing.",
                        "No Valid Files", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create and show the batch processing options dialog
            showBatchOptionsDialog();
        }
    }

    private void showBatchOptionsDialog() {
        // Create a dialog for batch processing options
        JDialog optionsDialog = new JDialog(mainFrame, "Batch Processing Options", true);
        optionsDialog.setLayout(new BorderLayout(10, 10));
        optionsDialog.setSize(450, 450); // Made larger to accommodate background options
        optionsDialog.setLocationRelativeTo(mainFrame);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add file info
        JPanel fileInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileInfoPanel.add(new JLabel("Processing " + batchInputFiles.size() + " files"));
        optionsPanel.add(fileInfoPanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add dimension inputs (reuse code from main panel)
        JPanel dimensionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField batchWidthField = new JTextField(5);
        JTextField batchHeightField = new JTextField(5);

        // Pre-fill with current or standard dimensions
        batchWidthField.setText(widthField.getText());
        batchHeightField.setText(heightField.getText());

        dimensionPanel.add(new JLabel("Width (mm):"));
        dimensionPanel.add(batchWidthField);
        dimensionPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        dimensionPanel.add(new JLabel("Height (mm):"));
        dimensionPanel.add(batchHeightField);
        optionsPanel.add(dimensionPanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add background type selection
        JPanel bgTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup bgTypeGroup = new ButtonGroup();

        JRadioButton batchSolidColorRadio = new JRadioButton("Solid Color");
        JRadioButton batchImageRadio = new JRadioButton("Image Background");

        // Set initial state based on current settings
        if (backgroundSettings.getType() == BackgroundSettings.BackgroundType.SOLID_COLOR) {
            batchSolidColorRadio.setSelected(true);
        } else {
            batchImageRadio.setSelected(true);
        }

        bgTypeGroup.add(batchSolidColorRadio);
        bgTypeGroup.add(batchImageRadio);

        bgTypePanel.add(new JLabel("Background Type:"));
        bgTypePanel.add(batchSolidColorRadio);
        bgTypePanel.add(batchImageRadio);
        optionsPanel.add(bgTypePanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add background color chooser
        JPanel bgColorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel colorPreview = new JLabel("  ");
        colorPreview.setOpaque(true);
        colorPreview.setBackground(backgroundSettings.getBackgroundColor());
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorPreview.setPreferredSize(new Dimension(20, 20));

        JButton bgColorButton = new JButton("Choose Background Color");
        bgColorButton.addActionListener(evt -> {
            Color initialColor = colorPreview.getBackground();
            Color selectedColor = JColorChooser.showDialog(optionsDialog, "Choose Background Color", initialColor);

            if (selectedColor != null) {
                colorPreview.setBackground(selectedColor);
            }
        });

        bgColorPanel.add(bgColorButton);
        bgColorPanel.add(colorPreview);
        optionsPanel.add(bgColorPanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add background image selector
        JPanel bgImagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField bgImagePathField = new JTextField(20);
        JButton browseImageButton = new JButton("Browse...");

        // Set initial path if available
        if (backgroundSettings.getType() == BackgroundSettings.BackgroundType.CUSTOM_IMAGE &&
                backgroundSettings.getBackgroundImagePath() != null) {
            bgImagePathField.setText(backgroundSettings.getBackgroundImagePath());
        }

        browseImageButton.addActionListener(evt -> {
            JFileChooser imageChooser = new JFileChooser();
            imageChooser.setFileFilter(new FileNameExtensionFilter(
                    "Image files", Constants.SUPPORTED_INPUT_FORMATS));

            if (imageChooser.showOpenDialog(optionsDialog) == JFileChooser.APPROVE_OPTION) {
                bgImagePathField.setText(imageChooser.getSelectedFile().getAbsolutePath());
            }
        });

        bgImagePanel.add(new JLabel("Background Image:"));
        bgImagePanel.add(bgImagePathField);
        bgImagePanel.add(browseImageButton);
        optionsPanel.add(bgImagePanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add output format selection
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> formatComboBox = new JComboBox<>(Constants.SUPPORTED_OUTPUT_FORMATS);
        formatComboBox.setSelectedItem(exportSettings.getFormat().getExtension());

        formatPanel.add(new JLabel("Output Format:"));
        formatPanel.add(formatComboBox);
        optionsPanel.add(formatPanel);
        optionsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add output directory selection
        JPanel outputDirPanel = new JPanel(new BorderLayout(5, 0));
        JTextField outputDirField = new JTextField(exportSettings.getOutputDirectory());
        JButton browseButton = new JButton("Browse...");

        browseButton.addActionListener(evt -> {
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dirChooser.setDialogTitle("Select Output Directory");

            if (dirChooser.showDialog(optionsDialog, "Select") == JFileChooser.APPROVE_OPTION) {
                outputDirField.setText(dirChooser.getSelectedFile().getAbsolutePath());
            }
        });

        outputDirPanel.add(new JLabel("Output Directory:"), BorderLayout.NORTH);
        outputDirPanel.add(outputDirField, BorderLayout.CENTER);
        outputDirPanel.add(browseButton, BorderLayout.EAST);
        optionsPanel.add(outputDirPanel);

        // Update UI based on selection
        ActionListener backgroundTypeListener = evt -> {
            boolean isColorType = batchSolidColorRadio.isSelected();

            // Enable/disable color controls
            bgColorPanel.setEnabled(isColorType);
            colorPreview.setEnabled(isColorType);
            bgColorButton.setEnabled(isColorType);

            // Enable/disable image controls
            bgImagePanel.setEnabled(!isColorType);
            bgImagePathField.setEnabled(!isColorType);
            browseImageButton.setEnabled(!isColorType);
        };

        batchSolidColorRadio.addActionListener(backgroundTypeListener);
        batchImageRadio.addActionListener(backgroundTypeListener);

        // Set initial state
        backgroundTypeListener.actionPerformed(null);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton startButton = new JButton("Start Batch Processing");

        cancelButton.addActionListener(evt -> optionsDialog.dispose());

        startButton.addActionListener(evt -> {
            // Validate inputs
            try {
                int width = Integer.parseInt(batchWidthField.getText().trim());
                int height = Integer.parseInt(batchHeightField.getText().trim());

                if (width <= 0 || height <= 0) {
                    throw new NumberFormatException("Dimensions must be positive");
                }

                String outputDir = outputDirField.getText().trim();
                if (outputDir.isEmpty()) {
                    JOptionPane.showMessageDialog(optionsDialog,
                            "Please select an output directory.",
                            "Missing Output Directory", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update settings based on user selections
                BackgroundSettings batchBgSettings = new BackgroundSettings();

                if (batchSolidColorRadio.isSelected()) {
                    batchBgSettings.setType(BackgroundSettings.BackgroundType.SOLID_COLOR);
                    batchBgSettings.setBackgroundColor(colorPreview.getBackground());
                } else {
                    batchBgSettings.setType(BackgroundSettings.BackgroundType.CUSTOM_IMAGE);
                    String bgImagePath = bgImagePathField.getText().trim();

                    if (bgImagePath.isEmpty()) {
                        JOptionPane.showMessageDialog(optionsDialog,
                                "Please select a background image.",
                                "Missing Background Image", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Validate image file exists
                    File bgImageFile = new File(bgImagePath);
                    if (!bgImageFile.exists() || !bgImageFile.isFile()) {
                        JOptionPane.showMessageDialog(optionsDialog,
                                "Background image file not found.",
                                "Invalid File", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    batchBgSettings.setBackgroundImagePath(bgImagePath);
                }

                // Configure export settings
                ExportSettings batchExportSettings = new ExportSettings();
                String format = (String) formatComboBox.getSelectedItem();
                if ("jpg".equals(format) || "jpeg".equals(format)) {
                    batchExportSettings.setFormat(ExportSettings.ImageFormat.JPEG);
                } else if ("png".equals(format)) {
                    batchExportSettings.setFormat(ExportSettings.ImageFormat.PNG);
                } else if ("bmp".equals(format)) {
                    batchExportSettings.setFormat(ExportSettings.ImageFormat.BMP);
                }
                batchExportSettings.setOutputDirectory(outputDir);

                // Close the options dialog
                optionsDialog.dispose();

                // Start batch processing
                startBatchProcessing(
                        batchInputFiles,
                        outputDir,
                        batchBgSettings,
                        batchExportSettings,
                        width,
                        height,
                        true // maintain aspect ratio
                );

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(optionsDialog,
                        "Please enter valid numeric dimensions.",
                        "Invalid Dimensions", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(startButton);

        // Assemble the dialog
        optionsDialog.add(new JScrollPane(optionsPanel), BorderLayout.CENTER);
        optionsDialog.add(buttonPanel, BorderLayout.SOUTH);
        optionsDialog.setVisible(true);
    }

    private void startBatchProcessing(
            List<File> inputFiles,
            String outputDir,
            BackgroundSettings bgSettings,
            ExportSettings exportSettings,
            int widthMM,
            int heightMM,
            boolean maintainAspectRatio) {

        // Create the batch processing panel and frame
        if (batchPanel == null) {
            batchPanel = new BatchProcessingPanel();
        } else {
            batchPanel.clearResults();
        }

        if (batchProcessingFrame == null) {
            batchProcessingFrame = new JFrame("Batch Processing");
            batchProcessingFrame.setSize(600, 400);
            batchProcessingFrame.setLocationRelativeTo(mainFrame);
            batchProcessingFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }

        batchPanel.setCloseAction(() -> batchProcessingFrame.setVisible(false));
        batchProcessingFrame.setContentPane(batchPanel);
        batchProcessingFrame.setVisible(true);

        // Start the batch processor
        BatchProcessor processor = new BatchProcessor(
                inputFiles,
                outputDir,
                bgSettings,
                exportSettings,
                widthMM,
                heightMM,
                maintainAspectRatio);

        // Set up progress callback
        processor.onProgress(progressPercent -> {
            SwingUtilities.invokeLater(() -> {
                batchPanel.setProgress(progressPercent);
            });
        });

        // Set up completion callback
        processor.onComplete(outputFiles -> {
            SwingUtilities.invokeLater(() -> {
                batchPanel.setResults(inputFiles, outputFiles);
                batchPanel.setProgress(100);

                // Show a notification
                JOptionPane.showMessageDialog(
                        batchProcessingFrame,
                        "Batch processing complete.\n" +
                                "Successfully processed " + outputFiles.size() + " out of " + inputFiles.size()
                                + " files.",
                        "Processing Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        });

        // Set up error callback
        processor.onError(errorMessage -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error: " + errorMessage);
            });
        });

        // Start processing
        processor.process();
    }

    private JPanel createCloudExportPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Cloud Export"));

        // Load available cloud services
        cloudServices = CloudServiceFactory.getInstance().getAvailableServices();

        // Create service dropdown
        JPanel servicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cloudServiceComboBox = new JComboBox<>();

        for (CloudStorageService service : cloudServices) {
            cloudServiceComboBox.addItem(service.getServiceName());
        }

        servicePanel.add(new JLabel("Cloud Service:"));
        servicePanel.add(cloudServiceComboBox);
        panel.add(servicePanel);

        // Add authentication status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authStatusLabel = new JLabel("Not authenticated");
        authButton = new JButton("Authenticate");

        // Update auth status when service is selected
        cloudServiceComboBox.addActionListener(e -> {
            updateCloudServiceAuthStatus();
        });

        // Handle authentication
        authButton.addActionListener(e -> {
            int selectedIndex = cloudServiceComboBox.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < cloudServices.size()) {
                CloudStorageService selectedService = cloudServices.get(selectedIndex);

                if (selectedService instanceof GoogleDriveCloudStorageService) {
                    showGoogleDriveAuthDialog((GoogleDriveCloudStorageService) selectedService);
                } else {
                    // Generic authentication approach
                    if (selectedService.authenticate()) {
                        updateCloudServiceAuthStatus();
                    }
                }
            }
        });

        statusPanel.add(authStatusLabel);
        statusPanel.add(authButton);
        panel.add(statusPanel);

        // Add export button
        JButton exportButton = new JButton("Export to Cloud");
        exportButton.addActionListener(this::handleCloudExport);
        panel.add(exportButton);

        return panel;
    }

    /**
     * Update the authentication status display
     */
    private void updateCloudServiceAuthStatus() {
        int selectedIndex = cloudServiceComboBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < cloudServices.size()) {
            CloudStorageService selectedService = cloudServices.get(selectedIndex);
            boolean isAuth = selectedService.isAuthenticated();

            authStatusLabel.setText(isAuth ? "Authenticated" : "Not authenticated");
            authButton.setEnabled(!isAuth);
        }
    }

    /**
     * Show authentication dialog for Google Drive
     */
    private void showGoogleDriveAuthDialog(GoogleDriveCloudStorageService driveService) {
        JDialog authDialog = new JDialog(mainFrame, "Google Drive Authentication", true);
        authDialog.setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Information label
        JLabel infoLabel = new JLabel("<html><body width='400px'>" +
                "To use Google Drive, you need to provide a client secrets JSON file.<br><br>" +
                "1. Go to the Google Cloud Console (console.cloud.google.com)<br>" +
                "2. Create a project and enable the Google Drive API<br>" +
                "3. Create OAuth 2.0 credentials (Desktop application type)<br>" +
                "4. Download the client secrets JSON file<br><br>" +
                "Select your client secrets file below:</body></html>");
        panel.add(infoLabel);

        // Add spacing
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // File selection area
        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        JTextField filePathField = new JTextField();
        filePathField.setEditable(false);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Google API Client Secrets");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));

            if (fileChooser.showOpenDialog(authDialog) == JFileChooser.APPROVE_OPTION) {
                filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        filePanel.add(new JLabel("Client Secrets JSON:"), BorderLayout.NORTH);
        filePanel.add(filePathField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);
        panel.add(filePanel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton authenticateButton = new JButton("Authenticate");

        cancelButton.addActionListener(e -> authDialog.dispose());

        authenticateButton.addActionListener(e -> {
            String filePath = filePathField.getText();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(
                        authDialog,
                        "Please select a client secrets file.",
                        "Missing File",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Show a warning about browser opening
            JOptionPane.showMessageDialog(
                    authDialog,
                    "Your browser will open for Google authentication.\n" +
                            "Please complete the authentication process in your browser.",
                    "Browser Authentication",
                    JOptionPane.INFORMATION_MESSAGE);

            // Close the dialog
            authDialog.dispose();

            // Run authentication in background thread to not freeze UI
            SwingWorker<Boolean, Void> authWorker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return driveService.setCredentialsFile(filePath);
                }

                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    "Successfully authenticated with Google Drive!",
                                    "Authentication Successful",
                                    JOptionPane.INFORMATION_MESSAGE);

                            // Update UI components to reflect authenticated state
                            updateCloudServiceAuthStatus();
                        } else {
                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    "Failed to authenticate with Google Drive.",
                                    "Authentication Failed",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "Error during authentication: " + ex.getMessage(),
                                "Authentication Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            authWorker.execute();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(authenticateButton);

        authDialog.add(panel, BorderLayout.CENTER);
        authDialog.add(buttonPanel, BorderLayout.SOUTH);
        authDialog.pack();
        authDialog.setLocationRelativeTo(mainFrame);
        authDialog.setVisible(true);
    }

    /**
     * Handle the export to cloud button click
     */
    private void handleCloudExport(ActionEvent e) {
        if (currentPhoto == null) {
            JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int selectedIndex = cloudServiceComboBox.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= cloudServices.size()) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a cloud service.", "No Service Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        CloudStorageService selectedService = cloudServices.get(selectedIndex);
        if (!selectedService.isAuthenticated()) {
            JOptionPane.showMessageDialog(mainFrame, "Authentication required.", "Not Authenticated",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get filename
        String suggestedName = exportSettings.getFileNamePrefix() + System.currentTimeMillis() + "."
                + exportSettings.getFormat().getExtension();
        String fileName = JOptionPane.showInputDialog(mainFrame, "Enter cloud file name:", suggestedName);

        if (fileName == null || fileName.trim().isEmpty()) {
            return; // User cancelled
        }

        // Show progress indicator
        statusLabel.setText("Uploading to " + selectedService.getServiceName() + "...");

        try {
            ImageExporter exporter = new ImageExporter(exportSettings);
            exporter.exportToCloud(currentPhoto, fileName, selectedService)
                    .thenAccept(url -> {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Upload complete. URL: " + url);

                            // Show success with copy link option
                            int option = JOptionPane.showOptionDialog(
                                    mainFrame,
                                    "File uploaded successfully!\nURL: " + url,
                                    "Upload Complete",
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null,
                                    new Object[] { "OK", "Copy Link" },
                                    "OK");

                            if (option == 1) {
                                // Copy URL to clipboard
                                StringSelection selection = new StringSelection(url);
                                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                clipboard.setContents(selection, null);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Upload failed: " + ex.getMessage());
                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    "Failed to upload file: " + ex.getMessage(),
                                    "Upload Error",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                        return null;
                    });
        } catch (Exception ex) {
            statusLabel.setText("Error preparing upload: " + ex.getMessage());
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Error preparing upload: " + ex.getMessage(),
                    "Upload Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}