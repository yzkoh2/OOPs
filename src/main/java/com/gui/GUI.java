package com.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import org.bytedeco.opencv.opencv_core.Mat;

import com.editor.BackgroundEditor;
import com.editor.ImageEditor;

public class GUI extends JFrame {
    private final BackgroundEditor backgroundEditor;
    private final ImageEditor imageEditor;
    private JPanel controlPanel;
    private JPanel toolsPanel;
    private ImagePanel imagePanel;
    private JLabel statusLabel;
    private Mat currentImage;
    private BufferedImage displayImage;
    private double zoomLevel = 1.0;
    private boolean isInCropMode = false;
    private final int defaultWidth = 500;
    private final int defaultHeight = 500;
    private JTextField widthField;
    private JTextField heightField;

    public GUI() {
        backgroundEditor = new BackgroundEditor();
        imageEditor = new ImageEditor();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("ID Photo Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        // Main layout
        setLayout(new BorderLayout());

        // Image display area
        imagePanel = new ImagePanel();
        imagePanel.setBackground(Color.LIGHT_GRAY);

        JScrollPane scrollPane = new JScrollPane(imagePanel);
        add(scrollPane, BorderLayout.CENTER);

        // Tools panel on the right (similar to JavaFX version)
        toolsPanel = createToolsPanel();
        add(toolsPanel, BorderLayout.EAST);

        // Control panel at the bottom
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        JButton uploadButton = new JButton("Upload Picture");
        uploadButton.addActionListener(this::handleUploadAction);
        controlPanel.add(uploadButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Status bar
        statusLabel = new JLabel("No image loaded");
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.NORTH);
    }

    private JPanel createToolsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(224, 224, 224));
        panel.setPreferredSize(new Dimension(250, 0));

        // File operations
        JPanel filePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        filePanel.setBackground(panel.getBackground());
        filePanel.setBorder(new TitledBorder("File Operations"));

        JButton saveButton = new JButton("Save Result");
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.addActionListener(this::handleSaveAction);

        filePanel.add(saveButton);

        // Tool selection
        JPanel toolSelectionPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        toolSelectionPanel.setBackground(panel.getBackground());
        toolSelectionPanel.setBorder(new TitledBorder("Tools"));

        ButtonGroup toolToggleGroup = new ButtonGroup();
        JRadioButton selectTool = new JRadioButton("Select");
        selectTool.setSelected(true);
        selectTool.addActionListener(e -> isInCropMode = false);

        JRadioButton cropTool = new JRadioButton("Crop");
        cropTool.addActionListener(e -> isInCropMode = true);

        toolToggleGroup.add(selectTool);
        toolToggleGroup.add(cropTool);

        toolSelectionPanel.add(selectTool);
        toolSelectionPanel.add(cropTool);

        // Resize options
        JPanel resizePanel = new JPanel();
        resizePanel.setLayout(new BoxLayout(resizePanel, BoxLayout.Y_AXIS));
        resizePanel.setBackground(panel.getBackground());
        resizePanel.setBorder(new TitledBorder("Resize Options"));

        JPanel widthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        widthPanel.setBackground(panel.getBackground());
        JLabel widthLabel = new JLabel("Width (px):");
        widthField = new JTextField(5);
        widthField.setText(String.valueOf(defaultWidth));
        widthPanel.add(widthLabel);
        widthPanel.add(widthField);

        JPanel heightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        heightPanel.setBackground(panel.getBackground());
        JLabel heightLabel = new JLabel("Height (px):");
        heightField = new JTextField(5);
        heightField.setText(String.valueOf(defaultHeight));
        heightPanel.add(heightLabel);
        heightPanel.add(heightField);

        JCheckBox preserveRatio = new JCheckBox("Preserve aspect ratio");
        preserveRatio.setBackground(panel.getBackground());
        preserveRatio.setSelected(true);

