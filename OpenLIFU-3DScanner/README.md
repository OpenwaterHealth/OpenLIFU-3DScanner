# OpenLIFU-3DScanner

Android application for capturing multi-angle photo collections of a subject's head and uploading them for cloud-based 3D mesh reconstruction, used in transducer localization for OpenLIFU.

---

## Table of Contents

1. [Android App Overview](#android-app-overview)
2. [How to Build the App](#how-to-build-the-app)
3. [APIs Used](#apis-used)
4. [Phone Storage](#phone-storage)
5. [Camera Usage](#camera-usage)

---

## Android App Overview

### Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3.0 |
| UI | Jetpack Compose (BOM 2026.01.01) |
| Dependency Injection | Hilt 2.59 |
| Navigation | Navigation Compose with sealed `Screen` class |
| Networking | Retrofit 3.0.0 + OkHttp 5.3.2 |
| Camera | CameraX 1.5.3 |
| Local DB | Room 2.7.0 |
| Background Jobs | WorkManager 2.10.0 |
| Real-time Updates | Socket.IO 2.1.2 |
| Analytics/Crash | Firebase Crashlytics + Analytics (BOM 34.8.0) |

### Architecture

The app follows MVVM with a repository layer:

- `screen/` — Jetpack Compose UI screens
- `viewmodel/` — ViewModels holding UI state and business logic
- `repository/` — Data layer (`CloudRepository`, `ReconstructionRepository`, `AuthRepository`)
- `network/` — Retrofit service interfaces and OkHttp configuration
- `di/` — Hilt modules (`ApiModule`, `AppModule`, etc.)

### App Flow

```
Login → Home → Create Collection → Scanner → Processing/Uploading → Photoscan Results
```

- **Create Collection**: User names the scan session and configures upload settings.
- **Scanner**: Camera screen that guides the user around the subject capturing photos at tracked angles.
- **Processing/Uploading**: Photos are resized and uploaded to the cloud; a WebSocket connection tracks reconstruction progress.
- **Photoscan Results**: Displays the completed 3D mesh and allows download.

### Authentication

Authentication uses a custom JWT-based system:

- `POST /users/auth/login` — exchanges credentials for access + refresh tokens
- `POST /users/auth/refresh_token` — silently refreshes expired access tokens
- Tokens are stored in `SharedPreferences` via the `Prefs` class
- A dedicated `@NoAuth` OkHttpClient is used for auth endpoints to avoid circular dependency with the authenticator

---

## How to Build the App

### Requirements

- **Android Studio** Narwhal (or newer) with Kotlin 2.3 support
- **JDK 21**
- **Android SDK**: Compile & Target SDK 36 (Android 15), Min SDK 24 (Android 7.0)

### Setup

1. Clone the repository.
2. Open the project root in Android Studio (`OpenLIFU-3DScanner/`).
3. Sync Gradle — dependencies will download automatically.
4. Create or configure a `google-services.json` for Firebase (Crashlytics/Analytics) and place it in `app/`.
5. For release builds, configure signing in `app/build.gradle.kts` with a valid keystore.

### Build Commands

```bash
# Quick Kotlin compilation check (fastest feedback)
./gradlew compileDebugKotlin

# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/

# Build release AAB (for Play Store)
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/
```

### Version Numbering

Version numbers are generated automatically from git history using the Grgit plugin:

- **Version Name**: `0.7.<commit_count>` (e.g., `0.7.347`)
- **Version Code**: `2_0_0_0000` base + commit count (integer, increments per commit)

### ProGuard / R8

Release builds have minification and resource shrinking enabled. Rules are in `app/proguard-rules.pro`.

### Key Dependency Versions (`gradle/libs.versions.toml`)

| Dependency | Version |
|-----------|---------|
| AGP | 9.0.0 |
| Kotlin | 2.3.0 |
| Retrofit | 3.0.0 |
| OkHttp | 5.3.2 |
| CameraX | 1.5.3 |
| Hilt | 2.59 |
| Room | 2.7.0 |
| ML Kit Face Detection | 16.1.7 |
| ML Kit Barcode Scanning | 17.3.0 |
| Firebase BOM | 34.8.0 |
| Socket.IO | 2.1.2 |

---

## APIs Used

### REST API

The app communicates with the OpenWater Health backend via REST. Base URLs are defined in `ApiEnvironment.kt`:

| Environment | Base URL |
|------------|----------|
| Production | `https://api.openwater.health/` |
| Dev | `https://dev.api.openwater.health/` |
| Sandbox | `https://sandbox.api.openwater.health/` |

**Retrofit** is used as the HTTP client with **GSON** for JSON serialization (`FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES`). OkHttp handles authentication, logging, and timeouts (connect: 5s, read: 15s, write: 15s).

Two OkHttpClient instances are configured via Hilt:
- **Main client** — attaches `Authorization: Bearer <token>` to every request and automatically refreshes expired tokens via an OkHttp `Authenticator`
- **NoAuth client** (`@NoAuth` qualifier) — no auth header; used only for login and token refresh endpoints

### API Endpoints

#### Authentication — `AuthApi`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/users/auth/login` | Login with credentials; returns access + refresh tokens |
| POST | `/users/auth/refresh_token` | Refresh an expired access token |

#### Photo Collections — `PhotocollectionService`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/photocollection` | Create a new photo collection |
| GET | `/photocollection/{id}` | Get collection details |
| GET | `/photocollection/account/{uid}` | List all collections for a user |
| DELETE | `/photocollection/{id}` | Delete a collection |
| POST | `/photocollection/{id}/photo/{name}` | Upload a single photo |
| GET | `/photocollection/{id}/photo/{name}` | Download a photo |
| POST | `/photocollection/{id}/start_photoscan` | Trigger 3D reconstruction |

#### Photoscans — `PhotoscanService`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/photoscan/{id}` | Get photoscan status/details |
| GET | `/photoscan/account/{uid}` | List all photoscans for a user |
| GET | `/photoscan/{id}/mesh` | Streaming download of the 3D mesh (.obj) |
| DELETE | `/photoscan/{id}` | Delete a photoscan |

#### Users — `UserService`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/users/{uid}` | Get user profile |
| POST | `/users/reset-password` | Request a password reset |

#### Subjects — `SubjectService`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/subjects/sessions` | List subject sessions |

### WebSocket API

Real-time reconstruction progress is tracked over **Socket.IO** connected to the `/progress` namespace on the backend. The `WebsocketService` listens for progress events and feeds them into the UI via StateFlow.

---

## Phone Storage

### Location

Photos and scan outputs are stored in the **external app-specific directory** using scoped storage (no `READ/WRITE_EXTERNAL_STORAGE` permission required):

```
<external-files-dir>/OpenLIFU-3DScanner/
├── <collection_name>/
│   ├── A_<timestamp>_<angle>.jpg     ← captured photos
│   └── scan/
│       └── texturedMesh.obj          ← downloaded 3D mesh
```

- `getExternalFilesDir(null)` is the root; accessed via extension `getModelsDir.kt`
- Files are scoped to the app and deleted when the app is uninstalled
- No special permissions needed on Android 11+

### Filename Convention

Photos are saved as: `A_<unix_timestamp_ms>_<relativeAngleDegrees>.jpg`

Example: `A_1705123456789_45.jpg`

The relative angle in the filename reflects the angular position captured during the scan orbit.

### ContentProvider for ADB / External Access

`PhotoscanContentProvider` exposes collections via a `ContentProvider` URI scheme:

```
content://health.openwater.openlifu3dscanner.photoscans/
```

Supported operations via ADB shell (`content query`, `content read`):

| Columns | Description |
|---------|-------------|
| `_id` | Unique identifier |
| `name` | Collection name |
| `path` | Filesystem path |
| `photo_count` | Number of photos in collection |
| `has_model` | Whether a 3D mesh has been downloaded |
| `last_modified` | Last modification timestamp |

Example ADB usage:
```bash
adb shell content query --uri content://health.openwater.openlifu3dscanner.photoscans/collections
```

### Storage Cleanup

- **`LocalScanCleanupWorker`** (WorkManager) periodically removes stale or incomplete scan directories
- `ScannerViewModel.resetForRecapture()` deletes a collection's local data before a re-capture session

---

## Camera Usage

### Camera API

The app uses **CameraX** (`androidx.camera` v1.5.3), which internally uses Camera2. Only the **back camera** is used (`CameraSelector.DEFAULT_BACK_CAMERA`). The camera is bound to the lifecycle via `ProcessCameraProvider.bindToLifecycle()`.

### CameraX Use Cases

| Use Case | Purpose |
|----------|---------|
| `Preview` | Live camera feed in the scanner UI via `PreviewView` |
| `ImageCapture` | Still JPEG capture to disk |
| `ImageAnalysis` | Real-time frame processing for face detection and QR scanning |

### Capture Modes

Three capture modes are available (`ImageCapture.CaptureMode`), selectable based on connectivity:

| Mode | When Used |
|------|-----------|
| `MINIMIZE_LATENCY` | Default / online mode — fastest capture |
| `MAXIMIZE_QUALITY` | Offline mode — highest image quality |
| `ZERO_SHUTTER_LAG` | Available but not default |

### Autofocus

Autofocus is handled automatically by CameraX in continuous autofocus mode. There is no explicit manual AF lock or tap-to-focus implementation — the camera focuses continuously throughout the scan session.

### Camera Metadata Captured

The app does **not** directly access `CaptureResult` metadata (e.g., ISO, shutter speed, white balance gains) — CameraX abstracts the Camera2 callback layer. However, **EXIF metadata** is preserved throughout the image pipeline:

| EXIF Field | Status |
|-----------|--------|
| Rotation / Orientation | Read, corrected, then reset to NORMAL after processing |
| Make / Model | Preserved (copied from original capture) |
| DateTime / DateTimeOriginal | Preserved |
| GPS coordinates | Preserved if present |
| Image dimensions (PixelXDimension, PixelYDimension) | Updated after resize |
| Flash, ExposureTime, FNumber, ISO | Preserved if written by device |

EXIF is managed in `ImageUtils.kt` using `androidx.exifinterface.media.ExifInterface`. When photos are resized before upload, the original EXIF is extracted, re-applied to the resized image, and orientation is reset to normal (since rotation correction has already been applied).

### Parameters NOT Explicitly Controlled

The following camera parameters are left to the device's automatic control (AWB, AE, AF algorithms):

- ISO / sensitivity
- Shutter speed / exposure time
- White balance gains
- Focal length (fixed by hardware)
- Aperture (fixed by hardware on most phones)

### Face Detection

`FaceAnalyzer.kt` runs **ML Kit Face Detection** on `ImageAnalysis` frames:

- Detects face bounding box (bounds, center point)
- Calculates face width and height as a fraction of the frame size
- Results are used to guide the user to keep the subject in frame and at the correct distance
- Analysis uses `STRATEGY_KEEP_ONLY_LATEST` — only the most recent frame is processed, older frames are dropped

### QR Code Scanning

**ML Kit Barcode Scanning** is used to read session QR codes (scanned from the OpenLIFU system) to associate a scan collection with a specific treatment session.

### Orientation / Rotation Tracking

The device's **`TYPE_GAME_ROTATION_VECTOR`** sensor is used at `SENSOR_DELAY_GAME` rate (~200 Hz) to track how far the user has rotated around the subject:

| Measurement | Threshold |
|------------|-----------|
| Pitch (forward/back tilt) | Must be within ±30° of vertical |
| Roll (sideways tilt) | Must be within ±30° |
| Compass heading | Tracked to calculate angular position for orbit guidance |

The rotation matrix from the sensor is remapped from device frame to world frame. The heading angle drives the orbit arc UI and determines the filename angle suffix for each captured photo.

### Image Processing Before Upload

Photos are resized to square format before upload (`ImageUploader.kt`, `ImageUtils.kt`):

1. Read JPEG from disk
2. Extract original EXIF
3. Decode bitmap with EXIF-based rotation correction (large images use `inSampleSize` downsampling to save memory)
4. Center-crop to square
5. Scale to target size: **1024×1024** (default) or **2048×2048** (high-quality mode)
6. Re-encode as JPEG
7. Re-apply EXIF attributes (orientation reset to NORMAL, dimensions updated)
8. Upload as `application/octet-stream`
