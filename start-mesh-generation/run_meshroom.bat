@echo off

REM Define the command to run Meshroom with input and output paths
set "input_folder=meshroom_image_location"
set "output_folder=output_mesh_obj_file_location"
set "meshroom_command=meshroom_batch --input "%input_folder%" --output "%output_folder%""

REM Run the Meshroom command
echo Running Meshroom...
%meshroom_command%

REM Check if the command was successful
if %errorlevel% equ 0 (
    echo Meshroom completed successfully.
) else (
    echo Meshroom failed. Please check the paths and try again.
)

pause
