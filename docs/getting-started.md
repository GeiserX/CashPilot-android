# Getting started

CashPilot Android installs from an APK on the project's GitHub releases. It works on its own, and it can pair with a CashPilot server so the phone shows up in your fleet.

## Before you start

- A phone or tablet on **Android 8.0 (API 26) or later**. No root.
- Optional: a [CashPilot](https://geiserx.github.io/CashPilot/) server, version 1.0.0 or later, that the phone can reach, and its fleet key. Without a server the app still shows which apps are running on the phone; the server adds the fleet view and the earnings figures.

The app is not in Google Play or F-Droid. The APK on GitHub is the only build, signed with the project's release key.

## Install

1. Open the [latest release](https://github.com/GeiserX/CashPilot-android/releases/latest) on the phone and download `app-release.apk`. The `.aab` file next to it is for app stores; ignore it.
2. Open the downloaded file. Android asks you to allow installs from the browser or file manager you used ("Install unknown apps"); allow it for that app, then install.
3. On Android 13 and later, the first launch asks to show notifications. Allow it: the app keeps one quiet notification while it monitors, and without the permission you cannot see it.

To update, install the newer APK over the old one. Settings are kept.

## First run

The first launch opens **Welcome to CashPilot**, four steps on one screen.

1. **Server Connection** (optional). Enter the **CashPilot Server URL**, for example `https://cashpilot.example.com`, and the **Fleet API Key**: the server's `CASHPILOT_API_KEY`, which the owner can copy from the fleet page with **Reveal API Key** ([how the server's keys work](https://geiserx.github.io/CashPilot/fleet/#authentication)). Leave both empty to run without a server; you can pair later from Settings.
2. **Notification Access**. **Open Settings** takes you to Android's list; switch CashPilot on. The app only checks whether a monitored app has an ongoing notification; it does not read notification content.
3. **Usage Access**. **Open Settings**, then switch CashPilot on. This gives the app each monitored app's last foreground time and its network traffic.
4. **Battery Optimization**. **Open Settings** and allow CashPilot to run unrestricted, or Android may stop the background service when the screen is off.

The main button unlocks once both accesses are granted. It reads **Continue to Dashboard** when a server URL and key are filled in, and **Continue without a server** when they are not. **Skip for now** leaves the setup screen without the permissions; the dashboard then asks for them.

## What a working start looks like

- The dashboard lists the 11 monitored apps. Installed ones show **Running**, **Stopped** or **Can't tell**; the others show **Not installed — tap to install**. The summary at the top counts them.
- A notification titled **CashPilot** stays in the shade. Without a server it reads **Monitoring apps...**; with one it reads, for example, **3/4 apps running** after each heartbeat.
- With a server, the dashboard's summary shows a green dot and **Last heartbeat** with a time, and the phone appears on the server's fleet page as its maker and model plus a short device id, for example `Google Pixel 8 (a1b2c3d4)`. The first heartbeat also enrolls the phone: the server issues it its own key ([Configuration](configuration.md#the-fleet-key-and-enrollment)).
- Without a server, the summary shows **Not paired — running standalone**. That is a supported mode, not an error.

If a step does not look like this, see [Troubleshooting](troubleshooting.md).
