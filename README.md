# X Player

X Player is a modern Android video player application built with Jetpack Compose and Media3 (ExoPlayer). It provides a seamless video playback experience with support for local video files.

## Features

- **Modern UI**: Built entirely with Jetpack Compose for a smooth and responsive user interface.
- **Video Playback**: Robust video playback using Android's Media3 and ExoPlayer libraries.
- **Background Playback**: Supports background playback using foreground services.
- **Local Media**: Automatically fetches and displays local videos from the device.
- **Picture-in-Picture**: Supports Picture-in-Picture mode for multitasking.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Media Playback**: [Media3 / ExoPlayer](https://developer.android.com/media/media3)
- **Image/Video Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Asynchronous Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)

## Prerequisites

- Android Studio Koala or newer.
- JDK 11 or newer.
- Android SDK API 24 (Nougat) or higher.

## Setup & Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/chintan992/x-player.git
    ```
2.  **Open in Android Studio:**
    Open Android Studio and select "Open an existing Android Studio project", then select the cloned directory.
3.  **Sync Gradle:**
    Allow Android Studio to download dependencies and sync the project.
4.  **Run the App:**
    Connect an Android device or start an emulator and run the `app` configuration.

## Permissions

The app requires the following permissions to function correctly:
- `READ_MEDIA_VIDEO`: To access video files on the device (Android 13+).
- `READ_EXTERNAL_STORAGE`: To access video files on older Android versions.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: For background playback controls.
