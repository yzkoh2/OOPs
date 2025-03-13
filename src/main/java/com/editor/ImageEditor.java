package com.editor;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

public class ImageEditor {
    private final OpenCVFrameConverter.ToMat converter;
    private final Java2DFrameConverter java2dConverter;

    public ImageEditor() {
        converter = new OpenCVFrameConverter.ToMat();
        java2dConverter = new Java2DFrameConverter();
    }

    /**
     * Resizes an image to the target dimensions, optionally preserving aspect ratio.
     * 
     * @param image The image to resize
     * @param targetWidth The target width
     * @param targetHeight The target height
     * @param preserveRatio Whether to preserve aspect ratio
     * @return The resized image
     */
    public Mat resizeImage(Mat image, int targetWidth, int targetHeight, boolean preserveRatio) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("Empty or null image provided");
        }

        // Calculate dimensions
        double width = targetWidth;
        double height = targetHeight;

        if (preserveRatio) {
            double ratio = image.cols() / (double) image.rows();

            // Adjust height or width to maintain aspect ratio
            if (width / height > ratio) {
                width = height * ratio;
            } else {
                height = width / ratio;
            }
        }

        // Create a new image with the target dimensions using OpenCV
        Mat resizedMat = new Mat();
        resize(image, resizedMat, new Size((int) width, (int) height));

        return resizedMat;
    }

    /**
     * Crops an image according to the given rectangle.
     * 
     * @param image The image to crop
     * @param rect The rectangle defining the crop area
     * @param zoomLevel The current zoom level, used to adjust the crop coordinates
     * @param panelWidth The width of the panel containing the image
     * @param panelHeight The height of the panel containing the image
     * @param displayWidth The width of the displayed image (may differ from image width due to zoom)
     * @param displayHeight The height of the displayed image (may differ from image height due to zoom)
     * @return The cropped image
     */
    public Mat cropImage(Mat image, Rectangle rect, double zoomLevel, 
                         int panelWidth, int panelHeight, 
                         int displayWidth, int displayHeight) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("Empty or null image provided");
        }

        if (rect == null || rect.width <= 0 || rect.height <= 0) {
            throw new IllegalArgumentException("Invalid crop rectangle");
        }

        // First, adjust for the image position within the panel
        int imgX = Math.max(0, (panelWidth - displayWidth) / 2);
        int imgY = Math.max(0, (panelHeight - displayHeight) / 2);

        // Adjust crop coordinates relative to image position
        int x = rect.x - imgX;
        int y = rect.y - imgY;

        // Check if crop rectangle is outside image bounds
        if (x < 0 || y < 0 || x > displayWidth || y > displayHeight) {
            throw new IllegalArgumentException("Crop area must be within image boundaries");
        }

        // Now adjust for zoom level
        x = (int) (x / zoomLevel);
        y = (int) (y / zoomLevel);
        int width = (int) (rect.width / zoomLevel);
        int height = (int) (rect.height / zoomLevel);

        // Check bounds
        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid crop dimensions");
        }
        if (x + width > image.cols())
            width = image.cols() - x;
        if (y + height > image.rows())
            height = image.rows() - y;

        // Create a new OpenCV Mat for the cropped region
        Rect cropRect = new Rect(x, y, width, height);

        // Check if the rect is valid
        if (cropRect.x() < 0 || cropRect.y() < 0 ||
                cropRect.width() <= 0 || cropRect.height() <= 0 ||
                cropRect.x() + cropRect.width() > image.cols() ||
                cropRect.y() + cropRect.height() > image.rows()) {
            throw new IllegalArgumentException("Invalid crop area");
        }

        return new Mat(image, cropRect);
    }

    /**
     * Converts a Mat to a BufferedImage
     * 
     * @param mat The Mat to convert
     * @return The converted BufferedImage
     */
    public BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            throw new IllegalArgumentException("Empty or null mat provided");
        }
        
        org.bytedeco.javacv.Frame frame = converter.convert(mat);
        return java2dConverter.getBufferedImage(frame);
    }

    /**
     * Converts a BufferedImage to a Mat
     * 
     * @param image The BufferedImage to convert
     * @return The converted Mat
     */
    public Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Null image provided");
        }
        
        org.bytedeco.javacv.Frame frame = java2dConverter.convert(image);
        return converter.convert(frame);
    }
}