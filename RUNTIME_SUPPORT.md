# NovaRun Runtime Support Matrix

This document records only capabilities integrated into the current Android app. A successful APK build does not prove runtime behavior on a physical device.

| Runtime / file type | Current state | Notes |
|---|---|---|
| HTML | Preview only | Serves validated project HTML and assets |
| CSS | Preview only | Served as text/css and loaded by HTML projects |
| SVG | Preview only | Served as image/svg+xml |
| JavaScript | Executable with limits + browser preview | Standalone .js uses bounded Rhino ES6; browser JavaScript runs in preview. Neither is Node.js |
| Markdown (.md) | Documentation | Preserved and detected; not executable |
| .gitkeep | Project metadata | Preserved by ZIP import; hidden from public HTTP serving with other dotfiles |
| SQL | Executable with limits | Project-isolated SQLite database |
| Python | Executable with limits; CI bridge verified | CPython 3.11 via Chaquopy; Android-specific package limitations; physical-device verification remains required |
| Node.js | Embedded runtime integrated; device verification pending | Node.js Mobile 18.20.4 is packaged for arm64-v8a and x86_64. User-selected server entry points can be started from the Node Backend Runner with visible foreground notification and Stop control. Physical-device compatibility remains unverified |
| npm / npx | Not integrated | package.json and lockfiles are inspected; no npm/npx CLI or dependency installation is bundled. Existing node_modules may be used by the Node server; lifecycle scripts are never auto-run |
| PHP | Planned | Composer/PHP manifests are inspected; no PHP CLI runtime is bundled or executed |
| Java | Planned | Requires compiler, DEX conversion, dependency resolution and constrained class loading |
| Kotlin | Planned | Requires compiler, DEX conversion, dependency resolution and constrained class loading |
| C / C++ | Planned | Requires Android-hosted compiler, target sysroot, linker and bounded native execution |

## Execution safety and verification

- Project code runs with the app's Android permissions; it is not a hostile-code sandbox.
- Runtime availability is shown separately from preview-only support.
- A runtime is marked "Ready to Run" only when its adapter is registered and reports available.
- Python script paths are canonicalized and restricted to the selected project. Output is capped and a cooperative execution deadline is applied.
- Standalone JavaScript runs in Rhino ES6 with a 1 MB script limit, instruction budget, five-second cooperative deadline and bounded output. Node APIs, browser DOM APIs and npm are not provided by Rhino.
- SQL uses a private database per project, with statement-count, file-size and output limits.
- Package installation and arbitrary project scripts require explicit user consent, project-scoped storage, cancellation, disk quotas and a documented security boundary before being enabled.

## ZIP import limits and path safety

- Maximum 2,000 archive entries.
- Maximum 200 MiB total extracted data and 64 MiB per individual file.
- Entry names are checked for absolute paths, parent traversal, drive-qualified paths, NUL characters and excessive length.
- Duplicate normalized paths and file/directory conflicts are rejected.
- Files are extracted into a temporary directory and promoted only after extraction completes.
- ZIP import is not a malware scanner.

## LAN server authentication and HTTPS

- LAN binding is opt-in and uses TLS 1.2 or TLS 1.3 with an RSA identity generated and retained by Android Keystore.
- The certificate is self-signed and device-local. Verify the displayed SHA-256 fingerprint before trusting it; HTTPS does not establish public-CA trust or automatic hostname/IP validation.
- Password protection can be enabled or disabled while the server is stopped. Credentials are encrypted at rest using Android Keystore AES-GCM.
- Password OFF allows any reachable LAN client to access served project content. Keep LAN sharing disabled on untrusted networks.
- TLS integration tests passed in CI; physical-device/browser trust and handshake testing remain release gates.

## Remaining integration gates

- **Node.js:** embedded Node.js Mobile 18.20.4 is now packaged for arm64-v8a and x86_64, with a JNI bridge and visible foreground service. Verify server startup, shutdown, logging, networking, core APIs and ABI compatibility on physical devices. Node.js Mobile has compatibility limitations, including unsupported child_process/cluster APIs.
- **npm / npx:** still not integrated. Add an explicit-consent package installation workflow with lockfile enforcement, registry TLS, project-scoped cache/storage quotas, cancellation, lifecycle-script approval and native-addon compatibility checks before enabling package installation.
- **PHP:** build and maintain PHP CLI for each supported Android ABI, select extensions, integrate process I/O and test Composer workflows.
- **Java:** bundle a compiler, convert class files to DEX, resolve dependencies and execute through a constrained class-loading boundary.
- **Kotlin:** integrate a compatible Kotlin compiler, convert output to DEX and verify Android API compatibility.
- **C/C++:** package an Android-hosted compiler and sysroot, define target ABIs and C++ runtime, compile Android ELF binaries and execute with strict resource/file-access limits.

## PHP project inspection

- Reads `composer.json` and detects Laravel, Symfony, Slim, CakePHP and CodeIgniter 4 dependencies.
- Reports PHP version constraints, required `ext-*` extensions, production/development dependency counts, `composer.lock`, conventional entry points and Composer scripts.
- Composer scripts are diagnostics only; NovaRun does not run Composer or PHP code until a maintained Android CLI build is integrated and explicit execution controls are in place.

## Node.js backend runner

- Embedded Node.js Mobile 18.20.4 is downloaded during CI and packaged for arm64-v8a and x86_64, together with a JNI bridge.
- A user-selected Node entry point is started in a dedicated Android service process with an ongoing notification and explicit Stop action.
- The runner checks common entry points, port range and the presence of `node_modules` when dependencies are declared. It reports compatibility warnings and captures server logs.
- The runner currently expects dependencies to be installed already. It does not run npm, package lifecycle scripts, child processes or cluster workers. Native addons require Android ABI-compatible binaries.
- CI verifies native compilation and JVM launch-plan tests; physical-device runtime verification remains outstanding.

## Node project inspection

- Reads `package.json` without executing project code.
- Detects npm, pnpm, Yarn and Bun lockfiles and suggests a reproducible install command (`npm ci`, `pnpm install --frozen-lockfile`, `yarn install --immutable`, or `bun install --frozen-lockfile`).
- Reports scripts, dependency counts, build command, native-addon indicators and lifecycle-script warnings in the Workspace.
- These are diagnostics only: NovaRun does not yet install dependencies or run Node/npm commands.

## Web project workflow

- Import ZIP projects while preserving HTML, CSS, JavaScript, Markdown, .gitkeep, SVG, images and fonts.
- Preview validation checks index.html in dist, build, out, then project root, and rejects canonical paths outside the imported project.
- Browser JavaScript uses browser APIs in preview; standalone JavaScript uses Rhino ES6.
- Missing static assets return 404; extensionless client-side routes may fall back to index.html.
