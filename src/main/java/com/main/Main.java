package com.main;

import com.gui.GUI;
import com.entities.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Application...");
        GUI gui = new GUI();
        gui.setVisible(true);
        Photo p = new Photo();
        System.out.println(p);
        
        // javax.swing.SwingUtilities.invokeLater(() -> {
        //     GUI gui = new GUI();
        //     gui.setVisible(true);
        // });
    }
}
