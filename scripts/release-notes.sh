#!/usr/bin/env bash
# The GitHub Release notes for a tag: its CHANGELOG.md entry.
#
#   scripts/release-notes.sh v1.0.3
#
# release.yml runs it on every v* tag, before the build, so a tag without an
# entry fails before anything is published. It also serves for an older tag by
# hand. Relative links become links into the tagged tree, so they work on the
# Releases page.
#
# A major release (X.0.0) needs its section in UPGRADING.md,
# "## Upgrading to X.0", and the notes open with a link to it: a major without
# that section fails here.
set -euo pipefail

tag="${1:?usage: scripts/release-notes.sh vX.Y.Z}"
version="${tag#v}"
repo="${GITHUB_REPOSITORY:-GeiserX/CashPilot-android}"
cd "$(dirname "$0")/.."

# Entries start "## [1.0.3] - 2026-11-27"; the entry ends at the next "## ".
notes=$(awk -v h="## [$version]" 'index($0, h) == 1 { f = 1; next } f && /^## / { exit } f' CHANGELOG.md | sed '/./,$!d')
if [ -z "$(printf '%s' "$notes" | tr -d '[:space:]')" ]; then
  echo "release-notes: CHANGELOG.md has no '## [$version]' entry" >&2
  exit 1
fi

case "$version" in
  *.0.0)
    major="${version%%.*}"
    if ! grep -Fqx "## Upgrading to $major.0" UPGRADING.md 2>/dev/null; then
      echo "release-notes: $version is a major release and UPGRADING.md has no '## Upgrading to $major.0' section" >&2
      exit 1
    fi
    notes="Upgrading? Read [Upgrading to $major.0](UPGRADING.md#upgrading-to-${major}0): what changed and every step.

$notes"
    ;;
esac

printf '%s\n' "$notes" | REF="$tag" REPO="$repo" perl -pe \
  's{\]\((?!https?://|#|mailto:)([^)\s]+)\)}{](https://github.com/$ENV{REPO}/blob/$ENV{REF}/$1)}g'
