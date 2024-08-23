
# Start Mesh Generation Script: `run_meshroom.bat`

This guide provides instructions for running the `run_meshroom.bat` script, which initiates the mesh generation process using Meshroom. Follow these steps after successfully setting up the Meshroom environment.

## Steps to Start Mesh Generation

### Step 1: Update the Image Location Variable

1. **Locate the `run_meshroom.bat` File**:
   - Open the `run_meshroom.bat` file in a text editor.

2. **Update `input_folder`**:
   - Find the variable `input_folder` in the script.
   - Replace the placeholder `"meshroom_image_location"` with the full path to the directory containing your images for processing.
   - Example:
     ```batch
     set "input_folder=C:\Users\YourUsername\Meshroom\Images"
     ```

### Step 2: Update the Output Location Variable

1. **Update `output_folder`**:
   - In the same `run_meshroom.bat` file, locate the variable `output_folder`.
   - Replace the placeholder `"output_mesh_obj_file_location"` with the full path to the directory where you want the `.obj` mesh file to be saved.
   - Example:
     ```batch
     set "output_folder=C:\Users\YourUsername\Meshroom\Output"
     ```

### Step 3: Execute the Script

1. **Run the `run_meshroom.bat` File**:
   - After updating the variables, save the `run_meshroom.bat` file.
   - Double-click on the `run_meshroom.bat` file to execute it.

2. **Wait for Processing**:
   - The script will start the mesh generation process using Meshroom.
   - This process may take a few minutes depending on the number and size of images.

3. **Check the Output**:
   - Once the process is complete, navigate to the directory specified in the `output_folder` variable.
   - You should find the generated `.obj` mesh file in this directory.

## Conclusion

Following these steps will allow you to generate a `.obj` mesh file using Meshroom. Ensure that the paths are correctly set in the `run_meshroom.bat` file to avoid errors during execution.
