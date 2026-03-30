# OpenLIFU-3DScanner

An Android application for capturing 3D photogrammetric meshes used in transducer localization for the [Open-LIFU](https://www.openwater.health/openlifu) low-intensity focused ultrasound research platform.

OpenLIFU-3DScanner turns a standard Android phone into a 3D scanning tool for medical research. The app guides users through capturing a series of photographs of a patient wearing a transducer device, then processes those images into a 3D mesh that feeds into the broader Open-LIFU neuronavigation and sonication-planning workflow.

## Features

- **3D Photogrammetric Scanning** — Guided image capture workflow that produces 3D mesh models from a sequence of photographs.
- **Face Detection** — ML Kit–powered facial analysis to assist with proper framing and positioning during scans.
- **QR Code Scanning** — Barcode recognition for linking scans to patient or session identifiers.
- **Real-Time Sync** — Socket.IO integration for live communication with backend reconstruction services.
- **Background Reconstruction** — A foreground service manages data synchronization and 3D mesh reconstruction without blocking the UI.
- **Offline Support** — Local Room database and content provider allow scans to be stored and managed on-device.
- **Automatic Cleanup** — WorkManager-based background tasks handle periodic cleanup of local scan data.
- **Firebase Integration** — Crashlytics and Analytics for monitoring app health and usage.

## Architecture

The app follows an **MVVM** (Model-View-ViewModel) architecture with **Dagger Hilt** dependency injection. The UI is built entirely with **Jetpack Compose** and **Material Design 3**.

```
app/src/main/java/health/openwater/openlifu3dscanner/
├── activity/            # Android Activities
├── core/                # Business logic (connectivity, face analysis, image upload, state)
├── db/                  # Room database (scan ownership persistence)
├── di/                  # Hilt dependency injection modules
├── extensions/          # Kotlin extension functions
├── navigation/          # Compose navigation graph
├── network/             # Retrofit + OkHttp API layer, Socket.IO client, DTOs
├── preferences/         # User preferences (DataStore)
├── provider/            # Content providers for scan data
├── repository/          # Repository pattern for data access
├── screen/              # Composable UI screens
│   ├── collection/      # Scan collection management
│   ├── create/          # New scan creation
│   ├── home/            # Home dashboard
│   ├── permissions/     # Runtime permission handling
│   ├── photoscan/       # Photo capture workflow
│   ├── processing/      # Scan processing status
│   ├── qr/              # QR code scanning
│   ├── scanner/         # 3D scanner interface
│   ├── settings/        # App settings
│   ├── signin/          # User authentication
│   ├── transfer/        # Scan transfer
│   └── uploading/       # Upload progress
├── service/             # Foreground reconstruction service
├── theme/               # Compose theming (colors, typography)
├── utils/               # Utility functions
├── viewmodel/           # ViewModels for each screen
├── work/                # WorkManager tasks (cleanup workers)
└── App.kt               # Application entry point
```

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 21** (JetBrains distribution recommended)
- **Android SDK 36** (compile & target SDK)
- An Android device or emulator running **API 24+** (Android 7.0 Nougat)

### Required Device Hardware

The target device must have a camera, accelerometer, and gyroscope.

## Getting Started

### Clone the repository

```bash
git clone https://github.com/OpenwaterHealth/OpenLIFU-3DScanner.git
cd OpenLIFU-3DScanner
git checkout v0.7
```

### Build a debug APK

```bash
cd OpenLIFU-3DScanner   # enter the inner project directory
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be written to `app/build/outputs/apk/debug/`.

### Install on a connected device

```bash
./gradlew installDebug
```

Or open the project in Android Studio and run it from there.

### Release builds

Release builds require additional credentials:

- `openwater.jks` — signing keystore (set `KEYSTORE_PASSWORD` as an environment variable)
- `google-services.json` — Firebase configuration
- `play-key.json` — Google Play API credentials

```bash
./gradlew assembleRelease
```

## CI/CD

A GitHub Actions workflow (`.github/workflows/android.yml`) runs on pushes to the `v0.7` branch. It builds a signed app bundle and publishes it to the Google Play **internal testing** track. The pipeline:

1. Checks out the full Git history (needed for version numbering).
2. Sets up JDK 21.
3. Decodes the signing keystore from a repository secret.
4. Generates release notes from recent commits.
5. Builds and uploads the bundle to Google Play.

## Versioning

Version codes are derived dynamically from the Git commit count, producing versions in the format `0.7.<commit-number>`.

## Related Projects

OpenLIFU-3DScanner is one component of the broader Open-LIFU ecosystem:

| Repository | Description |
|---|---|
| [OpenLIFU-app](https://github.com/OpenwaterHealth/OpenLIFU-app) | Main GUI for planning and controlling sonications |
| [SlicerOpenLIFU](https://github.com/OpenwaterHealth/SlicerOpenLIFU) | 3D Slicer extension for advanced interface workflows |
| [opw_neuromod_sw](https://github.com/OpenwaterHealth/opw_neuromod_sw) | Neuromodulation software |
| [opw_ustx](https://github.com/OpenwaterHealth/opw_ustx) | Ultrasound transmit module |

## Contributing

Openwater welcomes contributions. Please see the organization-level [CONTRIBUTING.md](https://github.com/OpenwaterHealth/.github/blob/main/CONTRIBUTING.md) for the contribution workflow, coding standards, and information about community grants and bounties.

## Community & Support

- **Discord** — `#dev-help` and `#hardware` channels
- **Email** — community@openwater.health
- **Code of Conduct** — [CODE_OF_CONDUCT.md](https://github.com/OpenwaterHealth/.github/blob/main/CODE_OF_CONDUCT.md)
- **Security** — [SECURITY.md](https://github.com/OpenwaterHealth/.github/blob/main/SECURITY.md)

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. See [LICENSE](LICENSE) for the full text.
