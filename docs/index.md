---
hide:
  - navigation
---

# CashPilot Android { .cpa-visually-hidden }

<p align="center">
  <img src="images/banner.svg" alt="CashPilot Android: passive income in your pocket" width="100%">
</p>

<p align="center">
  <a href="https://github.com/GeiserX/CashPilot-android/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/GeiserX/CashPilot-android/total?style=flat-square&logo=android"></a>
  <a href="https://github.com/GeiserX/CashPilot-android/stargazers"><img alt="GitHub Stars" src="https://img.shields.io/github/stars/GeiserX/CashPilot-android?style=flat-square&logo=github"></a>
  <a href="https://github.com/GeiserX/CashPilot-android/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/GeiserX/CashPilot-android?style=flat-square"></a>
  <a href="https://github.com/GeiserX/CashPilot-android/blob/main/LICENSE"><img alt="License: GPL-3.0-or-later" src="https://img.shields.io/github/license/GeiserX/CashPilot-android?style=flat-square"></a>
</p>

---

**CashPilot Android** tells you which of your passive income apps are actually running on your phone, without root. It watches 11 apps (EarnApp, IPRoyal Pawns, MystNodes, Grass and more) through three Android APIs. A bandwidth app that has stopped earns nothing and usually shows nothing on the phone; here it goes to the top of the list in red. Paired with a [CashPilot](https://geiserx.github.io/CashPilot/) server, the phone joins your fleet next to the Docker workers and shows what its platforms earned. Start with [Getting started](getting-started.md), then [Usage](usage.md).

<div class="grid cards" markdown>

-   :material-android: **[Getting started](getting-started.md)**

    ---

    Download the APK from GitHub releases and install it on Android 8.0 or later.

-   :material-play-circle-outline: **[First run](getting-started.md#first-run)**

    ---

    Grant two accesses and the battery exemption, and pair a server if you have one.

-   :material-view-dashboard-outline: **[Usage](usage.md)**

    ---

    The dashboard, what Running, Stopped and Can't tell mean, and the earnings card.

-   :material-format-list-bulleted: **[Configuration](configuration.md)**

    ---

    Every setting, the fleet key and enrollment, heartbeat timing, the app list.

</div>

## What it does

- Shows every monitored app as **Running**, **Stopped**, **Can't tell**, **Disabled** or **Not installed**, the ones that need attention first.
- Sees a running app the moment its ongoing notification appears, and confirms quiet ones from their recent foreground use and network traffic.
- Says **Can't tell** when a missing permission hides an app, instead of calling it stopped, so you fix the permission rather than restart an app that was fine.
- Sends a heartbeat to your CashPilot server every 30 seconds, so the fleet page lists the phone and its apps next to your servers.
- Shows the earnings the server recorded for the platforms on the phone, marks figures older than an hour, and never shows a missing figure as zero.
- Works without a server as a monitor for the phone alone.

[How it works](how-it-works.md) has the detection rule and what a heartbeat carries.

## How it runs

- One foreground service with a quiet notification, restarted when the phone boots. No root, no ADB, no accessibility service.
- Two special accesses, Notification Access and Usage Access, plus an exemption from battery optimization so Android does not stop the service with the screen off.
- One `POST /api/workers/heartbeat` to your server per interval, authenticated with this phone's own key after the first one. The settings are on [Configuration](configuration.md).
- Installed from the APK on [GitHub releases](https://github.com/GeiserX/CashPilot-android/releases/latest); it is not in Google Play or F-Droid.

## What it does not do

- It does not start, stop or control the apps. It watches them; restarting a stopped one is up to you.
- It does not read notification content, only whether a monitored app has an ongoing notification.
- It does not collect earnings itself. The figures come from the CashPilot server, and without one the earnings card stays empty.
- It watches the 11 apps it knows. A new app needs a new release.

## Privacy

- App states go only to the CashPilot server you enter. The one other request is to `api.ipify.org`, to show the phone's public IP, and only once a server is set.
- The heartbeat carries the phone's maker and model, a short Android ID, the Android version and the monitored apps' states and traffic; nothing about other apps. [How it works](how-it-works.md#what-a-heartbeat-carries) lists every field.
- The app accepts an `http://` server URL, and the key then travels unencrypted. Use `https://` for any server outside your own network; the app warns you when you do not.

## Disclosure

Tapping an app that is not installed opens its signup page. For most apps that is a referral link: if you sign up through it, the maintainer may earn a commission, at no cost to you. To avoid it, install the app from its own website or the Play Store instead.

## Getting help

- Something wrong: read [Troubleshooting](troubleshooting.md), then open an [issue](https://github.com/GeiserX/CashPilot-android/issues) with what its "Reporting a bug" section lists.
- A security problem: follow the [security policy](https://github.com/GeiserX/CashPilot-android/blob/main/SECURITY.md), never a public issue.
- Building it or sending a fix: [Development](development.md). The server, the desktop app and the integrations: [Related projects](related.md).

## License

CashPilot Android is released under the [GPL-3.0-or-later](https://github.com/GeiserX/CashPilot-android/blob/main/LICENSE) license.
