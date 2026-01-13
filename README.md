# X Player

X Player is a modern, premium Android video player built with **Jetpack Compose** and **Media3**. It offers a "Cinema" experience with a sleek dark UI, robust local media management, and the unique ability to switch between **ExoPlayer** and **MPV** engines for maximum format compatibility.

## ✨ Key Features

### 🎬 Cinema Experience
- **Dual Playback Engine**: Seamlessly switch between **ExoPlayer (Media3)** for standard streaming/stability and **MPV (libmpv)** for advanced format support (e.g., mKv with complex subtitles).
- **Premium Dark UI**: A strictly enforces dark "Cinema Theme" with a persistent bottom navigation bar.
- **Gesture Controls**: Intuitive brightness, volume, and seeking gestures.
- **Picture-in-Picture (PiP)**: Automatic PiP support for multitasking.
- **Background Playback**: Audio continues via foreground services when the app is minimized.

### 🌐 Network & Connectivity
- **WebDAV, SMB, & FTP**: Stream video directly from your local network servers (NAS, PC).
- **Network Stream**: Open direct video URLs (HLS, DASH, Progressive).
- **Cast Support**: Cast local and network content to compatible devices.

### 📚 Media Library
- **Smart Management**: Browse by **Folders** or **All Videos**.
- **Visuals**: Rich video thumbnails powered by Coil.
- **Privacy**: Hidden folder support.
- **Search**: integrated media search.

### ⌚ Wear OS Companion
- Includes a standalone Wear OS module for basic playback controls on your wrist.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
    - Material3 Design System
    - Navigation Compose
- **Architecture**: MVVM with [Hilt](https://dagger.dev/hilt/) Dependency Injection.
- **Media Engines**:
    - [Jetpack Media3](https://developer.android.com/media/media3) (ExoPlayer)
    - [MPV-Android](https://github.com/mpv-android/mpv-android) (libmpv)
- **Network Protocols**:
    - [SMBJ](https://github.com/hierynomus/smbj) (SMB)
    - [Commons Net](https://commons.apache.org/proper/commons-net/) (FTP)
    - [Sardine](https://github.com/lookfirst/sardine) (WebDAV)
- **Monitoring**: Firebase Crashlytics.

## 🚀 Getting Started

### Prerequisites
- Android Studio Koala or newer.
- JDK 11 (configured in project).
- Android Device/Emulator (API 24+).

### Installation
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/chintan992/x-player.git
    cd x-player
    ```
2.  **Configuration (Optional):**
    - **Signing**: Create a `keystore.properties` file in the root directory if you plan to build release versions.
    - **Firebase**: Add your `google-services.json` to the `app/` directory to enable Crashlytics.
3.  **Build & Run:**
    - Open in Android Studio.
    - Sync Gradle.
    - Select the `app` configuration and run on your device.

## 📱 Permissions

X Player respects your privacy and requests only essential permissions:
- **Storage**: `READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE` for library access.
- **Foreground Service**: `FOREGROUND_SERVICE` rights for background playback.
- **Internet**: Required for Network Streams (SMB/FTP/WebDAV/URL).

## 📄 License

This project is open source. GNU GENERAL PUBLIC LICENSE
