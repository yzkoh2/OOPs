package com.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import com.editor.InteractivePhotoEnhancer;

/**
 * Interactive panel for enhancing ID photos with real-time preview
 */
public class EnhancementPanel extends JDialog {
    private static final long serialVersionUID = 1L;
    
    // UI Components
    private JLabel previewLabel;
    private JSlider brightnessSlider;
    private JSlider contrastSlider;
    private JSlider smoothingSlider;
    private JButton previewButton;
    private JButton confirmButton;
    private JButton cancelButton;
    
    // Conversion utilities
    private final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
    private final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    
    // Enhancement parameters
    private double brightness = 0.0;
    private double contrast = 1.0;
    private int smoothingLevel = 0;
    
    // Source image
    private Frame originalFrame;
    private BufferedImage originalImage;
    private Frame previewFrame;
    
    // Callback function for when enhancement is confirmed
    private Consumer<Frame> onEnhancementConfirmed;
    
    // Flag to prevent multiple concurrent preview updates
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    // Flag to track if image has been modified from original
    private boolean imageModified = false;
    
    /**
     * Create a new enhancement panel
     * 
     * @param parent The parent frame
     * @param inputFrame The original frame to enhance
     * @param onConfirm Callback for when enhancement is confirmed
     */
    public EnhancementPanel(JFrame parent, Frame inputFrame, Consumer<Frame> onConfirm) {
        super(parent, "Photo Enhancement", true);
        this.originalFrame = inputFrame.clone();
        this.previewFrame = inputFrame.clone();
        this.onEnhancementConfirmed = onConfirm;
        
        // Convert frame to BufferedImage for preview
        originalImage = java2DConverter.convert(originalFrame);
        
        initializeUI();
        
        // Position and show
        pack();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(800, 600));
        
        // === Preview Panel (Center) ===
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        
        previewLabel = new JLabel();
        updatePreviewImage(originalImage);
        
        JScrollPane scrollPane = new JScrollPane(previewLabel);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        previewPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(previewPanel, BorderLayout.CENTER);
        
        // === Controls Panel (East) ===
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Enhancement Controls"));
        
        // Brightness slider
        JPanel brightnessPanel = createSliderPanel("Brightness", -100, 100, 0, 25);
        brightnessSlider = (JSlider) brightnessPanel.getClientProperty("slider");
        brightnessSlider.addChangeListener(e -> {
            brightness = brightnessSlider.getValue() / 10.0;
            imageModified = true;
            previewButton.setEnabled(true);
            if (!brightnessSlider.getValueIsAdjusting()) {
                schedulePreviewUpdate();
            }
        });
        controlsPanel.add(brightnessPanel);
        
        // Contrast slider
        JPanel contrastPanel = createSliderPanel("Contrast", 50, 200, 100, 25);
        contrastSlider = (JSlider) contrastPanel.getClientProperty("slider");
        contrastSlider.addChangeListener(e -> {
            contrast = contrastSlider.getValue() / 100.0;
            imageModified = true;
            previewButton.setEnabled(true);
            if (!contrastSlider.getValueIsAdjusting()) {
                schedulePreviewUpdate();
            }
        });
        controlsPanel.add(contrastPanel);
        
        // Skin smoothing slider
        JPanel smoothingPanel = createSliderPanel("Skin Smoothing", 0, 30, 0, 5);
        smoothingSlider = (JSlider) smoothingPanel.getClientProperty("slider");
        smoothingSlider.addChangeListener(e -> {
            smoothingLevel = smoothingSlider.getValue();
            imageModified = true;
            previewButton.setEnabled(true);
            if (!smoothingSlider.getValueIsAdjusting()) {
                schedulePreviewUpdate();
            }
        });
        controlsPanel.add(smoothingPanel);
        
        // Preview button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewButton = new JButton("Update Preview");
        previewButton.setEnabled(false);
        previewButton.addActionListener(e -> updatePreview());
        buttonPanel.add(previewButton);
        controlsPanel.add(buttonPanel);
        
        // Reset button
        JButton resetButton = new JButton("Reset All");
        resetButton.addActionListener(e -> resetControls());
        buttonPanel.add(resetButton);
        
