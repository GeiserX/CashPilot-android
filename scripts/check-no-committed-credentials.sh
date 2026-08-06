#!/usr/bin/env bash
# Fail if a tracked file states a credential literally.
#
# WHY THIS EXISTS, when GitGuardian is already installed
# ------------------------------------------------------
# The release keystore password sat in a tracked CLAUDE.md, in a PUBLIC
# repository, from 2026-05-25 until 2026-08-06. GitGuardian was running on this
# repo for that entire period and reported a passing check on every PR.
#
# It is not a GitGuardian failure. Secret scanners are built for high-entropy
# tokens and recognisable formats -- an AWS key, a JWT, a PEM block. What was
# committed here was a short memorable phrase in a sentence, after the word
# "password:". To an entropy-based detector that is a sentence.
#
# So this check does the opposite job, crudely and on purpose: it looks for the
# SHAPE A HUMAN WRITES when documenting a credential, and does not care whether
# the value looks random. Between them the two cover both halves.
#
# It reads TRACKED files only -- an untracked scratch file is not a disclosure.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

# "password: value" where value is a LITERAL, not a variable.
#
# The distinction is what stops this drowning in false positives: `apiKey =
# localKey` is code passing a value around, while `password: hunter2-abc` is
# somebody writing one down. So the value must either be QUOTED/BACKTICKED --
# how a literal is written in prose and in code -- or contain a character that
# a bare identifier cannot (a hyphen, an @, a dot), which is what most real
# passwords look like and what `localKey` is not.
PATTERN_QUOTED='(password|passwd|passphrase|secret|api[_-]?key|auth[_-]?token)[[:space:]]*(is|:|=)[[:space:]]*[`"'"'"'][A-Za-z0-9_@.+-]{8,}[`"'"'"']'
PATTERN_BARE='(password|passwd|passphrase|secret|api[_-]?key|auth[_-]?token)[[:space:]]*(is|:|=)[[:space:]]*[A-Za-z0-9_]*[-@.][A-Za-z0-9_@.+-]{6,}'

# Deliberately generous: the cost of a false positive is one line of allowlist,
# the cost of a false negative is a public credential.
ALLOW='example|placeholder|your-|<[a-z]|\$\{|\$\(|env|environment|variable|getenv|System\.|findProperty|stringPreferencesKey|BuildConfig|NOT recorded|_KEY|_PASSWORD|_TOKEN|_SECRET|storePassword|keyPassword|keystorePass|keyPass|secret: (true|false)|the (password|secret|token|key)|a (password|secret|token|key)|app signing key|upload key|Play App Signing'

# Test sources are excluded. Fixtures are SUPPOSED to contain credential-shaped
# strings ("test-api-key", "step2-key"), and scanning them produces permanent
# noise that gets the whole check disabled.
#
# The tradeoff, stated rather than hidden: a real secret pasted into a test file
# would not be caught here. That is a narrower risk than documentation -- a test
# fixture is written to be fake, a doc line is written to be true -- but it is a
# real gap, and GitGuardian remains the backstop for the high-entropy case.
hits=$(git ls-files -z \
  | grep -zvE '(^|/)src/(test|androidTest)/' \
  | xargs -0 grep -nEiI -e "$PATTERN_QUOTED" -e "$PATTERN_BARE" 2>/dev/null \
  | grep -viE "$ALLOW" || true)

if [ -n "$hits" ]; then
  echo "A tracked file appears to state a credential literally:" >&2
  echo "$hits" >&2
  echo >&2
  echo "This repository is PUBLIC. If it is a real credential: remove it, rotate it," >&2
  echo "and remember that redacting HEAD does not clear the history." >&2
  echo "If it is a false positive, add a term to ALLOW in $0." >&2
  exit 1
fi
echo "no literal credentials in tracked files"
