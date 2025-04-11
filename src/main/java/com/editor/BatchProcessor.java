package com.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.entities.BackgroundSettings;
import com.entities.ExportSettings;
import com.entities.Photo;
import com.util.Constants;
import com.util.FileUtils;

/**
 * Handles batch processing of multiple photos with the same settings.
 */
public class BatchProcessor {
    private final BackgroundSettings backgroundSettings;
    private final ExportSettings exportSettings;
    private final int targetWidth;
    private final int targetHeight;
    private final boolean maintainAspectRatio;
    private final List<File> inputFiles;
    private final String outputDir;
    private final ExecutorService executor;
    
    // For progress tracking
    private int totalFiles;
    private int processedFiles;
    private Consumer<Integer> progressCallback;
    private Consumer<List<File>> completionCallback;
    private Consumer<String> errorCallback;
    
    /**
     * Constructor for BatchProcessor
     * 
     * @param inputFiles List of input image files to process
     * @param outputDir Directory to save processed files
     * @param backgroundSettings Background removal settings
     * @param exportSettings Export settings
     * @param targetWidthMM Target width in millimeters
     * @param targetHeightMM Target height in millimeters
     * @param maintainAspectRatio Whether to maintain aspect ratio during resize
     */
    public BatchProcessor(
            List<File> inputFiles, 
            String outputDir,
            BackgroundSettings backgroundSettings, 
            ExportSettings exportSettings,
            int targetWidthMM, 
            int targetHeightMM,
            boolean maintainAspectRatio) {
        
        this.inputFiles = new ArrayList<>(inputFiles);
        this.outputDir = outputDir;
        this.backgroundSettings = backgroundSettings;
        this.exportSettings = exportSettings;
        this.targetWidth = targetWidthMM * Constants.PIXELS_PER_MM;
        this.targetHeight = targetHeightMM * Constants.PIXELS_PER_MM;
        this.maintainAspectRatio = maintainAspectRatio;
        
        this.totalFiles = inputFiles.size();
        this.processedFiles = 0;
        
        // Create a thread pool with a reasonable number of threads
        // (number of available processors, but at least 2)
        int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executor = Executors.newFixedThreadPool(numThreads);
    }
    
    /**
     * Set a callback to be called whenever progress is made
     * 
     * @param callback Consumer taking an integer representing progress percentage (0-100)
     * @return this BatchProcessor for method chaining
     */
    public BatchProcessor onProgress(Consumer<Integer> callback) {
        this.progressCallback = callback;
        return this;
    }
    
    /**
     * Set a callback to be called when processing is complete
     * 
     * @param callback Consumer taking a list of successfully processed output files
     * @return this BatchProcessor for method chaining
     */
    public BatchProcessor onComplete(Consumer<List<File>> callback) {
        this.completionCallback = callback;
        return this;
    }
    
    /**
     * Set a callback to be called when an error occurs
     * 
     * @param callback Consumer taking an error message string
     * @return this BatchProcessor for method chaining
     */
    public BatchProcessor onError(Consumer<String> callback) {
        this.errorCallback = callback;
        return this;
    }
    
    /**
     * Start batch processing all files
     */
    public void process() {
        // Create a list to hold the CompletableFuture for each file processing task
        List<CompletableFuture<File>> futures = new ArrayList<>();
        // Create a list to track successfully processed output files
        List<File> outputFiles = new ArrayList<>();
        
        // Make sure output directory exists
        FileUtils.ensureDirectoryExists(outputDir);
        
        // Process each file
        for (File inputFile : inputFiles) {
            CompletableFuture<File> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Load the photo
                    Photo photo = FileUtils.loadPhoto(inputFile);
                    
                    // Step 1: Remove Background
                    BackgroundRemover remover = new BackgroundRemover(
                            backgroundSettings, 
                            "model/modnet.onnx");
                    remover.process(photo);
                    
                    // Step 2: Resize the photo
                    ImageResizer resizer = new ImageResizer(
                            targetWidth,
                            targetHeight,
                            maintainAspectRatio);
                    resizer.process(photo);
                    
                    // Step 3: Export the processed photo
                    String outputFileName = generateOutputFileName(inputFile);
                    String outputPath = outputDir + File.separator + outputFileName;
                    
                    ImageExporter exporter = new ImageExporter(exportSettings);
                    File outputFile = exporter.export(photo, outputPath);
                    
                    // Update progress
                    updateProgress();
                    
                    return outputFile;
                } catch (Exception e) {
                    // Notify about the error but allow other files to continue processing
                    if (errorCallback != null) {
                        errorCallback.accept("Error processing " + inputFile.getName() + ": " + e.getMessage());
                    }
                    // Update progress even for failed files
                    updateProgress();
                    return null;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Combine all futures and handle completion
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        
        allFutures.thenRunAsync(() -> {
            // Collect all successfully processed files (non-null results)
            for (CompletableFuture<File> future : futures) {
                try {
                    File result = future.get();
                    if (result != null) {
                        outputFiles.add(result);
                    }
                } catch (Exception e) {
                    // Individual file exceptions were already handled
                }
            }
            
            // Call completion callback with the list of successfully processed files
            if (completionCallback != null) {
                completionCallback.accept(outputFiles);
            }
            
            // Shutdown the executor
            executor.shutdown();
        });
    }
    
    /**
     * Generate an output filename based on the input file
     * 
     * @param inputFile The input file
     * @return A filename for the processed output
     */
    private String generateOutputFileName(File inputFile) {
        String baseName = inputFile.getName();
        
        // Remove the extension
        int lastDotIndex = baseName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            baseName = baseName.substring(0, lastDotIndex);
        }
        
        // Add ID photo prefix and format extension
        return exportSettings.getFileNamePrefix() + baseName + "." + 
               exportSettings.getFormat().getExtension();
    }
    
    /**
     * Update the progress counter and notify via callback
     */
    private synchronized void updateProgress() {
        processedFiles++;
        
        if (progressCallback != null) {
            int progressPercent = (processedFiles * 100) / totalFiles;
            progressCallback.accept(progressPercent);
        }
    }
}