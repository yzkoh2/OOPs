package com.editor;

import com.entities.Photo;
import java.util.LinkedList;
import java.util.Deque;

/**
 * Manages a history of Photo states for implementing undo/redo functionality
 */
public class PhotoHistory {
    private final Deque<Photo> undoStack;
    private final Deque<Photo> redoStack;
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
        this.undoStack = new LinkedList<>();
        this.redoStack = new LinkedList<>();
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Save a photo state to undo history
     * @param photo The photo to save (will be cloned)
     */
    public void saveState(Photo photo) {
        if (photo == null) {
            return;
        }
        
        // Create a deep copy of the photo to store in history
        Photo clonedPhoto = photo.clone();
        
        // Add to undo stack
        undoStack.push(clonedPhoto);
        
        // Clear redo stack when a new action is performed
        redoStack.clear();
        
        // Enforce maximum history size
        while (undoStack.size() > maxHistorySize) {
            undoStack.removeLast();
        }
    }
    
    /**
     * Restore the previous state (undo)
     * @param currentPhoto The current photo state to save for potential redo
     * @return The previous Photo state, or null if history is empty
     */
    public Photo undo(Photo currentPhoto) {
        if (!canUndo()) {
            return null;
        }
        
        // Save current state to redo stack
        if (currentPhoto != null) {
            redoStack.push(currentPhoto.clone());
        }
        
        // Return the previous state
        return undoStack.pop();
    }
    
    /**
     * Restore a previously undone state (redo)
     * @param currentPhoto The current photo state to save in undo stack
     * @return The next Photo state from redo stack, or null if redo stack is empty
     */
    public Photo redo(Photo currentPhoto) {
        if (!canRedo()) {
            return null;
        }
        
        // Save current state to undo stack
        if (currentPhoto != null) {
            undoStack.push(currentPhoto.clone());
        }
        
        // Return the next state
        return redoStack.pop();
    }
    
    /**
     * Check if there are any states to undo
     * @return true if there are states in undo stack, false otherwise
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Check if there are any states to redo
     * @return true if there are states in redo stack, false otherwise
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Clear all history (both undo and redo stacks)
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}