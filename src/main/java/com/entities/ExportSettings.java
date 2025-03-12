package com.entities;

public class ExportSettings {
    public enum ImageFormat {
        JPEG("jpg"),
        PNG("png"),
        BMP("bmp");
        
        private final String extension;
        
        ImageFormat(String extension) {
            this.extension = extension;
        }
        
        public String getExtension() {
            return extension;
        }
    }
    
    private ImageFormat format;
    private int quality;
    private int width;
    private int height;
    private String outputDirectory;
    private String fileNamePrefix;
    private boolean maintainAspectRatio;
    private boolean generateMultiples;
    private int rows;
    private int columns;
    
    public ExportSettings() {
        // Default settings
        this.format = ImageFormat.JPEG;
        this.quality = 90;
        this.width = 35;  // mm
        this.height = 45; // mm
        this.outputDirectory = System.getProperty("user.home");
        this.fileNamePrefix = "ID_Photo_";
        this.maintainAspectRatio = true;
        this.generateMultiples = false;
        this.rows = 2;
        this.columns = 2;
    }
    
    // Getters and setters
    public ImageFormat getFormat() {
        return format;
    }
    
    public void setFormat(ImageFormat format) {
        this.format = format;
    }
    
    public int getQuality() {
        return quality;
    }
    
    public void setQuality(int quality) {
        this.quality = quality;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public String getFileNamePrefix() {
        return fileNamePrefix;
    }
    
    public void setFileNamePrefix(String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
    }
    
    public boolean isMaintainAspectRatio() {
        return maintainAspectRatio;
    }
    
    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
    }
    
    public boolean isGenerateMultiples() {
        return generateMultiples;
    }
    
    public void setGenerateMultiples(boolean generateMultiples) {
        this.generateMultiples = generateMultiples;
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
}
