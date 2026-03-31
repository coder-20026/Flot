# Coordinate Extractor

Advanced Android app for extracting GPS coordinates from screen content using OCR.

## Features

- **Floating Overlay Button**: Works on all screens with system overlay
- **Smart OCR**: Uses ML Kit for on-device text recognition
- **Bottom-Right ROI Detection**: Focuses on the area where coordinates typically appear
- **Real-time Mode**: Continuous capture and extraction
- **One-Tap Copy**: Copy coordinates to clipboard
- **Google Maps Integration**: Open extracted coordinates directly in Maps

## Architecture

- **MVVM Pattern**: Clean separation of concerns
- **Hilt DI**: Dependency injection throughout the app
- **Coroutines & Flow**: Asynchronous operations
- **DataStore**: Persistent preferences storage

## Tech Stack

- Kotlin
- Android Jetpack (ViewModel, LiveData, DataStore)
- Hilt for Dependency Injection
- ML Kit Text Recognition
- Material Design 3
- Coroutines & Flow

## Requirements

- Android 8.0 (API 26) or higher
- Screen capture permission
- Overlay permission (draw over other apps)
- Notification permission (Android 13+)

## Building

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## CI/CD

This project includes GitHub Actions workflow for:
- Automatic APK builds on push
- Unit test execution
- Release creation on main branch

## Project Structure

```
app/
├── src/main/
│   ├── java/com/coordextractor/app/
│   │   ├── capture/          # Screen capture logic
│   │   ├── di/               # Dependency injection
│   │   ├── ocr/              # ML Kit integration
│   │   ├── parser/           # Coordinate extraction
│   │   ├── processing/       # Image processing
│   │   ├── service/          # Floating overlay service
│   │   ├── ui/               # Activities & ViewModels
│   │   └── util/             # Utilities
│   └── res/
│       ├── drawable/         # Icons & backgrounds
│       ├── layout/           # XML layouts
│       └── values/           # Colors, strings, themes
└── build.gradle.kts
```

## Coordinate Formats Supported

- Standard: `22.1234N 71.1234E`
- No space: `22.1234N71.1234E`
- Decimal: `22.1234, 71.1234`
- DMS: `22°12'34"N 71°12'34"E`

## License

MIT License - Feel free to use and modify.
