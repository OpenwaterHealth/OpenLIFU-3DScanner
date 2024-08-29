import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import * as React from "react";
import CameraComponent from "./src/components/CameraScreen";
import FaceDetection from "./src/components/FaceDetection";
import IntroScreen from "./src/components/IntroScreen";

const Stack = createNativeStackNavigator();

function App() {
  return (
    <NavigationContainer>
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
            headerRight: null,
            headerTitleAlign: "center",
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
            headerTitleAlign: "center",
            headerTruncatedBackTitle: "Back",
            headerTitleAllowFontScaling: false,
            headerBackTitleVisible: true,
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

export default App;
