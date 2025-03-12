package com.editor;

import com.entities.ExportSettings;
import com.entities.Photo;
import com.util.FileUtils;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImageExporter {
    private final ExportSettings settings;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    
    public ImageExporter(ExportSettings settings) {
        this.settings = settings;
    }
    
    public File export(Photo photo) throws Exception {
        Frame frame = photo.getProcessedFrame();
        
        // Convert to Mat for processing
        Mat image = converter.convert(frame);
        
        // Create the output file
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = settings.getFileNamePrefix() + timestamp + "." + settings.getFormat().getExtension();
        File outputFile = new File(settings.getOutputDirectory(), fileName);
        
        // Ensure output directory exists
        FileUtils.ensureDirectoryExists(settings.getOutputDirectory());
        
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
