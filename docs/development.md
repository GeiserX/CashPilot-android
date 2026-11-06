# Development

## Building

```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Tests

```bash
./gradlew test                    # unit tests (JVM, Robolectric; no emulator)
./gradlew verifyRoborazziDebug    # icon goldens: fails on any visual change to an icon
./gradlew lintDebug               # lint against app/lint-baseline.xml; new errors fail
```

The icon goldens live in `app/src/test/screenshots/`. Re-record them with `./gradlew recordRoborazziDebug` only when an icon change is intended.

CI (`.github/workflows/ci.yml`) runs on pull requests and pushes to `main` that touch the app or the Gradle files: a check for credentials written into tracked files (`scripts/check-no-committed-credentials.sh`), the tests with coverage (`./gradlew jacocoTestReport`, sent to Codecov) and the icon goldens.

## Releases

The version lives in `gradle.properties` (`VERSION_NAME`, `VERSION_CODE`). Pushing a `v*` tag runs `.github/workflows/release.yml`, which builds the signed `app-release.apk` and `app-release.aab` and attaches both to a GitHub release. The release workflow derives the version code from the tag (`1.2.3` becomes `10203`).

## Documentation

The site is built with [Zensical](https://zensical.org/) from `docs/` and `zensical.toml`. To build it:

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install -r docs/requirements-docs.txt
python -m zensical build --clean --strict
python -I .github/scripts/site-check.py site
```

The Docs check runs the same two steps on every pull request. The strict build fails on a broken link or anchor; `site-check.py` fails on what the build lets through: a page left out of the nav, a nav entry for a missing file, a missing image, a link rooted at `/`. `python -m zensical serve` previews the site locally. `docs/research/` holds working notes, read on GitHub and never published.

## Stack

- Kotlin and Jetpack Compose
- Ktor HTTP client
- Kotlinx Serialization
- DataStore Preferences
- WorkManager (planned: periodic sync while the app is in the background)
- Material 3 with dynamic colour
