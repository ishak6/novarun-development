# NovaRun Runtime Support Matrix

This document records only capabilities integrated into the current Android app. A successful APK build does not prove runtime behavior on a physical device.

| Runtime / file type | Current state | Notes |
|---|---|---|
| HTML | Available for preview | Serves `index.html` and project assets from the validated preview root |
| CSS | Available for preview | Served as `text/css`; linked stylesheets load with the HTML project |
| JavaScript | Available with limits | Browser JavaScript in web preview plus bounded Rhino ES6 execution for standalone `.js`; not Node.js |
| SVG | Available for preview | Served as `image/svg+xml`; usable as image assets or inline HTML SVG |
| Markdown (`.md`) | Recognized as documentation | Preserved and detected in ZIP projects; not executed as code |
| `.gitkeep` | Recognized as project metadata | Preserved by ZIP import; hidden from public HTTP serving with other dotfiles |
| SQL | Available | Android SQLite adapter; project-isolated private database |
| Python | Integrated; CI verified, physical-device verification pending | CPython 3.11 via Chaquopy; standard library with Android-specific limitations |
| Node.js | Planned | Do not claim Node support until a maintained Android-compatible runtime is integrated and tested |
| npm | Planned | Requires Node.js plus package-install lifecycle, lockfile handling, and native-addon policy |
| PHP | Planned | Requires a maintained Android PHP CLI build for each supported ABI |
| Java | Planned | Requires a compiler plus JVM-bytecode-to-Dex conversion and controlled class loading |
| Kotlin | Planned | Requires Kotlin compiler integration plus Dex conversion; JVM bytecode cannot be loaded directly as Android Dex |
| C / C++ | Planned | Requires an Android-hosted compiler toolchain, sysroot, and compatible runtime |

## Python runtime scope

- CPython 3.11 is embedded in the APK for `arm64-v8a` and `x86_64`.
- `.py` files can be run from the code editor.
- Project root is used as the working directory and import path during execution.
- Standard output and error are captured and bounded.
- Script path is canonicalized and must remain inside the selected project.
- A cooperative execution deadline is applied. This is not a hostile-code sandbox and cannot guarantee termination of every native or blocking operation.
- User-provided Python code runs with the app's own permissions.
- Native Python packages must have compatible Android wheels; arbitrary desktop wheels are not supported.

## ZIP import limits and path safety

- Maximum 2,000 archive entries.
- Maximum 200 MiB total extracted data and 64 MiB per individual file.
- Entry names are checked for absolute paths, parent traversal, drive-qualified paths, NUL characters, and excessive length.
- Duplicate normalized paths and file/directory conflicts are rejected.
- Files are extracted into a temporary directory and promoted to the final project folder only after extraction completes.
- Failed imports remove the temporary extraction directory.
- ZIP import is not a malware scanner. Imported project code is not trusted merely because extraction succeeded.

## LAN server authentication

- LAN binding is opt-in through the foreground LAN server service; the server class defaults to loopback-only.
- Password protection can be enabled or disabled in the app while the server is stopped.
- When enabled, HTTP Basic authentication checks the configured username and password before serving project paths.
- The default username is `novarun`; users can change the username and password in the app.
- Credentials are encrypted at rest with an Android Keystore AES-GCM key.
- When password protection is disabled, anyone able to reach the LAN server may access its served project content.
- The transport remains HTTP. Basic authentication is not encrypted in transit, so use a trusted private network only. HTTPS/TLS is required before using an untrusted network.

## Release gate

Do not mark a runtime as Available until CI tests pass and an Android device test verifies a real script execution. Do not describe build success as proof of runtime compatibility.

## Remaining runtime integration gates

These items remain planned and must not be shown as runnable until the complete toolchain is packaged and verified on a physical Android device:

- **Node.js + npm:** choose a maintained Android-compatible Node release; package the correct ABI binaries; implement npm registry access, lockfile handling, lifecycle-script consent, disk quotas, cancellation, and native-addon compatibility. Rhino is not a Node substitute.
- **PHP:** build and maintain PHP CLI for each supported Android ABI, provide required extensions, connect process I/O and lifecycle to the app, and test common Composer workflows.
- **Java:** bundle a compiler, convert emitted class files to Android DEX, resolve dependencies, and execute only through a constrained class-loading boundary.
- **Kotlin:** integrate a compatible Kotlin compiler and dependency resolver, convert compiler output to DEX, and verify Android API compatibility.
- **C/C++:** package an Android-hosted compiler and sysroot, define supported target ABIs and C++ runtime, compile to Android ELF binaries, and execute with strict resource and file-access limits.

Package installation and arbitrary project scripts can execute untrusted code. Before enabling them, add explicit user consent, project-scoped storage, cancellation, resource limits, and a documented security boundary.

## Web project workflow

- Import a project ZIP and preserve its folder structure, including HTML, CSS, JavaScript, Markdown, `.gitkeep`, SVG, images, and fonts.
- Preview validation checks `index.html` in `dist`, `build`, `out`, then the project root, and rejects canonical paths that escape the imported project.
- Start the local server from the selected project root; supported MIME types include HTML, CSS, JavaScript modules, SVG, JSON, WASM, and common image/font formats.
- Relative asset paths should resolve from the HTML document.
- Browser JavaScript uses browser APIs in the preview. The standalone JavaScript Run action uses Rhino ES6 with execution and output limits; browser DOM APIs, Node.js APIs, and npm are not available in that interpreter.
- Missing static assets return 404; extensionless client-side routes may fall back to `index.html`.
