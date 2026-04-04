# CashPilot-android - AI Assistant Configuration

## Before Starting Any Coding Task

1. Read this file completely before making any changes
2. Verify you are on the correct branch (`main` or a feature branch)
3. Run `./gradlew build` to confirm the project compiles before starting

## Persona

You assist developers working on CashPilot-android, a lightweight Android agent that monitors passive income apps and reports their status to a CashPilot server.

## Tech Stack

- Kotlin 2.1+
- Jetpack Compose (Material 3)
- Ktor HTTP client
- Kotlinx Serialization
- DataStore Preferences
- Android SDK 26+ (minSdk)
- Gradle with version catalogs (`libs.versions.toml`)

> **AI Assistance:** Let AI analyze the codebase and suggest additional technologies and approaches as needed.

## Repository & Infrastructure

- **License:** GPL-3.0-only
- **Commits:** Follow [Conventional Commits](https://conventionalcommits.org) format
- **Versioning:** Follow [Semantic Versioning](https://semver.org) (semver)
- **Package:** `com.cashpilot.android`
- **Version source of truth:** `gradle.properties` (`VERSION_NAME`, `VERSION_CODE`). CI overrides via `-P` flags.
- **Release workflow:** Push a `v*` tag → GitHub Actions builds signed APK+AAB, creates GitHub Release with artifacts
- **CI workflow:** On push to main → build debug APK + lint + signed release APK. On PR → debug + lint only (no keystore access). Both APKs uploaded as artifacts. Always download `app-release` for installation (debug APK has different signature).
- **Signing keystore:** JKS at `~/repos/personal/keystores/cashpilot-release.jks` (alias: `cashpilot`, password: `cashpilot-release-2026`). Base64-encoded in GitHub secret `KEYSTORE_BASE64`.
- **F-Droid:** MR !35850 at gitlab.com/fdroid/fdroiddata. Metadata at `metadata/com.cashpilot.android.yml`. Uses `UpdateCheckData` to read version from `gradle.properties`. No `VercodeOperation` (VERSION_CODE is explicit).

## AI Behavior Rules

- Never modify this file unless explicitly told to
- Always check existing code patterns before adding new code
- Follow Kotlin coding conventions and Material 3 guidelines
- Keep the APK size minimal — avoid unnecessary dependencies
- All network calls must handle offline/timeout gracefully
- Never hardcode server URLs or tokens
- All strings must go through `strings.xml` for future i18n
- minSdk is 26 — guard any API 29+ calls with `Build.VERSION.SDK_INT` checks (lint enforces this)
- Use cancel-and-replace Job pattern for concurrent coroutine operations (see `refreshJob` in MainViewModel)
- Use `ensureActive()` after `withContext(Dispatchers.IO)` blocks to discard stale results from cancelled jobs
- Use `AppOpsManager` to check usage access permission (not `UsageStatsManager.queryUsageStats` heuristic)
- DataStore text field writes should be debounced (500ms per-field cancel-and-replace) to avoid writes on every keystroke
- POST_NOTIFICATIONS runtime permission must be requested on Android 13+ for foreground service notifications

## Architecture

```
com.cashpilot.android/
├── model/           # Data classes (Heartbeat, AppStatus, MonitoredApp, Settings)
├── service/         # Android services (HeartbeatService, AppNotificationListener, AppDetector)
├── ui/              # Compose UI (MainActivity, screens, theme, components)
│   ├── screen/      # Full-screen composables (Dashboard, Settings)
│   ├── theme/       # Material 3 theme
│   └── component/   # Reusable composable components
└── util/            # Utilities (SettingsStore, formatters)
```

### Key Components

- **HeartbeatService**: Foreground service that sends periodic HTTP POST to CashPilot server with app status. Uses Ktor client. Sticky service with boot receiver.
- **AppNotificationListener**: NotificationListenerService that detects when monitored apps have active foreground notifications. Primary detection mechanism — instant callback.
- **AppDetector**: Combines NotificationListener state with UsageStatsManager (last active time) and NetworkStatsManager (per-app bytes tx/rx) to build a complete picture of app health.
- **SettingsStore**: DataStore-backed persistence for server URL, fleet API key, heartbeat interval, and enabled app list.

### Detection Strategy

Three complementary APIs, no root required:

1. **NotificationListenerService** — proves the app's foreground service is alive (instant)
2. **UsageStatsManager** — last foreground time, ~2h bucket resolution
3. **NetworkStatsManager** — per-app bandwidth in last 24h, proves data is flowing

### Heartbeat Protocol

POST to `{serverUrl}/api/workers/heartbeat` with bearer auth (fleet API key via `CASHPILOT_API_KEY`). Payload matches the server's `WorkerHeartbeat` schema (`name`, `url`, `containers`, `system_info`). Android-specific app status is packed into `system_info.apps`.

## Known Apps

11 passive income apps with verified Android package names are defined in `KnownApps.kt`. All package names were verified via `adb shell pm list packages` on a real device.

When adding new apps:

1. **Verify the exact package name** via `adb shell pm list packages | grep -i <name>` — Play Store URLs and web searches are unreliable (many apps are delisted or have wrong IDs online)
2. Add to the `all` list in `KnownApps`
3. Add the package to `<queries>` in `AndroidManifest.xml` (required for Android 11+ package visibility)
4. Update the README app count

Apps removed (not on Play Store, APK-only distribution): Honeygain, PacketStream, Peer2Profit, GagaNode, PassiveApp, Repocket. Can be re-added if their real package names are confirmed via adb.

## Testing

- Unit tests go in `app/src/test/`
- Instrumented tests go in `app/src/androidTest/`
- Test detection logic with mock UsageStats/NetworkStats data

## Lint & Build Notes

- No local Java on macOS dev machine — all compilation is via GitHub Actions CI
- Lint baseline at `app/lint-baseline.xml` — new lint errors are fatal
- compileSdk=36, targetSdk=35, minSdk=26
- ProGuard enabled for release builds (`isMinifyEnabled = true`, `isShrinkResources = true`)

## APK Installation via adb

- **ALWAYS install the release-signed APK (`app-release`). NEVER use the debug APK.** Switching signatures requires a full uninstall, which wipes all app data (server URL, API key, permissions).
- Download from CI: `gh run download <run-id> --name app-release --dir /tmp/cashpilot-apk`
- Install: `adb install -r /tmp/cashpilot-apk/app-release.apk`
- If signature mismatch: `adb uninstall com.cashpilot.android` first, then install (user must re-configure)

## What NOT to Build Yet

- Earnings collection on Android (CashPilot server handles this via API collectors)
- Auto-start monitored apps (requires accessibility service, avoid for now)
- Root-only features (Shizuku process enumeration, etc.)
- Widget — defer until core monitoring is solid

---

*Generated by [LynxPrompt](https://lynxprompt.com) CLI*
