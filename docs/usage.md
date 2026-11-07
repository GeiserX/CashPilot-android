# Usage

Day to day you open the app, read the first line, and leave it alone. It keeps checking in the background.

## The home screen

From top to bottom:

- **The verdict.** One line that answers "is everything running?": **All 6 apps running**, **2 of 6 apps stopped**, or **4 of 6 apps running** when CashPilot can't see the rest. The ring beside it is green, amber or red to match.
- **The server line.** **No server · everything stays on this phone** without a server. With one: **Last report 6 min ago · sent every 15 min**, **Paired with … · waiting for the first report** before the first, or **Can't reach … for 25 min** in amber when reports fail.
- **What needs you.** When more than one thing is wrong, a numbered list in the order to fix them: stopped apps (the longest stopped first), then the server, then a missing access, then battery optimisation. Each item has its button. A single stopped app needs no list: its row has an **Open** button.
- **The apps.** One row per installed, watched app, problems first. Tap a row for its details.
- **The earnings strip** (below), and **N other supported apps** for the ones not installed.

Pull the screen down to check every app again. The app also checks each time you come back to it, for example after granting an access.

## What each app state means

| State | Colour | Meaning | What to do |
|---|---|---|---|
| Running | green | One signal proved the app is alive: an ongoing notification, foreground use in the last 15 minutes, or more than 1 KB of traffic in the last 2 hours. The row shows its traffic over the last 24 hours. | Nothing. |
| Stopped | red | Every signal was available and none showed life. The row says when it was last seen. | Tap **Open**. Opening the app usually starts it again. |
| Can't see it | amber | Nothing showed life, and one of the two accesses is missing, so the app cannot know. | Turn the missing access back on; do not restart the app. |
| Not watched | grey | You switched the app off. It leaves the home screen and the reports. | Switch it back on in Settings, Apps to watch. |

Can't see it is not Stopped on purpose. A phone that cannot see used to report every app as stopped, and a restart does not fix a missing permission. The server gets the same three answers: `running`, `stopped` or `unknown`.

## An app's details

Tap a row to see the app's state, an **Open** button, the server's figure for it, its data use over the last 24 hours, when it was last seen working, and a **Watched by CashPilot** switch. If the app has no launcher entry, **Open** shows its page in Android's settings instead.

## When the app cannot see

- **Neither access granted:** the home screen is replaced by **CashPilot can't see your apps yet**, with **Allow notification access** and **Allow usage access**. Nothing is shown as stopped, because nothing is known.
- **One access missing:** it appears in the list of what needs you, with **Turn on**.

Each **Allow** or **Turn on** first shows what CashPilot reads and what it never reads, then opens Android's settings. **Not now** goes back without opening them.

## Earnings

With a server paired, each report's answer can carry what the server has recorded for the platforms on this phone. The strip shows the total for the last 30 days (the server picks the window), and each app's detail page shows its own figure.

- **Nothing read yet** means the server has no figure for these apps. It is not zero, and the app never shows `$0.00` for it.
- **From 7 h ago** appears when the newest figure is more than an hour old. The app keeps the last figure while the phone is offline rather than blanking it.
- A figure marked **\*** is a provider balance for your whole account, because the app runs on more than one of your devices; one footnote under the strip says so.
- Without a server the strip reads **Add a server to see figures**; tap it to open Settings.

## No supported app installed

The home screen says **No supported app on this phone yet**, explains that CashPilot works without a server, and offers **Show a demo with sample data** and the list of supported apps. Installed apps appear by themselves.

## Supported apps

The list of apps CashPilot can watch, with a note at the top: links marked **Referral** open the provider's sign-up page with CashPilot's referral code, which supports the project; the others open the app store. What any app pays depends on the provider, your connection and your region.

## Demo mode

Settings, **Demo mode** (or the button on the empty home screen) shows the home screen with sample data under a **Demo · sample data, nothing here is real** banner. **Exit** or Back leaves it.

## Settings

The gear icon on the home screen opens Settings, grouped by what you are checking:

- **Server:** its state, the API key masked to its last four characters, the address and key fields, and **Test connection**, which sends one report now and says what the server answered ([Configuration](configuration.md)).
- **This phone:** notification access, usage access and battery optimisation, each with its current state. Tap one to change it.
- **Apps to watch:** a switch per installed app, and the full list of supported apps.
- **About:** the version and source code, the CashPilot server project, the sponsor page, and demo mode.

The app follows the phone's dark or light setting, with the CashPilot web dashboard's Classic night and Light colours.
