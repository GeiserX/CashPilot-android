# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- **The app now reports its own version to the server.** Every heartbeat's `system_info` carried `os`, `arch`, `os_version` and `device_type`, but never a `version`. The CashPilot server reads exactly that key to tell whether a worker is on a different release series from the UI, so every Android device showed as "version unknown" on the fleet page and there was no way to see which phones were running an outdated build.

  That is not cosmetic. Per-worker key enrollment shipped in 0.2.0, but two devices stayed on an older build for weeks, still authenticating with the shared bootstrap key, and nothing surfaced it — the fleet page had no version to show. It only came to light when the server began bounding how long an unconfirmed worker may keep using the shared key.

  An absent version still reads as *unknown*, never as a match: the server only reports a mismatch when both sides are known releases.

## [0.2.0] - 2026-07-11

### Added

- **Per-worker fleet keys.** The app now enrolls automatically on its first heartbeat against a CashPilot server (v1.0.0+): it receives its own per-worker key, persists it, and authenticates every subsequent heartbeat with it. The configured fleet API key becomes an enrollment-only bootstrap credential. No setup change is needed — existing devices re-enroll on their next heartbeat. Interoperates with both the CashPilot web UI and CashPilot-Desktop's fleet server. If the server ever rejects the per-worker key (HTTP 401), the app clears it and automatically re-enrolls on the next heartbeat. Heartbeats also keep working on the per-worker key even if the shared bootstrap key is later removed from Settings.

## [0.1.0] - 2026-03-31

### Added

- Heartbeat foreground service that sends periodic status to the CashPilot server via `POST /api/workers/heartbeat` with bearer auth
- NotificationListenerService for instant detection of running passive income apps via their foreground notifications
- AppDetector combining NotificationListener, UsageStatsManager, and NetworkStatsManager for comprehensive app health monitoring
- Detection of 17 passive income apps: Honeygain, EarnApp, IPRoyal Pawns, Mysterium, PacketStream, Traffmonetizer, Repocket, Peer2Profit, Bytelixir, ByteBenefit, Grass, GagaNode, Titan Network, Nodle Cash, PassiveApp, Uprock, Wipter
- Jetpack Compose UI with Material 3 dashboard showing real-time app status
- Settings screen with server URL, fleet API key, heartbeat interval, and per-app toggle
- Permission setup buttons for Notification Access, Usage Access, and Battery Optimization
- DataStore-backed settings persistence
- Boot receiver to restart heartbeat service after reboot
- Unique worker identification via ANDROID_ID
- Android 11+ package visibility support via `<queries>` manifest block
- Android 14+ foreground service compliance with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
- Backup disabled (`allowBackup="false"`) to prevent API key leakage

[0.1.0]: https://github.com/GeiserX/CashPilot-android/releases/tag/v0.1.0
