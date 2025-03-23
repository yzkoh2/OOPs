package com.ui.panels;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class CropPanel extends ImageSelectionPanel {
    public CropPanel(BufferedImage image) {
        super(
            image, 
            new Color(0, 120, 215, 128),  // Blue fill color
            Color.BLUE  // Blue border color
        );
    }
}