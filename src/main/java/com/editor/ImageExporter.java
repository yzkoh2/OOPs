package com.editor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
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
        Mat imageMat = converter.convert(frame);
    
        // Convert Mat to BufferedImage
        Java2DFrameConverter java2DConverter = new Java2DFrameConverter();
        BufferedImage image = java2DConverter.convert(frame);
    
        // Ensure output directory exists
        File outputFile = new File(outputPath);
        FileUtils.ensureDirectoryExists(outputFile.getParent());
    
        // Convert to 3BYTE_BGR for compatibility with all formats (esp. JPEG)
        BufferedImage bgrImage = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_3BYTE_BGR
        );
        Graphics2D g = bgrImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
    
        // Save image using selected format
        String format = settings.getFormat().getExtension(); // "jpg", "png", etc.
        ImageIO.write(bgrImage, format, outputFile);
    
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
