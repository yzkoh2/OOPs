package com.editor;

import java.awt.Color;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import com.entities.BackgroundSettings;
import com.entities.Photo;

import ai.onnxruntime.OrtException;

public class BackgroundRemover implements ImageProcessor {

    private final BackgroundSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
    private final ONNXMattePredictor mattePredictor;

    public BackgroundRemover(BackgroundSettings settings, String modelPath) throws OrtException {
        this.settings = settings;
        this.mattePredictor = new ONNXMattePredictor(modelPath);
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
            // Get alpha matte prediction (transparency mask)
            float[][] alphaMatte = mattePredictor.predictAlphaMatte(image);

            // Resize input image for processing if needed
            Mat resized = new Mat();
            opencv_imgproc.resize(image, resized, new Size(512, 512));

            // Create output image with the same size as the input
            Mat composite = new Mat(resized.size(), resized.type());
            UByteIndexer imgIdx = resized.createIndexer();
            UByteIndexer compIdx = composite.createIndexer();

            // Get background color from settings (user selected)
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

            imgIdx.release();
            compIdx.release();

            // Resize back to original dimensions before returning
            Mat finalImage = new Mat();
            opencv_imgproc.resize(composite, finalImage, image.size());
            composite.release();
            resized.release();

            return converter.convert(finalImage);

        } catch (OrtException e) {
            e.printStackTrace();
            System.err.println("Error in background removal: " + e.getMessage());
            return frame; // Return original frame if processing fails
        }
    }
}