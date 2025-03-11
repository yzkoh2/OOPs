package com.editor;

import org.bytedeco.opencv.opencv_core.Mat;

/**
 * Interface for all image processing operations.
 */
public interface ImageProcessor {
    /**
     * Process an input image and return the processed result.
     * 
     * @param inputImage The input image to process
     * @return The processed image
     */
    Mat process(Mat inputImage);
}