package com.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import com.models.MarkedPoint;
import com.models.RefinementModel;

/**
 * A drawing board component for interactive image marking.
 */
public class DrawingBoard extends JPanel {
    private final RefinementModel refinementModel;
    private BufferedImage backgroundImage;
    
    // Tracking for smooth drawing
    private Point lastMousePoint = null;
    private static final int POINT_INTERPOLATION_THRESHOLD = 5;
    
    /**
     * Creates a new DrawingBoard.
     * 
     * @param refinementModel The model to manage refinement state
     * @param backgroundImage The base image to draw on
     */
    public DrawingBoard(RefinementModel refinementModel, BufferedImage backgroundImage) {
        this.refinementModel = refinementModel;
        this.backgroundImage = backgroundImage;
        
        // Enable double buffering for smoother painting
        setDoubleBuffered(true);
        
        // Setup mouse listeners for drawing
        setupMouseListeners();
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
     * @param p Screen point to mark
     */
    private void addPoint(Point p) {
        // Ensure point is within image bounds
        if (p.x < 0 || p.y < 0 || 
            p.x >= getWidth() || p.y >= getHeight()) {
            return;
        }
        
        // Add point to refinement model
        refinementModel.addMarkedPoint(p);
        
        // Trigger repaint to show new mark
        repaint();
    }
    
    /**
     * Update the background image
     * 
     * @param newBackgroundImage New image to draw on
     */
    public void updateBackgroundImage(BufferedImage newBackgroundImage) {
        this.backgroundImage = newBackgroundImage;
        repaint();
    }
    
    @Override
    public Dimension getPreferredSize() {
        if (backgroundImage != null) {
            return new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight());
        }
        return super.getPreferredSize();
    }
    

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw background image if exists
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, this);
        }
        
        // Set up antialiasing for smoother rendering
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw foreground marks (translucent green)
        g2d.setColor(new Color(0, 255, 0, 100));
        for (MarkedPoint point : refinementModel.getForegroundPoints()) {
            int pointBrushSize = point.getBrushSize();
            g2d.fillOval(
                point.getPoint().x - pointBrushSize/2, 
                point.getPoint().y - pointBrushSize/2, 
                pointBrushSize, pointBrushSize
            );
        }
        
        // Draw background marks (translucent red)
        g2d.setColor(new Color(255, 0, 0, 100));
        for (MarkedPoint point : refinementModel.getBackgroundPoints()) {
            int pointBrushSize = point.getBrushSize();
            g2d.fillOval(
                point.getPoint().x - pointBrushSize/2, 
                point.getPoint().y - pointBrushSize/2, 
                pointBrushSize, pointBrushSize
            );
        }
        
        // Draw current brush preview
        Point mousePos = getMousePosition();
        if (mousePos != null) {
            int brushSize = refinementModel.getBrushSize();
            
            // Set color based on current mode with soft edge
            g2d.setStroke(new BasicStroke(2f));
            if (refinementModel.getCurrentMode() == RefinementModel.MarkingMode.FOREGROUND) {
                g2d.setColor(new Color(0, 255, 0, 150)); // Semi-transparent green
                g2d.drawOval(
                    mousePos.x - brushSize/2, 
                    mousePos.y - brushSize/2, 
                    brushSize, brushSize
                );
                g2d.setColor(new Color(0, 255, 0, 50)); // Softer fill
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
    public void releaseResources() {
        if (backgroundImage != null) {
            backgroundImage.flush(); // Release image resources
            backgroundImage = null;
        }
        // Remove listeners
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
        for (MouseMotionListener listener : getMouseMotionListeners()) {
            removeMouseMotionListener(listener);
        }
    }
}