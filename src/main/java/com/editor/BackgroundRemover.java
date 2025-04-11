package com.editor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import com.entities.BackgroundSettings;
import com.entities.BackgroundSettings.BackgroundType;
import com.entities.Photo;

import ai.onnxruntime.OrtException;

public class BackgroundRemover implements ImageProcessor {

    private final BackgroundSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
    private ONNXMattePredictor mattePredictor;
    private boolean modelInitialized = false;
    private String modelPath;
    private Mat backgroundImage = null;

    /**
     * Creates a new BackgroundRemover with the specified settings and model path.
     * The model is loaded lazily when first needed.
     * 
     * @param settings Background settings (color, image path, etc.)
     * @param modelPath Path to the ONNX model file
     */
    public BackgroundRemover(BackgroundSettings settings, String modelPath) {
        this.settings = settings;
        this.modelPath = modelPath;
        
        // Load background image if one is specified
        if (settings.getType() == BackgroundType.CUSTOM_IMAGE && 
            settings.getBackgroundImagePath() != null && 
            !settings.getBackgroundImagePath().isEmpty()) {
            loadBackgroundImage();
        }
    }
    
    /**
     * Load the background image from the path specified in settings
     */
    private void loadBackgroundImage() {
        String imagePath = settings.getBackgroundImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                // Check if file exists
                File bgFile = new File(imagePath);
                if (!bgFile.exists() || !bgFile.isFile()) {
                    System.err.println("Background image not found at: " + imagePath);
                    return;
                }
                
                // Load the image using OpenCV
                backgroundImage = opencv_imgcodecs.imread(imagePath);
                
                // Verify the image was loaded
                if (backgroundImage.empty()) {
                    System.err.println("Failed to load background image: " + imagePath);
                    backgroundImage = null;
                } else {
                    System.out.println("Background image loaded successfully: " + imagePath);
                }
            } catch (Exception e) {
                System.err.println("Error loading background image: " + e.getMessage());
                backgroundImage = null;
            }
        }
    }
    
    /**
     * Initialize the model if not already initialized
     */
    private synchronized void initializeModelIfNeeded() throws OrtException {
        if (!modelInitialized) {
            try {
                // Check if model file exists
                File modelFile = new File(modelPath);
                if (!modelFile.exists()) {
                    // Try to find the model in the classpath or relative to the working directory
                    String alternativePath = Paths.get(System.getProperty("user.dir"), modelPath).toString();
                    modelFile = new File(alternativePath);
                    
                    if (!modelFile.exists()) {
                        throw new OrtException("Model file not found at: " + modelPath + 
                                " or " + alternativePath);
                    }
                    
                    // Update path to the found location
                    this.modelPath = alternativePath;
                }
                
                // Initialize the model
                this.mattePredictor = new ONNXMattePredictor(modelPath);
                this.modelInitialized = true;
                System.out.println("ONNX model initialized successfully from: " + modelPath);
            } catch (OrtException e) {
                System.err.println("Failed to initialize model: " + e.getMessage());
                throw e; // Re-throw to let caller handle it
            }
        }
    }

    @Override
    public Photo process(Photo photo) {
        Frame frame = photo.getProcessedFrame().clone();
        Frame processed = process(frame);
        photo.setProcessedFrame(processed);
        return photo;
    }

    public Frame process(Frame frame) {
        Mat image = converter.convert(frame);
        
        try {
            // Ensure model is initialized
            initializeModelIfNeeded();
            
            // Get alpha matte prediction (transparency mask)
            float[][] alphaMatte = mattePredictor.predictAlphaMatte(image);

            // Resize input image for processing if needed
            Mat resized = new Mat();
            opencv_imgproc.resize(image, resized, new Size(512, 512));

            // Create output image with the same size as the input
            Mat composite = new Mat(resized.size(), resized.type());
            UByteIndexer imgIdx = resized.createIndexer();
            UByteIndexer compIdx = composite.createIndexer();
            
            // Handle custom background image if configured
            if (settings.getType() == BackgroundType.CUSTOM_IMAGE && backgroundImage != null) {
                // Resize background image to match the target size
                Mat resizedBg = new Mat();
                opencv_imgproc.resize(backgroundImage, resizedBg, new Size(512, 512));
                UByteIndexer bgIdx = resizedBg.createIndexer();
                
                // Apply alpha compositing with the background image
                for (int y = 0; y < 512; y++) {
                    for (int x = 0; x < 512; x++) {
                        float alpha = alphaMatte[y][x];
                        
                        // RGB order in OpenCV is BGR
                        int r = (int) (imgIdx.get(y, x, 2) * alpha + bgIdx.get(y, x, 2) * (1 - alpha));
                        int g = (int) (imgIdx.get(y, x, 1) * alpha + bgIdx.get(y, x, 1) * (1 - alpha));
                        int b = (int) (imgIdx.get(y, x, 0) * alpha + bgIdx.get(y, x, 0) * (1 - alpha));
                        
                        // Ensure valid color ranges
                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));
                        
                        compIdx.put(y, x, 2, r);
                        compIdx.put(y, x, 1, g);
                        compIdx.put(y, x, 0, b);
                    }
                }
                
                // Cleanup background resources
                bgIdx.release();
                resizedBg.release();
            }
            else {
                // Fall back to solid color background
                Color bgColor = settings.getBackgroundColor();
                int bgR = bgColor.getRed();
                int bgG = bgColor.getGreen();
                int bgB = bgColor.getBlue();
                
                // Apply alpha compositing with the selected background color
                for (int y = 0; y < 512; y++) {
                    for (int x = 0; x < 512; x++) {
                        float alpha = alphaMatte[y][x];
                        
                        // RGB order in OpenCV is BGR
                        int r = (int) (imgIdx.get(y, x, 2) * alpha + bgR * (1 - alpha));
                        int g = (int) (imgIdx.get(y, x, 1) * alpha + bgG * (1 - alpha));
                        int b = (int) (imgIdx.get(y, x, 0) * alpha + bgB * (1 - alpha));
                        
                        // Ensure valid color ranges
                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));
                        
                        compIdx.put(y, x, 2, r);
                        compIdx.put(y, x, 1, g);
                        compIdx.put(y, x, 0, b);
                    }
                }
            }

            // Cleanup resources
            imgIdx.release();
            compIdx.release();

            // Resize back to original dimensions before returning
            Mat finalImage = new Mat();
            opencv_imgproc.resize(composite, finalImage, image.size());
            
            // Cleanup resources
            composite.release();
            resized.release();
            
            Frame result = converter.convert(finalImage);
            finalImage.release();
            image.release();
            
            return result;

        } catch (OrtException e) {
            e.printStackTrace();
            System.err.println("Error in background removal: " + e.getMessage());
            // Ensure resources are released even on error
            image.release();
            return frame; // Return original frame if processing fails
        }
    }
    
    /**
     * Releases resources associated with this BackgroundRemover
     */
    public void close() {
        // Release the background image if loaded
        if (backgroundImage != null) {
            backgroundImage.release();
            backgroundImage = null;
        }
        
        // No explicit close method in ONNXMattePredictor yet, but would be good to add
        modelInitialized = false;
        mattePredictor = null;
        // System.gc() is generally not recommended but could be useful here
        // to ensure ONNX resources are released, especially during batch processing
        System.gc();
    }
}