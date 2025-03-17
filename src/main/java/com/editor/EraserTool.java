
package com.editor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class EraserTool extends JFrame {
    private BufferedImage image; // The modified image
    private int brushSize = 20;
    private EraserCompleteListener listener; // Callback for returning the modified image

    public interface EraserCompleteListener {
        void onEraserComplete(BufferedImage erasedImage);
    }

    public EraserTool(BufferedImage image, EraserCompleteListener listener) {
        this.image = deepCopy(image); // Work on a copy, not the original
        this.listener = listener;

        setTitle("Manual Background Eraser");
        setSize(image.getWidth(), image.getHeight() + 50);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        EraserPanel panel = new EraserPanel();
        add(panel, BorderLayout.CENTER);

        JButton doneButton = new JButton("Apply Background Removal");
        doneButton.addActionListener(e -> {
            dispose();
            listener.onEraserComplete(image); // Return modified image
        });

        add(doneButton, BorderLayout.SOUTH);
        setVisible(true);
    }

    private class EraserPanel extends JPanel {
        public EraserPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    erase(e.getX(), e.getY());
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    erase(e.getX(), e.getY());
                }
            });
        }

        private void erase(int x, int y) {
            Graphics2D g2 = image.createGraphics();
            g2.setComposite(AlphaComposite.Clear); // Make the erased part transparent
            g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.fillOval(x - brushSize / 2, y - brushSize / 2, brushSize, brushSize);
            g2.dispose();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
        }
    }

    private BufferedImage deepCopy(BufferedImage original) {
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}