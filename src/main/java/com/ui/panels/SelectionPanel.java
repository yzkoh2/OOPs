// SelectionPanel.java
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

public class SelectionPanel extends JPanel {
    private BufferedImage image;
    private Rectangle selectionRect;
    private Point startPoint;

    public SelectionPanel(BufferedImage image) {
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
            // Use different color for background selection (red instead of blue)
            g2d.setColor(new Color(215, 0, 0, 128));
            g2d.fillRect(selectionRect.x, selectionRect.y,
                    selectionRect.width, selectionRect.height);

            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(selectionRect.x, selectionRect.y,
                    selectionRect.width, selectionRect.height);
        }
    }

    public Rectangle getSelectionRectangle() {
        return selectionRect;
    }
}