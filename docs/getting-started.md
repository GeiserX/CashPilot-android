# Getting started

CashPilot Android installs from an APK on the project's GitHub releases. It works on its own, and it can pair with a CashPilot server so the phone shows up in your fleet.

## Before you start

- A phone or tablet on **Android 8.0 (API 26) or later**. No root.
- Optional: a [CashPilot](https://geiserx.github.io/CashPilot/) server, version 1.0.0 or later, that the phone can reach, and its fleet key. Without a server the app still shows which apps are running on the phone; the server adds the fleet view and the earnings figures.

The app is not in Google Play or F-Droid. The APK on GitHub is the only build, signed with the project's release key.

## Install

1. Open the [latest release](https://github.com/GeiserX/CashPilot-android/releases/latest) on the phone and download `app-release.apk`. The `.aab` file next to it is for app stores; ignore it.
2. Open the downloaded file. Android asks you to allow installs from the browser or file manager you used ("Install unknown apps"); allow it for that app, then install.

To update, install the newer APK over the old one. Settings are kept.

## First run

The first launch walks through three short steps.

1. **Know when an earning app stops.** What the app does, in four lines. **Get started**.
2. **Let CashPilot see your apps.** **Allow** on **Notification access** and **Usage access** first shows what CashPilot reads and what it never reads, then **Continue to Android settings** opens the right list; switch CashPilot on and come back. **Battery optimisation** is optional: **Open settings** opens Android's list of battery-optimised apps; choose All apps, then CashPilot, then Don't optimize. Without it, reports can arrive late while the screen is off. **Continue without them** is there too; the home screen then asks for the two accesses.
3. **Connect your server** (optional). Enter the **Server address**, for example `https://cashpilot.example.com`, and the **API key**: the server's `CASHPILOT_API_KEY`, which the owner can copy from the fleet page with **Reveal API Key** ([how the server's keys work](https://geiserx.github.io/CashPilot/fleet/#authentication)). **Test connection** sends one report and shows **Connected**, **The server refused this API key**, **No answer from …** or **This address needs https://**. After **Connected**, **Finish**. **Use without a server** finishes without one; you can pair later from Settings.

## What a working start looks like

- The home screen leads with one line, for example **All 3 apps running**, and lists the installed supported apps. **N other supported apps** leads to the rest.
- With a server, the line under it reads **Last report 6 min ago · sent every 15 min**, and the phone appears on the server's fleet page as its maker and model plus a short device id, for example `Google Pixel 8 (a1b2c3d4)`. The first report also enrolls the phone: the server issues it its own key ([Configuration](configuration.md#the-fleet-key-and-enrollment)).
- Without a server, the line reads **No server · everything stays on this phone**. That is a supported mode, not an error.

If a step does not look like this, see [Troubleshooting](troubleshooting.md).
