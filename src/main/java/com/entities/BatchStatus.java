package com.entities;

import java.io.File;

/**
 * Class to track the status of a single file in batch processing
 */
public class BatchStatus {
    
    public enum Status {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED
    }
    
    private final File inputFile;
    private File outputFile;
    private Status status;
    private String errorMessage;
    
    /**
     * Create a new BatchStatus for a file
     * 
     * @param inputFile The input file being processed
     */
    public BatchStatus(File inputFile) {
        this.inputFile = inputFile;
        this.status = Status.QUEUED;
    }
    
    /**
     * Mark this file as currently being processed
     */
    public void markProcessing() {
        this.status = Status.PROCESSING;
    }
    
    /**
     * Mark this file as successfully completed
     * 
     * @param outputFile The output file that was created
     */
    public void markCompleted(File outputFile) {
        this.status = Status.COMPLETED;
        this.outputFile = outputFile;
    }
    
    /**
     * Mark this file as failed with an error message
     * 
     * @param errorMessage The error message describing the failure
     */
    public void markFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    
    public File getInputFile() {
        return inputFile;
    }
    
    public File getOutputFile() {
        return outputFile;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get a string representation of the status
     * 
     * @return A human-readable status string
     */
    public String getStatusString() {
        switch (status) {
            case QUEUED:
                return "Queued";
            case PROCESSING:
                return "Processing";
            case COMPLETED:
                return "Completed";
            case FAILED:
                return "Failed";
            default:
                return "Unknown";
        }
    }
}