package com.editor;

import com.entities.Photo;
import java.util.LinkedList;
import java.util.Deque;

/**
 * Manages a history of Photo states for implementing undo functionality
 */
public class PhotoHistory {
    private final Deque<Photo> history;
    private final int maxHistorySize;
    
    /**
     * Creates a new PhotoHistory with default max size
     */
    public PhotoHistory() {
        this(10); // Default to storing 10 history states
    }
    
    /**
     * Creates a new PhotoHistory with specified max size
     * @param maxHistorySize Maximum number of history states to keep
     */
    public PhotoHistory(int maxHistorySize) {
        this.history = new LinkedList<>();
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Save a photo state to history
     * @param photo The photo to save (will be cloned)
     */
    public void saveState(Photo photo) {
        if (photo == null) {
            return;
        }
        
        // Create a deep copy of the photo to store in history
        Photo clonedPhoto = photo.clone();
        
        // Add to history
        history.push(clonedPhoto);
        
        // Enforce maximum history size
        while (history.size() > maxHistorySize) {
            history.removeLast();
        }
    }
    
    /**
     * Restore the previous state
     * @return The previous Photo state, or null if history is empty
     */
    public Photo undo() {
        if (history.isEmpty()) {
            return null;
        }
        
        return history.pop();
    }
    
    /**
     * Check if there are any states to undo
     * @return true if there are states in history, false otherwise
     */
    public boolean canUndo() {
        return !history.isEmpty();
    }
    
    /**
     * Clear all history
     */
    public void clear() {
        history.clear();
    }
}