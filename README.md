# OOPs

# ID Photo Processor

A Java-based desktop application for customisable ID photo processing, built using JavaCV (OpenCV bindings) and Swing for GUI. This tool allows users to upload a photo, process it with a wide range of editing tools, and export the final result either to their local machine or to Google Drive.

---

## Features

### Core Functionalities:
- Add Photo: Upload an image (JPG/PNG) from your local machine.
- Batch Processing: Apply settings (e.g., cropping, resizing, enhancement) to multiple photos at once.
- Cropping: Custom crop area based on face detection or manual selection.
- Define Output Size: Specify desired dimensions (e.g., 35x45mm) for ID photo requirements.
- Background Removal: Automatically detect and remove the background.
- Background Replacement:
  - Set a solid color(e.g., white, blue, red).
  - Replace with a custom background image.

### Photo Enhancement:
- Brightness, Contrast, and Sharpness adjustment.
- Option to auto-enhance with pre-configured settings.

### Edit History:
- Undo / Redo any previous action during the editing session.
- Reset the image back to the original uploaded version at any time.

### Export Options:
- Save to Local: Export as a .jpg file to your device.
- Save to Cloud: Upload final photo to Google Drive via integrated API.

---

## Tech Stack

Java - Main Language
Swing- GUI framework
JavaCV- Java wrapper for OpenCV(image processing, face detection, etc) 
Google Drive API for cloud based storage


---

## How It Works

1. Launch the app and upload your photo.
2. Use the editing toolbar to crop, enhance, or remove background.
3. Choose output size and preferred background color/image.
4. Save your final image locally or upload it to your Google Drive.

---

## Setup Instructions

### Prerequisites
- Java 8+
- JavaCV / OpenCV
- Google API credentials (for Drive integration)

### Running the App
1. Clone this repository  
2. Import into your IDE (e.g., Vscode, IntelliJ, Eclipse).
3. Make sure native OpenCV libraries are loaded via JavaCV (ensure system path is set).
4. Run the command ‘bash run.sh’ file.

---

## Google Drive Integration

To enable saving to Google Drive:
1. Generate OAuth 2.0 credentials from [Google Cloud Console](https://console.cloud.google.com/)
2. Download the secret.json from Google Cloud Console
3. The app will prompt for permission to the json file the first time.
