package com.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * A panel for displaying batch processing status and results.
 */
public class BatchProcessingPanel extends JPanel {
    
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JButton closeButton;
    
    /**
     * Create a new batch processing panel.
     */
    public BatchProcessingPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Status panel (top)
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Batch Processing Status",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Initializing batch processing...");
        statusPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(statusPanel, BorderLayout.NORTH);
        
        // Results panel (center)
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Processing Results",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        
        // Table for showing results
        String[] columns = {"File Name", "Status", "Output Path"};
        tableModel = new DefaultTableModel(columns, 0);
        resultsTable = new JTable(tableModel);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        resultsPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(resultsPanel, BorderLayout.CENTER);
        
        // Button panel (bottom)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButton = new JButton("Close");
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Set the progress percentage (0-100)
     * 
     * @param progressPercent Progress percentage
     */
    public void setProgress(int progressPercent) {
        progressBar.setValue(progressPercent);
        if (progressPercent < 100) {
            statusLabel.setText("Processing... " + progressPercent + "% complete");
        } else {
            statusLabel.setText("Batch processing complete");
        }
    }
    
    /**
     * Add a file to the results table with its status
     * 
     * @param fileName Original file name
     * @param status Processing status (Success/Error)
     * @param outputPath Path to the output file (or error message)
     */
    public void addResult(String fileName, String status, String outputPath) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addRow(new Object[]{fileName, status, outputPath});
        });
    }
    
    /**
     * Clear all results from the table
     */
    public void clearResults() {
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
    }
    
    /**
     * Set the close button action
     * 
     * @param action Action to perform when close is clicked
     */
    public void setCloseAction(Runnable action) {
        closeButton.addActionListener(e -> action.run());
    }
    
    /**
     * Update the table with a list of successfully processed files
     * 
     * @param inputFiles List of input files
     * @param outputFiles List of successfully processed output files
     */
    public void setResults(List<File> inputFiles, List<File> outputFiles) {
        clearResults();
        
        for (File inputFile : inputFiles) {
            String fileName = inputFile.getName();
            
            // Check if this file was successfully processed
            boolean success = false;
            String outputPath = "";
            
            for (File outputFile : outputFiles) {
                // Check if the output file name contains the input file name (without extension)
                String baseName = fileName;
                int dotIndex = baseName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = baseName.substring(0, dotIndex);
                }
                
                if (outputFile.getName().contains(baseName)) {
                    success = true;
                    outputPath = outputFile.getAbsolutePath();
                    break;
                }
            }
            
            // Add to results table
            if (success) {
                addResult(fileName, "Success", outputPath);
            } else {
                addResult(fileName, "Failed", "Processing error");
            }
        }
    }
}