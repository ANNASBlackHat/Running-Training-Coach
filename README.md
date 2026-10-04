# Running Training Companion

An Android app that guides interval-training sessions by voice — build a workout, start it, and the phone talks you through every segment change while tracking your pace and splits. No backend, no accounts: everything is stored locally on the device.

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.1.10, JVM target 17 |
| UI | Jetpack Compose (BOM 2025.02.00), Material 3, Navigation Compose 2.8.7 |
| Architecture | Single-activity Compose app + a `foregroundServiceType="location"` service, manual DI via `AppContainer` |
| Persistence | Room 2.6.1 (KSP codegen) with kotlinx.serialization JSON for workout payloads |
| Async | Coroutines 1.10.1 (`StateFlow` / `SharedFlow`) |
| Audio | Android `TextToSpeech` for voice cues + `ToneGenerator` beeps |
| Location | `LocationManager` GPS provider (wrapped behind a `LocationProvider` interface, with a mock provider for tests) |
| Build | Gradle 8.12 (wrapper), AGP 8.8.2, KSP 2.1.10-1.0.29 |
| Tests | JUnit 4, Google Truth, kotlinx-coroutines-test |

## Project structure

```
.
├── app/                     # The single Android application module
│   ├── build/               # Generated Gradle output (gitignored)
│   └── src/
│       ├── main/            # App code — see package layout below
│       └── test/            # JVM unit tests (JUnit4) for the domain layer
├── gradle/wrapper/          # Gradle 8.12 wrapper jar + properties
├── .specs/                  # Product specs: MVP scope, tech spec, user stories, UI style guide
├── build.gradle.kts         # Root build: declares plugin versions (apply false)
├── settings.gradle.kts      # Single-module build, plugin/dependency repositories
├── gradle.properties        # AndroidX flags and JVM heap settings
└── local.properties         # Local Android SDK path (gitignored, machine-specific)
```

Source packages under `app/src/main/java/com/runningcompanion/app/`:

```
data/         # Room database, DAOs, entities, repositories, location providers
domain/       # Pure Kotlin logic: workout models/templates, runner engine, pace + geo math
di/           # AppContainer — the manual dependency-injection root
service/      # Foreground workout service (owns the run) and the audio cue engine
simulation/   # MockLocationProvider — synthetic GPS track for tests
ui/           # Compose screens (home, detail, builder, run, results, history), components, theme
```

## Prerequisites

- **JDK 17** — required by `sourceCompatibility`/`jvmTarget` and AGP 8.8.2
- **Android SDK** with **compileSdk 35 / targetSdk 35** installed (`ANDROID_HOME` or `sdk.dir` in `local.properties`)
- **minSdk 26** (Android 8.0) minimum device/emulator
- Android Studio (Ladybug or newer) — optional, the Gradle wrapper works standalone

No Gradle installation needed; use the committed wrapper (`./gradlew`).

## Setup / Installation

```bash
# Point the build at your SDK (creates/updates local.properties)
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Build the debug APK
./gradlew assembleDebug

# Output
# app/build/outputs/apk/debug/app-debug.apk
```

On first run Gradle downloads the 8.12 distribution and all dependencies, so expect a slow initial build.

## Environment variables

There is no `.env.example` — this app has no backend and no build-time secrets. Configuration is limited to:

| Key | Where | Purpose |
| --- | --- | --- |
| `ANDROID_HOME` | environment | Android SDK location, used if `local.properties` is absent |
| `sdk.dir` | `local.properties` | Per-machine Android SDK path (gitignored) |

Runtime permissions are requested in-app, not via config: fine/coarse location, background location ("Allow all the time", needed for pocket use), `POST_NOTIFICATIONS`, plus manifest-declared `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, and `WAKE_LOCK`.

## Running the project

```bash
# Install onto a connected device or running emulator
./gradlew installDebug

# Launch the app
adb shell am start -n com.runningcompanion.app/.MainActivity
```

Or open the project in Android Studio and press Run on the `app` configuration.

Useful tasks while developing:

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew lintDebug          # Android lint
./gradlew clean              # wipe build outputs
```

### Trying it without running

`MainActivity` is the only entry point (`RunningApp` is the `Application` subclass that builds `AppContainer`). Pick a template on Home — *Norwegian 4x4* or *400m Repeats* — or build your own in the builder. During a session the foreground service drives `WorkoutRunner`, emits a `RunnerSnapshot` for `LiveRunScreen`, and speaks cues through `AudioCueEngine`. Finishing writes a `Session` to Room and opens the per-segment results screen.

## Running tests

Unit tests live in `app/src/test/java/com/runningcompanion/app/domain/` and cover the pure-Kotlin domain layer (no device needed):

```bash
./gradlew testDebugUnitTest        # run all JVM unit tests
./gradlew testDebugUnitTest --tests '*WorkoutRunnerTest'   # single class
```

HTML/XML reports land in `app/build/reports/tests/`.

| Test file | Covers |
| --- | --- |
| `WorkoutRunnerTest.kt` | Segment transitions, pause/resume, state machine |
| `CueSchedulerTest.kt` | When voice/beep cues fire (with cooldown behavior) |
| `MathAndPaceTest.kt` | Pace and geo calculations |
| `WorkoutEntitySerializationTest.kt` | Room entity ↔ JSON serialization round-trips |
| `SimulatedWorkoutIntegrationTest.kt` | Full simulated session driven by `MockLocationProvider` |

Instrumented tests are configured (`androidTestImplementation`, `testInstrumentationRunner = AndroidJUnitRunner`) but none exist yet:

```bash
./gradlew connectedDebugAndroidTest   # requires a connected device/emulator
```

## Deployment

<!-- TODO: no release signing config, no Play Store / CI setup in this repo yet. -->
No deployment pipeline is defined — there is no CI configuration (`.github/`, `.gitlab-ci.yml`, etc.) and no release signing setup. Note that `app/build.gradle.kts` currently points the `release` build type at the **debug** signing config, so `./gradlew assembleRelease` produces a debug-signed, minified APK suitable for local sideloading only. Before distributing, add a real keystore/signing config and a release build type.

```bash
./gradlew assembleRelease   # minified + resource-shrunk, debug-signed (local use only)
```

## Contributing

Single-developer project at the MVP stage; product intent and constraints live in `.specs/` (MVP scope, tech spec, user stories, UI style guide). Read those before changing scope.
