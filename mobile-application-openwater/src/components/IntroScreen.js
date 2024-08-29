import React, { useState } from "react";
import {
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
} from "react-native";
import Toast from "react-native-toast-message";
import AntDesign from "react-native-vector-icons/AntDesign";
import AiFace from "../../assets/Image/AiFacePlain.png";
import BGImage from "../../assets/Image/circle.png";

const { height: HEIGHT } = Dimensions.get("window");

const IntroScreen = ({ navigation }) => {
  const [referNumber, setReferNumber] = useState("");

  const handleStartRecording = () => {
    if (referNumber.trim() === "") {
      Toast.show({
        type: "error",
        text1: "Refer Number Required",
        text2: "Please enter your refer number to proceed.",
      });
    } else {
      navigation.navigate("FaceDetection", {
        referNumber: referNumber,
      });
    }
  };

  return (
    <ScrollView>
      <StatusBar
        translucent
        backgroundColor="transparent"
        barStyle={"dark-content"}
      />
      <View style={styles.container}>
        <ImageBackground source={BGImage} style={styles.bgStyle}>
          <View style={styles.LogoContainer}>
            <Text style={styles.LogoText}>3DMesh</Text>
          </View>
          <View style={styles.AiFaceContainer}>
            <Image source={AiFace} style={styles.AiFaceStyle} />
          </View>
          <View style={styles.HeaderTextContainer}>
            <Text style={[styles.HeaderText, { color: "#FFFFFF" }]}>
              Generate
            </Text>
            <Text style={[styles.HeaderText, { color: "#92E622" }]}>
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
              placeholder="Enter Your Refer Number"
              placeholderTextColor={"#0D0D0D"}
              onChangeText={setReferNumber}
              value={referNumber}
              style={styles.InputStyle}
              underlineColorAndroid="transparent"
              keyboardType="numeric"
            />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.ButtonContainer}
            onPress={handleStartRecording}
          >
            <Text style={styles.ButtonText}>Start recording</Text>
            <AntDesign name="arrowright" size={25} color="#0D0D0D" />
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
    flex: 1,
    height: HEIGHT * 1.05,
    backgroundColor: "#0D0D0D",
  },
  bgStyle: {
    width: "100%",
    height: "100%",
    resizeMode: "cover",
  },
  LogoContainer: {
    width: "100%",
    height: 80,
    marginTop: 60,
  },
  LogoText: {
    color: "#92E622",

    fontSize: 30,
    fontStyle: "italic",
    fontWeight: "800",
    lineHeight: 36,
    letterSpacing: -3,
    padding: 20,
  },
  AiFaceStyle: {
    width: 300,
    height: 292,
    borderRadius: 100,
    alignSelf: "center",
    resizeMode: "contain",
  },
  HeaderTextContainer: {
    width: "70%",
    height: 130,
    justifyContent: "center",
    alignItems: "center",
    marginTop: 20,
  },
  HeaderText: {
    fontSize: 54,
    fontStyle: "normal",
    fontWeight: "700",
    lineHeight: 57,
    letterSpacing: -1.08,
  },
  SubHeaderTextContainer: {
    width: "70%",
    height: 40,
  },
  SubHeaderText: {
    color: "#92E622",

    fontSize: 14,
    fontStyle: "normal",
    fontWeight: "600",
    lineHeight: 18,
    letterSpacing: -0.2,
    left: 12,
  },
  ButtonContainer: {
    width: "90%",
    height: 50,
    backgroundColor: "#92E622",
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 25,
    marginTop: 5,
    marginHorizontal: 20,
    elevation: 20,
    flexDirection: "row",
  },
  ButtonText: {
    color: "#0D0D0D",

    fontSize: 20,
    fontStyle: "normal",
    fontWeight: "800",
    letterSpacing: -0.4,
    lineHeight: 23,
    marginRight: 10,
  },
  HeaderTextWhite: {
    color: "#FFFFFF",
    fontSize: 54,
    fontWeight: "bold",
    textAlign: "center",

    fontWeight: "700",
    fontStyle: "normal",
  },
  HeaderTextGreen: {
    color: "#92E622",
    fontSize: 54,
    fontWeight: "bold",
    textAlign: "center",

    fontWeight: "700",
    fontStyle: "normal",
  },
  InputStyle: {
    width: "100%",
    height: 50,
    paddingHorizontal: 20,
    borderRadius: 25,
    fontSize: 16,
    fontWeight: "400",

    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
});
