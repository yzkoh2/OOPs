package com.main;

import com.gui.GUI;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Application...");
        GUI gui = new GUI();
        gui.setVisible(true);
        
        // javax.swing.SwingUtilities.invokeLater(() -> {
        //     GUI gui = new GUI();
        //     gui.setVisible(true);
        // });
    }
}
