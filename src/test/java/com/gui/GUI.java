package com.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import org.bytedeco.opencv.opencv_core.Mat;

import com.editor.BackgroundEditor;

public class GUI extends JFrame {
    private BackgroundEditor editor;
    private JPanel controlPanel;
    private JLabel imageLabel;
    private Mat currentImage;
    private OpenCVFrameConverter.ToMat converter;
    private Java2DFrameConverter java2dConverter;

    public GUI() {
        editor = new BackgroundEditor();
        converter = new OpenCVFrameConverter.ToMat();
        java2dConverter = new Java2DFrameConverter();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("ID Photo Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        // Main layout
        setLayout(new BorderLayout());

        // Image display area
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setBackground(Color.LIGHT_GRAY);
        imageLabel.setOpaque(true);
        imageLabel.setText("No image loaded");
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);
        // Control panel
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        JButton uploadButton = new JButton("Upload Picture");
        uploadButton.addActionListener(this::handleUploadAction);
        controlPanel.add(uploadButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void handleUploadAction(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImage(selectedFile.getAbsolutePath());
            showBackgroundRemovalOptions();
        }
    }

    private void loadImage(String path) {
        currentImage = imread(path);
        displayImage(currentImage);
    }

    private void displayImage(Mat image) {
        if (image == null || image.empty()) {
            imageLabel.setIcon(null);
            imageLabel.setText("Failed to load image");
            return;
        }

        // Convert JavaCV Mat to Java BufferedImage
        org.bytedeco.javacv.Frame frame = converter.convert(image);

        BufferedImage bufferedImage = java2dConverter.getBufferedImage(frame);

        // Display in the JLabel
        ImageIcon icon = new ImageIcon(bufferedImage);

        // Resize image if it's too large for display
        if (icon.getIconWidth() > 700 || icon.getIconHeight() > 500) {
            icon = new ImageIcon(icon.getImage().getScaledInstance(
                    -1, 500, Image.SCALE_SMOOTH));
        }

        imageLabel.setText(null);
        imageLabel.setIcon(icon);
        imageLabel.repaint();

        // Adjust frame size
        pack();
    }

    private void showBackgroundRemovalOptions() {
        // Clear existing background removal controls
        for (Component comp : controlPanel.getComponents()) {
            if (comp != controlPanel.getComponent(0)) { // Keep upload button
                controlPanel.remove(comp);
            }
        }

        // Add background removal options
        JButton removeBackgroundButton = new JButton("Remove Background");
        removeBackgroundButton.addActionListener(e -> {
            if (currentImage != null) {
                Mat result = editor.removeBackground(currentImage);
                displayImage(result);
                currentImage = result;
            }
        });

        JButton removeShadowsButton = new JButton("Remove Shadows");
        removeShadowsButton.addActionListener(e -> {
            if (currentImage != null) {
                Mat result = editor.removeShadows(currentImage);
                displayImage(result);
                currentImage = result;
            }
        });

        JButton saveButton = new JButton("Save Result");
        saveButton.addActionListener(this::handleSaveAction);

        controlPanel.add(removeBackgroundButton);
        controlPanel.add(removeShadowsButton);
        controlPanel.add(saveButton);

        controlPanel.revalidate();
        controlPanel.repaint();
    }

    private void handleSaveAction(ActionEvent e) {
        if (currentImage != null) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".png")) {
                    path += ".png";
                }
                imwrite(path, currentImage);
                JOptionPane.showMessageDialog(this, "Image saved successfully!");
            }
        }
    }
}
