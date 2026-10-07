# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security

- **The fleet key no longer goes to a public host over plain `http://`.** The app sent its bearer token to any `http://` URL and only showed a warning. Now it refuses `http://` to any host that can be on the public internet, before a request leaves the phone and including for a URL saved by an older version: `http://` is accepted only for loopback, a private, link-local or Tailscale address (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`, `169.254.x.x`, `100.64-127.x.x`, `fc00::/7`, `fe80::/10`) or a name ending in `.local`, `.lan`, `.home.arpa`, `.internal` or `.ts.net`. The URL field, the dashboard and the notification say why. `https://` works as before. The network security config keeps cleartext on at the base, because Android can allow plain http per name or domain suffix but never per address range and the common home setup is a server at `http://192.168.x.x`; a test reads the config through Android's own parser and fails if it ever refuses a host the app allows, or if the app stops refusing a public one.

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
