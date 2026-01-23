# Calendar DND

This is an Android application that automatically manages "Do Not Disturb" (DND) settings based on calendar events.

## Project Overview

- **Purpose:** Automatically enables/disables DND mode during calendar meetings.
- **Technologies:**
    - Kotlin
    - Android (with Compose for the UI)
    - Gradle for building
    - Firebase for analytics and crash reporting (in `play` and `manual` flavors)
- **Architecture:** The project follows a clean architecture pattern, with a separation of concerns into four layers: UI, Domain, Data, and System. The core logic is in the `domain` layer, which is pure Kotlin and has no Android dependencies.

## Building and Running

### Prerequisites

- Android Studio (Ladybug or newer)
- JDK 17 or higher
- Android SDK (API 26-36)

### Build Commands

- **Build debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```

- **Install on a connected device:**
  ```bash
  ./gradlew installDebug
  ```

### Running in Android Studio

1.  Open the project in Android Studio.
2.  Wait for the Gradle sync to complete.
3.  Select the `app` run configuration.
4.  Click the "Run" button.

### Product Flavors

The app has three product flavors:

-   `play`: For the Google Play Store, includes Firebase services.
-   `fdroid`: For F-Droid, with Firebase services removed.
-   `manual`: For manual distribution, includes Firebase and a manual update check.

## Development Conventions

-   **Code Style:** The project follows standard Kotlin coding conventions.
-   **Testing:** The project has a multi-layered testing strategy. When code is changed, tests will be run to verify the changes.
    -   **Domain Layer:** Pure unit tests.
    -   **Data Layer:** Unit tests with mocked repositories.
    -   **System Layer:** Instrumented tests.
    -   **UI Layer:** Compose UI tests.
-   **Branching:** The `main` branch is the primary development branch.
-   **Commits:** Commit messages should be clear and descriptive.

## Key Files

-   `README.md`: The main README file with user-facing information.
-   `build.gradle.kts`: The root Gradle build file.
-   `app/build.gradle.kts`: The app-level Gradle build file, with dependencies and flavor configurations.
-   `app/src/main/java/com/brunoafk/calendardnd/`: The main source code for the application.
-   `docs/`: Contains detailed documentation about the project's architecture, features, and development process.
-   `GEMINI.md`: This file, which provides context for the Gemini CLI.
