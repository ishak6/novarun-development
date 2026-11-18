# NovaRun — Zero-Pending Requirements Register

**Repository:** `ishak6/novarun-development`  
**Package:** `com.mangalocalrunnerplus`  
**Target:** Android-first, phone-first local development environment  
**Baseline commit audited:** `d964fd0374b8762443a449aeff5a56e16e9ca5f7`  
**Status vocabulary:** `Verified in CI`, `Implemented — physical-device verification required`, `Partially implemented`, `Not implemented`, `Blocked pending an explicit technical decision`.

This register is the source of truth for scope. A successful APK build is not proof that every feature works on a physical Android device. No item may be marked complete without test evidence.

## A. Product and delivery requirements

| ID | Requirement | Current evidence / status |
|---|---|---|
| P-01 | Android APK; Android-first delivery; package `com.mangalocalrunnerplus` | Implemented; APK built by Actions. Physical-device install/launch still required. |
| P-02 | Phone-first premium developer dashboard; macOS/iOS-inspired dark UI, blue/purple accents, rounded/glass cards | Implemented in Compose; visual/device-size audit remains. |
| P-03 | Home, Workspace, Preview, Settings navigation and clear project/runtime status | Implemented; end-to-end navigation audit remains. |
| P-04 | Build APK through GitHub Actions and retain test evidence | Verified in CI on baseline run 44098134644. |
| P-05 | Mobile-only workflow: do not require the user to run PC terminal commands; give complete copy-paste files when manual edits are needed | Binding delivery constraint. |

## B. Project import and workspace

| ID | Requirement | Current evidence / status |
|---|---|---|
| W-01 | Import ZIP through Android document picker | Implemented. |
| W-02 | ZIP signature/content validation and safe extraction | Implemented with tests; extend adversarial regression coverage. |
| W-03 | ZIP-slip, absolute path, drive path, NUL, duplicate normalized path and file/directory conflict protection | Implemented with tests. |
| W-04 | Hard caps: 2,000 entries, 200 MiB total extracted bytes, 64 MiB per file; preferences may lower but never raise hard caps | Implemented with unit tests. |
| W-05 | Extract to temporary directory and promote only after success; failed import must not corrupt an existing project | Implemented using temporary extraction and rename; verify rollback/failure cases on device. |
| W-06 | Preserve project files including HTML, CSS, JS, JSON, TXT, XML, Python, Markdown, images, fonts and SVG | Partially implemented; complete file-type matrix and round-trip tests required. |
| W-07 | Workspace project listing, folder navigation, file CRUD and project selection | Workspace/file explorer now delegates create/list/rename/delete to a tested file-operation layer. It rejects path traversal, control characters, root deletion and symlinks, including nested external symlinks. Project repository create/list/delete and symlink isolation passed in run 44285031576 (SHA 7135a89); expanded file-operation JVM tests are added and latest CI pending. |
| W-08 | Project backup and restore | Backup ZIP export is implemented with import-compatible hard caps and symlink rejection; Workspace restore imports as a separate project without overwriting the source. JVM and Android production-importer round-trip tests passed in run 44285031576 (SHA 7135a89). |
| W-09 | Nested ZIP wrapper/root detection and project entry-point detection | Implemented; extend fixtures and edge-case tests. |

## C. Editor and preview

