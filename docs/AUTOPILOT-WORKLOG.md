# Autopilot Worklog — per-worker keys for Android + Desktop-fleet compat

Append-only. Newest at the bottom. Every "done" needs evidence (test/build/commit).

---

### 2026-07-11 — kickoff
- Goal (docs/GOAL.md): per-worker fleet keys for **CashPilot-android** (client) + **CashPilot-Desktop**
  fleet_server (server), interoperable with the web UI v1.0.0 protocol.
- Branches: android `feat/per-worker-keys`; desktop `feat/fleet-per-worker-keys` (off origin/main).
- Beads filed:
  - android: CashPilot-android-btz (client enrollment, P1), -a5d (response model), -nvn (tests), -20v (changelog)
  - desktop: CashPilot-Desktop-jet (fleet server enrollment, P1), -xkf (store+migration, P1), -10u (tests), -rxx (docs)
- Protocol contract (from web UI v1.0.0): heartbeat with own key once enrolled, else shared bootstrap key;
  server returns `worker_key` once on enroll; reissue until confirmed; reject shared key for confirmed device.
- Next: implement android client (btz/a5d) then desktop server (jet/xkf), verify each.

### 2026-07-11 — both sides implemented + PRs open
- Android (client): PR #37 — Settings.workerKey/activeKey, SettingsStore persist, WorkerHeartbeatResponse,
  HeartbeatService capture via pure keyToPersist; v0.2.0; PerWorkerKeyTest. Beads btz/a5d/nvn/20v closed.
  CI compile break (Settings destructuring order) fixed by moving workerKey last (commit bacf9f1).
- Desktop (server): PR #91 — store api_key_hash+key_confirmed + guarded migration; classifyFleetAuth
  drives handleWorkerHeartbeat (enroll/reissue/confirm/reject); tests. Beads jet/xkf/10u/rxx closed.
  Verified locally: go build/vet clean, go test -race ./... green.
- Next: confirm both CIs green, address CodeRabbit, then architect review + cancel.
