package com.editor;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import static org.bytedeco.opencv.global.opencv_imgproc.bilateralFilter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

public class PhotoEnhancer {

    private static final String FACE_CASCADE_PATH = "facialDetectionResources/haarcascade_frontalface_default.xml";

    public static Frame enhance(Frame originalFrame) throws Exception {
        if (originalFrame == null) {
            throw new IllegalArgumentException("Original frame is null.");
        }

        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        Mat inputMat = matConverter.convert(originalFrame);

        if (inputMat == null || inputMat.empty()) {
            throw new RuntimeException("Image data is empty or invalid.");
        }

        // Clone to avoid modifying original input
        Mat workingMat = inputMat.clone();

        // === Step 1: Detect face and smooth ===
        CascadeClassifier faceDetector = new CascadeClassifier(FACE_CASCADE_PATH);
        if (faceDetector.empty()) {
            throw new RuntimeException("Failed to load face cascade classifier from " + FACE_CASCADE_PATH);
        }

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(workingMat, faces);

        if (faces.size() > 0) {
            Rect face = faces.get(0); // Assume first detected face
            Mat faceROI = new Mat(workingMat, face);

            Mat smoothedFace = new Mat();
            bilateralFilter(faceROI, smoothedFace, 9, 75, 75);

            // Paste the smoothed face back into the image
            smoothedFace.copyTo(new Mat(workingMat, face));

            // Clean up
            faceROI.release();
            smoothedFace.release();
        }

        // === Step 2: Apply brightness & contrast to full image ===
        Mat enhanced = new Mat();
        workingMat.convertTo(enhanced, -1, 1.4, 20); // contrast=1.4, brightness=20

        return matConverter.convert(enhanced);
    }
}
