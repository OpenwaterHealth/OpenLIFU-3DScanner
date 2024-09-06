import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { Camera } from "expo-camera";
import * as MediaLibrary from "expo-media-library"; // Import Media Library for storage permission
import React, { useEffect, useState } from "react";
import { Button, Text, View } from "react-native";
import CameraComponent from "./src/components/CameraScreen";
import FaceDetection from "./src/components/FaceDetection";
import IntroScreen from "./src/components/IntroScreen";

const Stack = createNativeStackNavigator();

function App() {
  const [hasPermission, setHasPermission] = useState(null);

  // Request Camera and Media Storage Permissions
  const requestPermissions = async () => {
    // Request camera permission
    const cameraStatus = await Camera.requestCameraPermissionsAsync();

    // Request media storage permission
    const mediaStatus = await MediaLibrary.requestPermissionsAsync();

    // Check if both permissions are granted
    if (cameraStatus.status === "granted" && mediaStatus.status === "granted") {
      setHasPermission(true); // Both permissions granted
    } else {
      setHasPermission(false); // One or both permissions denied
    }
  };

  console.log(hasPermission, "Permissions");

  // Ask for permissions when the app starts
  useEffect(() => {
    requestPermissions();
  }, []);

  // If permissions are not granted, ask repeatedly
  const renderPermissionScreen = () => {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
        <Text style={{ fontSize: 18 }}>
          Camera and media storage permissions are required to proceed
        </Text>
        <Button
          title="Grant Permissions"
          onPress={() => requestPermissions()}
        />
      </View>
    );
  };

  return (
    <NavigationContainer>
      {hasPermission === null ? (
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
