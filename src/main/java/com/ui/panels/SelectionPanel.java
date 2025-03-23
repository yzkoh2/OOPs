package com.ui.panels;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class SelectionPanel extends ImageSelectionPanel {
    public SelectionPanel(BufferedImage image) {
        super(
            image, 
            new Color(215, 0, 0, 128),  // Red fill color
            Color.RED  // Red border color
        );
    }
}