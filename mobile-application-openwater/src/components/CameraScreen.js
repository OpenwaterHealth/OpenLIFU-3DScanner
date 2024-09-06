import * as Battery from "expo-battery";
import { Camera } from "expo-camera";
import * as FaceDetector from "expo-face-detector";
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

async function requestStoragePermission() {
  if (Platform.OS === "android") {
    const { status } = await MediaLibrary.requestPermissionsAsync();
    return status === "granted";
  }
  return true;
}

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

  const photoCountRef = useRef(0); // Add this ref to track the photo count directly

  const captureInterval = useRef(null);
  const cameraRef = useRef(null);

  const TOTAL_IMAGES = 50;

  const progress = useMemo(() => photoCount / TOTAL_IMAGES, [photoCount]);

  const handlePermissionRequest = useCallback(async () => {
    const cameraGranted = await requestCameraPermission();
    const storageGranted = await requestStoragePermission();
    setHasPermission(cameraGranted && storageGranted);
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

  console.log(isCharging, "isCharging");

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

  const capturePhoto = useCallback(async () => {
    if (
      hasPermission &&
      cameraRef.current !== null &&
      photoCountRef.current < TOTAL_IMAGES
    ) {
      try {
        const photo = await cameraRef.current.takePictureAsync({
          quality: 1,
          skipProcessing: true, // Skip processing, which may include autofocus
        });
        const asset = await MediaLibrary.createAssetAsync(photo.uri);
        setImageSource((prevImages) => [...prevImages, asset.uri]);
        setPhotoCount((prevCount) => prevCount + 1); // Update the state for rendering
        photoCountRef.current += 1; // Update the ref immediately
      } catch (error) {
        console.error("Error capturing photo:", error);
      }
    } else if (photoCountRef.current >= TOTAL_IMAGES) {
      Alert.alert(
        "Limit Reached",
        "You have already captured 50 images. No more captures allowed.",
        [{ text: "OK", onPress: () => console.log("Limit alert dismissed") }]
      );
    }
  }, [hasPermission]);

  const startCapturing = useCallback(() => {
    if (hasPermission && photoCountRef.current < TOTAL_IMAGES) {
      if (captureInterval.current) {
        clearInterval(captureInterval.current);
      }
      setIsCapturing(true);
      captureInterval.current = setInterval(() => {
        if (photoCountRef.current < TOTAL_IMAGES) {
          capturePhoto();
        } else {
          clearInterval(captureInterval.current); // Stop interval when limit is reached
          setIsCapturing(false);
          Alert.alert(
            "Limit Reached",
            "You have captured 50 images, no more captures are allowed.",
            [
              {
                text: "OK",
                onPress: () => console.log("Capture limit reached"),
              },
            ]
          );
        }
      }, 1000);
    } else if (photoCountRef.current >= TOTAL_IMAGES) {
      Alert.alert(
        "Limit Reached",
        "You have already captured 50 images. No more captures allowed.",
        [{ text: "OK", onPress: () => console.log("Limit alert dismissed") }]
      );
    }
  }, [capturePhoto, hasPermission]);

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
  console.log(progress, "progress");
  console.log(photoCount, "photoCount");

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
              style={StyleSheet.absoluteFill}
              type={Camera.Constants.Type.back}
              onFacesDetected={handleFacesDetected}
              autoFocus={Camera.Constants.AutoFocus.off} // Disable autofocus
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
                  style={[
                    styles.camButton,
                    {
                      backgroundColor: "rgba(255, 255, 255, 0.12)",
                      borderColor: "white",
                      justifyContent: "center",
                      alignItems: "center",
                    },
                  ]}
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
              <TouchableOpacity style={styles.camPause} onPress={ForwardImages}>
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
                  style={{
                    backgroundColor: "#0D0D0D",
                    width: 127,
                    height: 60,
                    justifyContent: "center",
                    alignItems: "center",
                    borderRadius: 50,
                    flexDirection: "row",
                  }}
                  onPress={confirmRetake}
                >
                  <AntDesign name="back" size={15} color="#fff" />
                  <Text style={styles.retakeText}>Retake</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={{
                    backgroundColor: "#92E622",
                    paddingHorizontal: 10,
                    justifyContent: "center",
                    alignItems: "center",
                    borderRadius: 50,
                    width: 205,
                    height: 60,
                    flexDirection: "row",
                  }}
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
    borderWidth: 2,
    borderColor: "#92E622",
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
  lottie: {
    width: 256,
    height: 256,
    bottom: 60,
    position: "absolute",
  },
  textView: {
    position: "absolute",
    bottom: 170,
    color: "#fff",
    fontSize: 12,
  },
  indicatorStyles: {
    backgroundColor: "transparent",
    tintColor: "#fff",
    bottom: 180,
    position: "absolute",
    left: 0,
  },
  ProgressBarStyles: {
    bottom: 50,
    left: 10,
    position: "absolute",
    width: "90%",
  },
});

export default CameraComponent;