| ID | Requirement | Current evidence / status |
|---|---|---|
| E-01 | Editor for HTML, CSS, JavaScript, JSON, TXT, XML and Python | Editor and syntax support exist; verify every type and save/reopen. |
| E-02 | Syntax highlighting and editable code field | Implemented; device UX/performance audit remains. |
| E-03 | Autosave plus manual save without races or partial writes | Atomic/coordinated save logic exists; full interruption and process-death tests required. |
| E-04 | Unsaved-change warnings and read-only/permission handling | Partially implemented; verify all navigation/exit paths. |
| E-05 | Large-file protection and bounded editor memory | Editor now refuses binary/unsupported, unreadable and over-2 MB files with a read-only warning and rejects UTF-8 snapshots above 2 MB before writing; policy unit tests passed in run 44285031576 (SHA 7135a89). Full stress/performance matrix remains. |
| E-06 | Native in-app WebView preview; JavaScript enable/disable control | Native in-app WebView preview has per-project JavaScript ON/OFF and HTTPS external-network opt-in controls. External HTTPS requests and subresources are blocked by default; external cleartext HTTP and file/content URLs remain blocked. URL policy and per-project network/JavaScript preference instrumentation passed in run 44288231337 (SHA f627439). Preview listener cancellation cleanup and URL/project reload identity are implemented; latest small policy-atomicity change is under CI. |
| E-07 | Serve project HTML/assets with MIME handling, 404 behavior, safe canonical paths and SPA fallback | Implemented; security regression coverage required. |
| E-08 | React/Vite project preview from prebuilt output (`dist`, `build`, `out`) | Preview validator supports output folders; full React/Vite fixture test required. |
| E-09 | Build React/Vite source projects inside NovaRun | Not implemented: npm/npx/dependency installation is not bundled. Must be resolved by implementation or an explicit, technically justified user-approved alternative. |

## D. Local hosting and network

| ID | Requirement | Current evidence / status |
|---|---|---|
| H-01 | Start/Stop local server with visible status and error messages | Implemented; emulator tests exercise service start/stop. |
| H-02 | Localhost bind only to loopback; localhost must work without Wi-Fi/LAN | Implemented; emulator test returns HTTP 200 and verifies socket closes on Stop. |
| H-03 | Configurable/validated port and clear displayed localhost port | Partially implemented; localhost currently selects a free port in 18080–18100. Verify user-configurable localhost port requirement. |
| H-04 | LAN sharing only when explicitly enabled; same Wi-Fi/hotspot guidance | Implemented in service/UI; physical network test required. |
| H-05 | LAN HTTPS with Android Keystore identity and visible SHA-256 fingerprint | Implemented; physical browser handshake/trust test required. |
| H-06 | LAN authentication credentials encrypted at rest; optional auth state clearly explained | Implemented; Keystore failure fallback added; physical-device test required. |
| H-07 | Optional LAN failures (TLS, Keystore, credentials, IP, invalid/busy port) must not disable localhost | Implemented; invalid LAN port regression test passes in emulator. Keystore failure injection test still required. |
| H-08 | Start/Stop race must not leak sockets; Stop closes listeners | Emulator instrumentation covers normal stop and rapid start/stop; repeat stress on physical device. |
| H-09 | Background hosting survives app UI navigation and has an ongoing notification with user Stop action | Foreground service exists; both local-preview and Node services now promote to foreground before processing Start/Stop actions. Emulator regression covers immediate and delayed Stop; 11 instrumentation tests passed in run 44286135337 (SHA c734855). Android 13–15 physical-device lifecycle/permission verification remains. |
| H-10 | No accidental public-network exposure; no false claim that app code is sandboxed | Security policy documented; runtime code executes with app permissions and is not a hostile-code sandbox. |

## E. Runtime engines and developer tools

| ID | Requirement | Current evidence / status |
|---|---|---|
| R-01 | Static HTML/CSS/SVG/image/font preview | Implemented; regression matrix required. |
| R-02 | Browser JavaScript in preview and standalone JavaScript execution | Implemented; standalone uses Rhino ES6 with limits; Node/browser APIs are not available in Rhino. |
| R-03 | Python runtime | CPython 3.11 via Chaquopy integrated; CI bridge verified; physical-device package/runtime tests required. |
| R-04 | Node.js backend runtime | Embedded Node.js Mobile 24.22.1, arm64-v8a and x86_64 libraries, JNI bridge and foreground service integrated; emulator tests pass; physical arm64 lifecycle/API/network test required. |
| R-05 | Node server project detection, entry point selection, port validation, health check, logs and Stop | Implemented; test common layouts and process lifecycle. |
| R-06 | Node SQLite (`node:sqlite`) | Verified in x86_64 Android emulator against supplied ZIP fixtures; physical arm64 test required. |
| R-07 | npm/npx commands and package installation | Not implemented. Current inspector detects lockfiles and suggests commands only; no package installation or lifecycle scripts. Must be resolved. |
| R-08 | React/Vite dependency installation/build | Not implemented because npm/npx are not integrated; must be resolved with R-07. |
| R-09 | PHP runtime / CLI execution | Not implemented. Current PHP support is read-only Composer/framework inspection only. Must be resolved or explicitly replaced with user approval. |
| R-10 | SQL execution in project-isolated SQLite database with limits | Implemented; test isolation and limits. |
| R-11 | Runtime capability status must distinguish Ready, preview-only, unavailable and planned | Capability manager/status screens exist; audit for truthful state and no false Ready labels. |
| R-12 | Runtime console, stdout/stderr, bounded logs, errors and clear stop state | Implemented; Node log cap test passes; add cross-runtime log tests. |
| R-13 | Dependency/native-addon limitations and unsupported APIs must be visible before execution | Partial documentation exists; surface actionable compatibility report in UI. |
| R-14 | Runtime code must have documented permission/security boundaries, output/deadline/storage controls | Python, Rhino and SQL have limits; Node lacks CPU/memory/project-filesystem quotas and is not sandboxed. Security hardening required. |

