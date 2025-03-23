package com.ui.panels;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * Represents a marked point with its own brush size
 */
class MarkedPoint {
    private Point point;
    private int brushSize;

    public MarkedPoint(Point point, int brushSize) {
        this.point = point;
        this.brushSize = brushSize;
    }

    public Point getPoint() {
        return point;
    }

    public int getBrushSize() {
        return brushSize;
    }
}

/**
 * Panel for the refinement stage of GrabCut background removal.
 * Allows users to mark definite foreground and background regions to improve segmentation.
 */
public class ForegroundRefinementPanel extends JPanel {
    
    // Enum for marking mode
    public enum MarkingMode {
        FOREGROUND,
        BACKGROUND
    }
    
    // Visualization and drawing
    private BufferedImage visualizationImage;  // Current mask visualization
    private int imageOffsetX;                  // Image horizontal centering offset
    private int imageOffsetY;                  // Image vertical centering offset
    private MarkingMode currentMode = MarkingMode.FOREGROUND; // Current drawing mode
    private int brushSize = 10;                // Default brush size
    
    // User markings storage with individual brush sizes
    private List<MarkedPoint> foregroundPoints = new ArrayList<>();
    private List<MarkedPoint> backgroundPoints = new ArrayList<>();
    
    // Mouse tracking for smooth drawing
    private Point lastMousePoint = null;
    private static final int POINT_INTERPOLATION_THRESHOLD = 5;
    
    // UI Components
    private JRadioButton fgButton;
    private JRadioButton bgButton;
    private JSlider brushSizeSlider;
    private JButton applyButton;
    private JButton resetButton;
    
    // Converter for Frame to BufferedImage
    private Java2DFrameConverter frameConverter = new Java2DFrameConverter();
    
    // Callback for refinement actions
    private RefinementCallback callback;
    
    /**
     * Interface for refinement action callbacks
     */
    public interface RefinementCallback {
        void onApplyRefinement(
            List<Point> foregroundPoints, 
            List<Point> backgroundPoints, 
            List<Integer> foregroundBrushSizes, 
            List<Integer> backgroundBrushSizes
        );
        void onResetRefinement();
        void onUpdateVisualization();
    }
    
    /**
     * Constructor
     * 
     * @param visualizationFrame Initial visualization frame (overlay of mask on original)
     * @param callback Callback for refinement actions
     */
    public ForegroundRefinementPanel(Frame visualizationFrame, RefinementCallback callback) {
        this.callback = callback;
        
        // Convert the frame to BufferedImage for display
        this.visualizationImage = frameConverter.convert(visualizationFrame);
        
        // Set preferred size based on the image
        setPreferredSize(new Dimension(visualizationImage.getWidth(), visualizationImage.getHeight() + 80));
        
        // Set up the panel layout
        setLayout(new BorderLayout());
        
        // Create the toolbar for controls
        createToolbar();
        
        // Setup mouse listeners for drawing
        setupMouseListeners();
        
        // Enable double buffering for smoother painting
        setDoubleBuffered(true);
    }
    
