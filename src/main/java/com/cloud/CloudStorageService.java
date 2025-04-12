package com.cloud;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Interface defining common operations for cloud storage services
 */
public interface CloudStorageService {
    /**
     * Upload a file to cloud storage
     * 
     * @param file       The local file to upload
     * @param remotePath The path/name in cloud storage
     * @return A CompletableFuture with the public URL of the uploaded file
     */
    CompletableFuture<String> uploadFile(File file, String remotePath);

    /**
     * Get the display name of this cloud service
     */
    String getServiceName();

    /**
     * Check if the service is authenticated and ready to use
     */
    boolean isAuthenticated();

    /**
     * Start the authentication process
     * 
     * @return true if authentication succeeded or is in progress
     */
    boolean authenticate();
}