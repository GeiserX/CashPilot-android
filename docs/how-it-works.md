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

## Privacy

All app status data is sent only to your own CashPilot server. The app makes one more request, to `api.ipify.org`, to show your public IP on the dashboard, and only after the server URL and API key are configured. It sends app status to no other service; links you tap in the app (GitHub, signup pages, the Play Store) open in your browser.

The app accepts an `http://` server URL, and the bearer token then travels unencrypted. Use an `https://` URL for any server that is not on your own network.
