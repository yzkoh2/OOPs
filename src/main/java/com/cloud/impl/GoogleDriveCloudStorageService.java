package com.cloud.impl;

import com.cloud.CloudStorageService;
import com.config.ApplicationConfig;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of CloudStorageService for Google Drive
 */
public class GoogleDriveCloudStorageService implements CloudStorageService {
    private static final String APPLICATION_NAME = "ID Photo Generator";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String TOKENS_DIRECTORY_PATH = "tokens/drive";

    private Drive driveService;
    private boolean authenticated = false;
    private String credentialsFilePath;

    public GoogleDriveCloudStorageService() {
        // Try to initialize from config
        ApplicationConfig config = ApplicationConfig.getInstance();
        credentialsFilePath = config.getProperty("cloud.google.credentialsPath", "");

        if (!credentialsFilePath.isEmpty()) {
            java.io.File credentialsFile = new java.io.File(credentialsFilePath);
            if (credentialsFile.exists()) {
                try {
                    initDriveService();
                } catch (Exception e) {
                    System.err.println("Failed to initialize Google Drive service: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Initialize the Google Drive service using OAuth 2.0
     */
    private void initDriveService() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(new FileInputStream(credentialsFilePath)));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        // Authorize
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        // Build Drive service
        driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        authenticated = true;
    }

    @Override
    public CompletableFuture<String> uploadFile(java.io.File localFile, String remotePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isAuthenticated()) {
                    throw new IllegalStateException("Not authenticated with Google Drive");
                }

                // Create file metadata
                File fileMetadata = new File();
                fileMetadata.setName(remotePath);

                // Set MIME type based on file extension
                String mimeType = Files.probeContentType(localFile.toPath());
                if (mimeType == null) {
                    // Default to binary if can't determine MIME type
                    mimeType = "application/octet-stream";
                }

                // File's content
                FileContent mediaContent = new FileContent(mimeType, localFile);

                // Upload file
                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, webViewLink")
                        .execute();

                // Make file publicly accessible (optional - removes need for Google login to
                // view)
                makeFilePublic(uploadedFile.getId());

                // Return the shareable link
                return uploadedFile.getWebViewLink();
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload to Google Drive: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Make a file publicly accessible via link
     */
    private void makeFilePublic(String fileId) throws Exception {
        // This would update the file's permissions to make it accessible to anyone with
        // the link
        com.google.api.services.drive.model.Permission permission = new com.google.api.services.drive.model.Permission()
                .setType("anyone")
                .setRole("reader");

        driveService.permissions().create(fileId, permission).execute();
    }

    /**
     * Upload a file to a specific folder in Google Drive
     */
    public CompletableFuture<String> uploadFileToFolder(java.io.File localFile, String fileName, String folderName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isAuthenticated()) {
                    throw new IllegalStateException("Not authenticated with Google Drive");
                }

                // Find or create the folder
                String folderId = findOrCreateFolder(folderName);

                // Create file metadata
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(folderId));

                // Set MIME type based on file extension
                String mimeType = Files.probeContentType(localFile.toPath());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                // File's content
                FileContent mediaContent = new FileContent(mimeType, localFile);

                // Upload file
                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, webViewLink")
                        .execute();

                // Make file publicly accessible
                makeFilePublic(uploadedFile.getId());

                // Return the shareable link
                return uploadedFile.getWebViewLink();
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload to Google Drive folder: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Find a folder by name or create it if it doesn't exist
     */
    private String findOrCreateFolder(String folderName) throws Exception {
        // Check if folder exists
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";

        // Execute the query
        com.google.api.services.drive.model.FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        // Check if folder was found
        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            // Folder exists, return its ID
            return files.get(0).getId();
        } else {
            // Folder doesn't exist, create it
            File folderMetadata = new File();
            folderMetadata.setName(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            File folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            return folder.getId();
        }
    }

    @Override
    public String getServiceName() {
        return "Google Drive";
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated && driveService != null;
    }

    @Override
    public boolean authenticate() {
        if (isAuthenticated()) {
            return true;
        }

        // For UI integration, this just returns false
        // The actual authentication is handled by setCredentialsFile
        return false;
    }

    /**
     * Set the credentials file path and initialize the Drive service
     * 
     * @param credentialsPath Path to the Google client secrets JSON file
     * @return true if authentication was successful
     */
    public boolean setCredentialsFile(String credentialsPath) {
        try {
            this.credentialsFilePath = credentialsPath;

            // Ensure tokens directory exists
            Path tokensPath = Paths.get(TOKENS_DIRECTORY_PATH);
            if (!Files.exists(tokensPath)) {
                Files.createDirectories(tokensPath);
            }

            initDriveService();

            // Save to config
            ApplicationConfig config = ApplicationConfig.getInstance();
            config.setProperty("cloud.google.credentialsPath", credentialsPath);
            config.saveConfig();

            return true;
        } catch (Exception e) {
            System.err.println("Failed to initialize Google Drive with credentials: " + e.getMessage());
            return false;
        }
    }
}