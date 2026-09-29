# UI renewal: research

Research first, as `CashPilot-android-kv8` asks. Nothing here moves a
Composable; it is the note the proposal has to survive.

Everything about the current app was measured on 2026-08-07 against `main`, and
every external claim carries a source. Where I could check a thing in the code
rather than reason about it, I checked it.

---

## Start here: two of the brief's weaknesses are already fixed

The bead lists five specific weaknesses. **Two of them describe an app that no
longer exists**, and a redesign that "fixes" them would be rebuilding work that
is already done and already better than the brief assumes.

### "The dashboard treats all apps equally… sort by needs attention" — done

`AppPresentation.kt:99` already sorts by attention, then name:

```kotlin
apps.sortedWith(compareBy({ attentionRank(it.state) }, { it.app.displayName.lowercase() }))
```

and the ranking is more considered than "problems first" (`AppPresentation.kt:73`):

```kotlin
AppState.STOPPED -> 0
// Second, not first. An unknown app MIGHT be fine, while a stopped one
// is definitely not -- so a real problem still outranks a blind spot.
AppState.UNKNOWN -> 1
AppState.RUNNING -> 2
AppState.DISABLED -> 3
AppState.NOT_INSTALLED -> 4
```

There is even a guard test against two states sharing a rank, because that
would make the order depend on the input sequence. **Leave this alone.**

### "Permissions are a dismissible banner competing with content" — done

`DashboardScreen.kt:143` already splits the two cases:

```kotlin
if (isBlind) PermissionBlocker() else PermissionBanner(viewModel)
```

The blocker is documented as *"Deliberately NOT dismissible, unlike
PermissionBanner"*, and while blind the app grid is suppressed entirely rather
than showing eleven cards that all read "Can't tell".

What remains is narrower and arguably correct as-is: a *partial* permission
grant still shows a dismissible banner. That is a different, much smaller
question than the brief implies.

---

## The three that do hold

### 1. Pairing by hand is the worst moment in the product

Confirmed: **no QR, barcode or scanning code exists anywhere** in the app. A new
user types a URL and a 32-character key on a phone keyboard.

This is the highest-value single change in the whole renewal, and it is
independent of any visual redesign.

### 2. There is no empty state

`DashboardScreen.kt:161` renders the grid as:

```kotlin
if (!isBlind) {
    items(apps, key = { it.app.slug }) { info -> AppCard(...) }
}
```

There is no `apps.isEmpty()` branch. A user who has granted both permissions but
has **none of the eleven monitored apps installed** sees a summary header and
then nothing — which is exactly what someone who installed CashPilot before
installing any earning app sees.

Note the app *does* have a careful empty state for earnings
(`earnings_none_yet` / `_detail`, "Nothing read yet… not the same as having
earned nothing"). The pattern to follow already exists; it simply was never
applied to the app grid.

### 3. No screen-level visual coverage

- 16 golden PNGs exist, but they are **icon** goldens from #51.
- **Zero `@Preview` composables** in the entire app.

`CLAUDE.md` is explicit that this was deliberate: screen goldens are dominated
by text, text rendering depends on the fonts of the machine that rendered it, so
they fail for unrelated reasons and then get muted. It also says screen-level
goldens *"belong with the UI-renewal work, where the fonts can be pinned
deliberately."*

So this is not an oversight to fix casually — it is a prerequisite this work is
expected to solve, and pinning the font is the hard part.

---

## Material 3 Expressive: not yet

The app is on Compose BOM `2026.06.01` with `material3` **1.4.0 stable**.

Material 3 Expressive is **still alpha**: `material3:1.5.0-alpha24` shipped in
July 2026, individual features are being promoted to stable incrementally, and
the full stable target is 1.5.0
([release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3),
[M3 Compose blog](https://m3.material.io/blog/material-3-compose-stable)).

**Recommendation: do not adopt Expressive as part of this renewal.** Moving a
shipping app onto an alpha UI library buys a look and costs a dependency that
can break on any release. The expressive *ideas* — clearer type hierarchy,
motion that signals state change — are achievable inside stable M3 today. Revisit
when 1.5.0 is stable.

---

## The question the brief is right to raise: should this be a widget?

The bead puts it well — the app's whole value is *"is it still running and is it
earning"*. That is a glance, not a session. Opening an app to learn that nothing
has changed is the interaction failing.

**Jetpack Glance is the right tool and it has real limits**
([overview](https://developer.android.com/develop/ui/compose/glance)):

- it is **not yet stable 1.0** and remains in active development;
- it is **not interoperable** with regular Compose — widget UI is a separate,
  smaller component set, so nothing from the app screens is reusable;
- interaction is limited to `PendingIntent`;
- OS-level periodic widget updates are coarse (on the order of tens of minutes).

That last constraint matters less here than it first appears, and it is the one
thing worth knowing before costing this: **CashPilot already runs a foreground
service** that detects app state and heartbeats. A widget does not have to wait
for the OS refresh cycle — the existing service can push an update when state
actually changes, which is exactly when a monitoring widget should change.

So the widget is cheaper than the generic advice suggests, but it is a **second
UI in a second framework**, not a reskin of the first.

### This contradicts a standing rule, deliberately

`CLAUDE.md` lists **"Widget — defer until core monitoring is solid"** under
*What NOT to Build Yet*. This note recommends the widget as the largest win, so
one of the two has to give and it should be a decision rather than a drift.

The case that the condition is now met: detection runs on three complementary
APIs, the null-vs-zero rule is enforced end to end, the state machine is
covered by tests, and the icon goldens exist. "Solid" was never defined, but
the app is no longer the moving target that rule was written against.

The case for keeping the rule: a widget is a second UI in a second framework
on a library that is not yet 1.0, and everything in steps 1-3 below is cheaper
and useful regardless.

**I have not changed the rule.** If the widget is agreed, `CLAUDE.md` should be
updated in the same change, so the repository does not carry an instruction
its own roadmap contradicts.

### Wear OS

One paragraph, as asked. A tile would show the same two facts on a wrist. It
means a third UI surface, a separate module, and its own release path, for an
audience that is a subset of an already small user base. **Not worth building**
until the phone widget exists and someone asks for it.

---

## What I would propose, in order

Ordered by value per unit of risk, not by how much of the UI it touches.

1. **QR pairing.** Removes the single worst moment in the product. Independent
   of everything else, and it does not touch a single existing screen's layout.
2. **An empty state for the app grid.** Small, and it is the first thing a brand
   new user sees. The `earnings_none_yet` pair is the template.
3. **Pin fonts, then add screen goldens.** This is the prerequisite for any
   visual change being provable rather than hoped for — and `CLAUDE.md` already
   says this work owns that problem.
4. **The Glance widget.** The biggest genuine win, because it removes the need
   to open the app at all. Cost it as a new surface, not a refactor.
5. **Only then, visual renewal of the dashboard** — on stable M3, with goldens
   already in place to prove what changed.

Steps 1–3 are small, provable, and useful whatever is decided about the look.
Step 4 is the one worth real time. Step 5 is the one the brief was about and,
on this evidence, the least urgent of the five.

---

## What is not researched here

Comparable apps — Home Assistant, Tailscale, Uptime Kuma clients,
Syncthing-Fork — are named in the brief and are **not** covered above. Judging
how they present dense status honestly needs the apps in front of you, and
anything I wrote from screenshots would be assertion dressed as research. It is
worth a separate pass with a device.
