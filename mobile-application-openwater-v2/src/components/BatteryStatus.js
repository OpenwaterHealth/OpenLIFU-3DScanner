// BatteryStatus.js
import * as Battery from "expo-battery";
import React, { useEffect, useMemo, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

const BatteryStatus = () => {
  const [batteryState, setBatteryState] = useState(null);

  useEffect(() => {
    // Function to get the initial battery state
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

  return (
    <View style={styles.container}>
      {isCharging ? (
        <Text style={styles.text}>Connected</Text>
      ) : (
        <Text style={styles.text}>Not Connected</Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    justifyContent: "center",
    alignItems: "center",
  },
  text: {
    color: "#92E622",
    fontSize: 13,
    fontWeight: "600",
  },
});

export default BatteryStatus;
