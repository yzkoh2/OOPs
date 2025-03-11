package com.entities;

/**
 * Contains settings for exporting processed photos.
 */
public class ExportSettings {
    private String format;
    private int quality;
    private String outputFolder;
    private String outputFilename;
    private boolean createMultipleLayouts;
    private int rows;
    private int columns;
    
    public ExportSettings() {
        // Default settings
        this.format = "png";
        this.quality = 95;
        this.outputFolder = System.getProperty("user.home");
        this.outputFilename = "id_photo";
        this.createMultipleLayouts = false;
        this.rows = 2;
        this.columns = 2;
    }
    
    // Getters and setters
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public int getQuality() {
        return quality;
    }
    
    public void setQuality(int quality) {
        this.quality = quality;
    }
    
    public String getOutputFolder() {
        return outputFolder;
    }
    
    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }
    
    public String getOutputFilename() {
        return outputFilename;
    }
    
    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }
    
    public boolean isCreateMultipleLayouts() {
        return createMultipleLayouts;
    }
    
    public void setCreateMultipleLayouts(boolean createMultipleLayouts) {
        this.createMultipleLayouts = createMultipleLayouts;
    }
    
    public int getRows() {
        return rows;
    }
    
    public void setRows(int rows) {
        this.rows = rows;
    }
    
    public int getColumns() {
        return columns;
    }
    
    public void setColumns(int columns) {
        this.columns = columns;
    }
    
    public String getFullOutputPath() {
        return outputFolder + System.getProperty("file.separator") + 
               outputFilename + "." + format;
    }
}