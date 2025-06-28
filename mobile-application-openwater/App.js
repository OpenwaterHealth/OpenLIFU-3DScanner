import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import React, {useEffect, useState} from 'react';
import {
  ActivityIndicator,
  Alert,
  AppState,
  Button,
  Linking,
  PermissionsAndroid,
  Platform,
  Text,
  View,
} from 'react-native';
import KeepAwake from 'react-native-keep-awake';
import {Camera} from 'react-native-vision-camera'; // Importing Vision Camera for camera and microphone permissions
import CameraScreen from './src/Component/CameraScreen';
import IntroScreen from './src/Component/IntroScreen';

const Stack = createNativeStackNavigator();

function App() {
  const [hasPermission, setHasPermission] = useState(null); // Track permission status
  const [isLoading, setIsLoading] = useState(true); // State to handle loading

<<<<<<< HEAD
  useEffect(() => {
    KeepAwake.activate(); // Activates keep awake mode when component is mounted

    return () => {
      KeepAwake.deactivate(); // Deactivates keep awake mode when component unmounts
    };
  }, []);

  useEffect(() => {
    const handleAppStateChange = nextAppState => {
      if (nextAppState === 'active') {
        KeepAwake.activate(); // Ensure app stays awake when it comes to the foreground
      } else {
        KeepAwake.deactivate(); // Allow sleep when the app is backgrounded
      }
    };
=======
  const [hasPermission, setHasPermission] = useState(null);
  const [isLoading, setIsLoading] = useState(true); // New state for loading

  console.log(hasPermission);
>>>>>>> main

    AppState.addEventListener('change', handleAppStateChange);

    return () => {
      AppState.removeEventListener('change', handleAppStateChange);
    };
  }, []);

  // Request Camera, Microphone, and Storage Permissions upfront on app launch
  const requestPermissions = async () => {
    try {
<<<<<<< HEAD
      let storagePermissionGranted = true; // Assume true for Android 33 and above

      // Check if Android version is below 33, then request WRITE_EXTERNAL_STORAGE permission
      if (Number(Platform.Version) < 33) {
        const storagePermission = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
          PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
        );

        if (storagePermission === PermissionsAndroid.RESULTS.GRANTED) {
          storagePermissionGranted = true; // Permission granted
        } else if (
          storagePermission === PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN
        ) {
          Alert.alert(
            'Permission Denied',
            'You have permanently denied storage permission. Please enable it from app settings.',
            [
              {text: 'Cancel', style: 'cancel'},
              {text: 'Open Settings', onPress: () => Linking.openSettings()},
            ],
          );
          storagePermissionGranted = false; // Permission denied
        } else {
          storagePermissionGranted = false; // Permission denied
        }
      }
      // Request all permissions together
      const cameraPermission = await Camera.requestCameraPermission(); // Request camera permission
      const microphonePermission = await Camera.requestMicrophonePermission(); // Request microphone permission

      if (
        cameraPermission === 'granted' &&
        microphonePermission === 'granted'
      ) {
        setHasPermission(true); // All permissions granted
      } else if (
        storagePermission === PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN
      ) {
        Alert.alert(
          'Permission Denied',
          'You have permanently denied storage permission. Please enable it from app settings.',
          [
            {text: 'Cancel', style: 'cancel'},
            {text: 'Open Settings', onPress: () => Linking.openSettings()},
          ],
        );
      } else {
        setHasPermission(false); // Permissions denied
      }
    } catch (err) {
      console.warn(err);
    } finally {
      setIsLoading(false); // Stop loading after permissions check
    }
  };

  useEffect(() => {
    requestPermissions(); // Call the requestPermissions function on app launch
  }, []);

  // Render the screen when permissions are denied
  const renderPermissionScreen = () => (
    <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}>
      <Text style={{fontSize: 18}}>
        Camera, microphone, and storage permissions are required to proceed.
      </Text>
      <Button
        title="Grant Permissions"
        onPress={() => {
          setIsLoading(true); // Set loading to true while re-requesting permissions
          requestPermissions(); // Request permissions again
        }}
      />
    </View>
  );
=======
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
>>>>>>> main

  // Render the app
  return (
    <NavigationContainer>
      {isLoading ? (
<<<<<<< HEAD
=======
        // Loading screen while checking permissions
>>>>>>> main
        <View
          style={{
            flex: 1,
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: '#000',
            flexDirection: 'row',
          }}>
          <Text style={{fontSize: 16, color: '#fff'}}>
            Checking permissions...
          </Text>
          <ActivityIndicator size="small" color="#92E622" />
        </View>
      ) : hasPermission === false ? (
        renderPermissionScreen()
      ) : (
        <Stack.Navigator initialRouteName="IntroScreen">
          <Stack.Screen
            name="IntroScreen"
            component={IntroScreen}
            options={{
              headerShown: false,
            }}
          />
          <Stack.Screen
            name="CameraScreen"
            component={CameraScreen}
            options={{
              headerShown: false,
            }}
          />
        </Stack.Navigator>
      )}
    </NavigationContainer>
  );
}

export default App;
