# OpenLIFU-3DScanner
Scanning application for capturing 3D meshes used in transducer localization


## Project Structure

### 1. **mesh-server-library**

This directory contains the necessary libraries and server-side components that facilitate the processing and management of 3D mesh data. It includes various tools and scripts required to handle the backend operations, ensuring that the captured 3D meshes are stored, processed, and served efficiently.

### 2. **move-files-from-mobile-to-meshroom**

This directory provides the scripts and instructions needed to automate the transfer of image files from a mobile device to the Meshroom image processing directory. It includes a batch file (`move.bat`) that simplifies the process of moving images from the mobile device to the appropriate directory for further processing.

### 3. **start-mesh-generation**

This directory contains the script (`run_meshroom.bat`) that initiates the mesh generation process using Meshroom. After transferring images to the Meshroom directory, this script automates the process of generating a 3D mesh file (.obj) from the images, which is critical for transducer localization.

## How to Use

1. **Setup**: Follow the instructions in each directory's README file to set up the environment and tools needed for the specific tasks.
2. **Transfer Files**: Use the `move-files-from-mobile-to-meshroom` directory to transfer images from your mobile device to the Meshroom processing directory.
3. **Generate Mesh**: Run the script in the `start-mesh-generation` directory to begin the mesh generation process.

## Conclusion

The **OpenLIFU-3DScanner** project provides a complete solution for capturing and processing 3D meshes, specifically tailored for transducer localization. Each component of the project plays a crucial role in ensuring that the scanning and mesh generation processes are efficient and accurate.
