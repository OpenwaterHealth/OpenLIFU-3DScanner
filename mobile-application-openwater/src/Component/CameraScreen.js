import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  getBrightnessLevel,
  setBrightnessLevel,
} from '@reeq/react-native-device-brightness';
import piexif from 'piexifjs';
import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  ActivityIndicator,
  Alert,
  Animated,
  AppState,
  FlatList,
  Image,
  LogBox,
  Modal,
  Platform,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import DeviceInfo from 'react-native-device-info';
import FastImage from 'react-native-fast-image';
import RNFS from 'react-native-fs';
import {Gesture, GestureDetector} from 'react-native-gesture-handler';
import {Checkbox} from 'react-native-paper';
import {PERMISSIONS, request} from 'react-native-permissions';
import Icon from 'react-native-vector-icons/AntDesign';
import Video from 'react-native-video';
import {
  Camera,
  useCameraDevice,
  useCameraPermission,
} from 'react-native-vision-camera';
import UsbGif from '../../assets/Image/usb-connected.png';

const CameraComponent = ({route, navigation}) => {
  const referNumber = route.params?.referNumber || '3dFaceApp';
  const cameraRef = useRef(null);
  const device = useCameraDevice('back', {
    physicalDevices: [
      'ultra-wide-angle-camera',
      'wide-angle-camera',
      'telephoto-camera',
    ],
  });
  const isCapturingRef = useRef(false);
  const photoCountRef = useRef(0);
  const focusTimeout = useRef(null); // Store timeout reference to throttle focus

  const [isCameraInitialized, setIsCameraInitialized] = useState(false); // Check if camera is ready
  const [isFocusing, setIsFocusing] = useState(false); // Track whether focus is in progress
  const [tapPosition, setTapPosition] = useState({x: 0, y: 0}); // Position for the circle
  const [showCircle, setShowCircle] = useState(false); // Show or hide the circle
  const fadeAnim = useRef(new Animated.Value(1)).current; // Opacity for the circle
  const {hasPermission, requestPermission} = useCameraPermission();
  const [isProcessing, setIsProcessing] = useState(false);
  const [isCapturing, setIsCapturing] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [currentPhotoIndex, setCurrentPhotoIndex] = useState(0);
  const [stage, setStage] = useState('initial'); // 'initial', 'paused', 'stopped'
  const [progressBarColor, setProgressBarColor] = useState('#87CEEB');
  const [extraImages, setExtraImages] = useState(0); // Track extra images to be taken
  const [showCamera, setShowCamera] = useState(true);
  const [isChecked, setIsChecked] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [videoPaused, setVideoPaused] = useState(true); // Controls video playback
  const [dropdownPosition, setDropdownPosition] = useState({x: 0, y: 0}); // Position for the dropdown
  const [totalImages, setTotalImages] = useState(120); // Default total images is 120
  const [batteryState, setBatteryState] = useState(null);
  const [blink, setBlink] = useState(false); // State for blink effect
  const [delay, setDelay] = useState(1); // Default delay is 3 seconds
  const [isDropdownVisible, setIsDropdownVisible] = useState(false);
  const [isTotalImagesDropdownVisible, setIsTotalImagesDropdownVisible] =
    useState(false);
  const [cameraActive, setCameraActive] = useState(true); // To control camera state
  const [selectedButton, setSelectedButton] = useState('clock'); // Default selected button
  const [captureMessage, setCaptureMessage] = useState('Move'); // Track the current message
  const delayOptions = [5, 3, 2, 1.5, 1]; // Available delay options in seconds
  const totalImagesOptions = [120, 100, 80, 60, 40]; // Available total images options
  const [transferComplete, setTransferComplete] = useState(false); // Track when the transfer is complete
  const imageFolderPath = `${RNFS.ExternalStorageDirectoryPath}/DCIM/Camera/`; // To prevent multiple captures while processing
  const [fileCount, setFileCount] = useState(0);
  const [isMonitoring, setIsMonitoring] = useState(false); // Flag to track if monitoring is already started
  const [appState, setAppState] = useState(AppState.currentState); // Track app state
  const [backgroundColor, setBackgroundColor] = useState('#000'); // Set initial background color to black
  const [blinkCount, setBlinkCount] = useState(0);
  const [captureComplete, setCaptureComplete] = useState(false);
  const [cameraDimensions, setCameraDimensions] = useState({
    x: 0,
    y: 0,
    width: 0,
    height: 0,
  });
  const [brightness, setBrightness] = useState(null); // State to store brightness
  const [transferStatus, setTransferStatus] = useState('not_started'); // 'not_started', 'in_progress', 'completed'
  const background = require('../../assets/Video/3d_2.mp4');
  LogBox.ignoreAllLogs(true);

  const instructions = [
    'Choose a well-lit area (soft indoor light works best).',
    'Stand at eye level and walk slowly around the subject in a full circle(1-foot distance). capture at least 70 images.',
    'Subject should remain still with eyes closed and no expression change.',
    'After full circle shots, capture 10 vertical photos from chin to top of head.',
    'Focus only on the head, avoiding areas below the neck.',
  ];

  useEffect(() => {
    const fetchAndSetBrightness = async () => {
      try {
        const brightnessLevel = await getBrightnessLevel(); // Await for the brightness level
        setBrightness(brightnessLevel);

        if (brightnessLevel < 1) {
          await setBrightnessLevel(1.0);
        }
      } catch {}
    };

    fetchAndSetBrightness(); // Call the function to get and set the brightness level
  }, []);

  const handleAppStateChange = nextAppState => {
    if (appState.match(/inactive|background/) && nextAppState === 'active') {
      setCameraActive(true); // Ensure the camera is activated
    } else if (nextAppState === 'background') {
      setCameraActive(false); // Deactivate the camera when app goes to background
    }
    setAppState(nextAppState); // Update app state
  };

  // ___________________________________________Checking Permission_______________________________________________________//
  const checkPermission = async () => {
    const cameraPermission = await Camera.getCameraPermissionStatus();
    const microphonePermission = await Camera.getMicrophonePermissionStatus();

    if (
      cameraPermission !== 'authorized' ||
      microphonePermission !== 'authorized'
    ) {
      await Camera.requestCameraPermission();
      await Camera.requestMicrophonePermission();
    }

    if (Platform.OS === 'android') {
      const storagePermission = await request(
        PERMISSIONS.ANDROID.WRITE_EXTERNAL_STORAGE,
      );
    }
  };

  useEffect(() => {
    checkPermission();
  }, []);
  // ___________________________________________Checking Permission_______________________________________________________//

  // Function to initialize the camera if not ready
  const initializeCamera = async () => {
    if (!cameraRef.current || !hasPermission || !device) {
      console.log(
        'Camera initialization failed: device or permission not ready.',
      );
      return;
    }

    try {
      setIsCameraInitialized(true);
      cameraRef.current.initialize();
    } catch {}
  };

  useEffect(() => {
    const subscription = AppState.addEventListener(
      'change',
      handleAppStateChange,
    );

    return () => {
      subscription.remove(); // Clean up listener on unmount
    };
  }, [appState, hasPermission, device]);

  // __________________________________________Tab to focus ____________________________________________________________//

  // Throttle focus requests to only happen every 500ms
  const throttleFocus = (x, y) => {
    if (focusTimeout.current) {
      clearTimeout(focusTimeout.current); // Cancel the previous focus attempt
    }

    focusTimeout.current = setTimeout(() => {
      focusOnTap(x, y); // Trigger focus after throttling
    }, 500); // 500ms throttle time, adjust as needed
  };

  // Function to handle focus on tap
  const focusOnTap = async (x, y) => {
    if (isFocusing) return; // Prevent multiple focus requests

    setIsFocusing(true); // Set focusing to true
    setTapPosition({x, y}); // Update the tap position
    setShowCircle(true); // Show the focus circle

    try {
      if (cameraRef.current && device?.supportsFocus) {
        await cameraRef.current.focus({x, y, mode: 'auto'}); // Perform focus
      }
    } catch {
    } finally {
      setIsFocusing(false); // Reset focusing flag
      Animated.timing(fadeAnim, {
        toValue: 0, // Fade out the circle after focus
        duration: 500, // Circle fades out in 500ms
        useNativeDriver: true,
      }).start(() => setShowCircle(false)); // Hide the circle after fading
    }
  };

  // ____________________________________________Tab to focus __________________________________________________________//

  const handleButtonPress = button => {
    setSelectedButton(button); // Update the selected button

    // Automatically toggle the corresponding modal
    if (button === 'clock') {
      setIsDropdownVisible(!isDropdownVisible);
      setIsTotalImagesDropdownVisible(false); // Close other dropdown if open
    } else if (button === 'picture') {
      setIsTotalImagesDropdownVisible(!isTotalImagesDropdownVisible);
      setIsDropdownVisible(false); // Close other dropdown if open
    }
  };

  // __________________________________Transferring Files_____________________________________________//
  useEffect(() => {
    if (stage === 'stopped' && !isMonitoring) {
      setIsMonitoring(true); // Set monitoring flag to true
      startTransferMonitoring(); // Start monitoring file transfers
    }
  }, [stage, isMonitoring]);

  const startTransferMonitoring = () => {
    // Start monitoring the transfer of images
    const interval = setInterval(() => {
      checkTransferredImages();
    }, 5000); // Check every 5 seconds

    return () => clearInterval(interval); // Clear interval on component unmount or when monitoring stops
  };

  const checkTransferredImages = async () => {
    try {
      const files = await RNFS.readDir(imageFolderPath);

      const matchingFiles = files.filter(file =>
        file.name.startsWith(`${referNumber}_`),
      );

      const newFileCount = matchingFiles.length; // Count only matching files
      setFileCount(newFileCount); // Step 2: Update fileCount using setFileCount

      const remainingImages = newFileCount;

      // if (remainingImages > 0) {
      // } else if (remainingImages != newFileCount) {
      // } else if (remainingImages == 0) {
      //   setTransferComplete(true);
      //   resetTransfer(); // Set transfer complete when all images are transferred
      // }
    } catch {}
  };
  // __________________________________Transferring Files_____________________________________________//

  useEffect(() => {
    // Call `checkTransferredImages` periodically to check the progress
    const interval = setInterval(() => {
      if (!transferComplete) {
        checkTransferredImages();
      }
    }, 5000); // Update every 5 seconds

    return () => clearInterval(interval); // Clear interval on component unmount
  }, [transferComplete]);

  const toggleDropdown = event => {
    const {pageX, pageY} = event.nativeEvent;
    setDropdownPosition({x: pageX, y: pageY});
    setIsDropdownVisible(!isDropdownVisible);
  };

  const toggleTotalImagesDropdown = event => {
    const {pageX, pageY} = event.nativeEvent;
    setDropdownPosition({x: pageX, y: pageY});
    setIsTotalImagesDropdownVisible(!isTotalImagesDropdownVisible);
  };

  const selectTotalImages = selectedTotal => {
    setTotalImages(selectedTotal);
    setIsTotalImagesDropdownVisible(false);
  };

  const selectDelay = selectedDelay => {
    setDelay(selectedDelay);
    setIsDropdownVisible(false);
  };

  const toggleModal = () => {
    setVideoPaused(true);
    setIsModalVisible(!isModalVisible);
  };

  const handleCheckboxToggle = async () => {
    const newValue = !isChecked;
    setIsChecked(newValue);
    await AsyncStorage.setItem('showModal', (!newValue).toString()); // Save the inverse preference
  };

  useEffect(() => {
    const checkBatteryStatus = async () => {
      const isCharging = await DeviceInfo.isBatteryCharging();
      setBatteryState(isCharging ? 'Charging' : 'Not Charging');
      ``;
    };

    checkBatteryStatus();

    const interval = setInterval(() => {
      checkBatteryStatus();
    }, 5000); // Check every 5 seconds

    // Clear the interval on component unmount
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const checkModalPreference = async () => {
      const shouldShowModal = await AsyncStorage.getItem('showModal');
      if (shouldShowModal === null || shouldShowModal === 'true') {
        setIsModalVisible(true);
      }
    };
    checkModalPreference();
  }, []);

  const captureInterval = useRef(null);

  // const captureSound = new Sound(
  //   require('../../assets/mp3/cameasound.mp3'), // Make sure this path is correct
  //   Sound.MAIN_BUNDLE,
  //   error => {
  //     if (error) {
  //       return;
  //     }
  //     // Play the sound if it is loaded successfully
  //     captureSound.play();
  //   },
  // );

  const ForwardImages = useCallback(() => {
    if (captureInterval.current) {
      clearInterval(captureInterval.current);
      captureInterval.current = null;
    }
    setIsCapturing(false);
    setIsCameraInitialized(false);
    setShowCamera(false);
    setProgressBarColor('#92E622');
  }, []);

  const handleFinish = useCallback(() => {
    setBackgroundColor('#fff');
    setTransferComplete(false);
    ForwardImages();
  }, [ForwardImages]);

  useEffect(() => {
    // Request necessary permissions on component mount
    const getPermissions = async () => {
      await requestPermission(); // Request camera and microphone permissions
      await request(PERMISSIONS.ANDROID.WRITE_EXTERNAL_STORAGE); // Request storage permission
      await request(PERMISSIONS.ANDROID.READ_EXTERNAL_STORAGE); // Request read permission
    };

    getPermissions();
  }, [requestPermission]);

  // Check if the camera devices are still loading or if permission isn't available yet
  if (!device || !hasPermission) {
    return <ActivityIndicator size="large" color="#fff" />; // Show a loader until the camera is ready
  }

  const takeMultiplePhotos = () => {
    if (!cameraRef.current || isCapturing || isPaused) {
      console.log('Cannot capture, camera is not ready or process is paused.');
      setAppState('active');
      setIsCapturing(true);
      setStage('capturing');
      setIsPaused(false); // Ensure capturing is not paused
      isCapturingRef.current = true;
    }

    setIsCapturing(true);
    setStage('capturing');
    setIsPaused(false); // Ensure capturing is not paused
    isCapturingRef.current = true;

    const totalImagesToCapture = totalImages + extraImages;
    let currentImageIndex = photoCountRef.current;

    // Set an interval to take photos at regular intervals
    const captureInterval = setInterval(async () => {
      if (currentImageIndex < totalImagesToCapture && isCapturingRef.current) {
        try {
          setCaptureMessage('Stay'); // Display the move message
          await new Promise(resolve => setTimeout(resolve, 200)); // Wait for the
          await takePhoto(currentImageIndex + 1);
          setCaptureMessage('Move'); // Display the move message
          photoCountRef.current = currentImageIndex + 1;
          currentImageIndex++;
          // setCurrentPhotoIndex(currentImageIndex + 1);
          if (currentImageIndex === totalImagesToCapture) {
            clearInterval(captureInterval);
            setStage('stopped');
            setIsCapturing(false);
            setProgressBarColor('#FBBB04'); // Change progress bar color
            isCapturingRef.current = false;
          }
        } catch {}
      } else {
        clearInterval(captureInterval);
        setIsCapturing(false);
        isCapturingRef.current = false;
      }
    }, delay * 2200); // Interval based on delay (in milliseconds)
  };

  const triggerBlinkEffect = () => {
    setBlink(true);
    setBlinkCount(prevCount => prevCount + 1);
    setTimeout(() => setBlink(false), 100); // Blink for 100ms
  };

  useEffect(() => {
    if (blinkCount > 0) {
      setCurrentPhotoIndex(prevIndex => prevIndex + 1);
    }
  }, [blinkCount]);

  const takePhoto = async index => {
    if (!cameraRef.current || isProcessing) return;

    setIsProcessing(true);

    try {
      // Capture the photo
      const photo = await cameraRef.current.takePhoto({
        qualityPrioritization: 'quality',
        quality: 100,
        enableAutoStabilization: true,
      });

      // Move and save the photo to a permanent location
      const destPath = await moveAndSavePhoto(photo.path, referNumber, index);

      // Modify EXIF data in the saved photo using the new path
      await modifyExifData(destPath);

      triggerBlinkEffect(); // Blink after photo
      photoCountRef.current = index;
    } catch (error) {
      console.error('Error taking photo:', error);
    } finally {
      setIsProcessing(false);
    }
  };

  // Function to move the file and save it to the /DCIM/3DFACE folder
  const moveAndSavePhoto = async (sourcePath, referNumber, index) => {
    const fileName = `${referNumber}_${index}.jpg`;
    const cameraFolderPath = `${RNFS.ExternalStorageDirectoryPath}/DCIM/Camera`;

    try {
      // Check if /DCIM/Camera exists, and create it if not
      if (!(await RNFS.exists(cameraFolderPath))) {
        await RNFS.mkdir(cameraFolderPath);
      }

      // Move the file to /DCIM/Camera
      const destPath = `${cameraFolderPath}/${fileName}`;
      await RNFS.moveFile(sourcePath, destPath);

      return destPath; // Return the new destination path
    } catch (error) {
      console.error('Error saving photo with RNFS:', error);
    }
  };

  const modifyExifData = async filePath => {
    try {
      // Read the image as base64
      const imageBase64 = await RNFS.readFile(filePath, 'base64');

      // Load the EXIF data
      const exifData = piexif.load(`data:image/jpeg;base64,${imageBase64}`);

      // Modify FocalLengthIn35mmFilm if not present or invalid
      if (
        !exifData['Exif'] ||
        !exifData['Exif'][piexif.ExifIFD.FocalLengthIn35mmFilm] ||
        exifData['Exif'][piexif.ExifIFD.FocalLengthIn35mmFilm] === 0
      ) {
        exifData['Exif'][piexif.ExifIFD.FocalLengthIn35mmFilm] = 26;
      }

      // Dump the modified EXIF data back to the image
      const newExifBytes = piexif.dump(exifData);
      const updatedImageBase64 = piexif.insert(
        newExifBytes,
        `data:image/jpeg;base64,${imageBase64}`,
      );

      // Remove the 'data:image/jpeg;base64,' prefix
      const updatedImage = updatedImageBase64.replace(
        'data:image/jpeg;base64,',
        '',
      );

      // Write the modified image back to the file
      await RNFS.writeFile(filePath, updatedImage, 'base64');
    } catch (error) {
      console.error('Error modifying EXIF data:', error);
    }
  };

  const stopCapturing = () => {
    isCapturingRef.current = false; // Stop capturing
    setIsCapturing(false); // Update capturing state
    setProgressBarColor('#FBBB04'); // Change progress bar color
    setIsPaused(true); // Pause the capturing

    if (currentPhotoIndex === totalImages + extraImages) {
      setStage('stopped'); // Stop the stage if all images are captured
    } else {
      setStage('paused'); // Pause the stage if not all images are captured
    }
  };

  const handleCaptureMore = useCallback(() => {
    const totalImagesCaptured = photoCountRef.current;
    const totalImagesRequired = totalImages + extraImages;

    // If the user hasn't captured the total required images yet, resume without adding extra
    if (totalImagesCaptured < totalImagesRequired) {
      setIsPaused(false);
      setStage('paused');
      return; // Do not add extra images
    }

    // If the user has completed the total required images, add 20 extra images
    setExtraImages(prevExtraImages => {
      const newExtraImages = prevExtraImages + 20;
      return newExtraImages;
    });

    setIsPaused(false);
    setStage('paused');
  }, [totalImages, extraImages]);

  const resumeCapturing = useCallback(async () => {
    setAppState('active');
    if (!isCameraInitialized || isPaused || !cameraActive) {
      initializeCamera(); // Ensure the camera is initialized
      setCameraActive(true); // Activate the camera
      setIsPaused(false);
    }

    // Add a slight delay to ensure camera becomes active
    setTimeout(() => {
      if (cameraActive) {
        setIsPaused(false); // Un-pause the capturing process
        isCapturingRef.current = true; // Set capturing flag
        setProgressBarColor('#87CEEB');
        takeMultiplePhotos(); // Resume capturing photos
      } else {
      }
    }, 100); // 1-second delay to ensure initialization
  }, [totalImages, extraImages, isCameraInitialized, cameraActive]);

  const handleFinalStop = () => {
    setIsPaused(false);
    setStage('stopped');
  };

  const handleDiscard = useCallback(() => {
    Alert.alert(
      'Confirmation',
      'Are you sure you want to discard?',
      [
        {
          text: 'No',
          onPress: () => console.log('Discard canceled'),
          style: 'cancel',
        },
        {
          text: 'Yes, Discard',
          onPress: () => {
            resetAllStates();
          },
        },
      ],
      {cancelable: true},
    );
  }, []);

  const resetAllStates = () => {
    setProgressBarColor('#87CEEB');
    setIsPaused(false);
    setStage('initial');
    setIsCapturing(false);
    setCurrentPhotoIndex(0);
    photoCountRef.current = 0;
    setExtraImages(0);
    setShowCamera(true);
  };

  if (!hasPermission) return <ActivityIndicator />;
  if (device == null) return <ActivityIndicator />;

  const handleGoBackFinish = () => {
    setStage('initial');
    setTransferComplete(false); // Reset the transferComplete flag when going back
    navigation.goBack();
  };

  return (
    <GestureDetector
      gesture={Gesture.Tap().onEnd(({x, y}) => {
        // Check if the tap is inside the camera bounds
        if (
          x >= cameraDimensions.x &&
          x <= cameraDimensions.x + cameraDimensions.width &&
          y >= cameraDimensions.y &&
          y <= cameraDimensions.y + cameraDimensions.height
        ) {
          throttleFocus(x, y); // Only call focus if tap is inside camera view
          Animated.timing(fadeAnim, {
            toValue: 1, // Make the circle visible immediately on tap
            duration: 0,
            useNativeDriver: true,
          }).start(); // Animate tap circle feedback
        }
      })}>
      <View style={{flex: 1}}>
        {/* Ensure a single root View for GestureDetector */}
        <StatusBar backgroundColor={'#000'} barStyle={'light-content'} />
        <View style={[styles.container, {backgroundColor}]}>
          {fileCount === 0 ? (
            // Show this image when fileCount is 0
            <FastImage
              style={styles.fullImage}
              source={require('../../assets/Image/success.gif')}
              resizeMode="contain"
            />
          ) : batteryState !== 'Charging' ? (
            <FastImage
              style={styles.fullImage}
              source={require('../../assets/Image/connectusb.gif')}
              resizeMode="cover"
            />
          ) : (
            <Image
              style={styles.fullImage}
              source={UsbGif}
              resizeMode="cover"
            />
          )}
          <View style={styles.ProgressBarContainer}>
            <View
              style={[styles.ProgressBar, {backgroundColor: progressBarColor}]}
            />
            {batteryState !== 'Charging' ? (
              <Text style={styles.ProgressText}>
                {fileCount == 0
                  ? 'Transfer Successfully'
                  : `Total Captured Images : ${fileCount}`}
              </Text>
            ) : (
              <Text style={styles.ProgressText}>
                {fileCount == 0
                  ? 'Transfer Successfully'
                  : `Transferring Images: ${fileCount}`}
              </Text>
            )}
          </View>

          <TouchableOpacity style={[styles.camButton]}>
            <Text style={styles.buttonTextStyles}>
              {batteryState === 'Charging'
                ? 'USB Connected'
                : 'USB Not Connected'}
            </Text>
          </TouchableOpacity>

          {fileCount === 0 && (
            <TouchableOpacity
              style={[styles.BackToHome]}
              onPress={handleGoBackFinish}>
              <Text
                style={[
                  styles.buttonTextStyles,
                  {color: 'green', marginRight: 10},
                ]}>
                Back to Home
              </Text>
              <Icon name="rightcircleo" color={'green'} size={25} />
            </TouchableOpacity>
          )}

          {blink && <View style={styles.blinkOverlay} />}

          {showCamera && (
            <>
              <View
                style={styles.CameraContainer}
                onLayout={event => {
                  const {x, y, width, height} = event.nativeEvent.layout;
                  setCameraDimensions({x, y, width, height});
                }}>
                <Camera
                  style={[styles.camera]}
                  device={device}
                  isActive={cameraActive} // Control camera based on state
                  ref={cameraRef}
                  photo={true}
                  onInitialized={() => setIsCameraInitialized(true)} // Camera is ready
                  enableHighQualityPhotos={true}
                  orientation="portrait"
                  AutoFocusSystem="contrast-detection"
                />
              </View>
              {showCircle && (
                <Animated.View
                  style={[
                    styles.circle,
                    {
                      top: tapPosition.y - 25,
                      left: tapPosition.x - 25,
                      opacity: fadeAnim,
                    },
                  ]}>
                  <View style={styles.innerBorder}></View>
                </Animated.View>
              )}
              {isCameraInitialized && (
                <>
                  <View
                    style={[
                      styles.ProgressBarContainer,
                      {borderColor: '#fff'},
                    ]}>
                    <View
                      style={[
                        styles.ProgressBar,
                        {
                          width: `${
                            (currentPhotoIndex / (totalImages + extraImages)) *
                            100
                          }%`,
                          backgroundColor: progressBarColor,
                        },
                      ]}
                    />
                    <Text style={[styles.ProgressText, {color: '#fff'}]}>
                      {`${currentPhotoIndex}/${totalImages + extraImages}`}
                    </Text>
                  </View>

                  <TouchableOpacity
                    style={styles.IconStyles}
                    onPress={toggleModal}>
                    <Icon name={'questioncircleo'} color="#fff" size={25} />
                  </TouchableOpacity>

                  {/* Instruction Video Modal */}
                  <Modal
                    transparent={true}
                    visible={isModalVisible}
                    animationType="none"
                    onRequestClose={toggleModal}
                    onShow={() => setVideoPaused(false)} // Autoplay video when modal opens
                  >
                    <View style={styles.modalBackground}>
                      <View style={styles.modalContainer}>
                        <TouchableOpacity
                          style={styles.closeButton}
                          onPress={toggleModal}>
                          <Icon name="close" size={20} color="#fff" />
                        </TouchableOpacity>
                        <View style={styles.VideoContainer}>
                          <Video
                            source={background}
                            style={styles.video}
                            controls={false}
                            resizeMode="contain"
                            repeat={true}
                            paused={videoPaused}
                            bufferConfig={{
                              minBufferMs: 15000,
                              maxBufferMs: 30000,
                              bufferForPlaybackMs: 2500,
                              bufferForPlaybackAfterRebufferMs: 5000,
                            }}
                          />
                        </View>
                        {instructions?.map((item, index) => (
                          <View style={styles.TextIconStyles} key={index}>
                            <Icon name="checkcircle" size={14} color="#000" />
                            <Text style={styles.modalText}>{item}</Text>
                          </View>
                        ))}

                        <View style={styles.checkboxContainer}>
                          <Checkbox
                            status={isChecked ? 'checked' : 'unchecked'}
                            onPress={handleCheckboxToggle}
                            color="black"
                          />
                          <Text style={styles.checkboxLabel}>
                            Don't Show again
                          </Text>
                        </View>
                      </View>
                    </View>
                  </Modal>

                  {/* Dropdown for Time Management */}
                  <Modal
                    transparent={true}
                    visible={isDropdownVisible}
                    onRequestClose={() => setIsDropdownVisible(false)}>
                    <TouchableOpacity
                      style={styles.modalBackground}
                      onPress={() => setIsDropdownVisible(false)}>
                      <View
                        style={[
                          styles.dropdownContainer,
                          // {position: 'absolute', bottom: '34.8%', left: 150},
                        ]}>
                        <TouchableOpacity style={styles.dropdownItem}>
                          <Text style={styles.dropdownText}>Seconds</Text>
                        </TouchableOpacity>
                        <FlatList
                          data={delayOptions}
                          keyExtractor={item => item.toString()}
                          renderItem={({item}) => (
                            <TouchableOpacity
                              style={styles.dropdownItem}
                              onPress={() => selectDelay(item)}>
                              <Text
                                style={[
                                  styles.dropdownText,
                                  {color: item === delay ? '#fff' : '#979797'},
                                ]}>
                                {item}s
                              </Text>
                            </TouchableOpacity>
                          )}
                        />
                      </View>
                    </TouchableOpacity>
                  </Modal>

                  {/* Dropdown for Photo Management */}
                  <Modal
                    transparent={true}
                    visible={isTotalImagesDropdownVisible}
                    animationType="none"
                    onRequestClose={() =>
                      setIsTotalImagesDropdownVisible(false)
                    }>
                    <TouchableOpacity
                      style={styles.modalBackground}
                      onPress={() => setIsTotalImagesDropdownVisible(false)}>
                      <View style={[styles.dropdownContainer]}>
                        <TouchableOpacity style={styles.dropdownItem}>
                          <Text style={styles.dropdownText}>Photos</Text>
                        </TouchableOpacity>
                        <FlatList
                          data={totalImagesOptions}
                          keyExtractor={item => item.toString()}
                          renderItem={({item}) => (
                            <TouchableOpacity
                              style={styles.dropdownItem}
                              onPress={() => selectTotalImages(item)}>
                              <Text
                                style={[
                                  styles.dropdownText,
                                  {
                                    color:
                                      item === totalImages ? '#fff' : '#979797',
                                  },
                                ]}>
                                {item}
                              </Text>
                            </TouchableOpacity>
                          )}
                        />
                      </View>
                    </TouchableOpacity>
                  </Modal>

                  {/* Bottom Bar with Dynamic Button Color */}
                  <View style={styles.bottomBar}>
                    {stage === 'initial' && (
                      <>
                        <View style={styles.header} pointerEvents="box-none">
                          <TouchableOpacity
                            onPress={event => {
                              handleButtonPress('clock'); // Set selected button
                              toggleDropdown(event); // Show the modal
                            }}
                            style={[
                              styles.iconButton,
                              {
                                backgroundColor:
                                  selectedButton === 'clock'
                                    ? '#979797'
                                    : 'transparent',
                              },
                            ]}>
                            <Icon name="clockcircleo" size={20} color="#fff" />
                            <Icon
                              name={isDropdownVisible ? 'caretup' : 'caretdown'}
                              size={10}
                              color="#fff"
                              style={{marginLeft: 5}}
                            />
                          </TouchableOpacity>

                          <TouchableOpacity
                            onPress={event => {
                              handleButtonPress('picture'); // Set selected button
                              toggleTotalImagesDropdown(event); // Show the modal
                            }}
                            style={[
                              styles.iconButton,
                              {
                                backgroundColor:
                                  selectedButton === 'picture'
                                    ? '#979797'
                                    : 'transparent',
                              },
                            ]}>
                            <Icon name="picture" size={20} color="#fff" />
                            <Icon
                              name={
                                isTotalImagesDropdownVisible
                                  ? 'caretup'
                                  : 'caretdown'
                              }
                              size={10}
                              color="#fff"
                              style={{marginLeft: 5}}
                            />
                          </TouchableOpacity>
                        </View>

                        <View style={styles.OuterBorderStyles}>
                          <TouchableOpacity
                            style={[styles.StartButton]}
                            onPress={() => {
                              takeMultiplePhotos();
                              setStage('capturing');
                            }}
                          />
                        </View>
                      </>
                    )}

                    {stage === 'capturing' && (
                      <>
                        <View style={styles.TextShowContainer}>
                          <Text style={styles.textShowStyles}>
                            {captureMessage}
                          </Text>
                        </View>
                        <View style={styles.OuterBorderStyles}>
                          <TouchableOpacity
                            style={[styles.PauseButton]}
                            onPress={stopCapturing}
                          />
                        </View>
                      </>
                    )}

                    {stage === 'paused' && (
                      <>
                        <TouchableOpacity
                          style={[
                            styles.camButton,
                            {backgroundColor: '#92E622'},
                          ]}
                          onPress={resumeCapturing}>
                          <Text style={styles.buttonTextStyles}>
                            Resume Scan
                          </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          style={[
                            styles.camButton,
                            {backgroundColor: '#F03D2F'},
                          ]}
                          onPress={handleFinalStop}>
                          <Text style={styles.buttonTextStyles}>Stop Scan</Text>
                        </TouchableOpacity>
                      </>
                    )}

                    {stage === 'stopped' && (
                      <>
                        <TouchableOpacity
                          style={[
                            styles.camButton,
                            {backgroundColor: '#F03D2F'},
                          ]}
                          onPress={handleDiscard}>
                          <Text style={styles.buttonTextStyles}>Discard</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          style={[
                            styles.camButton,
                            {backgroundColor: '#92E622'},
                          ]}
                          onPress={() => {
                            setStage('paused');
                            handleCaptureMore();
                          }}>
                          <Text style={styles.buttonTextStyles}>
                            Capture More
                          </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          style={[
                            styles.camButton,
                            {backgroundColor: '#04AA6D'},
                          ]}
                          onPress={handleFinish}>
                          <Text style={styles.buttonTextStyles}>Finish</Text>
                        </TouchableOpacity>
                      </>
                    )}
                  </View>
                </>
              )}
            </>
          )}
        </View>
      </View>
    </GestureDetector>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  CameraContainer: {
    width: '100%',
    height: '65%',
    backgroundColor: '#000',
    position: 'absolute',
    top: 100,
  },
  circle: {
    position: 'absolute',
    width: 50,
    height: 50,
    borderRadius: 25,
    borderWidth: 2,
    borderColor: '#FFFFF7',
    justifyContent: 'center',
    alignItems: 'center',
    opacity: 0.5,
  },
  innerBorder: {
    position: 'absolute',
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#FFFFF7',
    opacity: 0.5,
  },
  OuterBorderStyles: {
    width: 80,
    height: 80,
    borderRadius: 50,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 4 / 2,
    borderColor: '#fff',
  },
  blinkOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    opacity: 0.7,
    zIndex: 1,
  },
  PauseButton: {
    width: 40,
    height: 40,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#ff0022',
  },
  header: {
    position: 'absolute',
    top: -150,
    zIndex: 1000,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    height: 50,
    width: 130,
    borderRadius: 25,
    justifyContent: 'space-evenly',
    alignItems: 'center',
    flexDirection: 'row',
  },
  StartButton: {
    width: 60,
    height: 60,
    borderRadius: 50,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#ff0022',
  },
  TextIconStyles: {
    flexDirection: 'row',
    justifyContent: 'flex-start',
    alignItems: 'center',
    padding: 5,
  },
  IconStyles: {
    position: 'absolute',
    zIndex: 1000,
    top: 60,
    right: 15,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  checkboxContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-start',
    marginTop: 10,
  },
  checkboxLabel: {
    fontSize: 16,
    marginLeft: 2,
    color: '#000', // Change the text color to black
  },
  camera: {
    ...StyleSheet.absoluteFillObject,
  },

  ProgressBarContainer: {
    width: '90%',
    height: 30,
    overflow: 'hidden',
    marginVertical: 10,
    justifyContent: 'center',
    position: 'absolute',
    top: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#000',
    borderRadius: 15,
  },
  ProgressBar: {
    height: '100%',
    backgroundColor: '#92E622',
    position: 'absolute',
    left: 0,
  },
  ProgressText: {
    position: 'absolute',
    color: '#000',
    fontWeight: 'bold',
    textAlign: 'center',
  },
  bottomBar: {
    flexDirection: 'column',
    position: 'absolute',
    bottom: 30,
    justifyContent: 'center',
    alignItems: 'center',
    width: '100%',
  },
  camButton: {
    height: 50,
    width: '90%',
    borderRadius: 10,
    marginBottom: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buttonTextStyles: {
    color: '#000',
    fontSize: 18,
    fontWeight: '600',
  },
  fullImage: {
    width: '80%',
    height: '30%',
    resizeMode: 'contain',
  },

  modalBackground: {
    flex: 1,
    backgroundColor: '#979797',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContainer: {
    width: '95%',
    backgroundColor: '#fff',
    padding: 30,
    borderRadius: 10,
  },
  closeButton: {
    position: 'absolute',
    top: '2%',
    right: 5,
    zIndex: 1000,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    padding: 2,
    borderRadius: 50,
  },
  modalText: {
    fontSize: 13,
    marginVertical: 3,
    color: '#000',
    marginLeft: 10,
    fontWeight: '#500',
    textAlign: 'left',
  },
  video: {
    width: '100%',
    height: 200,
    zIndex: -1,
  },
  iconButton: {
    flexDirection: 'row',
    backgroundColor: '#979797',
    borderRadius: 50,
    paddingHorizontal: 15,
    paddingVertical: 15,
    alignItems: 'center',
  },
  delayText: {
    marginLeft: 5,
    fontSize: 16,
    color: '#fff',
  },
  modalBackground: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  dropdownContainer: {
    width: 70,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderRadius: 10,
  },
  dropdownItem: {
    paddingVertical: 5,
    paddingHorizontal: 5,
  },
  dropdownText: {
    fontSize: 12,
    color: '#FFFFFF',
  },
  ButtonStyles: {
    backgroundColor: '#ff0022',
    padding: 10,
    borderRadius: 10,
    width: '50%',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 10,
    marginBottom: 10,
  },
  BackButtonText: {
    fontSize: 18,
    color: '#fff',
  },
  TextShowContainer: {
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 5,
    zIndex: 1000,
    marginBottom: 30,
  },
  textShowStyles: {
    fontSize: 40,
    color: '#87CEEB',
    fontWeight: 'bold',
  },
  VideoContainer: {
    borderRadius: 15,
    overflow: 'hidden',
    width: '100%',
    height: 170,
    justifyContent: 'center',
    marginBottom: 20,
  },
  BackToHome: {
    borderRadius: 35,
    borderColor: 'green',
    borderWidth: 3,
    padding: 20,
    justifyContent: 'space-evenly',
    alignItems: 'center',
    flexDirection: 'row',
  },
});

export default CameraComponent;
