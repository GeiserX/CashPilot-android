#!/usr/bin/env bash
# Run a Gradle task for this module on the build host, not on this Mac.
#
# WHY THIS EXISTS
# ---------------
# There is no working JVM on either Mac here. The dev machine has no Java at
# all, and Temurin 17 on the Mac mini crashes at startup:
#
#   SIGBUS (0xa) ... CodeHeap::allocate
#
# on `java -version`, in interpreted mode, and with a reduced code cache — so it
# is the JVM itself, not Gradle or the Android SDK. That left CI as the only way
# to compile, which is a slow loop for anything larger than a one-line change.
#
# A Linux container on the build host works fine, so this rsyncs the module
# there and runs Gradle inside `eclipse-temurin:17-jdk` — the same major version
# CI uses (see .github/workflows/ci.yml). The SDK and the Gradle cache are
# persistent volumes, so only the first run pays the download.
#
# It is a FASTER PRE-CHECK, NOT A REPLACEMENT FOR CI. CI remains the authority:
# it builds the signed release variant and runs lint with a baseline, neither of
# which this does. Never report a green run here as "CI passed".
#
#   scripts/remote-gradle.sh test
#   scripts/remote-gradle.sh assembleDebug
#   scripts/remote-gradle.sh 'test lint'
set -euo pipefail

HOST="${CASHPILOT_BUILD_HOST:-root@watchtower.mango-alpha.ts.net}"
REMOTE_ROOT="${CASHPILOT_BUILD_ROOT:-/mnt/user/appdata/androidbuild}"
TASK="${*:-test}"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# --delete keeps the remote tree honest: a file deleted locally must not linger
# there and keep compiling. .beads is excluded because these repositories are
# public and one bead holds real wallet addresses; it has no business leaving
# this machine even to a private host.
echo "==> syncing $HERE -> $HOST:$REMOTE_ROOT/src"
rsync -a --delete \
  --exclude '.git' --exclude '.beads' --exclude 'build/' --exclude '.gradle' \
  "$HERE/" "$HOST:$REMOTE_ROOT/src/"

echo "==> gradle $TASK"
ssh "$HOST" "docker run --rm \
  -v $REMOTE_ROOT/sdk:/sdk \
  -v $REMOTE_ROOT/src:/src \
  -v $REMOTE_ROOT/gradle:/root/.gradle \
  -w /src -e ANDROID_HOME=/sdk -e ANDROID_SDK_ROOT=/sdk \
  eclipse-temurin:17-jdk bash -lc './gradlew --no-daemon $TASK'"

# Bring back the SOURCE-TREE artefacts the build writes, which are the only
# outputs that belong under version control: the Roborazzi goldens
# (`recordRoborazziDebug` writes them) and the diff images a failed
# `verifyRoborazziDebug` leaves behind. Without this the record task appears to
# do nothing -- the images exist, on a machine you are not looking at.
#
# Everything under build/ is deliberately NOT synced back: it is large,
# regenerable, and not the point.
for dir in app/src/test/screenshots app/build/outputs/roborazzi; do
  if ssh "$HOST" "test -d '$REMOTE_ROOT/src/$dir'"; then
    echo "==> fetching $dir"
    mkdir -p "$HERE/$dir"
    rsync -a "$HOST:$REMOTE_ROOT/src/$dir/" "$HERE/$dir/"
  fi
done
