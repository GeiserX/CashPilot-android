# Publishing CashPilot for Android

A runbook for getting this app into F-Droid and Google Play. Written against
the repository as it actually is, so each section says whether the thing is
**done**, **missing**, or **a decision**.

The listed name is simply **CashPilot**.

---

## Read this first: the app is not useless without a server

It is easy to assume a store user installs this, has no CashPilot server, and
gets nothing. That is not what happens, and it changes what the listing should
promise.

- The **app grid** — which monitored apps are installed, which are running,
  when each was last active — is gated on **permissions**, not on a server.
  `AppDetector` uses three Android APIs and makes no network call.
- The **server** gates exactly one thing: the earnings card. Without one it
  renders *"Pair a CashPilot server to see earnings"*.

So a store user who grants the two access permissions gets a working
app-state monitor. Whether that is a listing worth shipping — and whether it
invites one-star reviews from people who came for earnings — is a **product
decision**, not a missing feature. Decide it deliberately before submitting.

---

## Already done

| Thing | State |
|---|---|
| Release signing | **Done.** `app/build.gradle.kts` reads `CASHPILOT_KEYSTORE_PATH`, `_PASSWORD`, `CASHPILOT_KEY_ALIAS`, `_KEY_PASSWORD` from Gradle properties or the environment, and only applies the config when all four are present. The keystore is never in the repository. |
| Release artifacts | **Done.** `release.yml` builds **both** `app-release.aab` (what Play wants) and `app-release.apk` (what F-Droid and direct installs want). |
| Special-use foreground service | **Done, and this one usually bites.** `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` is declared with a justification string. Google requires it and rejects builds that declare `FOREGROUND_SERVICE_SPECIAL_USE` without one. |
| Store text | **Done.** `fastlane/metadata/android/en-US/` has `title.txt`, `short_description.txt`, `full_description.txt` and `changelogs/`. |
| Licence | **Done.** GPL-3.0, no proprietary dependencies — which is what makes F-Droid possible at all. |

## Missing, and required by both stores

**Screenshots and a feature graphic.** `fastlane/metadata/` contains no
`images/` directory and no PNGs at all.

Neither store will list without them, so this is the first real piece of work:

```
fastlane/metadata/android/en-US/images/
    icon.png                  512x512
    featureGraphic.png        1024x500        (Play only)
    phoneScreenshots/1.png    at least 2, 16:9 or 9:16
```

Take them from a device or emulator showing a populated dashboard. A screenshot
of an empty, unpaired app is an honest picture of a bad first run and will cost
installs.

---

## F-Droid: an attempt was already made, and it did not land

**Check this before assuming anything.** `CLAUDE.md` records *"F-Droid: MR
!35850 at gitlab.com/fdroid/fdroiddata"*, which reads like a submission in
flight. It is not:

- MR **!35850 is CLOSED, never merged** — last updated 2026-07-04.
- `metadata/com.cashpilot.android.yml` **does not exist** on `fdroiddata`
  master (404, while a known-good file returns 200).
- `https://f-droid.org/packages/com.cashpilot.android/` returns **404**, and
  their API reports no versions.

So the app is not in F-Droid, and has not been since the attempt a month ago.
Nothing here says *why* the MR closed — read the thread before resubmitting,
because resubmitting into the same objection wastes a reviewer's time and is
exactly the kind of thing that makes a project unwelcome.

It is still the right track to do **before** Play: same metadata layout, no
account fee, no identity verification, no data-safety form. But it is a
*resubmission*, not a first attempt.

1. Read the closed MR's discussion and address whatever it raised.
2. Confirm the build is reproducible from a clean checkout.
3. Decide whether F-Droid builds from source and signs with their key, or
   accepts the upstream signed APK — that choice determines whether a user can
   later move between an F-Droid install and a Play install without
   uninstalling.

---

## Google Play

### Start the slow thing first

Play Console requires a one-off registration fee **and identity verification
for individual developers**. Verification takes days to weeks and everything
else waits behind it. Begin it before writing a single line of listing copy.

### App signing

Two keys, and they are easy to confuse:

- the **upload key** — what you sign the AAB with locally, i.e. the keystore
  the Gradle config above reads;
- the **app signing key** — what Play re-signs with before distribution, held
  by Google under Play App Signing.

Enrol in Play App Signing. It means a lost upload key is recoverable, where a
lost app signing key would end the listing permanently.

> **Keep the keystore out of the repository** and out of any backup that syncs
> to a public place. It is already excluded; do not "helpfully" add it.

### The Data Safety form is the hard part

This app requests access Google scrutinises. Each declaration below is
*genuinely* justified — the access **is** the detection mechanism — but the form
must say so precisely.

| Access | Why the app needs it |
|---|---|
| `PACKAGE_USAGE_STATS` | Special access, granted by the user in system settings. Detects whether a monitored earning app is actually running rather than merely installed. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Special access. Several earning apps report their state only through a persistent notification; reading it is how the app knows they are alive. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | The heartbeat that reports app status to the user's own server. Justification string already declared in the manifest. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Play restricts this and it is a common rejection. Be ready to argue that a monitoring app whose service is dozed reports stale state, and to accept the answer if they disagree. |
| `RECEIVE_BOOT_COMPLETED` | Resume monitoring after a restart without the user reopening the app. |
| `READ_NETWORK_USAGE_HISTORY` | **The app genuinely depends on this** — `NetworkStatsManager` is the third of `AppDetector`'s three detection signals (per-app bytes tx/rx, ~2h buckets), alongside usage stats and notifications. Do not drop it to dodge a question; the per-app byte counts are how a running-but-silent app is distinguished from an idle one. Note the effective grant comes from usage access, so expect the reviewer to treat the two together. |

State plainly that notification and usage data are read **on device**, are used
only to determine app state, and — where a server is paired — that what leaves
the device is app status, not notification content. If that is not exactly
true, fix the app rather than the wording.

### Then the treadmill

Play raises its **minimum target API level** every year, and an app that falls
behind is delisted from new installs. Publishing is a recurring obligation, not
a one-off.

---

## The question worth asking before any of it

A public listing invites support requests from people who have never run a
self-hosted service. Decide whether that is wanted before opening the door,
because closing it again means unpublishing.
