# Configuration

Everything the app stores, where you set it, and what it does. There are no config files or environment variables: the settings live in the app's own storage on the phone (Android DataStore, file `cashpilot_settings`), and the app opts out of Android backup (`allowBackup="false"`), so the key never leaves the phone that way.

## Settings

| Setting | Where you set it | Stored as | Default | What it does |
|---|---|---|---|---|
| CashPilot Server URL | Setup step 1, or Settings, Server Connection | `server_url` | empty | Base URL of your CashPilot server. Heartbeats go to `<url>/api/workers/heartbeat`; a trailing `/` is ignored. Empty means standalone: no heartbeats, no earnings. |
| Fleet API Key | Setup step 1, or Settings, Server Connection | `api_key` | empty | The server's `CASHPILOT_API_KEY`. Used only until the phone has its own key (next section). |
| Per-worker key | Nothing to set: the server issues it | `worker_key` | empty | This phone's own key, received on its first heartbeat and used for every heartbeat after that. |
| Monitored apps | Settings, Monitored Apps | `enabled_slugs` | all 11 on | Which apps the dashboard and the heartbeat include. A switched-off app shows as **Disabled** and is left out of the heartbeat. |
| Heartbeat interval | No control in the app | `heartbeat_interval` | 30 seconds | Time between heartbeats. The service never goes below 5 seconds. |
| Setup completed | Finishing or skipping the setup screen | `setup_completed` | false | Whether the setup screen is shown at launch. An install that already had a URL and a key before this setting existed counts as set up. |

Changes to the URL and the key are saved half a second after you stop typing. The heartbeat service reads the settings again before every heartbeat, so a change takes effect on the next one; no restart is needed.

## The server URL: use https

The app accepts an `http://` URL only for a server it can tell is private, because a CashPilot server on a home network often has no certificate. It decides from the host as written, without a DNS lookup:

- loopback: `localhost`, `127.0.0.1`, `::1`;
- private and link-local addresses: `10.x.x.x`, `172.16.x.x` to `172.31.x.x`, `192.168.x.x`, `169.254.x.x`, and IPv6 `fc00::/7` and `fe80::/10`;
- Tailscale addresses (`100.64.x.x` to `100.127.x.x`) and MagicDNS names ending in `.ts.net`, because the tunnel encrypts;
- names that cannot be public: ending in `.local`, `.lan`, `.home.arpa` or `.internal`, or a bare name with no dot. A bare name (`http://cashpilot:8080`) is resolved through the network's search domain, so on a network whose search domain points at public hosts it could reach one; use the address or a suffixed name there.

A public host over `http://` is refused (`http://cashpilot.example.com`, `http://8.8.8.8`). Android itself lets an app allow plain http per host name or domain suffix, never per address range, so the app keeps cleartext on at the platform level and does the refusing itself, before any request is made.

On the hosts above, the fleet key, the per-worker key and the app data travel unencrypted, and the URL field turns red with **Insecure: API key and app data will be sent unencrypted. Use https:// if possible.** Any other `http://` URL is refused: the field shows **CashPilot sends your API key over http:// only to a server on your own network: localhost, a LAN, link-local or Tailscale address, a name ending in .local, .lan, .home.arpa, .internal or .ts.net, or a bare name with no dot. Use https:// for anything else.**, the dashboard and the notification read **Not sending heartbeats: this server URL needs https://**, and no request leaves the phone. An `http://` URL saved by an older version is refused the same way.

## The fleet key and enrollment

CashPilot servers from 1.0.0 give every worker its own key; the shared `CASHPILOT_API_KEY` is only for a worker's first contact.

1. The first heartbeat authenticates with the Fleet API Key you entered.
2. The server answers with a key for this phone. The app stores it as `worker_key` and uses it from the next heartbeat on. The server repeats the key in its answers until the phone has used it once.
3. From then on the phone keeps working even if you clear the Fleet API Key in Settings.
4. If the server answers **401** to the per-worker key (for example after the worker was removed on the server), the app deletes that key and enrolls again on the next heartbeat with the Fleet API Key.

The server side of this is on CashPilot's [fleet page](https://geiserx.github.io/CashPilot/fleet/#authentication).

## Heartbeat timing

- One heartbeat every 30 seconds while a server URL and a key are set.
- After a failed heartbeat the wait doubles, up to eight times the interval and never more than 5 minutes: 60, 120, 240, then 240 seconds again with the default interval. The first successful heartbeat resets it.
- Each request gives up after 15 seconds (10 seconds to connect).
- The service starts when the app opens and again after the phone restarts.

## Monitored apps

The app knows these 11 apps. The slug is the name the CashPilot server uses for the service; the package name is what the phone looks for.

| App | Slug | Package |
|---|---|---|
| EarnApp | `earnapp` | `com.brd.earnapp.play` |
| IPRoyal Pawns | `iproyal` | `com.iproyal.android` |
| MystNodes | `mysterium` | `network.mysterium.provider` |
| Traffmonetizer | `traffmonetizer` | `com.traffmonetizer.client` |
| Bytelixir | `bytelixir` | `com.bytelixir.blapp` |
| ByteBenefit | `bytebenefit` | `io.bytebenefit.app` |
| Grass | `grass` | `io.getgrass.www` |
| Titan Network | `titan` | `com.titan_network_vip.titan_app` |
| Nodle Cash | `nodle` | `io.nodle.cash` |
| Uprock | `uprock` | `com.uprock.mining` |
| Wipter | `wipter` | `com.wipter.app` |

Adding an app takes a new release: the list is in the code, and Android 11 and later only let the app see packages declared in its manifest.