        JButton applyResizeButton = new JButton("Apply Resize");
        applyResizeButton.addActionListener(e -> {
            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                resizeImage(width, height, preserveRatio.isSelected());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter valid numeric dimensions.",
                        "Invalid dimensions",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        resizePanel.add(widthPanel);
        resizePanel.add(heightPanel);
        resizePanel.add(preserveRatio);
        resizePanel.add(Box.createVerticalStrut(5));
        resizePanel.add(applyResizeButton);

        // Crop controls
        JPanel cropPanel = new JPanel(new GridLayout(1, 1));
        cropPanel.setBackground(panel.getBackground());
        cropPanel.setBorder(new TitledBorder("Crop"));

        JButton applyCropButton = new JButton("Apply Crop");
        applyCropButton.addActionListener(e -> applyCrop());
        cropPanel.add(applyCropButton);

        // Zoom controls
        JPanel zoomPanel = new JPanel();
        zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.Y_AXIS));
        zoomPanel.setBackground(panel.getBackground());
        zoomPanel.setBorder(new TitledBorder("Zoom"));

        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 10, 300, 100);
        zoomSlider.setBackground(panel.getBackground());
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);

        JLabel zoomValueLabel = new JLabel("1.0x");
        zoomValueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        zoomSlider.addChangeListener((ChangeEvent e) -> {
            zoomLevel = zoomSlider.getValue() / 100.0;
            zoomValueLabel.setText(String.format("%.1fx", zoomLevel));
            imagePanel.setZoom(zoomLevel);
            imagePanel.repaint();
        });

        zoomPanel.add(zoomSlider);
        zoomPanel.add(zoomValueLabel);

        // Background removal options
        JPanel backgroundPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        backgroundPanel.setBackground(panel.getBackground());
        backgroundPanel.setBorder(new TitledBorder("Background Processing"));

        JButton removeBackgroundButton = new JButton("Remove Background");
        removeBackgroundButton.addActionListener(e -> {
            if (currentImage != null) {
                Mat result = backgroundEditor.removeBackground(currentImage);
                updateImage(result);
            }
        });

        JButton removeShadowsButton = new JButton("Remove Shadows");
        removeShadowsButton.addActionListener(e -> {
            if (currentImage != null) {
                Mat result = backgroundEditor.removeShadows(currentImage);
                updateImage(result);
            }
        });

        backgroundPanel.add(removeBackgroundButton);
        backgroundPanel.add(removeShadowsButton);

        // Add all panels to the main panel with separators
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        panel.add(toolSelectionPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        panel.add(resizePanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        panel.add(cropPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        panel.add(zoomPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        panel.add(backgroundPanel);

        return panel;
    }

    private void handleUploadAction(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();

        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                "Image Files", "png", "jpg", "jpeg", "gif", "bmp", "heic");
        FileNameExtensionFilter allFilter = new FileNameExtensionFilter(
                "All Files", "*");

        fileChooser.addChoosableFileFilter(imageFilter);
        fileChooser.addChoosableFileFilter(allFilter);
        fileChooser.setFileFilter(imageFilter);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImage(selectedFile.getAbsolutePath());

            try {
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                resizeImage(width, height, true);

                updateStatusLabel(String.format("Loaded: %s (Original: %.0fx%.0f, Resized to: %dx%d)",
                        selectedFile.getName(),
                        displayImage.getWidth() / zoomLevel,
                        displayImage.getHeight() / zoomLevel,
                        width, height));
            } catch (NumberFormatException ex) {
                updateStatusLabel(String.format("Loaded: %s (%.0fx%.0f)",
                        selectedFile.getName(),
                        displayImage.getWidth() / zoomLevel,
                        displayImage.getHeight() / zoomLevel));
            }
        }
    }

    private void loadImage(String path) {
        currentImage = imread(path);
        if (currentImage == null || currentImage.empty()) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load image",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        updateImage(currentImage);
        imagePanel.resetCropRect();
    }

    private void updateImage(Mat newImage) {
        currentImage = newImage;
        displayImage = imageEditor.matToBufferedImage(newImage);
        imagePanel.setImage(displayImage);
        imagePanel.repaint();
    }

    private void updateStatusLabel(String message) {
        statusLabel.setText(message);
    }

    private void resizeImage(int targetWidth, int targetHeight, boolean preserveRatio) {
        if (currentImage == null || currentImage.empty()) {
            JOptionPane.showMessageDialog(this,
                    "Please load an image first.",
                    "No Image",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Mat resizedMat = imageEditor.resizeImage(currentImage, targetWidth, targetHeight, preserveRatio);
            updateImage(resizedMat);
            
            // Get the actual dimensions after resize (may be different if preserving ratio)
            int actualWidth = resizedMat.cols();
            int actualHeight = resizedMat.rows();
            updateStatusLabel(String.format("Resized to: %dx%d", actualWidth, actualHeight));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred while resizing: " + ex.getMessage(),
                    "Error Resizing",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyCrop() {
        if (currentImage == null || currentImage.empty() || !imagePanel.isCropRectVisible()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid area to crop.",
                    "Invalid Crop",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Rectangle rect = imagePanel.getCropRect();
            
            // Call the ImageEditor to perform the crop
            Mat croppedMat = imageEditor.cropImage(
                currentImage, 
                rect, 
                zoomLevel,
                imagePanel.getWidth(),
                imagePanel.getHeight(),
                (int)(displayImage.getWidth() * zoomLevel),
                (int)(displayImage.getHeight() * zoomLevel)
            );

            updateImage(croppedMat);
            imagePanel.resetCropRect();
            updateStatusLabel(String.format("Cropped to: %dx%d", croppedMat.cols(), croppedMat.rows()));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred while cropping: " + ex.getMessage(),
                    "Error Cropping",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSaveAction(ActionEvent e) {
        if (currentImage != null && !currentImage.empty()) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".png")) {
                    path += ".png";
                }
                imwrite(path, currentImage);
                JOptionPane.showMessageDialog(this, "Image saved successfully!");
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "No image to save.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    // Custom image panel that handles the display and cropping
    class ImagePanel extends JPanel {
        private BufferedImage image;
        private Rectangle cropRect;
        private Point cropStart;
        private boolean isCropRectVisible = false;
        private double zoom = 1.0;

        public ImagePanel() {
            setBackground(Color.LIGHT_GRAY);

            // Mouse adapters for cropping functionality
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (image == null || !isInCropMode)
                        return;

                    cropStart = e.getPoint();
                    cropRect = new Rectangle(cropStart.x, cropStart.y, 0, 0);
                    isCropRectVisible = true;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (cropRect != null && cropRect.width < 5 && cropRect.height < 5) {
                        // Reset if the rectangle is too small
                        resetCropRect();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (image == null || !isInCropMode || !isCropRectVisible)
                        return;

                    int x = Math.min(cropStart.x, e.getX());
                    int y = Math.min(cropStart.y, e.getY());
                    int width = Math.abs(e.getX() - cropStart.x);
                    int height = Math.abs(e.getY() - cropStart.y);

                    cropRect.setBounds(x, y, width, height);
                    repaint();

                    updateStatusLabel(String.format("Crop: %dx%d", width, height));
                }
            });
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            if (img != null) {
                setPreferredSize(new Dimension(
                        (int) (img.getWidth() * zoom),
                        (int) (img.getHeight() * zoom)));
            }
            revalidate();
        }

        public void setZoom(double zoomLevel) {
            this.zoom = zoomLevel;
            if (image != null) {
                setPreferredSize(new Dimension(
                        (int) (image.getWidth() * zoom),
                        (int) (image.getHeight() * zoom)));
                revalidate();
            }
        }

        public void resetCropRect() {
            cropRect = null;
            isCropRectVisible = false;
            repaint();
        }

        public boolean isCropRectVisible() {
            return isCropRectVisible && cropRect != null &&
                    cropRect.width > 5 && cropRect.height > 5;
        }

        public Rectangle getCropRect() {
            return cropRect;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image != null) {
                Graphics2D g2d = (Graphics2D) g;

                // Draw the image with zoom
                int imgWidth = (int) (image.getWidth() * zoom);
                int imgHeight = (int) (image.getHeight() * zoom);

                // Center the image in the panel
                int x = (getWidth() - imgWidth) / 2;
                int y = (getHeight() - imgHeight) / 2;

                x = Math.max(0, x);
                y = Math.max(0, y);

                g2d.drawImage(image, x, y, imgWidth, imgHeight, this);

                // Draw crop rectangle if visible
                if (isCropRectVisible && cropRect != null) {
                    g2d.setColor(Color.RED);

                    // Draw dashed rectangle
                    float[] dashPattern = { 5f, 5f };
                    java.awt.Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(new java.awt.BasicStroke(
                            2f, java.awt.BasicStroke.CAP_BUTT,
                            java.awt.BasicStroke.JOIN_MITER,
                            10f, dashPattern, 0f));

                    g2d.drawRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
                    g2d.setStroke(oldStroke);
                }
            } else {
                g.setColor(getForeground());
                g.drawString("No image loaded", getWidth() / 2 - 50, getHeight() / 2);
            }
        }
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            new GUI().setVisible(true);
        });
    }
}