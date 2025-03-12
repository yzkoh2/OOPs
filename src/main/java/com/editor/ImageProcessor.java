package com.editor;

import com.entities.Photo;
import org.bytedeco.javacv.Frame;

public interface ImageProcessor {
    /**
     * Process a frame and return the processed frame
     * @param frame Input frame to process
     * @return Processed frame
     */
    Frame process(Frame frame);
    
    /**
     * Process a photo object
     * @param photo Photo object containing the frame to process
     * @return Updated photo with processed frame
     */
    default Photo process(Photo photo) {
        Frame processedFrame = process(photo.getProcessedFrame().clone());
        photo.setProcessedFrame(processedFrame);
        return photo;
    }
}
