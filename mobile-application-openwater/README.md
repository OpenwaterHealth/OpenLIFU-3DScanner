# 3D ARCore App and Face Detection  

This project is an Android application that integrates **face detection** using Google ML Kit and **augmented reality (AR)** functionalities using ARCore. It captures the user's face, detects key landmarks (e.g., the nose), and transitions to an AR screen to render 3D visual elements.

---

## Features

- **Face Detection**: Detects faces and identifies key landmarks like the nose using Google ML Kit.
- **Augmented Reality**: Uses ARCore to dynamically render 3D elements in a real-world environment.
- **Logging**: Logs app activities and errors in `app_log.txt` and `app_logcat.txt` for debugging and monitoring.
- **Permissions Handling**: Dynamically requests camera and storage permissions for compatibility across Android versions.

---

## Specifications

- **Language**: Kotlin
- **Frameworks/Tools**:
  - Google ML Kit for face detection.
  - ARCore for AR functionality.
- **Minimum Android API Level**: 24 (Android 7.0)
- **Target Android API Level**: 34 (Android 14)
- **Gradle Version**: 8.1
- **Android Plugin Version**: 8.1.1
- **Compatible Devices**:
  - Supports devices with ARCore compatibility.

---

## Prerequisites

1. **Android Studio**: Install the latest version (recommended: Arctic Fox or later).
2. **Android SDK Tools**: Ensure the following SDK versions are installed:
   - **Minimum SDK Version**: API 24 (Android 7.0)
   - **Target SDK Version**: API 34 (Android 14)
3. **Device Compatibility**: The device must support ARCore. You can check the list of supported devices [here](https://developers.google.com/ar/devices).

---

## Setup Instructions

### Clone the Repository

1. Open your terminal and clone the repository:
   ```bash
   git clone https://github.com/OpenwaterHealth/OpenLIFU-3DScanner.git
   cd mobile-application-openwater-arcore