        // Add a note about undo functionality
        JLabel noteLabel = new JLabel("<html><body style='width: 200px'>" +
                                     "<i>Note: You can use Undo (Ctrl+Z) after " +
                                     "applying changes if needed.</i></body></html>");
        noteLabel.setForeground(new java.awt.Color(80, 80, 80));
        JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        notePanel.add(noteLabel);
        controlsPanel.add(notePanel);
        
        // Add spacing
        controlsPanel.add(Box.createVerticalStrut(20));
        
        // Add control panel to main layout
        add(controlsPanel, BorderLayout.EAST);
        
        // === Button Panel (South) ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        
        confirmButton = new JButton("Apply Changes");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> {
            if (onEnhancementConfirmed != null && previewFrame != null) {
                // Create a fresh, completely separate clone of the preview frame
                Frame freshClone = previewFrame.clone();
                onEnhancementConfirmed.accept(freshClone);
            }
            dispose();
        });
        
        bottomPanel.add(cancelButton);
        bottomPanel.add(confirmButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Helper method to create a labeled slider panel
     */
    private JPanel createSliderPanel(String title, int min, int max, int initial, int majorTick) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, initial);
        slider.setMajorTickSpacing(majorTick);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        
        // Custom labels if needed
        if ("Contrast".equals(title)) {
            Hashtable<Integer, JLabel> labels = new Hashtable<>();
            labels.put(50, new JLabel("0.5x"));
            labels.put(100, new JLabel("1.0x"));
            labels.put(150, new JLabel("1.5x"));
            labels.put(200, new JLabel("2.0x"));
            slider.setLabelTable(labels);
        }
        
        panel.add(slider, BorderLayout.CENTER);
        panel.putClientProperty("slider", slider);
        
        return panel;
    }
    
    /**
     * Schedule a preview update with a small delay to avoid too many updates
     * when slider is being adjusted
     */
    private void schedulePreviewUpdate() {
        javax.swing.Timer timer = new javax.swing.Timer(200, e -> {
            ((javax.swing.Timer)e.getSource()).stop();
            updatePreview();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * Update the preview with current enhancement settings
     */
    private void updatePreview() {
        // Avoid multiple concurrent updates
        if (isUpdating.getAndSet(true)) {
            return;
        }
        
        new SwingWorker<Frame, Void>() {
            @Override
            protected Frame doInBackground() throws Exception {
                // Get a fresh copy of the original frame - important for undo to work properly!
                Frame frameToProcess = originalFrame.clone();
                return InteractivePhotoEnhancer.enhance(frameToProcess, contrast, brightness, smoothingLevel);
            }
            
            @Override
            protected void done() {
                try {
                    previewFrame = get();
                    BufferedImage newImage = java2DConverter.convert(previewFrame);
                    updatePreviewImage(newImage);
                    confirmButton.setEnabled(imageModified);
                    previewButton.setEnabled(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    isUpdating.set(false);
                }
            }
        }.execute();
    }
    
    /**
     * Update the preview image display
     */
    private void updatePreviewImage(BufferedImage image) {
        int maxPreviewWidth = 500;
        
        // Scale down image if needed
        if (image.getWidth() > maxPreviewWidth) {
            double aspectRatio = (double) image.getHeight() / image.getWidth();
            int scaledHeight = (int) (maxPreviewWidth * aspectRatio);
            previewLabel.setIcon(new ImageIcon(image.getScaledInstance(
                    maxPreviewWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH)));
        } else {
            previewLabel.setIcon(new ImageIcon(image));
        }
        
        previewLabel.revalidate();
        previewLabel.repaint();
    }
    
    /**
     * Reset all controls to their default values
     */
    private void resetControls() {
        brightnessSlider.setValue(0);
        contrastSlider.setValue(100);
        smoothingSlider.setValue(0);
        
        brightness = 0.0;
        contrast = 1.0;
        smoothingLevel = 0;
        
        // Reset preview to original image
        previewFrame = originalFrame.clone();
        updatePreviewImage(originalImage);
        confirmButton.setEnabled(false);
        previewButton.setEnabled(false);
        imageModified = false;
    }
}