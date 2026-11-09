# NovaRun Runtime Support Matrix

This document records only runtime capabilities which are actually integrated. A successful Android APK build does not prove a language runtime works on a physical device.

| Runtime | Current state | Notes |
|---|---|---|
| HTML | Available for preview | Serves index.html and project assets from the selected project root |
| CSS | Available for preview | Served as text/css; linked stylesheets load with the HTML project |
| JavaScript | Available | Browser JavaScript in web preview plus bounded Rhino ES6 execution for standalone .js; not Node.js |
| SVG | Available for preview | Served as image/svg+xml; usable as image assets or inline HTML SVG |
| SQL | Available | Android SQLite adapter; project-isolated private database |
| Python | Integrated; CI/device verification pending | CPython 3.11 via Chaquopy; standard library with Android-specific limitations |
| Node.js | Planned | The official nodejs-mobile Android release line is Node 18.20.4; Node 18 is end-of-life. Do not ship it as a new production runtime without a maintained supported build |
| npm | Planned | Requires Node.js plus a package install lifecycle and ABI/native-addon policy |
| PHP | Planned | An Android PHP runtime layer must be cross-compiled and linked; a static archive alone is not an executable runtime |
| Java | Planned | Requires a compiler plus JVM-bytecode-to-Dex conversion and controlled class loading |
| Kotlin | Planned | Requires Kotlin compiler integration plus Dex conversion; JVM bytecode cannot be loaded directly as Android Dex |
| C / C++ | Planned | Requires an Android-hosted compiler toolchain; desktop NDK clang binaries cannot simply be executed as Android app binaries |

## Python runtime scope

- CPython 3.11 is embedded in the APK for `arm64-v8a` and `x86_64`.
- `.py` files can be run from the code editor.
- Project root is used as the working directory and import path during execution.
- Standard output and error are captured and bounded.
- Script path is canonicalized and must remain inside the selected project.
- User-provided Python code runs with the app's own permissions. This is not a hostile-code sandbox.
- Native Python packages must have compatible Android wheels; arbitrary desktop wheels are not supported.

## LAN server authentication

- LAN binding is opt-in through the foreground LAN server service; the server class defaults to loopback-only.
- Each server session receives a cryptographically random, 192-bit password.
- HTTP Basic authentication is required before any project path is served.
- The username is `novarun`; the password is displayed in the app while the session is active.
- Passwords are not persisted as a reusable setting and are cleared when the service stops.
- The transport remains HTTP. Basic authentication is not encrypted in transit, so use a trusted private network only. HTTPS/TLS is a separate required hardening step for untrusted networks.

## Release gate

Do not mark a runtime as Available until CI tests pass and an Android device test verifies a real script execution. Do not describe build success as proof of runtime compatibility.

## Remaining runtime integration gates

These items remain planned and must not be shown as runnable until the complete toolchain is packaged and verified on a physical Android device:

- **Node.js + npm:** choose a maintained Android-compatible Node release; package the correct ABI binaries; implement npm registry access, lockfile handling, lifecycle-script controls, disk quotas, cancellation, and native-addon compatibility. Rhino is not a Node substitute.
- **PHP:** build and maintain PHP CLI for each supported Android ABI, provide required extensions, connect process I/O and lifecycle to the app, and test common Composer workflows. A static PHP archive is not a runnable CLI.
- **Java:** bundle a compiler, convert emitted class files to Android DEX, resolve dependencies, and execute only through a constrained class-loading boundary.
- **Kotlin:** integrate a compatible Kotlin compiler and dependency resolver, convert compiler output to DEX, and verify Android API compatibility. Kotlin/JVM output cannot be loaded as raw DEX without conversion.
- **C/C++:** package an Android-hosted compiler and sysroot, define supported target ABIs and C++ runtime, compile to Android ELF binaries, and execute with strict resource and file-access limits.

Do not silently route any of these languages to JavaScript, the browser, or an external browser. A missing adapter must produce a clear “not installed / adapter planned” result. Package installation and arbitrary project scripts can execute untrusted code; before enabling them, add explicit user consent, project-scoped storage, cancellation, resource limits, and a documented security boundary.


## Web project workflow

- Import a project ZIP and keep its folder structure, including index.html, CSS, JavaScript, SVG, images, and fonts.
- Start the local server from the project root; extension MIME types include HTML, CSS, JavaScript modules, SVG, JSON, WASM, and common image/font formats.
- Open the project preview at the local server URL. Relative asset paths should resolve from the HTML document.
- Browser JavaScript uses browser APIs in the preview. The standalone JavaScript Run action uses Rhino ES6 with execution and output limits; browser DOM APIs, Node.js APIs, and npm are not available in that interpreter.
- Missing static assets return 404; extensionless client-side routes may fall back to index.html.
