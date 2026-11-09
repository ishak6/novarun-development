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
| Node.js | Planned | Not bundled; Android support is experimental. Project package metadata is inspected, but Node code is not executed |
| npm / npx | Planned | Requires a working Node runtime and package lifecycle / native-addon controls |
| PHP | Planned | Requires maintained PHP CLI binaries for supported Android ABIs |
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

- **Node.js + npm:** select a maintained Android-compatible build (official Node.js documentation currently classifies Android as unsupported), package supported ABI binaries, and test Node core APIs, npm registry, lockfiles, lifecycle consent, quotas and native add-ons.
- **PHP:** build and maintain PHP CLI for each supported Android ABI, select extensions, integrate process I/O and test Composer workflows.
- **Java:** bundle a compiler, convert class files to DEX, resolve dependencies and execute through a constrained class-loading boundary.
- **Kotlin:** integrate a compatible Kotlin compiler, convert output to DEX and verify Android API compatibility.
- **C/C++:** package an Android-hosted compiler and sysroot, define target ABIs and C++ runtime, compile Android ELF binaries and execute with strict resource/file-access limits.

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
