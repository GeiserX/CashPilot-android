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

- The **status board** — which supported apps are installed, which are
  running, when each was last seen — is gated on **permissions**, not on a
  server. `AppDetector` uses three Android APIs and makes no network call.
- The **server** gates exactly one thing: the earnings strip. Without one it
  reads *"Add a server to see figures"*.
- A reviewer with none of the supported apps sees *"No supported app on this
  phone yet"* and can open **Show a demo with sample data**, labelled as
  sample data on every screen.

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
| Version in one place | **Done.** `gradle.properties` holds `VERSION_NAME` and `VERSION_CODE` (major*10000 + minor*100 + patch). `release.yml` runs `scripts/check-release-version.sh` first and refuses a tag that disagrees with the file. F-Droid builds from that file without our `-P` flags, so the two must agree. |
| Special-use foreground service | **Gone.** The heartbeat runs on WorkManager every 15 minutes, so there is no special-use foreground service to justify. WorkManager's merged manifest still lists `FOREGROUND_SERVICE` for its own optional service, which the app never starts. |
| Store text and graphics | **Done.** `fastlane/metadata/android/en-US/` has `title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/<versionCode>.txt`, `images/icon.png` (512x512, from `docs/images/icon.svg`) and `images/featureGraphic.png` (1024x500, from `docs/images/banner.svg`). |
| F-Droid recipe | **Done.** `fdroid/com.cashpilot.android.yml` is the file to place in fdroiddata as `metadata/com.cashpilot.android.yml`. Fill in the tag's full commit sha first. |
| Licence | **Done.** GPL-3.0, no proprietary dependencies — which is what makes F-Droid possible at all. |

## Missing, and required by both stores

**Phone screenshots.** The icon and the feature graphic are in place. The
screenshots are not:

```
fastlane/metadata/android/en-US/images/phoneScreenshots/1.png    at least 2, 9:16
```

Take them on a real phone that has several of the monitored apps installed and
running. An emulator has none of them, and a screenshot of an empty grid is an
honest picture of a bad first run and will cost installs.

---

## F-Droid: resubmit, the first attempt closed for inactivity

MR !35850 at https://gitlab.com/fdroid/fdroiddata/-/merge_requests/35850 was
closed on 2026-07-04 for inactivity. It was not rejected. It had two open items:

1. **The dependency info block.** Fixed: `dependenciesInfo` is off in
   `app/build.gradle.kts` since v0.1.3.
2. **"Fix the pipeline".** The root cause was version drift. `gradle.properties`
   still said `VERSION_NAME=0.2.0` / `VERSION_CODE=200` at tag v0.3.0, while
   `release.yml` built that tag as 0.3.0 / 300 through `-P` flags. F-Droid builds
   from `gradle.properties` and then compares its APK with ours (`Binaries`), so
   the two could never match. Fixed: the file is the only source, and the release
   refuses a tag that disagrees with it.

The app is still not listed (`f-droid.org/packages/com.cashpilot.android/` is
404).

We declare the **NonFreeAdd** anti-feature up front, because the install buttons
promote non-free apps and several open referral links:
https://f-droid.org/docs/Anti-Features/

We keep our own signature (`Binaries` plus `AllowedAPKSigningKeys`). F-Droid
publishes our signed APK once its own build matches it byte for byte. Users can
then move between the F-Droid, IzzyOnDroid and GitHub installs without
uninstalling. If the rebuild ever stops matching, the fallback is to drop those
two keys and let F-Droid sign. That costs the interchangeability.

To resubmit:

1. Tag the release whose `gradle.properties` matches (v0.4.0 is the first).
2. Copy `fdroid/com.cashpilot.android.yml` to `metadata/com.cashpilot.android.yml`
   in a fork of fdroiddata, and put the tag's full commit sha in `commit:`.
3. Run `fdroid lint com.cashpilot.android` and
   `fdroid build -v -l com.cashpilot.android` there, then open the MR and answer
   reviewers promptly. Inactivity is what closed the last one.

**IzzyOnDroid** is the faster path. It ships our signed APK from the GitHub
release, and the dependency info block is already off. Inclusion starts with an
issue on its repository.

---

## Google Play

### What stands in the way, as of 2026-11

- **The proxy rule does not apply.** Play's Device and Network Abuse policy
  limits apps that let third parties use the device as a proxy. This app
  proxies nothing; it only watches other apps:
  https://support.google.com/googleplay/android-developer/answer/9888379
  Reviewers who see the names of bandwidth-sharing apps may still be wary,
  given Google's enforcement against proxy SDKs.
- **Closed testing first.** A personal developer account created after
  2023-11-13 must run a closed test with at least 12 testers for 14 days before
  production. Confirm the current numbers in Play Console.
- **2 of the 10 monitored apps are not on Play** (Traffmonetizer and ByteBenefit
  return 404 for their package ids). Linking to apps outside Play from a Play
  listing invites questions.
- **Android developer verification.** Google started requiring verified
  developer identities for sideloaded apps on 2026-09-30 in Brazil, Indonesia,
  Singapore and Thailand, and plans it globally in 2027. That affects F-Droid,
  IzzyOnDroid and GitHub installs too, not only Play.

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
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | **Not requested.** Play allows it only for chat, VoIP, safety, automation and peripheral apps. The app opens the battery settings list instead, which needs no permission; `BackgroundPolicyTest` fails if the permission or the direct dialog comes back. |
| `RECEIVE_BOOT_COMPLETED` | Added by WorkManager, which uses it to restore the heartbeat schedule after a restart. |
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
