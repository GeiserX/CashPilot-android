# Troubleshooting

Symptom, cause, fix. The texts in bold are what the app shows.

## Apps show Can't see it, or CashPilot can't see your apps yet

**Cause:** Notification Access or Usage Access is off. Without both, a quiet app cannot be told apart from a stopped one, so the app says it cannot tell.

**Fix:** tap **Allow notification access** and **Allow usage access** on the home screen (or **Turn on** in the list of what needs you), or find CashPilot under Android's Settings, Apps, Special app access, and switch both on. Come back to the app; it checks again when it opens.

## An app shows Stopped but it is running

**Cause:** Stopped means that with both accesses granted, the app had no ongoing notification, no foreground use in the last 15 minutes and no more than 1 KB of traffic in the last 2 hours. Android's traffic counters are grouped in blocks of about two hours, so an app that only just started may not show traffic yet.

**Fix:** pull the home screen down to check again after a few minutes. If it stays Stopped while the app is earning, open an issue with the app's name and version.

## Heartbeats arrive late when the screen is off

**Cause:** first check that the phone has a connection: a report waits for a network, so a phone with no Wi-Fi or data sends nothing. If it is online, it is usually battery optimization. Android holds back scheduled work of optimized apps while the phone is idle, and some makers add their own limits on top. Reports are due every 15 minutes, but Android sets no upper bound on the delay; a phone Android rates as rarely used can wait hours.

**Fix:** Settings, This phone, **Battery optimisation**. It opens Android's list; choose All apps, then CashPilot, then Don't optimize, until the row reads **Off for CashPilot**. On phones with an extra "auto-start" or "background activity" setting, allow CashPilot there too. The schedule survives a restart of the phone.

## Server rejected heartbeat (401)

**Cause:** the server did not accept the key. If the phone had its own key, the app deletes it and enrolls again on the next heartbeat with the fleet key. If that key is wrong or was changed on the server, enrollment fails too.

**Fix:** copy the current `CASHPILOT_API_KEY` from the server's fleet page (**Reveal API Key**) into Settings, Server, API key, then tap **Test connection**: it reads **Connected** once the key is right. See [The fleet key and enrollment](configuration.md#the-fleet-key-and-enrollment).

## Server rejected heartbeat with another code

**Cause:** the URL reaches something that is not the CashPilot heartbeat endpoint (404: a wrong path or an older server), or the server failed (5xx).

**Fix:** enter the server's base URL only, for example `https://cashpilot.example.com`, without `/api/...`. The app adds `/api/workers/heartbeat` itself. Check the server's log for the 5xx.

## Can't reach your server

**Cause:** the last report failed; the home screen reads **Can't reach … for …** and lists **Your server isn't answering**. If the server answered with an error, see the 401 and other-code entries above. Otherwise the phone cannot reach the server: wrong host or port, the phone is off your network, or a certificate the phone does not trust (the app trusts only the system certificate authorities, so a self-signed certificate, or one from an authority you installed on the phone yourself, fails). The app tries again at the next 15-minute report, or when you open it or change the server URL or key.

**Fix:** **Check connection** (or Settings, **Test connection**) sends a report now and names the cause: **No answer from …**, **The server refused this API key**, or **The server answered with an error**. Then open the server URL in the phone's browser. If the browser cannot load it, neither can the app. A server that is only on your home network needs the phone on that network or on a VPN to it.

## Not sending to …: it needs https://

**Cause:** the URL starts with `http://` and its host can be on the public internet: a public name or address, not loopback, a LAN, link-local or Tailscale address, or a `.lan`, `.local`, `.home.arpa`, `.internal` or `.ts.net` name. The app refuses to send the keys and makes no request.

**Fix:** use the server's `https://` address, or reach it on your own network: its LAN or Tailscale address, or a private name. [Which hosts work over http](configuration.md#the-server-url-use-https).

## The address field warns about plain http://

**Cause:** the address starts with `http://` on your own network, so the keys and the app data travel unencrypted on it.

**Fix:** use the server's `https://` address if it has one. On loopback or a private name `http://` works, and the note stays.

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
