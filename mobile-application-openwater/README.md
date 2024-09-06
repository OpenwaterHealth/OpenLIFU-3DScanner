# Auto image capturing and face detection

# SDK Version 49 -- node version 16

# Steps to start

- npm install
- cd face_detector
- npx expo start

-

## Overview

This project is a mobile application built with React Native, utilizing Expo for face detection and battery status monitoring. The app captures images automatically when a face is detected within the camera's view, and it provides various functionalities like saving images, detecting battery charging status, and more.

## Features

- **Face Detection**: Uses Expo's FaceDetector API to detect faces in real-time through the camera.
- **USB Detection**: Monitors the device's USB status, providing feedback on whether the device is connected or not.
- **Automatic Image Capture**: Automatically captures photos at intervals when a face is detected.
- **Gallery Integration**: Allows users to view captured images and open the device's gallery.
- **Responsive UI**: The app is optimized for various screen sizes, ensuring a consistent user experience.

## Installation

### Prerequisites

- **Node.js**: Make sure you have Node.js installed on your system.
- **Expo CLI**: You need to have Expo CLI installed globally. You can install it using the following command:

  ```bash
  npm install -g expo-cli
  ```

### Clone the Repository

### Install Dependencies

Navigate to the project directory and install the necessary dependencies:

```bash
cd mobile-application-openwater
npm install
```

### Run the Application

To start the application, run the following command:

```bash
expo start
```

This will open the Expo development tools in your browser. You can then run the app on an Android/iOS emulator or scan the QR code with the Expo Go app on your mobile device.

## Usage

### Face Detection

- **Automatic Detection**: Once the app is running, it will automatically detect faces in front of the camera.
- **Photo Capture**: The app captures photos at intervals when a face is detected, and these images are stored in the device's media library.

### USB Status

- If the device is connected, a USB connection message is shown, otherwise, a prompt to connect the USB is displayed.

### Gallery Access

- Users can access the device's gallery directly from the app to view the captured images.

## Configuration

### Permissions

Ensure the following permissions are requested in your app:

- **Camera**: For face detection and image capture.
- **Media Library**: For saving images and accessing the gallery.
- **Battery**: For monitoring the battery status with USB. to get USB status

### Modifying Face Detection

To customize the face detection functionality, you can adjust the settings in the `handleFacesDetected` callback. The current implementation draws rectangles around detected faces.

### Customization

You can customize various aspects of the app, such as:

- **Face Box Color**: Modify the color of the bounding box drawn around detected faces in the styles.
- **Capture Interval**: Adjust the frequency of photo captures by changing the interval in the `startCapturing` function.

## Troubleshooting

### Common Issues

- **Permission Denied**: Ensure that the app has the necessary permissions to access the camera and media library.
- **Camera Error**: If the camera fails to initialize, check the device's camera settings and try restarting the app.

### Debugging

Use the React Native debugger and Expo's logging to troubleshoot any issues. The app logs useful information to the console for debugging purposes.

## Acknowledgments

- **Expo**: For providing a powerful and easy-to-use framework for building mobile apps.
- **React Native**: For enabling cross-platform mobile development.
- **Vector Icons**: The app uses icons from the `react-native-vector-icons` library for a better UI experience.

