# NovaRun — Zero-Pending Milestone Pipeline

**Scope authority:** [PROJECT_REQUIREMENTS.md](PROJECT_REQUIREMENTS.md)  
**Baseline:** `d964fd0374b8762443a449aeff5a56e16e9ca5f7`  
**Active integration branch:** `fix/localhost-independent-lan` (PR #16; not merged)  
**Delivery:** Android APK through GitHub Actions. No PC-only workflow for the user.

## Pipeline protocol

For every milestone:

1. Freeze the requirement IDs and acceptance criteria before coding.
2. Inspect all touched files and their callers; record line-level findings.
3. Implement the complete vertical slice, not UI-only placeholders.
4. Run static checks, unit tests and compatibility checks.
5. Start the milestone's Build/Run immediately when its code is ready.
6. While that build/test is running, begin the next milestone's independent code.
7. Rebase/merge only through the active PR branch and rerun all affected checks.
8. Fix every discovered regression and rerun the failed and dependent tests.
9. Record exact commit SHA, workflow run, tests, outcomes and known limits.
10. Never call a milestone complete if any acceptance item is untested.

The workflow is sequentially gated by dependencies; independent work may overlap CI, but later milestones cannot be declared complete before earlier integration gates pass.

## Milestones

| Milestone | Scope / requirement IDs | Acceptance evidence | Current state |
|---|---|---|---|
| M0 — Requirements, repository baseline and CI contract | P-01–P-05, all requirement inventory, build workflow, test inventory | Audited repository tree and runtime support matrix; published requirements register and milestone plan; baseline CI SHA captured | **In progress:** repository/runtime audit and requirements register committed; baseline CI passed |
| M1 — Localhost/LAN hosting reliability | H-01–H-10, S-03/S-04 | Android service start/stop, localhost HTTP 200, listener closed after Stop, rapid start/stop, invalid LAN port fallback, TLS/auth regression | **CI verified on emulator** at `d964fd0`; physical-device LAN/Keystore gates remain |
| M2 — ZIP import, workspace and project lifecycle | W-01–W-09, S-01, S-10 | Valid/invalid ZIP corpus, ZIP-slip/duplicate/path conflict tests, hard-cap tests, failed-import preservation, backup/restore round-trip, project CRUD/navigation persistence | **In progress:** backup ZIP export/restore-as-new, safe project name policy, project-root symlink isolation, and centralized file create/list/rename/delete operations implemented. Backup/repository Android tests passed in run 44285031576 (SHA 7135a89); expanded file CRUD/symlink JVM tests and latest foreground-service changes are under CI verification |
| M3 — Editor, save/recovery and preview | E-01–E-08, S-02, S-10 | Per-file-type edit/save/reopen tests, atomic save interruption, unsaved warning navigation, large-file stress, WebView JS toggle, static assets/SPA/404/path traversal, React/Vite prebuilt output fixture | **In progress:** 2 MB UTF-8 and text-only file policy plus read-only large/unreadable/binary state added; unit tests passed in run 44285031576 (SHA 7135a89). Per-project JavaScript toggle and cancellation-safe preview listener cleanup added; preference instrumentation test and latest CI pending. Autosave/save interruption and full preview acceptance remain |
| M4 — Existing runtimes hardening | R-01–R-06, R-10–R-14, S-06 | JavaScript/Python/SQL tests and quotas; Node x86_64 + arm64 ABI, SQLite, health check, logs, cancellation, stop/restart and device verification; capability labels verified | **Partially integrated:** Node 24/SQLite, Python 3.11 and SQL are present; both local and Node foreground-service race fixes passed 11 Android Emulator instrumentation tests in run 44286135337 (SHA c734855); runtime security warnings added. Physical arm64 tests and Node CPU/memory/filesystem quotas remain |
| M5 — npm/npx and React/Vite build workflow | R-07–R-08, R-13 | Explicit-consent install flow, lockfile-aware reproducible install, registry TLS, project-scoped cache/quota, cancellation, lifecycle-script approval, native-addon diagnostics, Vite build + preview fixture | **Not implemented**; technical design and security review required before enablement |
| M6 — PHP runtime | R-09, R-11, R-13 | Supported Android ABI PHP CLI, documented extensions, real PHP project integration fixture, HTTP health test, cancellation/logging/limits, framework compatibility matrix | **Research in progress:** pinned PHP 8.4.25 Android arm64-v8a Runtime Layer and PHPX Android NDK build path identified; a JNI/Embed SAPI wrapper, isolated execution process, output/time limits, license review and device tests are still required. The selected published layer does not establish x86_64 support. |
| M7 — Security, resilience and performance | S-01–S-10, H-10, R-14 | Threat model, permission boundary, Keystore fault injection, malicious ZIP/HTTP corpus, runtime resource limits, crash/ANR/storage/low-memory tests, import/editor/server benchmarks | **Not started**; Node is explicitly not a hostile-code sandbox and lacks CPU/memory/project-filesystem quotas |
| M8 — UI/UX and accessibility completion | P-02–P-03, E-04, H-01/H-03, R-11/R-12 | Full screen walkthrough, phone-size layouts, navigation/back behavior, empty/error/loading states, truthful runtime badges, accessibility labels and dark theme consistency | **Not started**; Compose screens and design system exist; full device audit required |
| M9 — Final integration, release and physical-device acceptance | Q-01–Q-06 and all requirement IDs | Full clean build, unit/instrumentation/integration/regression/security/performance suite, arm64 physical device tests, release APK/checksum, evidence index, no unresolved requirements | **Blocked until M0–M8 gates pass** |

## Known technical boundaries to resolve, not silently omit

- Node.js Mobile 24.22.1 currently supports arm64-v8a and x86_64; CI proves x86_64 emulator execution and APK packaging of both ABIs. Physical arm64 behavior is not yet proven.
- Node.js Mobile does not support `child_process`/ `cluster` fully; native addons require Android-compatible binaries. The current Node service bounds logs but does not impose CPU, memory or project-filesystem quotas.
- npm/npx and dependency installation are not integrated. Existing `node_modules` can be used; lifecycle scripts are not auto-run.
- PHP is inspection-only today; no PHP CLI is packaged or executed.
- Python is CPython 3.11 via Chaquopy; third-party packages may require Android-compatible wheels/native libraries.
- LAN HTTPS uses a device-local self-signed certificate; browsers may show a trust warning. Physical-device handshake and fingerprint workflow remain required.
- Runtime code executes with app permissions. NovaRun is not currently a hostile-code sandbox.
- Build provenance for the pinned Node.js Mobile distribution must remain pinned and reviewed; do not silently substitute an unverified binary.

These boundaries are active work items. They are not user-approved removals and do not permit a final completion claim.

## Evidence ledger

Every milestone must append:

- Milestone and requirement IDs
- Commit SHA and changed-file list
- Review findings and fixes
- Build workflow URL and exact run SHA
- Unit/instrumentation/integration test names and counts
- Security/performance evidence
- Physical-device model, Android version, ABI and results (when performed)
- Remaining blockers and the user's explicit decision for any alternative

## Completion gate

NovaRun is complete only when every requirement in `PROJECT_REQUIREMENTS.md` is either:
- implemented and verified with reproducible evidence; or
- changed through an explicit user-approved alternative/removal, with the register and acceptance tests updated.

No silent deferral, placeholder-only feature, stale CI run, emulator-to-device extrapolation, or unverified APK claim satisfies this gate.
