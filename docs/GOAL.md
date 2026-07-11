# GOAL

Started: 2026-07-11

## Directive (verbatim)

that app. Also it should be compatible with cashpilot-desktop, so file beads there and start implementing extensively and thoroughly with ralph

## Established context (from the preceding conversation)

"That app" = **CashPilot-android** (the mobile heartbeat client). The CashPilot **web** UI already
shipped **per-worker fleet keys (v1.0.0)**: on a worker's first heartbeat the server issues it a unique
key (returned once as `worker_key`), which the client persists and uses thereafter; the shared
`CASHPILOT_API_KEY` becomes enrollment-only, and is rejected for a confirmed worker. There is a
re-delivery ("reissue") safety net so a dropped enrollment response can't lock a client out.

**Goal:** bring **CashPilot-android** onto the same per-worker-key protocol as a client (mirror the
Docker worker's client side), AND make **CashPilot-Desktop** compatible — its `fleet_server.go` must
implement the server side of the same enrollment protocol (enroll → issue key → confirm → reject shared
key), so the desktop's fleet server and the android client interoperate. File beads in both repos.

Client side (android, Kotlin/Ktor, DataStore):
- On heartbeat, use the persisted per-worker key if present, else the shared key (bootstrap).
- Capture `worker_key` from the heartbeat response, persist it (DataStore), use it thereafter.

Server side (desktop, Go, fleet_server.go):
- Enrollment + per-worker key issuance/storage + confirm + reject-shared-once-confirmed + reissue,
  matching the web UI's protocol so any client (android, docker worker) works against it.

## Working rules
- Per-repo conventions: android = Gradle/Kotlin, `./gradlew test` + ktlint/detekt if present; desktop =
  Go, `go build/vet/test -race`. Keep CI green. Conventional commits. PR workflow; never merge without
  Sergio's approval. No AI attribution. Beads via `bd`, committed `.beads`, commit via PR.
