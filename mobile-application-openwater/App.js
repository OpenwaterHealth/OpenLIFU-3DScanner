import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { Camera } from "expo-camera";
import { useKeepAwake } from "expo-keep-awake"; // Import useKeepAwake
import * as MediaLibrary from "expo-media-library"; // Import Media Library for storage permission
import React, { useEffect, useState } from "react";
import { Button, Text, View } from "react-native";
import CameraComponent from "./src/components/CameraScreen";
import FaceDetection from "./src/components/FaceDetection";
import IntroScreen from "./src/components/IntroScreen";

const Stack = createNativeStackNavigator();

function App() {
  // Use the hook to keep the screen awake
  useKeepAwake();

  const [hasPermission, setHasPermission] = useState(null);
  const [isLoading, setIsLoading] = useState(true); // New state for loading

  console.log(hasPermission);

  // Request Camera and Media Storage Permissions
  const requestPermissions = async () => {
    try {
      // Request camera permission
      const cameraStatus = await Camera.requestCameraPermissionsAsync();

      // Request media storage permission
      const mediaStatus = await MediaLibrary.requestPermissionsAsync();

      // Check if both permissions are granted
      if (
        cameraStatus.status === "granted" &&
        mediaStatus.status === "granted"
      ) {
        setHasPermission(true); // Both permissions granted
      } else {
        setHasPermission(false); // One or both permissions denied
      }
    } catch (error) {
      console.error("Error requesting permissions", error);
      setHasPermission(false); // Handle the error case
    } finally {
      setIsLoading(false); // Ensure loading is set to false after permissions check
    }
  };

  // Ask for permissions when the app starts
  useEffect(() => {
    // Call the requestPermissions function and wait for it to complete
    const checkPermissions = async () => {
      await requestPermissions();
    };

    checkPermissions();
  }, []);

  // If permissions are not granted, show permission request screen
  const renderPermissionScreen = () => {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
        <Text style={{ fontSize: 18 }}>
          Camera and media storage permissions are required to proceed
        </Text>
        <Button
          title="Grant Permissions"
          onPress={() => {
            setIsLoading(true); // Set loading to true while requesting permissions again
            requestPermissions();
          }}
        />
      </View>
    );
  };

  // Render the app
  return (
    <NavigationContainer>
      {isLoading ? (
        // Loading screen while checking permissions
        <View
          style={{ flex: 1, justifyContent: "center", alignItems: "center" }}
        >
          <Text>Loading...</Text>
        </View>
      ) : hasPermission === false ? (
        // Show permission request screen if permissions are not granted
        renderPermissionScreen()
      ) : (
        // Show the app's main content if permissions are granted
        <Stack.Navigator initialRouteName="IntroScreen">
          <Stack.Screen
            name="IntroScreen"
            component={IntroScreen}
            options={{
              headerShown: false,
            }}
          />
          <Stack.Screen
            name="FaceDetection"
            component={FaceDetection}
            options={{
              headerTitle: "Scan your head",
              headerStyle: { backgroundColor: "#0D0D0D" },
              headerTitleStyle: { color: "white" },
              headerBackTitleStyle: { color: "white" },
              headerBackTitleVisible: true,
              headerTitleAlign: "center",
              headerTruncatedBackTitle: "Back",
              headerTitleAllowFontScaling: false,
              headerBackTitleVisible: true,
            }}
          />
          <Stack.Screen
            name="CameraComponent"
            component={CameraComponent}
            options={{
              headerTitle: "Face Detected",
              headerStyle: { backgroundColor: "#0D0D0D" },
              headerTitleStyle: { color: "white" },
              headerBackTitleStyle: { color: "white" },
              headerBackTitleVisible: true,
              headerLeft: null,
              headerRight: null,
              headerTitleAlign: "center",
              headerTruncatedBackTitle: "Back",
              headerTitleAllowFontScaling: false,
              headerBackTitleVisible: true,
            }}
          />
        </Stack.Navigator>
      )}
    </NavigationContainer>
  );
}

export default App;
