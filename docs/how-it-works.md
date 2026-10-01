# How it works

CashPilot Android detects which passive income apps are installed and running on the phone with three Android APIs. None of them needs root.

| API | Signal | Latency |
|-----|--------|---------|
| **NotificationListenerService** | The app has an ongoing (non-dismissable) notification showing | Instant |
| **NetworkStatsManager** | The app sent or received more than 1 KB in the last 2 hours | Up to 2 h |
| **UsageStatsManager** | The app was in the foreground in the last 15 minutes | Up to 15 min |

It sends periodic heartbeats to your CashPilot server, so the fleet view shows phones next to the Docker workers.

```
CashPilot Server (fleet dashboard)
        ^
        | POST /api/workers/heartbeat (Bearer auth; HTTPS if the server URL uses it)
        |
CashPilot Android
├── HeartbeatService (foreground, periodic POST)
├── AppNotificationListener (instant app detection)
├── AppDetector (UsageStats + NetworkStats)
└── Jetpack Compose UI (dashboard + settings)
```

## Running, stopped or can't tell

One signal is enough to call an app running, because each one is proof of life. Calling it stopped needs every signal to have been available and silent. Anything in between is "can't tell", never a guess:

| Signals the phone can see | Any of them positive | Answer |
|---|---|---|
| any | yes | running |
| all (both accesses granted) | no | stopped |
| some or none | no | can't tell |

Usage Access covers two of the three signals (foreground time and traffic), so without it most bandwidth apps, which run in the background without a visible notification, cannot be judged. The heartbeat sends the same three answers as `running`, `stopped` and `unknown`, so the fleet page does not report a stopped app the phone never saw.

## What a heartbeat carries

- A name: the phone's maker and model plus the first 8 characters of its Android ID, for example `Google Pixel 8 (a1b2c3d4)`. The Android ID is specific to this app on this phone and needs no permission.
- For each installed, switched-on app: its slug, the running answer, whether its ongoing notification is showing, bytes uploaded and downloaded over the last 24 hours, and its last foreground time.
- The same apps again as `containers`, the shape older CashPilot servers read, so they still list the phone.
- `system_info`: `Android`, the Android version and API level, the CPU architecture, `device_type: android` and this app's version, which lets the fleet page flag phones on an old build.

The server's answer can carry this phone's own key (see [Configuration](configuration.md#the-fleet-key-and-enrollment)) and the earnings figures the dashboard shows.

## Privacy

All app status data is sent only to your own CashPilot server. The app makes one more request, to `api.ipify.org`, to show your public IP on the dashboard, and only after the server URL and API key are configured. It sends app status to no other service; links you tap in the app (GitHub, signup pages, the Play Store) open in your browser.

The app sends the bearer token over `http://` only to a private address (LAN, loopback, link-local or Tailscale) and refuses `http://` to anything else; see [the server URL](configuration.md#the-server-url-use-https). Use an `https://` URL for any server that is not on your own network.