    /**
     * Create toolbar with marking controls
     */
    private void createToolbar() {
        // Create toolbar panel
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Add marking mode radio buttons
        ButtonGroup modeGroup = new ButtonGroup();
        
        fgButton = new JRadioButton("Mark Foreground", true);
        fgButton.setToolTipText("Mark areas that should definitely be in the foreground");
        fgButton.addActionListener(e -> currentMode = MarkingMode.FOREGROUND);
        
        bgButton = new JRadioButton("Mark Background", false);
        bgButton.setToolTipText("Mark areas that should definitely be in the background");
        bgButton.addActionListener(e -> currentMode = MarkingMode.BACKGROUND);
        
        modeGroup.add(fgButton);
        modeGroup.add(bgButton);
        
        toolbarPanel.add(fgButton);
        toolbarPanel.add(bgButton);
        
        // Add brush size slider
        JLabel brushLabel = new JLabel("Brush Size: ");
        toolbarPanel.add(brushLabel);
        
        brushSizeSlider = new JSlider(JSlider.HORIZONTAL, 2, 50, brushSize);
        brushSizeSlider.setMajorTickSpacing(10);
        brushSizeSlider.setMinorTickSpacing(2);
        brushSizeSlider.setPaintTicks(true);
        brushSizeSlider.setToolTipText("Adjust the size of the marking brush");
        
        brushSizeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                brushSize = brushSizeSlider.getValue();
                repaint(); // Update brush preview
            }
        });
        
        toolbarPanel.add(brushSizeSlider);
        
        // Add action buttons
        applyButton = new JButton("Apply");
        applyButton.setToolTipText("Apply refinements and run GrabCut again");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Capture points and their brush sizes immediately
                List<Point> currentForegroundPoints = getForegroundPoints();
                List<Point> currentBackgroundPoints = getBackgroundPoints();
                List<Integer> currentForegroundBrushSizes = getForegroundBrushSizes();
                List<Integer> currentBackgroundBrushSizes = getBackgroundBrushSizes();
                
                // Clear UI immediately
                foregroundPoints.clear();
                backgroundPoints.clear();
                fgButton.setSelected(true);
                currentMode = MarkingMode.FOREGROUND;
                repaint();
                
                // Run processing in background thread
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        if (callback != null) {
                            callback.onApplyRefinement(
                                currentForegroundPoints, 
                                currentBackgroundPoints, 
                                currentForegroundBrushSizes,
                                currentBackgroundBrushSizes
                            );
                        }
                        return null;
                    }
                };
                worker.execute();
            }
        });
        
        resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset all refinement markings");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                foregroundPoints.clear();
                backgroundPoints.clear();
                if (callback != null) {
                    callback.onResetRefinement();
                }
                repaint();
            }
        });
        
        toolbarPanel.add(applyButton);
        toolbarPanel.add(resetButton);
        
        // Add toolbar to main panel
        add(toolbarPanel, BorderLayout.NORTH);
    }
    
    /**
     * Setup mouse listeners for drawing with the brush
     */
    private void setupMouseListeners() {
        MouseAdapter drawingAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePoint = e.getPoint();
                addPoint(e.getPoint());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getPoint();
                
                // Interpolate points for smoother drawing
                if (lastMousePoint != null) {
                    double distance = lastMousePoint.distance(currentPoint);
                    
                    if (distance > POINT_INTERPOLATION_THRESHOLD) {
                        // Interpolate points to create smoother lines
                        int steps = (int) (distance / POINT_INTERPOLATION_THRESHOLD);
                        for (int i = 1; i <= steps; i++) {
                            double t = (double) i / steps;
                            int interpX = (int) (lastMousePoint.x + t * (currentPoint.x - lastMousePoint.x));
                            int interpY = (int) (lastMousePoint.y + t * (currentPoint.y - lastMousePoint.y));
                            addPoint(new Point(interpX, interpY));
                        }
                    }
                }
                
                addPoint(currentPoint);
                lastMousePoint = currentPoint;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                lastMousePoint = null;
            }
        };
        
        addMouseListener(drawingAdapter);
        addMouseMotionListener(drawingAdapter);
    }
    
    /**
     * Add a marking point at the specified location
     * 
     * @param p Screen point
     */
    private void addPoint(Point p) {
        // Calculate image offsets if not already done
        if (imageOffsetX == 0 && imageOffsetY == 0) {
            imageOffsetX = (getWidth() - visualizationImage.getWidth()) / 2;
            imageOffsetY = (getHeight() - visualizationImage.getHeight() - 80) / 2 + 80; // Account for toolbar
        }
        
        // Adjust point to image coordinates
        Point imagePoint = new Point(
            p.x - imageOffsetX, 
            p.y - imageOffsetY
        );
        
        // Skip if outside image bounds
        if (imagePoint.x < 0 || imagePoint.y < 0 || 
            imagePoint.x >= visualizationImage.getWidth() || 
            imagePoint.y >= visualizationImage.getHeight()) {
            return;
        }
        
        // Create a MarkedPoint with the current brush size
        MarkedPoint markedPoint = new MarkedPoint(imagePoint, brushSize);
        
        // Add to appropriate list based on current mode
        if (currentMode == MarkingMode.FOREGROUND) {
            foregroundPoints.add(markedPoint);
        } else {
            backgroundPoints.add(markedPoint);
        }
        
        // Request repaint to show new mark
        repaint();
    }
    
    /**
     * Update the visualization image
     * 
     * @param visualizationFrame New visualization frame
     */
    public void updateVisualization(Frame visualizationFrame) {
        if (visualizationFrame != null) {
            this.visualizationImage = frameConverter.convert(visualizationFrame);
            repaint();
        }
    }
    
    /**
     * Clear all markings
     */
    public void clearMarkings() {
        foregroundPoints.clear();
        backgroundPoints.clear();
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Don't proceed if image isn't loaded yet
        if (visualizationImage == null) {
            return;
        }
        
        // Calculate offsets to center the image
        int width = getWidth();
        int height = getHeight();
        imageOffsetX = (width - visualizationImage.getWidth()) / 2;
        imageOffsetY = (height - visualizationImage.getHeight() - 80) / 2 + 80; // Account for toolbar
        
        // Set up antialiasing for smoother rendering
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Draw the visualization image
        g2d.drawImage(visualizationImage, imageOffsetX, imageOffsetY, null);
        
        // Draw foreground marks (translucent green)
        g2d.setColor(new Color(0, 255, 0, 100));
        for (MarkedPoint mp : foregroundPoints) {
            Point p = mp.getPoint();
            int pointBrushSize = mp.getBrushSize();
            g2d.fillOval(
                p.x + imageOffsetX - pointBrushSize/2, 
                p.y + imageOffsetY - pointBrushSize/2, 
                pointBrushSize, pointBrushSize
            );
        }
        
        // Draw background marks (translucent red)
        g2d.setColor(new Color(255, 0, 0, 100));
        for (MarkedPoint mp : backgroundPoints) {
            Point p = mp.getPoint();
            int pointBrushSize = mp.getBrushSize();
            g2d.fillOval(
                p.x + imageOffsetX - pointBrushSize/2, 
                p.y + imageOffsetY - pointBrushSize/2, 
                pointBrushSize, pointBrushSize
            );
        }
        
        // Draw current brush preview at mouse position (if mouse is over the image)
        Point mousePos = getMousePosition();
        if (mousePos != null) {
            // Calculate if inside image bounds
            int imgX = mousePos.x - imageOffsetX;
            int imgY = mousePos.y - imageOffsetY;
            
            if (imgX >= 0 && imgY >= 0 && 
                imgX < visualizationImage.getWidth() && 
                imgY < visualizationImage.getHeight()) {
                
                // Set color based on current mode with soft edge
                g2d.setStroke(new BasicStroke(2f));
                if (currentMode == MarkingMode.FOREGROUND) {
                    g2d.setColor(new Color(0, 255, 0, 150)); // Semi-transparent green
                    g2d.drawOval(
                        mousePos.x - brushSize/2, 
                        mousePos.y - brushSize/2, 
                        brushSize, brushSize
                    );
                    g2d.setColor(new Color(0,255, 0, 50)); // Softer fill
                    g2d.fillOval(
                        mousePos.x - brushSize/2, 
                        mousePos.y - brushSize/2, 
                        brushSize, brushSize
                    );
                } else {
                    g2d.setColor(new Color(255, 0, 0, 150)); // Semi-transparent red
                    g2d.drawOval(
                        mousePos.x - brushSize/2, 
                        mousePos.y - brushSize/2, 
                        brushSize, brushSize
                    );
                    g2d.setColor(new Color(255, 0, 0, 50)); // Softer fill
                    g2d.fillOval(
                        mousePos.x - brushSize/2, 
                        mousePos.y - brushSize/2, 
                        brushSize, brushSize
                    );
                }
            }
        }
    }
    
    /**
     * Get the list of foreground marking points
     * 
     * @return List of foreground points
     */
    public List<Point> getForegroundPoints() {
        return foregroundPoints.stream()
            .map(MarkedPoint::getPoint)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the list of background marking points
     * 
     * @return List of background points
     */
    public List<Point> getBackgroundPoints() {
        return backgroundPoints.stream()
            .map(MarkedPoint::getPoint)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the current brush sizes for foreground points
     * 
     * @return List of brush sizes for foreground points
     */
    public List<Integer> getForegroundBrushSizes() {
        return foregroundPoints.stream()
            .map(MarkedPoint::getBrushSize)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the current brush sizes for background points
     * 
     * @return List of brush sizes for background points
     */
    public List<Integer> getBackgroundBrushSizes() {
        return backgroundPoints.stream()
            .map(MarkedPoint::getBrushSize)
            .collect(Collectors.toList());
    }
    
    /**
     * Get the current brush size
     * 
     * @return Current brush size in pixels
     */
    public int getBrushSize() {
        return brushSize;
    }
    
    /**
     * Get the current marking mode
     * 
     * @return Current marking mode (FOREGROUND or BACKGROUND)
     */
    public MarkingMode getCurrentMode() {
        return currentMode;
    }
}