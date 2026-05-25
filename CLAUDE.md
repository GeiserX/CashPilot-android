# CLAUDE.md — CashPilot Android

## Overview
Android monitoring agent for CashPilot that tracks passive income apps (EarnApp, IPRoyal Pawns, MystNodes, Traffmonetizer, Bytelixir, ByteBenefit, Grass, Titan Network, Nodle Cash, Uprock, Wipter) without root, reporting their status to the CashPilot fleet dashboard via periodic heartbeats.

## Tech Stack
- Kotlin 2.1+
- Jetpack Compose (Material 3)
- Ktor HTTP client
- Kotlinx Serialization
- DataStore Preferences
- Android SDK 26+ (minSdk), compileSdk 36, targetSdk 35
- Gradle with version catalogs (`libs.versions.toml`)
- ProGuard (release builds: `isMinifyEnabled = true`, `isShrinkResources = true`)

## Development

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test
```

**No local Java on macOS dev machine** — all compilation is via GitHub Actions CI. Lint baseline at `app/lint-baseline.xml` — new lint errors are fatal.

### Repository & Infrastructure

- **Package:** `com.cashpilot.android`
- **Version source of truth:** `gradle.properties` (`VERSION_NAME`, `VERSION_CODE`). CI overrides via `-P` flags.
- **Release workflow:** Push a `v*` tag -> GitHub Actions builds signed APK+AAB, creates GitHub Release
- **CI workflow:** On push to main -> debug APK + lint + signed release APK. On PR -> debug + lint only (no keystore access)
- **Signing keystore:** JKS at `~/repos/personal/keystores/cashpilot-release.jks` (alias: `cashpilot`, password: `cashpilot-release-2026`). Base64-encoded in GitHub secret `KEYSTORE_BASE64`.
- **F-Droid:** MR !35850 at gitlab.com/fdroid/fdroiddata. Metadata at `metadata/com.cashpilot.android.yml`. Uses `UpdateCheckData` to read version from `gradle.properties`.

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

- **HeartbeatService**: Foreground service sending periodic HTTP POST to CashPilot server with app status. Uses Ktor client. Sticky service with boot receiver.
- **AppNotificationListener**: NotificationListenerService that detects when monitored apps have active foreground notifications. Primary detection mechanism — instant callback.
- **AppDetector**: Combines NotificationListener state with UsageStatsManager (last active time) and NetworkStatsManager (per-app bytes tx/rx) for complete app health picture.
- **SettingsStore**: DataStore-backed persistence for server URL, fleet API key, heartbeat interval, and enabled app list.

### Detection Strategy

Three complementary APIs, no root required:

1. **NotificationListenerService** — proves the app's foreground service is alive (instant)
2. **UsageStatsManager** — last foreground time, ~2h bucket resolution
3. **NetworkStatsManager** — per-app bandwidth in last 24h, proves data is flowing

### Heartbeat Protocol

POST to `{serverUrl}/api/workers/heartbeat` with bearer auth (fleet API key via `CASHPILOT_API_KEY`). Payload matches the server's `WorkerHeartbeat` schema (`name`, `url`, `containers`, `system_info`). Android-specific app status is packed into `system_info.apps`.

## Key Rules
- Never hardcode the CashPilot server URL or API key
- Uses Bearer auth for server communication
- minSdk is 26 — guard any API 29+ calls with `Build.VERSION.SDK_INT` checks (lint enforces)
- Use cancel-and-replace Job pattern for concurrent coroutine operations (see `refreshJob` in MainViewModel)
- Use `ensureActive()` after `withContext(Dispatchers.IO)` blocks to discard stale results
- Use `AppOpsManager` to check usage access permission (not `UsageStatsManager.queryUsageStats` heuristic)
- DataStore text field writes should be debounced (500ms per-field cancel-and-replace)
- POST_NOTIFICATIONS runtime permission must be requested on Android 13+
- License: GPL-3.0

## Known Apps

11 passive income apps with verified Android package names defined in `KnownApps.kt`. All verified via `adb shell pm list packages`.

When adding new apps:
1. **Verify exact package name** via `adb shell pm list packages | grep -i <name>` — Play Store URLs are unreliable
2. Add to the `all` list in `KnownApps`
3. Add the package to `<queries>` in `AndroidManifest.xml` (Android 11+ package visibility)
4. Update the README app count

Apps removed (APK-only, not on Play Store): Honeygain, PacketStream, Peer2Profit, GagaNode, PassiveApp, Repocket.

## APK Installation via adb

- **ALWAYS install the release-signed APK (`app-release`). NEVER use debug APK.** Switching signatures requires full uninstall (wipes all app data).
- Download from CI: `gh run download <run-id> --name app-release --dir /tmp/cashpilot-apk`
- Install: `adb install -r /tmp/cashpilot-apk/app-release.apk`
- If signature mismatch: `adb uninstall com.cashpilot.android` first

## What NOT to Build Yet

- Earnings collection on Android (server handles this)
- Auto-start monitored apps (requires accessibility service)
- Root-only features (Shizuku process enumeration)
- Widget — defer until core monitoring is solid

*Generated by [LynxPrompt](https://lynxprompt.com) CLI*
