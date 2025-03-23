package com.ui.panels;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.Dimension;

public abstract class ImageSelectionPanel extends JPanel {
    protected BufferedImage image;
    protected Rectangle selectionRect;
    protected Point startPoint;
    protected int imageOffsetX;
    protected int imageOffsetY;
    
    // Configurable selection colors
    protected Color selectionFillColor;
    protected Color selectionBorderColor;

    public ImageSelectionPanel(BufferedImage image, Color fillColor, Color borderColor) {
        this.image = image;
        this.selectionFillColor = fillColor;
        this.selectionBorderColor = borderColor;
        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

        // Add mouse listeners for interactive selection
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Calculate image offsets
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                imageOffsetX = (panelWidth - image.getWidth()) / 2;
                imageOffsetY = (panelHeight - image.getHeight()) / 2;

                // Adjust mouse point relative to image
                Point adjustedPoint = new Point(
                    e.getX() - imageOffsetX, 
                    e.getY() - imageOffsetY
                );

                startPoint = adjustedPoint;
                selectionRect = null;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint != null) {
                    // Adjust mouse point relative to image
                    Point adjustedPoint = new Point(
                        e.getX() - imageOffsetX, 
                        e.getY() - imageOffsetY
                    );

                    int x = Math.min(startPoint.x, adjustedPoint.x);
                    int y = Math.min(startPoint.y, adjustedPoint.y);
                    int width = Math.abs(adjustedPoint.x - startPoint.x);
                    int height = Math.abs(adjustedPoint.y - startPoint.y);

                    selectionRect = new Rectangle(x, y, width, height);
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

        // Calculate centering offsets
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // Calculate x and y offsets to center the image
        int x = (panelWidth - imageWidth) / 2;
        int y = (panelHeight - imageHeight) / 2;

        // Draw the image at the centered position
        g.drawImage(image, x, y, null);

        // Draw selection rectangle if it exists
        if (selectionRect != null) {
            Graphics2D g2d = (Graphics2D) g;
            // Fill with semi-transparent selection color
            g2d.setColor(selectionFillColor);
            g2d.fillRect(selectionRect.x, selectionRect.y,
                    selectionRect.width, selectionRect.height);

            // Draw border
            g2d.setColor(selectionBorderColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(selectionRect.x, selectionRect.y,
                    selectionRect.width, selectionRect.height);
        }
    }

    public Rectangle getSelectionRectangle() {
        return selectionRect;
    }
}