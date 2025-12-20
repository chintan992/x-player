# X Player

X Player is a modern, premium Android video player built with **Jetpack Compose** and **Media3**. It features a sleek "Cinema Theme," robust local media management, and a powerful playback engine.

## ✨ Key Features

### 🎬 Cinema Experience
- **Premium Dark UI**: A clean, immersive interface designed to let your content shine.
- **Cinema Theme**: Optimized dark palette with subtle "Brand Accent" highlights.
- **Smooth Animations**: Polished transitions and interactions.

### 📚 Media Library
- **Smart Organization**: Browse videos by **Folders** or **All Videos**.
- **Flexible Views**: Switch between **Grid** (with gradient overlays) and **List** layouts.
- **Sorting & Filtering**: Sort by name, date, size, or duration. Filter visible metadata.
- **Hidden Folders**: Toggle visibility of hidden content directly from settings.
- **Search**: (Coming Soon) Quickly find your media.

### ⏯️ Advanced Playback
- **Media3 (ExoPlayer)**: Industry-standard playback reliability.
- **Gesture Controls**: 
    - Swipe **Left** for Brightness.
    - Swipe **Right** for Volume.
    - Double-tap to Seek (Forward/Rewind).
- **Picture-in-Picture (PiP)**: Multitask seamlessly with auto-PiP support.
- **Background Playback**: Audio continues safely in the background via foreground services.
- **Aspect Ratio Control**: Switch between Fill, Fit, Zoom, and Stretch modes.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
    - Material3 Design System
    - Compose Navigation
- **Architecture**: MVVM with [Hilt](https://dagger.dev/hilt/) Dependency Injection.
- **Media**: 
    - [Jetpack Media3](https://developer.android.com/media/media3) (ExoPlayer, Session)
    - [Coil](https://coil-kt.github.io/coil/) for efficient video thumbnail loading.
- **Concurrency**: Coroutines & Flow.

## 🚀 Getting Started

### Prerequisites
- Android Studio Koala or newer.
- JDK 11+.
- Android Device/Emulator (API 24+).

### Installation
1.  **Clone the repo:**
    ```bash
    git clone https://github.com/chintan992/x-player.git
    ```
2.  **Open in Android Studio**.
3.  **Sync Gradle** to download dependencies.
4.  **Run** on your device.

## 📱 Permissions

X Player respects your privacy and requests only essential permissions:
- **Storage/Video Access**: `READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE` to display your library.
- **Foreground Service**: `FOREGROUND_SERVICE_MEDIA_PLAYBACK` to keep audio playing when you leave the app.
