package com.cloud;

import com.cloud.impl.GoogleDriveCloudStorageService;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for managing available cloud storage services
 */
public class CloudServiceFactory {
    private static CloudServiceFactory instance;
    private List<CloudStorageService> availableServices;

    private CloudServiceFactory() {
        availableServices = new ArrayList<>();
        // Add implemented cloud services
        availableServices.add(new GoogleDriveCloudStorageService());
        // Add other cloud services as needed
    }

    /**
     * Get the singleton instance of the factory
     */
    public static synchronized CloudServiceFactory getInstance() {
        if (instance == null) {
            instance = new CloudServiceFactory();
        }
        return instance;
    }

    /**
     * Get a list of all available cloud services
     */
    public List<CloudStorageService> getAvailableServices() {
        return new ArrayList<>(availableServices);
    }

    /**
     * Find a cloud service by its name
     * 
     * @param name The service name to find
     * @return The matching service or null if not found
     */
    public CloudStorageService getServiceByName(String name) {
        return availableServices.stream()
                .filter(service -> service.getServiceName().equals(name))
                .findFirst()
                .orElse(null);
    }
}