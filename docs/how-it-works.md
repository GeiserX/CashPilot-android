# How it works

CashPilot Android detects which passive income apps are installed and running on the phone with three Android APIs. None of them needs root.

| API | Signal | Latency |
|-----|--------|---------|
| **NotificationListenerService** | The app's foreground-service notification is active | Instant |
| **NetworkStatsManager** | The app is transferring data (bytes sent and received per app) | About 2 h buckets |
| **UsageStatsManager** | The app was recently in the foreground | About 2 h buckets |

It sends periodic heartbeats to your CashPilot server, so the fleet view shows phones next to the Docker workers.

```
CashPilot Server (fleet dashboard)
        ^
        | HTTPS POST /api/workers/heartbeat (Bearer auth)
        |
CashPilot Android
├── HeartbeatService (foreground, periodic POST)
├── AppNotificationListener (instant app detection)
├── AppDetector (UsageStats + NetworkStats)
└── Jetpack Compose UI (dashboard + settings)
```

## Privacy

All app status data is sent only to your own CashPilot server. The app makes one more request, to `api.ipify.org`, to show your public IP on the dashboard, and only after the server URL and API key are configured. No other third-party service is contacted.
