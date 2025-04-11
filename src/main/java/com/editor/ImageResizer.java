package com.editor;

import java.awt.Color;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

public class ImageResizer implements ImageProcessor {

    private final int targetWidth;
    private final int targetHeight;
    private final boolean maintainAspectRatio;
    private Rect cropRect;
    private final Color backgroundColor;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    /**
     * Constructor for simple resizing (scaling) with default white background
     */
    public ImageResizer(int targetWidth, int targetHeight, boolean maintainAspectRatio) {
        this(targetWidth, targetHeight, maintainAspectRatio, Color.WHITE);
    }

    /**
     * Constructor for simple resizing (scaling) with custom background color
     */
    public ImageResizer(int targetWidth, int targetHeight, boolean maintainAspectRatio, Color backgroundColor) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maintainAspectRatio = maintainAspectRatio;
        this.cropRect = null; // No cropping, just resize
        this.backgroundColor = backgroundColor;
    }

    /**
     * Constructor for crop + resize operations with default white background
     */
    public ImageResizer(Rect cropRect, int targetWidth, int targetHeight, boolean maintainAspectRatio) {
        this(cropRect, targetWidth, targetHeight, maintainAspectRatio, Color.WHITE);
    }

    /**
     * Constructor for crop + resize operations with custom background color
     */
    public ImageResizer(Rect cropRect, int targetWidth, int targetHeight, boolean maintainAspectRatio, Color backgroundColor) {
        this.cropRect = cropRect;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maintainAspectRatio = maintainAspectRatio;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public Frame process(Frame frame) {
        if (frame == null) throw new IllegalArgumentException("Input frame is null");
    
        Mat image = converter.convert(frame);
        if (image == null || image.empty()) throw new IllegalArgumentException("Converted image is empty");
    
        // --- STEP 1: Center crop around face if available ---
        Rect face = detectFace(image);
        if (face != null) {
            int centerX = face.x() + face.width() / 2;
            int centerY = face.y() + face.height() / 2;
    
            int cropWidth = Math.min(targetWidth, image.cols());
            int cropHeight = Math.min(targetHeight, image.rows());
    
            int x = Math.max(0, centerX - cropWidth / 2);
            int y = Math.max(0, centerY - cropHeight / 2);
    
            // Clamp to image bounds
            if (x + cropWidth > image.cols()) x = image.cols() - cropWidth;
            if (y + cropHeight > image.rows()) y = image.rows() - cropHeight;
    
            cropRect = new Rect(x, y, cropWidth, cropHeight);
            image = new Mat(image, cropRect);
        }
    
        // --- STEP 2: Resize with aspect ratio and padding ---
        Mat resizedImage;
        if (maintainAspectRatio) {
            double scale = Math.min((double) targetWidth / image.cols(), (double) targetHeight / image.rows());
            int newWidth = (int) (image.cols() * scale);
            int newHeight = (int) (image.rows() * scale);
    
            Mat scaledImage = new Mat();
            opencv_imgproc.resize(image, scaledImage, new Size(newWidth, newHeight));
    
            Mat paddedImage = new Mat(targetHeight, targetWidth, image.type(),
                new org.bytedeco.opencv.opencv_core.Scalar(
                    backgroundColor.getBlue(),
                    backgroundColor.getGreen(),
                    backgroundColor.getRed(),
                    255
                )
            );
    
            int xOffset = (targetWidth - newWidth) / 2;
            int yOffset = (targetHeight - newHeight) / 2;
            Mat roi = new Mat(paddedImage, new Rect(xOffset, yOffset, newWidth, newHeight));
            scaledImage.copyTo(roi);
    
            resizedImage = paddedImage;
        } else {
            resizedImage = new Mat();
            opencv_imgproc.resize(image, resizedImage, new Size(targetWidth, targetHeight));
        }
    
        return converter.convert(resizedImage);
    }
    

    private Rect detectFace(Mat image) {
        // Load the cascade file (make sure to use the correct path for the cascade file)
        CascadeClassifier faceCascade = new CascadeClassifier("./facialDetectionResources/haarcascade_frontalface_alt2.xml");
        Mat grayImage = new Mat();
        opencv_imgproc.cvtColor(image, grayImage, opencv_imgproc.COLOR_BGR2GRAY);

        // Detect faces
        RectVector faces = new RectVector();
        faceCascade.detectMultiScale(grayImage, faces);

        if (faces.size() > 0) {
            // If faces are detected, return the largest one (or you can refine this logic)
            return faces.get(0);
        }

        return null; // No face detected
    }

}
