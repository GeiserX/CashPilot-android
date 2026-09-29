<p align="center">
  <img src="docs/images/banner.svg" alt="CashPilot Android" width="900"/>
</p>

# CashPilot Android

[![Release](https://img.shields.io/github/v/release/GeiserX/CashPilot-android?style=flat-square)](https://github.com/GeiserX/CashPilot-android/releases/latest)
[![CI](https://github.com/GeiserX/CashPilot-android/actions/workflows/ci.yml/badge.svg)](https://github.com/GeiserX/CashPilot-android/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/GeiserX/CashPilot-android?style=flat-square)](LICENSE)
[![codecov](https://codecov.io/gh/GeiserX/CashPilot-android/graph/badge.svg)](https://codecov.io/gh/GeiserX/CashPilot-android)

CashPilot Android is the Android companion app for [CashPilot](https://github.com/GeiserX/CashPilot). It detects which passive income apps are installed and running on your phone, without root, and reports them to your CashPilot fleet dashboard.

## Features

- Detects 11 passive income apps: EarnApp, IPRoyal Pawns, MystNodes, Traffmonetizer, Bytelixir, ByteBenefit, Grass, Titan Network, Nodle Cash, Uprock, Wipter
- Instant detection from each app's ongoing notification (NotificationListenerService)
- Confirms activity from per-app network traffic and recent foreground use (NetworkStatsManager, UsageStatsManager)
- Periodic heartbeats to your server (`POST /api/workers/heartbeat`), so phones appear next to Docker workers in the fleet view
- Sends data only to your own CashPilot server, plus one request to `api.ipify.org` to show your public IP
- No root required; Android 8.0 or later

## Quick start

1. Download `app-release.apk` from the [latest release](https://github.com/GeiserX/CashPilot-android/releases/latest) and install it.
2. Grant the three permissions it asks for: Notification Access, Usage Access, and the battery-optimisation exemption.
3. In Settings, enter your CashPilot server URL and the fleet key (`CASHPILOT_API_KEY` on the server).

Needs Android 8.0 (API 26) or later and a reachable CashPilot 1.x server.

## Documentation

- [How it works](docs/how-it-works.md): the three detection APIs, the heartbeat flow, privacy
- [Development](docs/development.md): building with Gradle, the stack

## Related projects

[CashPilot](https://github.com/GeiserX/CashPilot), [CashPilot-Desktop](https://github.com/GeiserX/CashPilot-Desktop), [cashpilot-ha](https://github.com/GeiserX/cashpilot-ha), [cashpilot-mcp](https://github.com/GeiserX/cashpilot-mcp).

## License

[GPL-3.0-or-later](LICENSE)
