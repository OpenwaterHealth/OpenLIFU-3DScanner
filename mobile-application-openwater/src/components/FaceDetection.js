import { Camera } from "expo-camera";
import * as FaceDetector from "expo-face-detector";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

export default function FaceDetection({ navigation, route }) {
  const referNumber = useRef(route?.params?.referNumber);
  const [hasPermission, setHasPermission] = useState(null);
  const [faceDetected, setFaceDetected] = useState(false);
  const [showSuccessMessage, setShowSuccessMessage] = useState(false);
  const [isCameraActive, setIsCameraActive] = useState(true);
  const [cameraError, setCameraError] = useState(null);
  const faceDataRef = useRef([]); // Using useRef to store face data without triggering re-renders
  const detectionTimeoutRef = useRef(null); // Ref for the timeout

  useEffect(() => {
    (async () => {
      try {
        const { status } = await Camera.requestCameraPermissionsAsync();
        setHasPermission(status === "granted");
      } catch (error) {
        console.error("Error requesting camera permissions:", error);
        setCameraError("Failed to request camera permissions.");
        setHasPermission(false);
      }
    })();

    return () => {
      setIsCameraActive(false); // Cleanup camera when component unmounts or navigation happens
      if (detectionTimeoutRef.current) {
        clearTimeout(detectionTimeoutRef.current); // Clear timeout if component unmounts
      }
    };
  }, []);

  const handleCameraError = useCallback((error) => {
    console.error("Camera Error: ", error);
    setCameraError(
      "There was an error accessing the camera. Please try again."
    );
    setIsCameraActive(false);
  }, []);

  const handleFacesDetected = useCallback(
    ({ faces }) => {
      if (faces.length > 0) {
        // console.log("Faces detected:", faces);
        faceDataRef.current = faces; // Update face data reference

        if (!faceDetected) {
          setFaceDetected(true);

          detectionTimeoutRef.current = setTimeout(() => {
            setShowSuccessMessage(true);

            // Navigate to CameraComponent after 2 seconds and turn off the camera
            setTimeout(() => {
              setIsCameraActive(false); // Turn off the camera
              navigation.navigate("CameraComponent", {
                referNumber: referNumber.current,
              });
            }, 2000);
          }, 3000);
        }
      } else {
        setFaceDetected(false);
        setShowSuccessMessage(false);
      }
    },
    [faceDetected, navigation]
  );

  const renderFaceBoxes = () => {
    return faceDataRef.current.map((face, index) => {
      const { origin, size } = face.bounds;

      // Calculate the left, top, width, and height of the square
      const left = origin.x;
      const top = origin.y;
      const width = size.width;
      const height = size.height;

      return (
        <View
          key={index}
          style={[
            styles.faceBox,
            {
              left: left,
              top: top,
              width: width,
              height: height,
            },
          ]}
        />
      );
    });
  };

  if (hasPermission === null) {
    return (
      <View>
        <Text>Requesting camera permission...</Text>
      </View>
    );
  }
  if (hasPermission === false) {
    return <Text>No access to camera</Text>;
  }

  return (
    <View style={{ flex: 1 }}>
      {isCameraActive && (
        <Camera
          style={styles.camera}
          type={Camera.Constants.Type.back}
          onFacesDetected={handleFacesDetected}
          faceDetectorSettings={{
            mode: FaceDetector.FaceDetectorMode.fast,
            detectLandmarks: FaceDetector.FaceDetectorLandmarks.all,
            runClassifications: FaceDetector.FaceDetectorClassifications.none,
            tracking: true,
          }}
          onMountError={(error) => {
            handleCameraError(error);
          }}
        >
          {renderFaceBoxes()}
        </Camera>
      )}
      <View style={styles.successMessageContainer}>
        <Text style={styles.successMessageText}>
          {cameraError
            ? cameraError
            : showSuccessMessage
            ? "Face detected successfully!"
            : "Scan in progress"}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  camera: {
    flex: 1,
  },
  faceBox: {
    position: "absolute",
    borderWidth: 2,
    borderColor: "#92E622",
    backgroundColor: "transparent",
  },
  successMessageContainer: {
    position: "absolute",
    bottom: 50,
    left: 0,
    right: 0,
    alignItems: "center",
  },
  successMessageText: {
    fontSize: 12,
    color: "#fff",
    backgroundColor: "#00000059",
    padding: 10,
    borderRadius: 5,
  },
});
