package com.models;

import java.awt.Point;

/**
 * Represents a marked point with its own brush size for image segmentation and refinement.
 */
public class MarkedPoint {
    private final Point point;
    private final int brushSize;

    /**
     * Constructs a MarkedPoint with a specific point and brush size.
     * 
     * @param point The coordinates of the marked point
     * @param brushSize The size of the brush used to mark the point
     */
    public MarkedPoint(Point point, int brushSize) {
        this.point = new Point(point); // Create a defensive copy
        this.brushSize = brushSize;
    }

    /**
     * Get the point's coordinates.
     * 
     * @return A copy of the point to prevent direct modification
     */
    public Point getPoint() {
        return new Point(point);
    }

    /**
     * Get the brush size used for marking.
     * 
     * @return The brush size in pixels
     */
    public int getBrushSize() {
        return brushSize;
    }
}