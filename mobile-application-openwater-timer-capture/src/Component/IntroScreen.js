import React, {useEffect, useState} from 'react';
import {
  Alert,
  BackHandler,
  Dimensions,
  Image,
  ImageBackground,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import Toast from 'react-native-toast-message';
import AiFace from '../../assets/Image/AiFacePlain.png';
import BGImage from '../../assets/Image/circle.png';

const {height: HEIGHT} = Dimensions.get('window');
const {width: WIDTH} = Dimensions.get('window');

const IntroScreen = ({navigation}) => {
  const [referNumber, setReferNumber] = useState('');

  const handleStartRecording = () => {
    if (referNumber.trim() === '') {
      Toast.show({
        type: 'error',
        text1: 'Refer Number Required',
        text2: 'Please enter your refer number to proceed.',
      });
    } else {
      navigation.navigate('CameraScreen', {
        referNumber: referNumber,
      });
      console.log('Click');
    }
  };

  const handleBackButtonPress = () => {
    Alert.alert(
      'Exit App',
      'Are you sure you want to exit?',
      [
        {
          text: 'No',
          onPress: () => null, // Do nothing on "No"
          style: 'cancel',
        },
        {
          text: 'Yes',
          onPress: () => BackHandler.exitApp(), // Exit the app on "Yes"
        },
      ],
      {cancelable: false},
    );
    return true; // Prevent default back button behavior
  };

  useEffect(() => {
    const backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      handleBackButtonPress,
    );

    return () => backHandler.remove(); // Clean up the event listener
  }, []);

  return (
    <ScrollView>
      <StatusBar backgroundColor="#000" barStyle={'light-content'} />
      <View style={styles.container}>
        <ImageBackground source={BGImage} style={styles.bgStyle}>
          <View style={styles.LogoContainer}>
            <Text style={styles.LogoText}>3DMesh</Text>
          </View>

          <View style={styles.AiFaceContainer}>
            <Image source={AiFace} style={styles.AiFaceStyle} />
          </View>
          <View style={styles.HeaderTextContainer}>
            <Text style={[styles.HeaderText, {color: '#FFFFFF'}]}>
              Generate
            </Text>
            <Text style={[styles.HeaderText, {color: '#92E622'}]}>
              3D Mesh.
            </Text>
          </View>
          <View style={styles.SubHeaderTextContainer}>
            <Text style={styles.SubHeaderText}>
              Scan your face and generate a 3D mesh.
            </Text>
          </View>

          <TouchableOpacity style={styles.ButtonContainer}>
            <TextInput
              placeholder="Scan ID"
              placeholderTextColor={'#0D0D0D'}
              onChangeText={setReferNumber}
              value={referNumber}
              style={styles.InputStyle}
              underlineColorAndroid="transparent"
            />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.ButtonContainer}
            onPress={handleStartRecording}>
            <Text style={styles.ButtonText}>Start</Text>
          </TouchableOpacity>
        </ImageBackground>
      </View>
      <Toast />
    </ScrollView>
  );
};

export default IntroScreen;

const styles = StyleSheet.create({
  container: {
    height: HEIGHT,
    backgroundColor: '#0D0D0D',
    justifyContent: 'center',
    alignItems: 'center',
  },
  bgStyle: {
    width: WIDTH,
    height: HEIGHT,
    resizeMode: 'cover',
    justifyContent: 'center',
    alignItems: 'center',
  },
  LogoContainer: {
    width: WIDTH,
    justifyContent: 'space-around',
    alignItems: 'center',
    flexDirection: 'row',
  },
  LogoText: {
    color: '#92E622',
    fontSize: 40,
    fontStyle: 'italic',
    fontWeight: 'bold',
    lineHeight: 36,
    letterSpacing: -3,
    padding: 10,
  },
  AiFaceStyle: {
    width: 300,
    height: 292,
    alignSelf: 'center',
    resizeMode: 'contain',
  },
  HeaderTextContainer: {
    width: '90%',
    height: 130,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
    marginTop: 20,
  },
  HeaderText: {
    fontSize: 54,
    fontStyle: 'normal',
    fontWeight: '700',
    lineHeight: 57,
    letterSpacing: -1.08,
  },
  SubHeaderTextContainer: {
    width: '90%',
    height: 40,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
  },
  SubHeaderText: {
    color: '#92E622',
    fontSize: 14,
    fontStyle: 'normal',
    fontWeight: '600',
    lineHeight: 18,
    letterSpacing: -0.2,
    left: 12,
  },
  ButtonContainer: {
    width: '90%',
    height: 50,
    backgroundColor: '#92E622',
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 10,
    marginTop: 5,
    marginHorizontal: 20,
    elevation: 20,
    flexDirection: 'row',
    marginBottom: 10,
  },
  ButtonText: {
    color: '#0D0D0D',

    fontSize: 20,
    fontStyle: 'normal',
    fontWeight: '800',
    letterSpacing: -0.4,
    lineHeight: 23,
    marginRight: 10,
  },
  HeaderTextWhite: {
    color: '#FFFFFF',
    fontSize: 54,
    fontWeight: 'bold',
    textAlign: 'center',

    fontWeight: '700',
    fontStyle: 'normal',
  },
  HeaderTextGreen: {
    color: '#92E622',
    fontSize: 54,
    fontWeight: 'bold',
    textAlign: 'center',

    fontWeight: '700',
    fontStyle: 'normal',
  },
  InputStyle: {
    width: '100%',
    height: 50,
    paddingHorizontal: 20,
    borderRadius: 25,
    fontSize: 16,
    fontWeight: '400',
    color: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  UsbConnector: {
    width: 130,
    height: 40,
    borderRadius: 100,
    borderColor: '#92E622',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 10,
    shadowOpacity: 0.2,
    shadowRadius: 4,
    borderWidth: 2,
    flexDirection: 'row',
  },
  StatusText: {
    color: '#92E622',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 5,
  },
});
