# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-11-07

1.0 is the first release we consider complete. The app answers one question when you open it, whether every app you run is running, and it does that without root, without an account and without sending anything to anyone but your own server. It ships alongside CashPilot web 2.0 and CashPilot Desktop 1.0.

### Added

- **A new home screen that leads with one line.** "All 6 apps running", or what is wrong and what to do about it. When more than one thing needs you, the home screen shows them as a short ordered list. A stopped app has an **Open** button. One line shows the server state: when the phone last reported, or "No server · everything stays on this phone". Earnings come from your server, labelled with how old they are, and a single footnote marks a balance the provider reports for the whole account rather than for this phone.
- **A three-step setup** (welcome, permissions, server). Before each Android permission prompt, a screen says what CashPilot reads with that access and what it never reads. You can continue without the permissions or without a server; the home screen then says calmly what it cannot see.
- **A real connection test.** "Test connection" sends one report to your server and says what happened: connected, the server refused the key, the address needs `https://`, the address answers but is not CashPilot (a login page or another app), or no answer. Each case says what to check next. A report counts only when the server registered the phone.
- **Late reports show.** When no report has gone out for over an hour, the server line turns amber and the home screen says reports are late, instead of staying green while Android holds the work back.
- **Grouped settings** with the API key masked, a **Classic night** and a **Light** palette that follow the system theme, a demo mode with sample data and a banner saying so, a screen for when no supported app is installed, and a list of supported apps where links that carry CashPilot's referral code are labelled **Referral**.
- **Accessibility checked by tests.** Every tappable control is at least 48 dp and has a name TalkBack reads; every screen has a heading; the home screen reads in visual order; nothing is cut off or unreachable at font scale 1.3; text in both palettes meets WCAG AA contrast. Screenshot goldens of every screen, dark and light, in four situations (all running, problems, no server, no permissions) fail CI on any unintended visual change.

### Security

- **This phone's own key never goes to another server.** Changing the server address, or typing a different fleet key, drops the key the old server issued, so the next report enrolls again with the details just entered.

### Removed

- **The public IP lookup.** Earlier versions asked `api.ipify.org` for the phone's public address. Nothing uses it now, so the app makes no request except to the server you set.
- **Uprock is no longer watched on Android.** Google Play Protect removes Uprock's own app from phones, so a Play-listed CashPilot that sends people to install it would put the app's Play review at risk. The app now watches 10 apps. CashPilot web and Desktop keep Uprock.

### Changed

- **Heartbeats run every 15 minutes on WorkManager instead of every 30 seconds from a foreground service.** The always-on special-use foreground service and its permanent notification are gone, and so is the restart-on-boot receiver: WorkManager keeps the schedule across restarts. The app also reports once when it is opened and once when the server URL or key changes. The last report and the last earnings figure are kept on disk, so the dashboard still shows them after Android stops the app between runs, and it now says "Last report 6 min ago · sent every 15 min" instead of implying live monitoring.
- **The battery button opens Android's battery-optimisation list instead of the direct exemption dialog**, and the app no longer requests `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Google Play allows that permission only for chat, VoIP, safety, automation and peripheral apps. The setup screen says which entries to pick in the list. `POST_NOTIFICATIONS` is no longer requested either, because nothing posts a notification any more.

## [0.4.0] - 2026-11-06

### Changed

- **Native libraries are packaged exactly as their dependency ships them.** AGP strips debug symbols from `.so` files only when it finds the NDK it wants, so a builder with that NDK and one without produced different `lib/*/libdatastore_shared_counter.so`, and F-Droid's rebuild could not match our APK. `packaging.jniLibs.keepDebugSymbols` now keeps every `.so` untouched, and two clean builds, one with NDK 28.2 and one with none, give a byte-identical APK.
- **The release refuses a tag that disagrees with `gradle.properties`.** v0.3.0 was built as 0.3.0 / 300 while the file still said 0.2.0 / 200. F-Droid builds from the file, so its APK could never match the one we publish. The file now says 0.4.0 / 400, and `scripts/check-release-version.sh` stops the release before it builds if the tag and the file differ.
- Store listing: a corrected description, an icon and a feature graphic for F-Droid and Play, plus the F-Droid recipe in `fdroid/`.

### Security

- **The fleet key no longer goes to a public host over plain `http://`.** The app sent its bearer token to any `http://` URL and only showed a warning. Now it refuses `http://` to any host that can be on the public internet, before a request leaves the phone and including for a URL saved by an older version: `http://` is accepted only for loopback, a private, link-local or Tailscale address (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`, `169.254.x.x`, `100.64-127.x.x`, `fc00::/7`, `fe80::/10`) or a name ending in `.local`, `.lan`, `.home.arpa`, `.internal` or `.ts.net`. The URL field, the dashboard and the notification say why. `https://` works as before. The network security config keeps cleartext on at the base, because Android can allow plain http per name or domain suffix but never per address range and the common home setup is a server at `http://192.168.x.x`; a test reads the config through Android's own parser and fails if it ever refuses a host the app allows, or if the app stops refusing a public one.

### Fixed

- The install buttons for MystNodes, ByteBenefit, Bytelixir and Grass now open the same referral sign-up pages as the CashPilot web catalog. MystNodes and ByteBenefit had no link, Bytelixir and Grass used outdated URLs.
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
