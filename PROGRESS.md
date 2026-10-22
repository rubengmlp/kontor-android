# Kontor — project status

**Status: complete.** The app, tests, documentation and CI workflow are ready for review.

## Verified locally

- Clean debug + release build: `./gradlew clean testDebugUnitTest :core:domain:test lintDebug assembleDebug assembleRelease`
- JVM tests: **34 passing** — domain 18, accounts 9, transfer 7.
- Compose UI tests: **2 passing** on `Medium_Phone_API_36.1` (`UnlockScreenTest`).
- Lint: **0 errors, 32 warnings**, all dependency-update notices except one obsolete `mipmap-anydpi-v26` qualifier warning. The warning is intentional: adaptive icons require the `-v26` folder for AAPT.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (~21 MB, installable).
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (~2.4 MB, R8 + resource shrinking).
- Manual emulator pass: unlock, home, activity, detail, transfer form → review → success, updated balance, transfer-tab reset, profile, about, dark theme, hidden balances, status-bar icons.

## Demo constraints

- The demo is self-contained: `DemoApiInterceptor` serves bundled JSON through the real Retrofit/OkHttp stack.
- The unlock gate is a presentation feature, not production authentication.
- The release APK is unsigned because no release keystore is checked in.
- `:app` disables `lint.checkTestSources` due an AGP/Kotlin 2.2 FIR crash while resolving Compose UI-test symbols; the instrumentation tests still run via `connectedDebugAndroidTest`.

## Repository layout

See [README.md](README.md) for the architecture, demo script and production gaps, and [docs/GUIA_ESTUDIO_ES.md](docs/GUIA_ESTUDIO_ES.md) for the Spanish study guide.
