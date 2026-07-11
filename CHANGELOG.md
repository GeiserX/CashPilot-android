# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-07-11

### Added

- **Per-worker fleet keys.** The app now enrolls automatically on its first heartbeat against a CashPilot server (v1.0.0+): it receives its own per-worker key, persists it, and authenticates every subsequent heartbeat with it. The configured fleet API key becomes an enrollment-only bootstrap credential. No setup change is needed — existing devices re-enroll on their next heartbeat. Interoperates with both the CashPilot web UI and CashPilot-Desktop's fleet server.

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
