package com.editor;

import com.entities.BackgroundSettings;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;


import java.awt.Color;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class BackgroundRemover implements ImageProcessor {
    private final BackgroundSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    public BackgroundRemover(BackgroundSettings settings) {
        this.settings = settings;
    }

    @Override
    public Frame process(Frame frame) {
        Mat image = converter.convert(frame);
        
        // Create mask for GrabCut
        Mat mask = new Mat(image.rows(), image.cols(), opencv_core.CV_8UC1, new org.bytedeco.opencv.opencv_core.Scalar(0));
        
        // Define rectangle for GrabCut (slight margin from edges)
        int margin = Math.min(image.rows(), image.cols()) / 10;
        Rect rectangle = new Rect(
            margin, 
            margin, 
            image.cols() - 2 * margin, 
            image.rows() - 2 * margin
        );
        
        // Create temporary matrices for GrabCut algorithm
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        
        // Apply GrabCut algorithm
        grabCut(image, mask, rectangle, bgModel, fgModel, 
                settings.getIterations(), GC_INIT_WITH_RECT);
        
        // Create foreground mask
        Mat foregroundMask = new Mat();
        Mat prFgdMat = new Mat(mask.size(), mask.type());
        Mat scalarMat = new Mat(1, 1, prFgdMat.type(), new org.bytedeco.opencv.opencv_core.Scalar(GC_PR_FGD));
        prFgdMat.setTo(scalarMat);
        scalarMat.release();
        opencv_core.compare(mask, prFgdMat, foregroundMask, opencv_core.CMP_EQ);
        prFgdMat.release();
        
        Mat fgdMat = new Mat(mask.size(), mask.type());
        Mat scalarMatFgd = new Mat(1, 1, fgdMat.type(), new org.bytedeco.opencv.opencv_core.Scalar(GC_FGD));
        fgdMat.setTo(scalarMatFgd);
        scalarMatFgd.release();
        opencv_core.compare(mask, fgdMat, mask, opencv_core.CMP_EQ);
        fgdMat.release();
        opencv_core.bitwise_or(foregroundMask, mask, foregroundMask);
        
        // Create foreground image
        Mat foreground = new Mat(image.size(), image.type(), new Scalar(0, 0, 0, 0));
        image.copyTo(foreground, foregroundMask);
        
        // Create background based on settings
        Mat background;
        if (settings.getType() == BackgroundSettings.BackgroundType.SOLID_COLOR) {
            Color color = settings.getBackgroundColor();
            background = new Mat(image.size(), image.type(), 
                    new Scalar(color.getBlue(), color.getGreen(), color.getRed(), 255));
        } else {
            // Load custom background image
            // This is simplified - in a real implementation you'd need to load and resize the image
            background = new Mat(image.size(), image.type(), 
                    new Scalar(255, 255, 255, 255)); // Default to white if loading fails
        }
        
        // Create inverse mask for background
        Mat backgroundMask = new Mat();
        opencv_core.bitwise_not(foregroundMask, backgroundMask);
        
        // Apply background to original image
        Mat result = new Mat(image.size(), image.type());
        background.copyTo(result, backgroundMask);
        foreground.copyTo(result, foregroundMask);
        
        // Clean up resources
        mask.release();
        bgModel.release();
        fgModel.release();
        foregroundMask.release();
        background.release();
        foreground.release();
        backgroundMask.release();
        
        return converter.convert(result);
    }
}
