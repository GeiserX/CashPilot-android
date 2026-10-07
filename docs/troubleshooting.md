# Troubleshooting

Symptom, cause, fix. The texts in bold are what the app shows.

## Every app shows Can't tell, or CashPilot can't see your apps

**Cause:** Notification Access or Usage Access is off. Without both, a quiet app cannot be told apart from a stopped one, so the app says it cannot tell.

**Fix:** tap **Grant Notification Access** and **Grant Usage Access** on the dashboard, or find CashPilot under Android's Settings, Apps, Special app access, and switch both on. Come back to the app; it checks again when it opens.

## An app shows Stopped but it is running

**Cause:** Stopped means that with both accesses granted, the app had no ongoing notification, no foreground use in the last 15 minutes and no more than 1 KB of traffic in the last 2 hours. Android's traffic counters are grouped in blocks of about two hours, so an app that only just started may not show traffic yet.

**Fix:** pull the dashboard down to check again after a few minutes. If it stays Stopped while the app is earning, open an issue with the app's name and version.

## Heartbeats stop when the screen is off

**Cause:** battery optimization. Android pauses or kills background services of optimized apps, and some makers add their own limits on top.

**Fix:** Settings, **Disable Battery Optimization**, until it reads **Battery optimization disabled**. On phones with an extra "auto-start" or "background activity" setting, allow CashPilot there too. The service starts again on its own after a restart of the phone.

## Server rejected heartbeat (401)

**Cause:** the server did not accept the key. If the phone had its own key, the app deletes it and enrolls again on the next heartbeat with the Fleet API Key. If the Fleet API Key is wrong or was changed on the server, enrollment fails too.

**Fix:** copy the current `CASHPILOT_API_KEY` from the server's fleet page (**Reveal API Key**) into Settings, Fleet API Key. See [The fleet key and enrollment](configuration.md#the-fleet-key-and-enrollment).

## Server rejected heartbeat with another code

**Cause:** the URL reaches something that is not the CashPilot heartbeat endpoint (404: a wrong path or an older server), or the server failed (5xx).

**Fix:** enter the server's base URL only, for example `https://cashpilot.example.com`, without `/api/...`. The app adds `/api/workers/heartbeat` itself. Check the server's log for the 5xx.

## Heartbeat failed — retrying...

**Cause:** the phone cannot reach the server: wrong host or port, the phone is off your network, or a certificate the phone does not trust (the app trusts only the system certificate authorities, so a self-signed certificate, or one from an authority you installed on the phone yourself, fails). The app retries with a growing wait, up to 4 minutes apart with the default interval.

**Fix:** open the server URL in the phone's browser. If the browser cannot load it, neither can the app. A server that is only on your home network needs the phone on that network or on a VPN to it.

## Not sending heartbeats: this server URL needs https://

**Cause:** the URL starts with `http://` and its host can be on the public internet: a public name or address, not loopback, a LAN, link-local or Tailscale address, or a `.lan`, `.local`, `.home.arpa`, `.internal` or `.ts.net` name. The app refuses to send the keys and makes no request.

**Fix:** use the server's `https://` address, or reach it on your own network: its LAN or Tailscale address, or a private name. [Which hosts work over http](configuration.md#the-server-url-use-https).

## The URL field is red: Insecure

**Cause:** the URL starts with `http://`, so the keys and the app data would travel unencrypted.

**Fix:** use the server's `https://` address. On loopback or a private name `http://` works, and the warning stays.

## Earnings say Nothing read yet

**Cause:** the server has no earnings figure for the platforms on this phone yet, or it is a server version that does not send them. This is not zero.

**Fix:** nothing on the phone. Figures appear once the server has collected balances for those platforms.

## Reporting a bug

Open an [issue](https://github.com/GeiserX/CashPilot-android/issues) with:

- the app version (Android's Settings, Apps, CashPilot) and the Android version;
- the phone's maker and model;
- the state the app shows and the one you expected, and the notification text;
- whether a server is paired, and its CashPilot version.

Leave out the server URL and the keys. For a security problem, follow the [security policy](https://github.com/GeiserX/CashPilot-android/blob/main/SECURITY.md) instead of opening an issue.
