package com.editor;

import ai.onnxruntime.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.global.opencv_imgproc;
import java.util.*;


public class ONNXMattePredictor {

    private OrtEnvironment env;
    private OrtSession session;

    public ONNXMattePredictor(String modelPath) throws OrtException {
        env = OrtEnvironment.getEnvironment();
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
    }

    public float[][] predictAlphaMatte(Mat image) throws OrtException {
        Mat resized = new Mat();
        opencv_imgproc.resize(image, resized, new org.bytedeco.opencv.opencv_core.Size(512, 512));

        float[][][][] input = new float[1][3][512][512];

        UByteIndexer indexer = resized.createIndexer();

        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                int blue  = indexer.get(y, x, 0);
                int green = indexer.get(y, x, 1);
                int red   = indexer.get(y, x, 2);

                input[0][0][y][x] = red / 255.0f;   // R
                input[0][1][y][x] = green / 255.0f; // G
                input[0][2][y][x] = blue / 255.0f;  // B
            }
        }
        indexer.release();

        OnnxTensor tensor = OnnxTensor.createTensor(env, input);
        OrtSession.Result result = session.run(Collections.singletonMap("input", tensor));

        float[][][][] output = (float[][][][]) result.get(0).getValue(); // Shape: [1][1][512][512]
        return output[0][0]; // 2D alpha matte
    }
}
