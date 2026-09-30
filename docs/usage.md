# Usage

Day to day you open the dashboard, look at what needs attention, and leave the app alone. It keeps checking in the background.

## The dashboard

From top to bottom:

- **The summary.** How many monitored apps are **Running**, **Stopped**, **unknown** and **N/A** (not installed), the upload and download traffic of all monitored apps over the last 24 hours, and, once a server is set, the phone's public IP.
- **The server line.** **Not paired — running standalone** without a server (tap it to open Settings). With a server: a green dot and **Last heartbeat** with the time, a red dot when the last heartbeat failed, a grey one and **No heartbeat yet** before the first.
- **The earnings card** (below).
- **One card per app**, the apps that need attention first: Stopped, then Can't tell, then Running, then Disabled, then Not installed.

Pull the list down to check every app again. The app also checks each time you come back to it, for example after granting a permission.

## What each app state means

| State | Colour | Meaning | What to do |
|---|---|---|---|
| Running | green | One signal proved the app is alive: an ongoing notification, foreground use in the last 15 minutes, or more than 1 KB of traffic in the last 2 hours. | Nothing. |
| Stopped | red | Every signal was available and none showed life. The card shows **Last:** with the last foreground time when there is one. | Open the app and start it again. |
| Can't tell | amber | Nothing showed life, and one of the two accesses is missing, so the app cannot know. | Grant the missing access; do not restart the app. |
| Disabled | grey | You switched the app off in Settings. | Switch it back on if you want it watched. |
| Not installed — tap to install | faded | The app is not on this phone. | Tap to open its signup page, or its Play Store page. |

A card with an ongoing notification also shows **Notification active**.

Can't tell is not Stopped on purpose. A phone that cannot see used to report every app as stopped, and a restart does not fix a missing permission. The server gets the same three answers: `running`, `stopped` or `unknown`.

## When the app cannot see

- **Neither access granted:** the app list is replaced by one card, **CashPilot can't see your apps**, with **Grant Notification Access** and **Grant Usage Access** buttons. It cannot be dismissed, because the list would say nothing true.
- **One access missing:** a **Permissions needed** banner above the list with the button for the missing one. You can dismiss it; it comes back if a permission is revoked.

## Earnings

With a server paired, each heartbeat's answer can carry what the server has recorded for the platforms on this phone. The card shows the total for the **Last 30 days** (the server picks the window) and each app card shows its own figure.

- **Nothing read yet** means the server has no figure for these apps. It is not zero, and the app never shows `$0.00` for it. Apps without a figure show a dash.
- **May be out of date** and **Last updated** appear when the newest figure is more than an hour old. The app keeps the last figure while the phone is offline rather than blanking it.
- **Shared across your devices** marks a platform whose provider reports one balance for your whole account, so the figure is not what this phone earned alone.
- Without a server the card reads **Pair a CashPilot server to see earnings**.

## The notification

The service keeps one silent, low-priority notification titled **CashPilot**:

| Text | When |
|---|---|
| Monitoring apps... | The service has started and has sent no heartbeat yet; without a server it stays this way. |
| 3/4 apps running | After a heartbeat: running apps out of installed, switched-on apps. |
| 3/4 apps running, 1 unknown | Same, with apps in Can't tell. |
| Server rejected heartbeat (401) | The server answered with an error code. |
| Heartbeat failed — retrying... | The server could not be reached. |

## Settings

The gear icon on the dashboard opens Settings:

- **Server Connection:** the server URL and the Fleet API Key ([Configuration](configuration.md)).
- **Disable Battery Optimization**, or **Battery optimization disabled** when it already is.
- **Monitored Apps:** a switch per app, with its package name. Only installed apps are reported.
- **About:** links to this app's and CashPilot's GitHub pages, and to the sponsor page.
