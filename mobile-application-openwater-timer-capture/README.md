Here's a `README.md` file formatted for GitHub:

markdown
# React Native Android Setup Guide

A step-by-step guide to set up and run a React Native project for Android using **Node.js v20**.

---

## Prerequisites

Ensure you have the following installed on your system:

- **Node.js v20:**  
  Install Node.js v20.x and ensure `npm` is included.

- **React Native CLI:**  
  Install globally:
  bash
  npm install -g react-native-cli
  

- **Java Development Kit (JDK):**  
  Use JDK 11 or 17.

- **Android Studio:**  
  Install Android SDK, Platform Tools, and AVD.  
  Ensure `ANDROID_HOME` is set in your environment variables:
  bash
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools
  

---

## Getting Started

### 1. Clone the Repository
Clone this project using:
bash
git clone <repository-url>


### 2. Navigate to the Project Directory
bash
cd <project-directory>


### 3. Install Dependencies
Run the following command to install all dependencies:
bash
npm install


### 4. Start Metro Bundler
Start the React Native Metro server:
bash
npx react-native start


### 5. Run the App on Android
- Start an emulator from Android Studio **or** connect a physical device via USB.
- Run the project on the Android emulator or connected device:
  bash
  npx react-native run-android
  

---

## Common Issues and Solutions

### Metro Bundler Not Running
Ensure the Metro server is running:
bash
npx react-native start


### Android Device Not Detected
Check if `adb` detects your device:
bash
adb devices


### Build Errors
1. Verify `ANDROID_HOME` is set up correctly.
2. Clear the cache and rebuild:
   bash
   npx react-native start --reset-cache
   npx react-native run-android
   

---

## Useful Commands

| Command                                  | Description                       |
|------------------------------------------|-----------------------------------|
| `npx react-native start`                 | Start Metro bundler.              |
| `npx react-native run-android`           | Build and run the app on Android. |
| `npm install <package-name>`             | Install a new dependency.         |
| `npx react-native link`                  | Link native modules.              |
| `adb devices`                            | List connected Android devices.   |

---

## Environment Details

- **Node.js Version**: 20.x
- **React Native Version**: Check in `package.json`
- **Java Development Kit**: JDK 11 or 17
- **Android SDK**: Installed via Android Studio

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Happy Coding! 🎉