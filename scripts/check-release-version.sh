#!/usr/bin/env bash
# Fail unless a release tag matches gradle.properties.
#
#   scripts/check-release-version.sh v0.4.0 [path/to/gradle.properties]
#
# gradle.properties is the one place the version lives. F-Droid builds from it
# without our -P flags, so if a tag says one version and the file another, F-Droid's
# APK can never match the one we publish and the update is rejected. versionCode is
# major*10000 + minor*100 + patch, the same formula release.yml uses.
set -euo pipefail

tag="${1:-}"
props="${2:-$(dirname "$0")/../gradle.properties}"

if [[ ! "$tag" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  echo "usage: $0 vMAJOR.MINOR.PATCH [gradle.properties]" >&2
  exit 2
fi
name="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"
# Leading zeros are refused above: release.yml's shell arithmetic would read
# them as octal and publish a different versionCode than F-Droid builds.
code=$(( 10#${BASH_REMATCH[1]} * 10000 + 10#${BASH_REMATCH[2]} * 100 + 10#${BASH_REMATCH[3]} ))

# Gradle reads Java properties, which allow blanks, ':' and backslash line
# continuations; F-Droid's UpdateCheckData reads the plain KEY=value form only.
# So the file must declare each key exactly once, as KEY=value at the start of a
# line, with no continuation anywhere: then Gradle, F-Droid and this check all
# read the same value. Anything else is refused rather than interpreted.
clean="$(tr -d '\r' < "$props")"
if grep -q '\\$' <<<"$clean"; then
  echo "::error::gradle.properties has a line continuation (a line ending in \\); keep every property on one line" >&2
  exit 1
fi
prop() {
  local n
  n="$(grep -c -E "^[[:space:]]*$1[[:space:]]*[=:]" <<<"$clean" || true)"
  if [ "$n" != 1 ] || ! grep -q -E "^$1=" <<<"$clean"; then
    echo "::error::gradle.properties must declare $1 exactly once, as $1=value at the start of a line (found $n declarations)" >&2
    return 1
  fi
  sed -n "s/^$1=//p" <<<"$clean"
}
file_name="$(prop VERSION_NAME)" || exit 1
file_code="$(prop VERSION_CODE)" || exit 1

status=0
if [ "$file_name" != "$name" ]; then
  echo "::error::tag $tag wants VERSION_NAME=$name, gradle.properties has VERSION_NAME=${file_name:-<missing>}" >&2
  status=1
fi
if [ "$file_code" != "$code" ]; then
  echo "::error::tag $tag wants VERSION_CODE=$code, gradle.properties has VERSION_CODE=${file_code:-<missing>}" >&2
  status=1
fi
if [ "$status" -ne 0 ]; then
  echo "Bump gradle.properties in a PR, merge it, then tag that commit." >&2
  exit 1
fi
echo "gradle.properties matches $tag (VERSION_NAME=$name, VERSION_CODE=$code)"
