# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the only module. Source lives under `app/src/main/java/com/brunoafk/calendardnd/`.
- Android resources are in `app/src/main/res/` (layouts, values, drawables, mipmaps).
- The manifest is `app/src/main/AndroidManifest.xml`.
- Unit tests are in `app/src/test/java/...`, instrumented tests in `app/src/androidTest/java/...`.
- Build configuration lives in `build.gradle.kts`, `app/build.gradle.kts`, and `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: build a debug APK.
- `./gradlew assembleRelease`: build a release APK (no minify enabled currently).
- `./gradlew test`: run JVM unit tests in `app/src/test`.
- `./gradlew connectedAndroidTest`: run instrumented tests on a connected device or emulator.
- `./gradlew lint`: run Android Lint for static analysis.

## Coding Style & Naming Conventions
- Language: Kotlin (JVM target 11). Use 4-space indentation and Kotlin standard formatting.
- Naming: packages are lowercase; classes/objects `PascalCase`; functions/vars `camelCase`; constants `UPPER_SNAKE_CASE`.
- Keep files scoped to feature areas (e.g., `domain/`, `data/`, `system/`, `ui/`, `util/`).

## Testing Guidelines
- Unit tests use JUnit 4; instrumented tests use AndroidX JUnit and Espresso.
- Name tests clearly for behavior (e.g., `SchedulePlannerTest`).
- Prefer unit tests for domain logic; use instrumented tests for UI/Android framework behavior.
- After any code changes, run `./gradlew test`.

## Commit & Pull Request Guidelines
- Git history is minimal and does not show a strict convention. Use concise, imperative commit messages (e.g., "Add calendar observer").
- PRs should include: a short summary, testing notes (`./gradlew test`, device tests), and screenshots for UI changes.

## Configuration Tips
- Ensure `local.properties` points to your Android SDK.
- Minimum SDK is 26; target/compile SDK is 36.
