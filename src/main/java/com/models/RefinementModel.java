package com.models;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model representing the state of image refinement markings.
 */
public class RefinementModel {
    // Enum for marking modes
    public enum MarkingMode {
        FOREGROUND,
        BACKGROUND
    }

    // Current marking mode
    private MarkingMode currentMode = MarkingMode.FOREGROUND;

    // Lists to store marked points
    private final List<MarkedPoint> foregroundPoints = new ArrayList<>();
    private final List<MarkedPoint> backgroundPoints = new ArrayList<>();

    // Current brush size
    private int brushSize = 10;

    /**
     * Add a marked point to the appropriate list based on current mode.
     * 
     * @param point The point to mark
     */
    public void addMarkedPoint(Point point) {
        MarkedPoint markedPoint = new MarkedPoint(point, brushSize);
        
        if (currentMode == MarkingMode.FOREGROUND) {
            foregroundPoints.add(markedPoint);
        } else {
            backgroundPoints.add(markedPoint);
        }
    }

    /**
     * Get unmodifiable list of foreground points.
     * 
     * @return List of foreground marked points
     */
    public List<MarkedPoint> getForegroundPoints() {
        return Collections.unmodifiableList(foregroundPoints);
    }

    /**
     * Get unmodifiable list of background points.
     * 
     * @return List of background marked points
     */
    public List<MarkedPoint> getBackgroundPoints() {
        return Collections.unmodifiableList(backgroundPoints);
    }

    /**
     * Clear all markings.
     */
    public void clearMarkings() {
        foregroundPoints.clear();
        backgroundPoints.clear();
    }

    /**
     * Set the current marking mode.
     * 
     * @param mode The marking mode to set
     */
    public void setCurrentMode(MarkingMode mode) {
        this.currentMode = mode;
    }

    /**
     * Get the current marking mode.
     * 
     * @return Current marking mode
     */
    public MarkingMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Set the current brush size.
     * 
     * @param size Brush size in pixels
     */
    public void setBrushSize(int size) {
        if (size > 0) {
            this.brushSize = size;
        }
    }

    /**
     * Get the current brush size.
     * 
     * @return Current brush size in pixels
     */
    public int getBrushSize() {
        return brushSize;
    }

    /**
     * Extract points for processing.
     * 
     * @return List of points to be processed
     */
    public List<Point> extractForegroundPoints() {
        return foregroundPoints.stream()
            .map(MarkedPoint::getPoint)
            .collect(Collectors.toList());
    }
    /**
     * Extract points for processing.
     * 
     * @return List of points to be processed
     */
    public List<Point> extractBackgroundPoints() {
        return backgroundPoints.stream()
            .map(MarkedPoint::getPoint)
            .collect(Collectors.toList());
    }

    /**
     * Extract brush sizes for foreground points.
     * 
     * @return List of brush sizes
     */

     public List<Integer> extractForegroundBrushSizes() {
        return foregroundPoints.stream()
            .map(MarkedPoint::getBrushSize)
            .collect(Collectors.toList());
    }
    /**
     * Extract brush sizes for background points.
     * 
     * @return List of brush sizes
     */
    public List<Integer> extractBackgroundBrushSizes() {
        return backgroundPoints.stream()
            .map(MarkedPoint::getBrushSize)
            .collect(Collectors.toList());
    }
}