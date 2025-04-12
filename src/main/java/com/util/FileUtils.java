package com.util;

import com.entities.Photo;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.io.IOException;

public class FileUtils {
    public static void ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public static Photo loadPhoto(File file) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            Frame frame = grabber.grabImage();
            if (frame == null) {
                throw new IOException("Failed to grab frame from file: " + file.getAbsolutePath());
            }
            return new Photo(frame, file.getName(), file);
        }
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
            return name.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public static boolean isImageFile(File file) {
        if (!file.isFile()) {
            return false;
        }

        String extension = getFileExtension(file);
        return extension.equals("jpg") ||
                extension.equals("jpeg") ||
                extension.equals("png") ||
                extension.equals("bmp") ||
                extension.equals("gif");
    }
}
