package com.editor;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

public class ImageResizer implements ImageProcessor {
    private final int targetWidth;
    private final int targetHeight;
    private final boolean maintainAspectRatio;
    private final Rect cropRect;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    
    public ImageResizer(int targetWidth, int targetHeight, boolean maintainAspectRatio) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maintainAspectRatio = maintainAspectRatio;
        this.cropRect = null; // No cropping, just resize
    }
    
    public ImageResizer(Rect cropRect, int targetWidth, int targetHeight, boolean maintainAspectRatio) {
        this.cropRect = cropRect;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maintainAspectRatio = maintainAspectRatio;
    }
    
    @Override
    public Frame process(Frame frame) {
        Mat image = converter.convert(frame);
        Mat result;
        
        // First crop if a crop rectangle is specified
        if (cropRect != null) {
            // Ensure crop rectangle is within image bounds
            Rect validRect = new Rect(
                Math.max(0, cropRect.x()),
                Math.max(0, cropRect.y()),
                Math.min(cropRect.width(), image.cols() - cropRect.x()),
                Math.min(cropRect.height(), image.rows() - cropRect.y())
            );
            
            Mat croppedImage = new Mat(image, validRect);
            result = croppedImage.clone();
            croppedImage.release();
        } else {
            result = image.clone();
        }
        
        // Then resize
        Mat resizedImage = new Mat();
        
        if (maintainAspectRatio) {
            // Calculate the scaling factor
            double widthRatio = (double) targetWidth / result.cols();
            double heightRatio = (double) targetHeight / result.rows();
            double scaleFactor = Math.min(widthRatio, heightRatio);
            
            int newWidth = (int) (result.cols() * scaleFactor);
            int newHeight = (int) (result.rows() * scaleFactor);
            
            opencv_imgproc.resize(result, resizedImage, new Size(newWidth, newHeight), 0, 0, opencv_imgproc.INTER_AREA);
            
            // If we need to pad to reach exact target dimensions
            if (newWidth != targetWidth || newHeight != targetHeight) {
                Mat paddedImage = new Mat(targetHeight, targetWidth, result.type(), new Scalar(255, 255, 255, 255));
                int x = (targetWidth - newWidth) / 2;
                int y = (targetHeight - newHeight) / 2;
                
                Mat roi = new Mat(paddedImage, new Rect(x, y, newWidth, newHeight));
                resizedImage.copyTo(roi);
                roi.release();
                
                Mat temp = resizedImage;
                resizedImage = paddedImage;
                temp.release();
            }
        } else {
            // Just resize to target dimensions without preserving aspect ratio
            opencv_imgproc.resize(result, resizedImage, new Size(targetWidth, targetHeight), 0, 0, opencv_imgproc.INTER_AREA);
        }
        
        result.release();
        Frame outputFrame = converter.convert(resizedImage);
        resizedImage.release();
        
        return outputFrame;
    }
}
