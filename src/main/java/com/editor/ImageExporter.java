package com.editor;

import java.io.File;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;

import com.entities.ExportSettings;
import com.entities.Photo;
import com.util.FileUtils;

public class ImageExporter {
    private final ExportSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    public ImageExporter(ExportSettings settings) {
        this.settings = settings;
    }

    public File export(Photo photo, String outputPath) throws Exception {
        Frame frame = photo.getProcessedFrame();

        // Convert to Mat for processing
        Mat image = converter.convert(frame);

        // Use the provided output path
        File outputFile = new File(outputPath);

        // Ensure output directory exists
        FileUtils.ensureDirectoryExists(outputFile.getParent());

        // Save the image
        if (settings.isGenerateMultiples()) {
            // Create a grid of images
            Mat gridImage = createImageGrid(image);
            opencv_imgcodecs.imwrite(outputFile.getAbsolutePath(), gridImage);
            gridImage.release();
        } else {
            // Save single image
            opencv_imgcodecs.imwrite(outputFile.getAbsolutePath(), image);
        }

        return outputFile;
    }

    private Mat createImageGrid(Mat singleImage) {
        int rows = settings.getRows();
        int columns = settings.getColumns();

        // Calculate dimensions for the grid
        int gridWidth = singleImage.cols() * columns;
        int gridHeight = singleImage.rows() * rows;

        // Create empty grid image (white background)
        Mat gridImage = new Mat(gridHeight, gridWidth, singleImage.type(), new Scalar(255, 255, 255, 255));

        // Copy the single image to each cell in the grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int x = c * singleImage.cols();
                int y = r * singleImage.rows();

                Mat roi = new Mat(gridImage, new Rect(x, y, singleImage.cols(), singleImage.rows()));
                singleImage.copyTo(roi);
                roi.release();
            }
        }

        return gridImage;
    }
}
