package com.editor;

import java.awt.Color;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import static org.bytedeco.opencv.global.opencv_imgproc.GC_FGD;
import static org.bytedeco.opencv.global.opencv_imgproc.GC_INIT_WITH_RECT;
import static org.bytedeco.opencv.global.opencv_imgproc.GC_PR_FGD;
import static org.bytedeco.opencv.global.opencv_imgproc.grabCut;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import com.entities.BackgroundSettings;
import com.entities.Photo;

public class BackgroundRemover implements ImageProcessor {

    private final BackgroundSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
    private Rect selectionRect = null;

    public BackgroundRemover(BackgroundSettings settings) {
        this.settings = settings;
    }

    /**
     * Set the manually drawn selection rectangle
     *
     * @param startX X coordinate of starting point
     * @param startY Y coordinate of starting point
     * @param endX X coordinate of ending point
     * @param endY Y coordinate of ending point
     */
    public void setSelectionRect(int startX, int startY, int endX, int endY) {
        // Ensure correct rectangle even if drawn from bottom-right to top-left
        int x = Math.min(startX, endX);
        int y = Math.min(startY, endY);
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);

        // Create the rectangle object
        this.selectionRect = new Rect(x, y, width, height);
    }

    /**
     * Clear the selection rectangle
     */
    public void clearSelection() {
        this.selectionRect = null;
    }

    /**
     * Check if a selection has been made
     */
    public boolean hasSelection() {
        return this.selectionRect != null;
    }

    @Override
    public Photo process(Photo photo) {
        // Get the processed frame directly instead of getting a BufferedImage
        Frame frame = photo.getProcessedFrame().clone();

        // Process the frame using your existing process method
        Frame processedFrame = process(frame);

        // Set the processed frame back to the photo
        photo.setProcessedFrame(processedFrame);

        // Return the updated photo object
        return photo;
    }

    public Frame process(Frame frame) {
        Mat image = converter.convert(frame);

        // Create mask for GrabCut
        // Step 1: Properly Initialize mask as a Single-Channel Grayscale Image
        Mat mask = new Mat(image.rows(), image.cols(), opencv_core.CV_8UC1, new Scalar(opencv_imgproc.GC_BGD));

        // Define rectangle for GrabCut - use manual selection if available, otherwise default
        Rect rectangle;
        if (selectionRect != null) {
            rectangle = selectionRect;
        } else {
            // Fall back to automatic rectangle with margin
            int margin = Math.min(image.rows(), image.cols()) / 10;
            rectangle = new Rect(
                    margin,
                    margin,
                    image.cols() - 2 * margin,
                    image.rows() - 2 * margin
            );
        }

        // Create temporary matrices for GrabCut algorithm
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        // Convert image to HSV color space
        Mat hsvImage = new Mat();
        opencv_imgproc.cvtColor(image, hsvImage, opencv_imgproc.COLOR_BGR2HSV);

// Define skin and clothing color ranges (Modify for different lighting conditions)
        Scalar lowerSkin = new Scalar(0, 30, 60, 0);
        Scalar upperSkin = new Scalar(20, 150, 255, 0);
        Scalar lowerClothes = new Scalar(0, 50, 50, 0);
        Scalar upperClothes = new Scalar(180, 255, 255, 0);

        Mat lowerSkinMat = new Mat(1, 1, opencv_core.CV_8UC3, lowerSkin);
        Mat upperSkinMat = new Mat(1, 1, opencv_core.CV_8UC3, upperSkin);
        Mat lowerClothesMat = new Mat(1, 1, opencv_core.CV_8UC3, lowerClothes);
        Mat upperClothesMat = new Mat(1, 1, opencv_core.CV_8UC3, upperClothes);

// Create masks for skin and clothing
        Mat skinMask = new Mat();
        Mat clothesMask = new Mat();
        opencv_core.inRange(hsvImage, lowerSkinMat, upperSkinMat, skinMask);
        opencv_core.inRange(hsvImage, lowerClothesMat, upperClothesMat, clothesMask);

// Combine skin and clothing masks
        Mat combinedMask = new Mat();
        opencv_core.bitwise_or(skinMask, clothesMask, combinedMask);

        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(5, 5));
        opencv_imgproc.dilate(combinedMask, combinedMask, kernel);

        // Ensure probableFgMask is the same size and type as mask
        Mat probableFgMask = new Mat(mask.size(), mask.type());
        Mat whiteMat = new Mat(combinedMask.size(), combinedMask.type(), new Scalar(255)); // Mat filled with 255
        opencv_core.compare(combinedMask, whiteMat, probableFgMask, opencv_core.CMP_EQ);

        probableFgMask.convertTo(probableFgMask, opencv_core.CV_8UC1); // Convert to 8-bit

// Step 2: Update the mask properly
        Mat fgMaskValue = new Mat(mask.size(), mask.type(), new Scalar(opencv_imgproc.GC_PR_FGD));
        fgMaskValue.copyTo(mask, probableFgMask);

        new Rect(image.cols() / 10, image.rows() / 10, image.cols() * 8 / 10, image.rows() * 8 / 10);

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
