# NovaRun Runtime Support Matrix

This document records only runtime capabilities which are actually integrated. A successful Android APK build does not prove a language runtime works on a physical device.

| Runtime | Current state | Notes |
|---|---|---|
| JavaScript | Available | Rhino ES6 interpreter; not Node.js and does not provide npm or Node built-ins |
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