## F. Security, reliability and performance

| ID | Requirement | Current evidence / status |
|---|---|---|
| S-01 | ZIP/path traversal and duplicate-path defenses | Unit-tested. |
| S-02 | HTTP path traversal, hidden-file and MIME protections | Implemented; preview WebView blocks external network requests by default and only allows external HTTPS after per-project opt-in; URL-policy instrumentation passed in run 44288231337 (SHA f627439). |
| S-03 | LAN TLS/authentication and encrypted credential storage | Implemented; physical-device verification required. |
| S-04 | Keystore unavailable/corrupt state must not crash app or block localhost | Fallback implemented; fault-injection test required. |
| S-05 | No secrets in user-visible logs or project export | Audit required; never surface first-run setup tokens in assistant output. |
| S-06 | Runtime output limits and bounded memory use | Node output capped at 2 MiB; Python/Rhino/SQL have limits; Node CPU/memory quotas remain absent. |
| S-07 | Crash, ANR, low-memory, storage-full and permission-denied handling | Not fully verified; dedicated resilience tests required. |
| S-08 | Compatibility across supported Android versions and arm64/x86_64 | CI build packages both Node ABIs; emulator x86_64 tests pass; physical arm64 and Android-version matrix required. |
| S-09 | Performance: import, file browsing, editor, preview and runtime startup | No complete benchmark evidence yet. |
| S-10 | Backup/restore and recovery from interrupted writes/imports | Backup ZIP export and restore-as-new are implemented; the Android emulator round-trip test `exportedBackupRestoresThroughProductionZipImporter` passed in [run 44308780215](https://github.com/ishak6/novarun-development/actions/runs/44308780215) (tested SHA `71f882e`). Recovery from interrupted imports/editor writes remains unverified. |

## G. Release and acceptance

| ID | Requirement | Current evidence / status |
|---|---|---|
| Q-01 | Line-by-line review for each changed file and compatibility review against callers/dependencies | Required for every milestone; record findings in milestone evidence. |
| Q-02 | Unit, instrumentation, integration, regression, security and performance tests | Existing suites; full acceptance matrix incomplete. |
| Q-03 | Each milestone builds/runs while the next milestone is developed | Required workflow; CI evidence must identify exact commit SHA. |
| Q-04 | Physical Android install, launch, import, preview, runtime, localhost and LAN tests | Not yet performed in this session; user device confirmation/evidence required. |
| Q-05 | Release audit, APK artifact, checksums, known limitations and recovery notes | Partial; final release package and evidence index required. |
| Q-06 | Zero pending, zero unverified claims, zero unapproved omissions | Final acceptance gate; no completion declaration until every row is verified or the user explicitly approves a limitation/alternative. |

## Acceptance policy

1. A feature is **not complete** merely because a screen, class, button or placeholder exists.
2. A CI pass applies only to the exact tested commit and tested environment.
3. Emulator success does not establish physical-device success.
4. `Planned`, `Not implemented`, `Partially implemented` and `Physical-device verification required` are not completion states.
5. Any technical blocker must be described with evidence and a concrete alternative; the user must approve any scope reduction.
