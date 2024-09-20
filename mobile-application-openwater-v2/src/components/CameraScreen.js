import * as Battery from "expo-battery";
import { Camera } from "expo-camera";
import * as FaceDetector from "expo-face-detector";
import * as FileSystem from "expo-file-system";
import * as MediaLibrary from "expo-media-library";
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Alert,
  AppState,
  Image,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import UsbGif from "../../assets/Image/usb-connected.png";

// Request Storage Permission
async function requestStoragePermission() {
  const permission = await MediaLibrary.getPermissionsAsync();
  if (permission.granted) {
    return true; // Permission already granted
  }

  if (permission.canAskAgain) {
    const { status } = await MediaLibrary.requestPermissionsAsync();
    return status === "granted";
  }

  return false;
}

// Request Camera Permission
async function requestCameraPermission() {
  const { status } = await Camera.requestCameraPermissionsAsync();
  return status === "granted";
}

function CameraComponent({ navigation, route }) {
  const referNumber = route.params?.referNumber;
  const [hasPermission, setHasPermission] = useState(null);
  const [photoCount, setPhotoCount] = useState(0);
  const [isCapturing, setIsCapturing] = useState(false);
  const [showCamera, setShowCamera] = useState(true);
  const [faceData, setFaceData] = useState([]);
  const [cameraError, setCameraError] = useState(null);
  const [batteryState, setBatteryState] = useState(null);
  const [appState, setAppState] = useState(AppState.currentState); // State to track app state
  const [extraImages, setExtraImages] = useState(0); // Track extra images to be taken
  const [progressBarColor, setProgressBarColor] = useState("green"); // Default color set to orange
  const [isPaused, setIsPaused] = useState(false); // Track if the scan is paused
  const [showStopOptions, setShowStopOptions] = useState(false); // Track if "Discard," "Capture More," and "Finish" buttons are visible

  console.log(isCapturing, isPaused, showStopOptions, "***isCapturing*");

  // Add log in useEffect to check AppState changes
  useEffect(() => {
    const subscription = AppState.addEventListener("change", (nextAppState) => {
      if (appState.match(/inactive|background/) && nextAppState === "active") {
        console.log("App has come to the foreground!");
        // Only resume if it was paused before
        if (isPaused) {
          resumeCapturing();
        }
      } else if (nextAppState === "background") {
        console.log("App is going to the background!");
        if (isCapturing) {
          pauseCapturing();
        }
      }
      setAppState(nextAppState);
    });

    return () => {
      subscription.remove();
    };
  }, [appState, isCapturing, isPaused, resumeCapturing, pauseCapturing]);

  const photoCountRef = useRef(0); // Add this ref to track the photo count directly

  const captureInterval = useRef(null);
  const cameraRef = useRef(null);

  const TOTAL_IMAGES_INITIAL = 120;

  const progress = useMemo(
    () => photoCount / TOTAL_IMAGES_INITIAL,
    [photoCount]
  );

  const handlePermissionRequest = useCallback(async () => {
    try {
      const cameraGranted = await requestCameraPermission();
      const storageGranted = await requestStoragePermission();
      setHasPermission(cameraGranted && storageGranted);

      if (!cameraGranted || !storageGranted) {
        Alert.alert(
          "Permissions Required",
          "Camera and storage permissions are required for this feature to work. Please enable them in your device settings.",
          [{ text: "OK" }]
        );
      }
    } catch (error) {
      console.error("Error requesting permissions:", error);
    }
  }, []);

  useEffect(() => {
    handlePermissionRequest();
  }, [handlePermissionRequest]);

  useEffect(() => {
    // Get the initial battery state
    const getBatteryState = async () => {
      const state = await Battery.getBatteryStateAsync();
      setBatteryState(state);
    };

    // Subscribe to battery state changes
    const subscription = Battery.addBatteryStateListener(({ batteryState }) => {
      setBatteryState(batteryState);
    });

    // Get the initial state
    getBatteryState();

    // Cleanup subscription on component unmount
    return () => {
      subscription.remove();
    };
  }, []);

  const isCharging = useMemo(
    () => batteryState === Battery.BatteryState.CHARGING,
    [batteryState]
  );

  const handleCameraError = useCallback((error) => {
    console.error("Camera Error: ", error);
    setCameraError("Error accessing camera. Please try again.");
  }, []);

  const handleFacesDetected = useCallback(({ faces }) => {
    if (faces.length > 0) {
      setFaceData(faces);
    }
  }, []);

  const renderFaceBoxes = useMemo(() => {
    return faceData.map((face, index) => {
      const { origin, size } = face.bounds;

      return (
        <View
          key={index}
          style={[
            styles.faceBox,
            {
              left: origin.x,
              top: origin.y,
              width: size.width,
              height: size.height,
            },
          ]}
        />
      );
    });
  }, [faceData]);

  // Create folder for referNumber
  const createFolderIfNotExists = async (referNumber) => {
    const folderUri = `${FileSystem.documentDirectory}${referNumber}`;
    const folderInfo = await FileSystem.getInfoAsync(folderUri);

    if (!folderInfo.exists) {
      await FileSystem.makeDirectoryAsync(folderUri, { intermediates: true });
    }

    return folderUri;
  };

  // Capture photo and save in referNumber folder
  const capturePhoto = useCallback(async () => {
    // Use TOTAL_IMAGES to consider extraImages
    const TOTAL_IMAGES = TOTAL_IMAGES_INITIAL + extraImages;

    if (
      hasPermission &&
      cameraRef.current !== null &&
      photoCountRef.current < TOTAL_IMAGES
    ) {
      try {
        // Creating folder based on referNumber
        const folderUri = await createFolderIfNotExists(referNumber);

        const photo = await cameraRef.current.takePictureAsync({
          quality: 1,
          skipProcessing: true,
          mute: true, // Mute the camera sound
        });

        const fileName = `${folderUri}/${referNumber}_${photoCountRef.current}.jpg`;
        await FileSystem.moveAsync({
          from: photo.uri,
          to: fileName,
        });

        await MediaLibrary.createAssetAsync(fileName);

        setPhotoCount((prevCount) => prevCount + 1);
        photoCountRef.current += 1;
      } catch (error) {
        console.error("Error capturing photo:", error);
      }
    } else if (photoCountRef.current >= TOTAL_IMAGES) {
    }
  }, [hasPermission, extraImages, referNumber]); // Add extraImages and referNumber as dependencies

  const startCapturing = useCallback(async () => {
    const TOTAL_IMAGES = TOTAL_IMAGES_INITIAL + extraImages;

    setProgressBarColor("#87CEEB");

    if (!hasPermission) {
      const cameraGranted = await requestCameraPermission();
      const storageGranted = await requestStoragePermission();
      setHasPermission(cameraGranted && storageGranted);
      if (!cameraGranted || !storageGranted) {
        console.error("Permissions not granted.");
        return;
      }
    }

    if (photoCountRef.current < TOTAL_IMAGES) {
      if (captureInterval.current) {
        clearInterval(captureInterval.current);
      }
      setIsCapturing(true);

      // Updated captureInterval block
      captureInterval.current = setInterval(() => {
        try {
          if (photoCountRef.current < TOTAL_IMAGES) {
            capturePhoto();
          } else {
            clearInterval(captureInterval.current); // Stop interval when limit is reached
            captureInterval.current = null; // Reset interval
            setIsCapturing(false);

            // Set progress bar color to green when finished
            setProgressBarColor("#92E622");
            setShowStopOptions(true);
          }
        } catch (error) {
          console.error("Error during capture interval:", error);
          clearInterval(captureInterval.current); // Stop interval on error
          captureInterval.current = null; // Reset interval
          setIsCapturing(false);

          // Set progress bar color to green if an error occurs
          setProgressBarColor("#92E622");
          setShowStopOptions(true);
        }
      }, 2000); // Adjust the interval time here if needed
    } else if (photoCountRef.current >= TOTAL_IMAGES) {
      setProgressBarColor("#92E622");
      setShowStopOptions(true);
    }
  }, [capturePhoto, hasPermission, extraImages]);

  useEffect(() => {
    if (extraImages > 0) {
      startCapturing(); // Start capturing again when extraImages is updated
    }
  }, [extraImages, startCapturing]);

  const handleDiscard = useCallback(() => {
    Alert.alert(
      "Confirmation", // Title of the alert
      "Are you sure?", // Message of the alert
      [
        {
          text: "No",
          onPress: () => console.log("Discard Canceled"), // Do nothing on "No"
          style: "cancel",
        },
        {
          text: "Yes, Discard",
          onPress: () => {
            // Reset to the initial state if the user confirms
            resetAllStates();
          },
        },
      ],
      { cancelable: true }
    );
  }, []);

  const handleCaptureMore = useCallback(() => {
    setShowStopOptions(false); // Hide options
    setExtraImages((prevExtraImages) => prevExtraImages + 20); // Allow capturing more images
  }, []);

  const handleFinish = useCallback(() => {
    setShowStopOptions(false); // Hide options
    resetAllStates();
    ForwardImages(); // Finish capturing and move to the next step
  }, [ForwardImages]);

  // Function to reset all states
  // Function to reset all states
  const resetAllStates = () => {
    setShowStopOptions(false); // Hide options
    setPhotoCount(0); // Reset photo count
    setIsCapturing(false); // Set capturing to false
    setIsPaused(false); // Reset paused state
    setShowCamera(true); // Show camera again
    setProgressBarColor("green"); // Reset the progress bar color
    setExtraImages(0); // Reset extra images count

    // Clear capture interval if active
    if (captureInterval.current) {
      clearInterval(captureInterval.current);
      captureInterval.current = null;
    }

    // Reset the photo count ref
    photoCountRef.current = 0;
  };

  const pauseCapturing = useCallback(() => {
    setProgressBarColor("#FBBB04");
    setIsPaused(true); // Set the scan as paused

    if (captureInterval.current) {
      clearInterval(captureInterval.current);
      captureInterval.current = null;
    }
    setIsCapturing(false);
  }, []);

  const resumeCapturing = useCallback(() => {
    setIsPaused(false); // Resume the scan
    startCapturing();
  }, [startCapturing]);

  const handleStopScan = useCallback(() => {
    setIsPaused(false); // Reset the paused state
    setShowStopOptions(true); // Show "Discard," "Capture More," and "Finish" options
  }, []);

  const ForwardImages = useCallback(() => {
    if (captureInterval.current) {
      clearInterval(captureInterval.current);
      captureInterval.current = null;
    }
    setIsCapturing(false);
    setShowCamera(false);

    // Set progress bar color to green when finishing
    setProgressBarColor("#92E622");
  }, []);

  if (hasPermission === null) {
    return <View />;
  }

  if (hasPermission === false) {
    return (
      <View style={styles.container}>
        <Text>No access to camera. Please allow camera permissions.</Text>
        <TouchableOpacity
          style={styles.permissionButton}
          onPress={handlePermissionRequest}
        >
          <Text style={styles.permissionButtonText}>Grant Permissions</Text>
        </TouchableOpacity>
      </View>
    );
  }

  console.log(!isCapturing && !showStopOptions && !isPaused, "start ");

  console.log(isPaused && !showStopOptions, "resume");

  return (
    <>
      <StatusBar barStyle={"dark-content"} />
      <View style={styles.container}>
        {cameraError && <Text style={styles.errorText}>{cameraError}</Text>}
        <View style={styles.ProgressBarContainer}>
          <View
            style={[
              styles.ProgressBar,
              {
                width: "100%",
                backgroundColor: "#92E622",
              },
            ]}
          />
          <Text style={styles.ProgressText}>Scan Completed</Text>
        </View>

        {!isCharging ? (
          <Image
            style={styles.fullImage}
            source={require("../../assets/Image/connectusb.gif")}
            resizeMode="cover"
          />
        ) : (
          <Image style={styles.fullImage} source={UsbGif} resizeMode="cover" />
        )}

        {showCamera ? (
          <>
            <Camera
              ref={cameraRef}
              mute={true}
              style={StyleSheet.absoluteFill}
              type={Camera.Constants.Type.back}
              onFacesDetected={handleFacesDetected}
              autoFocus={Camera.Constants.AutoFocus.on} // Enable autofocus
              faceDetectorSettings={{
                mode: FaceDetector.FaceDetectorMode.fast,
                detectLandmarks: FaceDetector.FaceDetectorLandmarks.all,
                runClassifications:
                  FaceDetector.FaceDetectorClassifications.none,
                tracking: true,
              }}
              onMountError={handleCameraError}
            >
              {renderFaceBoxes}
            </Camera>

            {(isCapturing || showStopOptions || isPaused) && (
              <View style={styles.ProgressBarContainer}>
                <View
                  style={[
                    styles.ProgressBar,
                    {
                      width: `${progress * 100}%`,
                      backgroundColor: progressBarColor,
                    }, // Dynamic color applied here
                  ]}
                />
                <Text style={styles.ProgressText}>{`${photoCount}/${
                  TOTAL_IMAGES_INITIAL + extraImages
                }`}</Text>
              </View>
            )}

            <View style={styles.bottomBar}>
              {!isCapturing && !showStopOptions && !isPaused ? (
                // Initial Stage: Show Start Scan and Cancel buttons
                <>
                  <TouchableOpacity
                    style={styles.camButton}
                    onPress={startCapturing}
                  >
                    <Text
                      style={[
                        styles.buttonTextStyles,
                        { color: "#000", fontWeight: "600" },
                      ]}
                    >
                      Start Scan
                    </Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.camButton, { backgroundColor: "red" }]}
                    onPress={() => {
                      resetAllStates(); // Reset the state before navigating away
                      navigation.navigate("IntroScreen");
                    }}
                  >
                    <Text
                      style={[
                        styles.buttonTextStyles,
                        { color: "#fff", fontWeight: "600" },
                      ]}
                    >
                      Cancel
                    </Text>
                  </TouchableOpacity>
                </>
              ) : isCapturing ? (
                // Scanning State: Show Stop Scan button
                <TouchableOpacity
                  style={[styles.camButton, styles.pauseButton]}
                  onPress={pauseCapturing} // When pressed, it will pause capturing
                >
                  <Text style={styles.buttonTextStyles}>Stop Scan</Text>
                </TouchableOpacity>
              ) : isPaused && !showStopOptions ? (
                // Paused State: Show Resume Scan and Stop Scan buttons
                <>
                  <TouchableOpacity
                    style={[styles.camButton, styles.resumeButton]}
                    onPress={resumeCapturing}
                  >
                    <Text style={styles.buttonTextStyles}>Resume Scan</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.camButton, styles.pauseButton]}
                    onPress={handleStopScan} // When pressed, it shows the final set of buttons
                  >
                    <Text style={styles.buttonTextStyles}>Stop Scan</Text>
                  </TouchableOpacity>
                </>
              ) : showStopOptions ? (
                // Show Discard, Capture More, and Finish buttons
                <>
                  <TouchableOpacity
                    style={[styles.camButton, styles.discardButton]}
                    onPress={handleDiscard}
                  >
                    <Text style={styles.buttonTextStyles}>Discard</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.camButton, styles.captureMoreButton]}
                    onPress={handleCaptureMore}
                  >
                    <Text style={styles.buttonTextStyles}>Capture More</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.camButton, styles.finishButton]}
                    onPress={handleFinish}
                  >
                    <Text style={styles.buttonTextStyles}>Finish</Text>
                  </TouchableOpacity>
                </>
              ) : null}
            </View>
          </>
        ) : (
          <>
            <View style={styles.buttonContainer}>
              <View style={styles.buttons}>
                <TouchableOpacity style={styles.processButton}>
                  <Text style={styles.processText}>
                    {isCharging ? "Connected" : "Waiting For USB..."}
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
          </>
        )}
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#fff",
  },
  buttonContainer: {
    position: "absolute",
    alignItems: "center",
  },
  discardButton: {
    backgroundColor: "#F03D2F",
    justifyContent: "center",
    alignItems: "center",
  },
  finishButton: {
    backgroundColor: "#04AA6D",
    justifyContent: "center",
    alignItems: "center",
  },
  buttons: {},
  camButton: {
    height: 50,
    width: "90%",
    borderRadius: 10,
    backgroundColor: "#92E622",
    marginBottom: 10,
    justifyContent: "center",
    alignItems: "center",
  },
  pauseButton: {
    backgroundColor: "red",
    justifyContent: "center",
    alignItems: "center",
  },
  camFlip: {
    height: 50,
    width: "90%",
    borderRadius: 10,
    backgroundColor: "red",
    borderColor: "white",
    justifyContent: "center",
    alignItems: "center",
  },
  buttonTextStyles: {
    color: "#fff",
    fontSize: 18,
    letterSpacing: 1,
  },
  image: {
    width: "100%",
    height: "100%",
  },
  camPause: {
    height: 50,
    width: 70,
    borderRadius: 40,
    backgroundColor: "rgba(255, 255, 255, 0.12)",
    borderColor: "white",
    justifyContent: "center",
    alignItems: "center",
  },
  ProgressBarContainer: {
    width: "90%",
    height: 30,
    overflow: "hidden",
    marginVertical: 10,
    justifyContent: "center", // Remove any centering styles
    position: "absolute",
    top: 10, // Adjust position as needed
    left: 10, // Adjust position as needed
    alignItems: "center",
    borderWidth: 2,
    borderColor: "#000", // Progress bar border color
  },

  ProgressBar: {
    height: "100%",
    backgroundColor: "#92E622", // Progress bar color
    position: "absolute", // Make sure this is absolute to fill from the left
    left: 0, // Ensure it starts from the left
  },

  ProgressText: {
    position: "absolute",
    color: "#000", // Text color for the count
    fontWeight: "bold",
    textAlign: "center",
  },

  bottomBar: {
    flexDirection: "column",
    position: "absolute",
    bottom: 30,
    justifyContent: "center",
    alignItems: "center",
    width: "80%",
    zIndex: 1,
  },
  retakeText: {
    color: "#ffffff",
    paddingLeft: 6,
  },
  processText: {
    color: "#0D0D0D",
    fontSize: 18,
  },
  fullImage: {
    width: "100%",
    height: "80%",
    resizeMode: "contain",
  },
  modalContainer: {
    flex: 1,
    justifyContent: "flex-end",
    alignItems: "center",
    backgroundColor: "rgba(0, 0, 0, 0.5)",
  },
  modalContent: {
    width: "100%",
    padding: 20,
    backgroundColor: "white",
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    alignItems: "center",
  },
  modalTitle: {
    fontSize: 15,
    fontWeight: "600",
    color: "#0D0D0D",
  },
  BoldTextStyle: {
    fontSize: 20,
    fontWeight: "600",
    color: "#0D0D0D",
    marginBottom: 10,
  },
  permissionButton: {
    marginTop: 20,
    padding: 10,
    backgroundColor: "#92E622",
    borderRadius: 5,
  },
  permissionButtonText: {
    color: "#0D0D0D",
    fontSize: 16,
  },
  faceBox: {
    position: "absolute",
    backgroundColor: "transparent",
  },
  errorText: {
    color: "red",
    marginBottom: 10,
  },
  UserIdentifiedContainer: {
    width: "100%",
    height: 40,
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    bottom: 120,
    position: "absolute",
  },
  ProgressBarStyles: {
    top: 10,
    left: 10,
    position: "absolute",
    width: "90%",
  },
  retakeButton: {
    backgroundColor: "#0D0D0D",
    width: 127,
    height: 60,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 50,
    flexDirection: "row",
  },
  processButton: {
    backgroundColor: "#ffff",
    paddingHorizontal: 10,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 10,
    width: 205,
    height: 60,
    borderWidth: 4,
  },
  indicatorStyles: {
    backgroundColor: "transparent",
    tintColor: "#fff",
    bottom: 180,
    position: "absolute",
    left: 0,
  },
});

export default CameraComponent;
