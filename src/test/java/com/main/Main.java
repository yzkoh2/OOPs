package com.main;

import com.gui.GUI;

public class Main {
    public static void main(String[] args) {
        // Launch the GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();
            gui.setVisible(true);
        });
    }
}
