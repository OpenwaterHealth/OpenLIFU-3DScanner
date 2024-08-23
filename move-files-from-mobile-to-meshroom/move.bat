@echo off

REM Source and destination directories
set "source_folder=mobile_device_image_location"
set "destination_folder=meshroom_image_location"

REM Check if source folder exists
if not exist "%source_folder%" (
    echo Source folder does not exist: %source_folder%
    exit /b
)

REM Check if destination folder exists, create if it doesn't
if not exist "%destination_folder%" (
    echo Destination folder does not exist, creating: %destination_folder%
    mkdir "%destination_folder%"
)

REM Delete previous files in destination folder
echo Deleting previous files in %destination_folder%...
del /q "%destination_folder%\*"

REM Move files from source to destination
move "%source_folder%\*" "%destination_folder%\"

REM Check if move was successful
if %errorlevel% equ 0 (
    echo Files moved successfully from %source_folder% to %destination_folder%.
) else (
    echo Failed to move files. Check paths and try again.
)

pause
