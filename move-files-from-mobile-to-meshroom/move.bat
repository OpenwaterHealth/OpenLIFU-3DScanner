@echo off
setlocal

REM Define source and destination directories
set "source_folder=mobile_device_image_location"
set "destination_folder=meshroom_image_location"

REM Ask the user for the reference number
set /p reference_number=Enter the reference number: 

REM Add underscore to the reference number
set "reference_number=%reference_number%_"

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

REM Move files from source to destination with the specific reference number prefix
move "%source_folder%\%reference_number%*" "%destination_folder%\"

REM Check if move was successful
if %errorlevel% equ 0 (
    echo Files starting with %reference_number% moved successfully from %source_folder% to %destination_folder%.
) else (
    echo Failed to move files. Check paths and try again.
)

pause
