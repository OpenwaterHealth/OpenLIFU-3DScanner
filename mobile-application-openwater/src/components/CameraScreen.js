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
  Linking,
  Modal,
  Platform,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { MD3Colors, ProgressBar } from "react-native-paper";
import AntDesign from "react-native-vector-icons/AntDesign";
import Feather from "react-native-vector-icons/Feather";
import MaterialIcons from "react-native-vector-icons/MaterialIcons";
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
  const [imageSource, setImageSource] = useState([]);
  const [photoCount, setPhotoCount] = useState(0);
  const [isCapturing, setIsCapturing] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [showCamera, setShowCamera] = useState(true);
  const [faceData, setFaceData] = useState([]);
  const [cameraError, setCameraError] = useState(null);
  const [batteryState, setBatteryState] = useState(null);
  const [appState, setAppState] = useState(AppState.currentState); // State to track app state
  const [extraImages, setExtraImages] = useState(0); // Track extra images to be taken

  // Add log in useEffect to check AppState changes
  useEffect(() => {
    console.log("AppState change detected: ", appState);
    const subscription = AppState.addEventListener("change", (nextAppState) => {
      if (appState.match(/inactive|background/) && nextAppState === "active") {
        console.log("App has come to the foreground!");
        if (isCapturing) {
          startCapturing(); // Resume capturing if it was active
        }
      } else if (nextAppState === "background") {
        console.log("App is going to the background!");
        pauseCapturing();
      }
      setAppState(nextAppState);
    });

    return () => {
      subscription.remove();
    };
  }, [appState, isCapturing, startCapturing, pauseCapturing]);

  console.log(appState, "appState");

  const photoCountRef = useRef(0); // Add this ref to track the photo count directly

  const captureInterval = useRef(null);
  const cameraRef = useRef(null);

  const TOTAL_IMAGES_INITIAL = 60;

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
      photoCountRef.current < TOTAL_IMAGES // Use TOTAL_IMAGES instead of TOTAL_IMAGES_INITIAL
    ) {
      try {
        console.log("Attempting to capture a photo...");
        // Create folder based on referNumber
        const folderUri = await createFolderIfNotExists(referNumber);

        const photo = await cameraRef.current.takePictureAsync({
          quality: 1,
          skipProcessing: true,
          mute: true, // Mute the camera sound
        });
        console.log("Photo captured successfully.");

        // Move the photo to the folder
        const fileName = `${folderUri}/${referNumber}_${photoCountRef.current}.jpg`;
        await FileSystem.moveAsync({
          from: photo.uri,
          to: fileName,
        });

        // Add the moved image to Media Library (to make it visible in the gallery)
        await MediaLibrary.createAssetAsync(fileName);

        // Update state
        setImageSource((prevImages) => [...prevImages, fileName]);
        setPhotoCount((prevCount) => prevCount + 1);
        photoCountRef.current += 1;
      } catch (error) {
        console.error("Error capturing photo:", error);
      }
    } else if (photoCountRef.current >= TOTAL_IMAGES) {
      console.log("Reached the limit for total images.");

      // Adjust to TOTAL_IMAGES
      Alert.alert("Limit Reached", "Do you want to take more pictures", [
        { text: "Finish", onPress: ForwardImages },
        {
          text: "Take More",
          onPress: handleTakeMore, // Call handleTakeMore if user wants to take more images
        },
      ]);
    }
  }, [hasPermission, extraImages, referNumber]); // Add extraImages and referNumber as dependencies

  const startCapturing = useCallback(async () => {
    console.log("startCapturing called.");
    const TOTAL_IMAGES = TOTAL_IMAGES_INITIAL + extraImages; // Adjust the total images based on extraImages

    // Wait for the permission to be checked before starting
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
      console.log("Starting to capture photos...");

      // Updated captureInterval block
      captureInterval.current = setInterval(() => {
        try {
          if (photoCountRef.current < TOTAL_IMAGES) {
            capturePhoto();
          } else {
            clearInterval(captureInterval.current); // Stop interval when limit is reached
            captureInterval.current = null; // Reset interval
            setIsCapturing(false);
            Alert.alert("Limit Reached", "Do you want to take more pictures?", [
              { text: "Finish", onPress: ForwardImages },
              {
                text: "Take More",
                onPress: handleTakeMore, // Call handleTakeMore if user wants to take more images
              },
            ]);
          }
        } catch (error) {
          console.error("Error during capture interval:", error);
          clearInterval(captureInterval.current); // Stop interval on error
          captureInterval.current = null; // Reset interval
          setIsCapturing(false);
        }
      }, 2000); // Adjust the interval time here if needed
    } else if (photoCountRef.current >= TOTAL_IMAGES) {
      Alert.alert("Limit Reached", "Do you want to take more pictures?", [
        { text: "Finish", onPress: ForwardImages },
        {
          text: "Take More",
          onPress: handleTakeMore, // Call handleTakeMore if user wants to take more images
        },
      ]);
    }
  }, [capturePhoto, hasPermission, extraImages]);

  useEffect(() => {
    if (extraImages > 0) {
      startCapturing(); // Start capturing again when extraImages is updated
    }
  }, [extraImages, startCapturing]);

  const handleTakeMore = useCallback(() => {
    setExtraImages((prevExtraImages) => prevExtraImages + 20); // Just update the extra images count
    console.log(extraImages, "extraImages");
  }, []);

  const pauseCapturing = useCallback(() => {
    if (captureInterval.current) {
      clearInterval(captureInterval.current);
      captureInterval.current = null;
    }
    setIsCapturing(false);
  }, []);

  const ForwardImages = useCallback(() => {
    if (captureInterval.current) {
      clearInterval(captureInterval.current);
      captureInterval.current = null;
    }
    setIsCapturing(false);
    setShowCamera(false);
  }, []);

  const confirmRetake = () => {
    Alert.alert(
      "Retake",
      "Are you sure you want to retake the images?",
      [
        {
          text: "Cancel",
          onPress: () => console.log("Cancel Pressed"),
          style: "cancel",
        },
        {
          text: "Yes",
          onPress: handleRetake, // Call handleRetake if confirmed
        },
      ],
      { cancelable: false }
    );
  };

  // Adding a delay before remounting the camera
  const handleRetake = useCallback(() => {
    setShowCamera(false);
    setTimeout(() => {
      setShowCamera(true);
    }, 300); // Adding a 300ms delay
  }, []);

  const openGallery = useCallback(() => {
    if (Platform.OS === "android") {
      Linking.openURL("content://media/internal/images/media");
    } else if (Platform.OS === "ios") {
      Linking.openURL("photos-redirect://");
    }
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

  return (
    <>
      <StatusBar barStyle={"dark-content"} />
      <View style={styles.container}>
        {cameraError && <Text style={styles.errorText}>{cameraError}</Text>}
        {imageSource.length > 0 && (
          <Image
            style={styles.image}
            source={{ uri: imageSource[imageSource.length - 1] }}
          />
        )}
        <TouchableOpacity
          style={[styles.progressBarImage, { flexDirection: "row" }]}
          onPress={openGallery}
        >
          <MaterialIcons name="photo-library" size={20} color={"#fff"} />
          <Text style={styles.progressText}>+ {photoCount} images</Text>
        </TouchableOpacity>

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
            <View style={styles.indicatorStyles}>
              <Image
                style={{ width: 100, height: 100 }}
                source={require("../../assets/Image/mobile.png")}
              />
              <Image
                style={{
                  width: 30,
                  height: 30,
                  left: 30,
                }}
                source={require("../../assets/Image/left.gif")}
              />
            </View>
            <View style={styles.UserIdentifiedContainer}>
              <View style={styles.ProgressBarStyles}>
                <ProgressBar progress={progress} color={MD3Colors.error50} />
              </View>

              <TouchableOpacity
                style={[styles.progressBar, { flexDirection: "row" }]}
              >
                <MaterialIcons name="face" size={20} color={"#fff"} />
                <Text style={styles.progressText}>face detected</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.progressBar, { flexDirection: "row" }]}
                onPress={openGallery}
              >
                <MaterialIcons name="photo-library" size={20} color={"#fff"} />
                <Text style={styles.progressText}>+ {photoCount} images</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.progressBar, { flexDirection: "row" }]}
              >
                <MaterialIcons name="usb" size={20} color={"#fff"} />
                <Text style={styles.progressText}>
                  {batteryState === Battery.BatteryState.CHARGING
                    ? "Connected"
                    : "Not Connected"}
                </Text>
              </TouchableOpacity>
            </View>
            <View style={styles.bottomBar}>
              <TouchableOpacity
                style={styles.camFlip}
                onPress={() => navigation.navigate("IntroScreen")}
              >
                <AntDesign name="back" size={25} color="#fff" />
              </TouchableOpacity>
              {isCapturing ? (
                <TouchableOpacity
                  style={[styles.camButton, styles.pauseButton]}
                  onPress={pauseCapturing}
                >
                  <Feather name="pause" size={25} color="#fff" />
                </TouchableOpacity>
              ) : (
                <TouchableOpacity
                  style={styles.camButton}
                  onPress={startCapturing}
                ></TouchableOpacity>
              )}
              <TouchableOpacity
                style={styles.camPause}
                onPress={() => {
                  Alert.alert(
                    "Finish Scan",
                    "Do you want to finish this scan or take more?",
                    [
                      {
                        text: "Finish",
                        onPress: ForwardImages, // Call ForwardImages when user clicks "Finish"
                      },
                      {
                        text: "Take More",
                        onPress: handleTakeMore, // Call handleTakeMore to take more images
                      },
                    ]
                  );
                }}
              >
                <Text style={styles.progressText}>Done</Text>
              </TouchableOpacity>
            </View>
          </>
        ) : (
          <>
            {imageSource.length > 0 && (
              <Image
                style={styles.fullImage}
                source={{ uri: imageSource[imageSource.length - 1] }}
                resizeMode="cover"
              />
            )}
            <TouchableOpacity
              style={[styles.progressBarImage, { flexDirection: "row" }]}
              onPress={openGallery}
            >
              <MaterialIcons name="photo-library" size={20} color={"#fff"} />
              <Text style={styles.progressText}>+ {photoCount} images</Text>
            </TouchableOpacity>

            <View style={styles.buttonContainer}>
              <View style={styles.buttons}>
                <TouchableOpacity
                  style={styles.retakeButton}
                  onPress={confirmRetake}
                >
                  <AntDesign name="back" size={15} color="#fff" />
                  <Text style={styles.retakeText}>Retake</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.processButton}
                  onPress={() => setIsModalVisible(true)}
                >
                  <AntDesign name="sharealt" size={18} color="#0D0D0D" />
                  <Text style={styles.processText}>Send for processing</Text>
                </TouchableOpacity>
              </View>
            </View>
          </>
        )}

        <Modal
          animationType="slide"
          transparent={true}
          visible={isModalVisible}
          onRequestClose={() => {
            setIsModalVisible(false);
          }}
        >
          {!isCharging ? (
            <TouchableOpacity
              style={styles.modalContainer}
              activeOpacity={1}
              onPressOut={() => setIsModalVisible(false)}
            >
              <View style={styles.modalContent}>
                <Image
                  style={{ width: 200, height: 200 }}
                  source={require("../../assets/Image/connectusb.gif")}
                />
                <Text style={styles.modalTitle}>Reference number</Text>
                <Text style={styles.BoldTextStyle}>{referNumber}</Text>
                <Text style={styles.modalTitle}>
                  USB is not connected. Please connect USB.
                </Text>
              </View>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity
              style={styles.modalContainer}
              activeOpacity={1}
              onPressOut={() => setIsModalVisible(false)}
            >
              <View style={styles.modalContent}>
                <Image style={{ width: 200, height: 200 }} source={UsbGif} />
                <Text style={styles.modalTitle}>Reference number</Text>
                <Text style={styles.BoldTextStyle}>{referNumber}</Text>
                <Text style={styles.modalTitle}>USB is connected</Text>
              </View>
            </TouchableOpacity>
          )}
        </Modal>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  buttonContainer: {
    position: "absolute",
    justifyContent: "space-around",
    alignItems: "center",
    width: "100%",
    bottom: 0,
    paddingHorizontal: 10,
    paddingBottom: 30,
  },
  buttons: {
    flexDirection: "row",
    justifyContent: "space-between",
    width: "100%",
  },
  camButton: {
    height: 80,
    width: 80,
    borderRadius: 40,
    backgroundColor: "red",
    alignSelf: "center",
    borderWidth: 4,
    borderColor: "white",
  },
  pauseButton: {
    backgroundColor: "rgba(255, 255, 255, 0.12)",
    borderColor: "white",
    justifyContent: "center",
    alignItems: "center",
  },
  camFlip: {
    height: 50,
    width: 50,
    borderRadius: 40,
    backgroundColor: "rgba(255, 255, 255, 0.12)",
    borderColor: "white",
    justifyContent: "center",
    alignItems: "center",
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
  progressBarImage: {
    backgroundColor: "#00000059",
    flexDirection: "row",
    paddingVertical: 8,
    paddingHorizontal: 20,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 20,
    position: "absolute",
    bottom: 120,
  },
  progressBar: {
    backgroundColor: "#00000059",
    flexDirection: "row",
    paddingVertical: 8,
    paddingHorizontal: 7,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 20,
  },
  progressText: {
    color: "#ffffff",
    paddingLeft: 6,
  },
  bottomBar: {
    flexDirection: "row",
    position: "absolute",
    bottom: 30,
    justifyContent: "space-around",
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
    paddingLeft: 6,
  },
  fullImage: {
    width: "100%",
    height: "100%",
    flex: 1,
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
    bottom: 50,
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
    backgroundColor: "#92E622",
    paddingHorizontal: 10,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 50,
    width: 205,
    height: 60,
    flexDirection: "row",
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
