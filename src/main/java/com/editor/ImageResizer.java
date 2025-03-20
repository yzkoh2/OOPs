package com.editor;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;


public class ImageResizer implements ImageProcessor {

    private final int targetWidth;
    private final int targetHeight;
    private final boolean maintainAspectRatio;
    private Rect cropRect;
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
        // Mat image = converter.convert(frame);
        // Mat result;

        // // Step 1: Detect face if not given a cropRect
        // if (cropRect == null) {
        //     // Detect face using Haar cascade
        //     CascadeClassifier faceCascade = new CascadeClassifier("path_to_haarcascade_frontalface_default.xml");
        //     Mat grayImage = new Mat();
        //     opencv_imgproc.cvtColor(image, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
        //     RectVector faces = new RectVector();
        //     faceCascade.detectMultiScale(grayImage, faces);
        //     // If faces are detected, choose the largest face
        //     if (faces.size() > 0) {
        //         Rect face = faces.get(0);
        //         for (int i = 1; i < faces.size(); i++) {
        //             if (faces.get(i).width() * faces.get(i).height() > face.width() * face.height()) {
        //                 face = faces.get(i);
        //             }
        //         }
        //         // Use the face rectangle as the crop region
        //         this.cropRect = face;
        //     }
        //     grayImage.release();
        // }
        // // First crop if a crop rectangle is specified
        // if (cropRect != null) {
        //     // Ensure crop rectangle is within image bounds
        //     Rect validRect = new Rect(
        //             Math.max(0, cropRect.x()),
        //             Math.max(0, cropRect.y()),
        //             Math.min(cropRect.width(), image.cols() - cropRect.x()),
        //             Math.min(cropRect.height(), image.rows() - cropRect.y())
        //     );
        //     Mat croppedImage = new Mat(image, validRect);
        //     result = croppedImage.clone();
        //     croppedImage.release();
        // } else {
        //     result = image.clone();
        // }
        // // Then resize
        // Mat resizedImage = new Mat();
        // if (maintainAspectRatio) {
        //     // Calculate the scaling factor
        //     double widthRatio = (double) targetWidth / result.cols();
        //     double heightRatio = (double) targetHeight / result.rows();
        //     double scaleFactor = Math.min(widthRatio, heightRatio);
        //     int newWidth = (int) (result.cols() * scaleFactor);
        //     int newHeight = (int) (result.rows() * scaleFactor);
        //     opencv_imgproc.resize(result, resizedImage, new Size(newWidth, newHeight), 0, 0, opencv_imgproc.INTER_AREA);
        //     // If we need to pad to reach exact target dimensions
        //     if (newWidth != targetWidth || newHeight != targetHeight) {
        //         Mat paddedImage = new Mat(targetHeight, targetWidth, result.type(), new Scalar(255, 255, 255, 255));
        //         int x = (targetWidth - newWidth) / 2;
        //         int y = (targetHeight - newHeight) / 2;
        //         Mat roi = new Mat(paddedImage, new Rect(x, y, newWidth, newHeight));
        //         resizedImage.copyTo(roi);
        //         roi.release();
        //         Mat temp = resizedImage;
        //         resizedImage = paddedImage;
        //         temp.release();
        //     }
        // } else {
        //     // Just resize to target dimensions without preserving aspect ratio
        //     opencv_imgproc.resize(result, resizedImage, new Size(targetWidth, targetHeight), 0, 0, opencv_imgproc.INTER_AREA);
        // }
        // result.release();
        // Frame outputFrame = converter.convert(resizedImage);
        // resizedImage.release();
        // return outputFrame;
        Mat image = converter.convert(frame);

        // Face detection part
        Rect face = detectFace(image); // Implement this method to get the face's location
        if (face != null) {
            // Calculate the center of the face
            int centerX = face.x() + face.width() / 2;
            int centerY = face.y() + face.height() / 2;

            // Define the new center-aligned crop rectangle
            int cropWidth = targetWidth;
            int cropHeight = targetHeight;

            // Shift the crop rectangle so that the face is centered
            int xOffset = centerX - cropWidth / 2;
            int yOffset = centerY - cropHeight / 2;

            // Ensure the crop rectangle stays within the bounds of the image
            xOffset = Math.max(0, Math.min(xOffset, image.cols() - cropWidth));
            yOffset = Math.max(0, Math.min(yOffset, image.rows() - cropHeight));

            // Update the crop rectangle
            Rect newCropRect = new Rect(xOffset, yOffset, cropWidth, cropHeight);

            // Crop the image to the new rectangle
            Mat croppedImage = new Mat(image, newCropRect);

            // Resize and process as needed
            Mat resizedImage = new Mat();
            opencv_imgproc.resize(croppedImage, resizedImage, new Size(targetWidth, targetHeight));

            // Return the resized image
            return converter.convert(resizedImage);
        }

        // If no face detected, just return the original image (or handle accordingly)
        return frame;
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

    public Frame resizeToIDPhotoSize(Frame frame) {
        Mat image = converter.convert(frame);

        // Define the target size (passport photo size)
        int widthMm = 35; // Passport photo width (in mm)
        int heightMm = 45; // Passport photo height (in mm)
        int dpi = 300; // DPI for passport photo
    
        // Convert to pixels based on DPI
        int pixelsPerMm = dpi / 25; // 25.4mm per inch, simplified to 25
        int widthPx = widthMm * pixelsPerMm;
        int heightPx = heightMm * pixelsPerMm;
    
        // Calculate the scaling factor
        double widthRatio = (double) widthPx / image.cols();
        double heightRatio = (double) heightPx / image.rows();
        double scaleFactor = Math.min(widthRatio, heightRatio);
    
        // Calculate the new dimensions while maintaining the aspect ratio
        int newWidth = (int) (image.cols() * scaleFactor);
        int newHeight = (int) (image.rows() * scaleFactor);
    
        // Resize the image proportionally
        Mat resizedImage = new Mat();
        opencv_imgproc.resize(image, resizedImage, new Size(newWidth, newHeight));
    
        // Create a padded image if needed
        Mat paddedImage = new Mat(heightPx, widthPx, image.type(), new Scalar(255, 255, 255,255)); // White background
        int xOffset = (widthPx - newWidth) / 2;
        int yOffset = (heightPx - newHeight) / 2;
    
        // Copy the resized image to the center of the padded image
        Mat roi = new Mat(paddedImage, new Rect(xOffset, yOffset, newWidth, newHeight));
        resizedImage.copyTo(roi);
    
        // Return the final image with padding
        return converter.convert(paddedImage);
    }
}
