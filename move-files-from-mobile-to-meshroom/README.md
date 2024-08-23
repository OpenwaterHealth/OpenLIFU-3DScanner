
# Mobile to Meshroom Image Transfer

This project facilitates the automated transfer of images from a mobile device to a designated Meshroom image directory using a batch file (`move.bat`). Follow the steps below to set up and execute the transfer process.

## Prerequisites

- A Windows-based system.
- A mobile device connected to the system.
- File paths and directory management.

## Steps to Transfer Images

### Step 1: Connect Your Mobile Device

1. **Connect the Mobile Device**: 
   - Use a USB cable to connect your mobile device to the system.
   - Ensure that your device is properly recognized by the system.
   - Verify that you can access the image directory of your mobile device through your file explorer.

### Step 2: Configure the Mobile Device Image Location

1. **Identify the Mobile Image Directory**:
   - Navigate to the directory on your system where the mobile device's images are stored.
   - Copy the full path to this directory.

2. **Update the `move.bat` File**:
   - Open the `move.bat` file in a text editor.
   - Locate the variable `source_folder`.
   - Replace the placeholder path with the full path to your mobile device’s image directory.
   - Example:
     ```batch
     set "source_folder=C:\Users\YourUsername\MobileDevice\DCIM"
     ```

### Step 3: Configure the Meshroom Image Directory

1. **Identify the Meshroom Image Directory**:
   - Determine the directory where Meshroom stores its images.
   - Copy the full path to this directory.

2. **Update the `move.bat` File**:
   - In the same `move.bat` file, locate the variable `destination_folder`.
   - Replace the placeholder path with the full path to your Meshroom image directory.
   - Example:
     ```batch
     set "destination_folder=C:\Users\YourUsername\Meshroom\Images"
     ```

### Step 4: Execute the Transfer

1. **Run the `move.bat` File**:
   - After completing the above configurations, save the `move.bat` file.
   - Double-click the `move.bat` file to execute it.
   - The script will automatically transfer the images from the specified mobile device directory to the Meshroom image directory.

2. **Verify the Transfer**:
   - Once the script has run, navigate to the Meshroom image directory to ensure that the images have been successfully transferred.

## Troubleshooting

- **Mobile Device Not Recognized**: Ensure that the mobile device is connected properly and that you have the necessary drivers installed.
- **Incorrect Path Errors**: Double-check the paths you’ve set in the `move.bat` file for any typos or errors.
- **Permission Issues**: Make sure you have the necessary permissions to access both the mobile device and the Meshroom image directories.

## Conclusion

Following these steps will allow you to seamlessly transfer images from your mobile device to the Meshroom image directory with a simple execution of the `move.bat` file. This setup is especially useful for automating workflows in environments where Meshroom is used for 3D reconstruction or other imaging processes.
