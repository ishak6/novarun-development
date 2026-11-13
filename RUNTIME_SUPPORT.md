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
| Node.js | Embedded runtime integrated; emulator verified | Node.js Mobile 24.22.1 full build is packaged for arm64-v8a and x86_64. The release archive SHA-256 is pinned and verified in CI; the full build retains built-in `node:sqlite`. The x86_64 Android emulator starts both supplied ZIP backends and verifies HTTP, SQLite, logs and Stop behavior. Physical-device ABI and lifecycle validation remain required |
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

- **Node.js:** embedded Node.js Mobile 24.22.1 full build is now packaged for arm64-v8a and x86_64, with a JNI bridge and visible foreground service. The pinned release archive is SHA-256 verified in CI. Emulator tests prove the x86_64 runtime starts both supplied projects, opens `node:sqlite`, serves the storefront and stops. Verify arm64-v8a, process lifecycle, logging, networking and core APIs on physical devices. Node.js Mobile has compatibility limitations, including unsupported child_process/cluster APIs.
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

- Embedded Node.js Mobile 24.22.1 full build is downloaded during CI, SHA-256 verified and packaged for arm64-v8a and x86_64, together with a JNI bridge.
- A user-selected Node entry point is started in a dedicated Android service process with an ongoing notification and explicit Stop action.
- The runner detects nested ZIP wrapper folders containing a package manifest and server entry point, checks the requested Node engine against the bundled 24.22.1 version, validates the port and checks for `node_modules` when dependencies are declared. It reports compatibility warnings and captures stdout/stderr through a bounded native pipe; the combined log is capped at 2 MiB with a visible truncation marker. This does not impose a CPU, memory, or project-filesystem quota on Node code.
- The two supplied clothing-store ZIPs declare no npm dependencies and use built-in `node:sqlite`; the full Node 24 runtime provides that module. The runner does not yet install npm dependencies or run package lifecycle scripts, child processes or cluster workers. Native addons require Android ABI-compatible binaries.
- CI downloads the fixtures by pinned commit, verifies ZIP integrity, imports them with the production ZIP importer, validates their HTML entry points, serves static storefront assets through the loopback preview server, then starts the Node backend and checks its health route, SQLite database and storefront assets. The emulator is x86_64; this does not prove physical-device or arm64-v8a behavior.

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


### Target ZIP compatibility

- Both repository ZIPs contain the same nested project folder, `clothing_store_secure/`, with `server.js`, `public/`, `data/.gitkeep` and no declared npm dependencies.
- Their `package.json` requires Node.js `>=22.13.0`; the Node 24.22.1 full mobile build satisfies this and includes the built-in SQLite module required by the server.
- Default local settings use `HOST=127.0.0.1`, `PORT=3000`, `data/store.sqlite` and `public/uploads/`. NovaRun sets the host and port and runs with the nested folder as the working directory.
- The first-run setup token is written to the Node server log; the user must create the first admin account in the in-app website preview. Do not expose the local server on a LAN or public network without a separately configured authenticated/TLS deployment.
- The Node 24.22.1 mobile binary is from the pinned fogtape/nodejs-mobile v24.22.1-0 release, not the upstream nodejs-mobile project's official release line. CI verifies its published GitHub SHA-256 digest before packaging. Review the fork's source/provenance and keep the version pinned until a trusted upstream release is available.
