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

**No working JVM on either Mac** — the dev machine has no Java, and the mini's Temurin 17 crashes at startup (`SIGBUS ... CodeHeap::allocate`, even on `java -version` and in `-Xint`). Use `scripts/remote-gradle.sh test|assembleDebug|lintDebug`, which runs Gradle in `eclipse-temurin:17-jdk` on the build host (~26s warm). That is a **faster pre-check, not a replacement for CI** — CI additionally builds the signed release variant and runs lint against the baseline, so never report a green local run as "CI passed". Lint baseline at `app/lint-baseline.xml` — new lint errors are fatal.

### Icon goldens

`app/src/test/screenshots/` holds one golden PNG per icon the app draws.
Roborazzi renders Compose on the JVM through Robolectric, so there is no
emulator and no device involved and it runs anywhere Gradle does.

```bash
./scripts/remote-gradle.sh verifyRoborazziDebug   # fail on any visual change (what CI runs)
./scripts/remote-gradle.sh recordRoborazziDebug   # re-record, only when a change is INTENDED
```

`remote-gradle.sh` syncs the goldens and any failure diffs back from the build
host; without that the record task looks like it did nothing.

They exist for one job: `material-icons-extended` is frozen at 1.7.8 and the
migration to per-icon vector drawables is sixteen substitutions whose only
acceptable outcome is "nothing changed visually". A diff during that migration
means the glyph is not the same one.

**Icons only, not whole screens — on purpose.** Screen goldens are dominated by
text, and text rendering depends on the fonts on the machine that rendered it,
so they fail for reasons unrelated to the change and then get muted. Icons are
pure vector geometry. Screen-level goldens belong with the UI-renewal work,
where the fonts can be pinned deliberately.

### Repository & Infrastructure

- **Package:** `com.cashpilot.android`
- **Version source of truth:** `gradle.properties` (`VERSION_NAME`, `VERSION_CODE`). CI overrides via `-P` flags.
- **Release workflow:** Push a `v*` tag -> GitHub Actions builds signed APK+AAB, creates GitHub Release
- **CI workflow:** On push to main -> debug APK + lint + signed release APK. On PR -> debug + lint only (no keystore access)
- **Signing keystore:** JKS at `~/repos/personal/keystores/cashpilot-release.jks` (alias: `cashpilot`). **The password is NOT recorded here — this repository is public.** It lives with the other credentials in `~/repos/personal/.claude/reference/`. The keystore is base64-encoded in the GitHub secret `KEYSTORE_BASE64`.
- **F-Droid: NOT listed.** MR !35850 at gitlab.com/fdroid/fdroiddata was **closed without merging** (2026-07-04). `metadata/com.cashpilot.android.yml` is **not** on their master (404, verified against a known-good file returning 200), and `f-droid.org/packages/com.cashpilot.android/` is 404. The recipe used `UpdateCheckData` to read the version from `gradle.properties`. Read the closed MR's discussion before resubmitting — see `PUBLISHING.md`.

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


<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:6cd5cc61 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:
   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   git push
   git status
   ```
5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**
- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->

## Where the tracker syncs

This repo is public, so its tracker syncs only to the private remote named by `sync.remote` in `.beads/config.yaml`. The block above says sync uses "your git remote". Here that never means this GitHub repo. Don't add it as a Dolt remote and don't push `refs/dolt/*` to it.
