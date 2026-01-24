# Repository Guidelines

## Project Structure & Module Organization
- Single module: `app/`.
- Kotlin source: `app/src/main/java/com/brunoafk/calendardnd/` (feature folders: `ui/`, `domain/`, `data/`, `system/`, `util/`).
- Android resources: `app/src/main/res/` (layouts, values, drawables, mipmaps).
- Manifest: `app/src/main/AndroidManifest.xml`.
- Tests: unit tests in `app/src/test/java/...`, instrumented tests in `app/src/androidTest/java/...`.
- Build config: `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- Docs: `docs/` and release notes in `release-notes/`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build a debug APK.
- `./gradlew assembleRelease` — build a release APK (no minify currently).
- `./gradlew test` — run JVM unit tests in `app/src/test`.
- `./gradlew connectedAndroidTest` — run instrumented tests on device/emulator.
- `./gradlew lint` — run Android Lint.

## Coding Style & Naming Conventions
- Language: Kotlin (JVM target 11). Indentation: 4 spaces.
- Naming: packages `lowercase`; classes/objects `PascalCase`; functions/vars `camelCase`; constants `UPPER_SNAKE_CASE`.
- Keep files scoped to feature areas (e.g., `domain/`, `data/`, `system/`, `ui/`, `util/`).
- Format with Android Studio’s Kotlin formatter when in doubt.

## Testing Guidelines
- Unit tests use JUnit 4; instrumented tests use AndroidX JUnit and Espresso.
- Name tests by behavior (e.g., `SchedulePlannerTest`).
- Prefer unit tests for domain logic; use instrumented tests for Android framework behavior.
- After any code change, run `./gradlew test` before marking the task complete.

## Commit & Pull Request Guidelines
- Git history uses concise, imperative commit messages (e.g., “Add calendar observer”).
- PRs should include a brief summary, testing notes (commands run), and screenshots for UI changes.

## Configuration Tips
- Ensure `local.properties` points to your Android SDK.
- Minimum SDK 26; target/compile SDK 36.
- For release signing, set keystore values in `local.properties` (see `docs/configuration.md`).
