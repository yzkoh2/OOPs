package com.editor;

import java.awt.Color;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
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

    private static final int FINAL_WIDTH = 600;
    private static final int FINAL_HEIGHT = 600;

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
            float[][] alphaMatte = mattePredictor.predictAlphaMatte(image);

            Mat resized = new Mat();
            opencv_imgproc.resize(image, resized, new Size(512, 512));

            Mat composite = new Mat(resized.size(), resized.type());
            UByteIndexer imgIdx = resized.createIndexer();
            UByteIndexer compIdx = composite.createIndexer();

            Color bgColor = settings.getBackgroundColor();
            int bgR = bgColor.getRed();
            int bgG = bgColor.getGreen();
            int bgB = bgColor.getBlue();

            for (int y = 0; y < 512; y++) {
                for (int x = 0; x < 512; x++) {
                    float alpha = alphaMatte[y][x];
                    int r = (int) (imgIdx.get(y, x, 2) * alpha + bgR * (1 - alpha));
                    int g = (int) (imgIdx.get(y, x, 1) * alpha + bgG * (1 - alpha));
                    int b = (int) (imgIdx.get(y, x, 0) * alpha + bgB * (1 - alpha));
                    compIdx.put(y, x, 2, r);
                    compIdx.put(y, x, 1, g);
                    compIdx.put(y, x, 0, b);
                }
            }

            imgIdx.release();
            compIdx.release();

            // Add padding and resize to final passport format
            Mat finalImage = new Mat(new Size(FINAL_WIDTH, FINAL_HEIGHT), composite.type(), new Scalar((double) bgB, (double) bgG, (double) bgR, 255.0)
            );
            int xOffset = (FINAL_WIDTH - 512) / 2;
            int yOffset = (FINAL_HEIGHT - 512) / 2;
            Mat roi = finalImage.apply(new Rect(xOffset, yOffset, 512, 512));

            composite.copyTo(roi);

            return converter.convert(finalImage);

        } catch (OrtException e) {
            e.printStackTrace();
            return frame;
        }
    }
}