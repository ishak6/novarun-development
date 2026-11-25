# NovaRun Physical Android Device Validation

This checklist covers device-only release gates that Android Emulator CI cannot establish. Use a trusted device and trusted ZIP projects only. NovaRun executes Node code with the app's UID and permissions; it is not a hostile-code sandbox.

## Test build

- Release: [NovaRun Debug APK 383.1](https://github.com/ishak6/novarun-development/releases/tag/novarun-debug-383-1)
- APK: [app-debug.apk](https://github.com/ishak6/novarun-development/releases/download/novarun-debug-383-1/app-debug.apk)
- Release asset SHA-256 (GitHub asset digest): `02b8f3d4b6bda0df2bbc07b898cdafc814df7b7989733e92b90f9a09f386a346`
- CI run: [#383](https://github.com/ishak6/novarun-development/actions/runs/45394800830) — Build, Android Emulator runtime and publish succeeded.

Before testing, export/backup important projects. This Debug APK is not signed with a stable release key, so Android may reject an in-place update from a differently signed NovaRun build. Do not uninstall an existing app until its project data is backed up.

## A. Install and record device details

1. Download the APK from the release link above.
2. Install it using Android's package installer. If signature mismatch is shown, stop; back up app data before considering uninstall/reinstall.
3. Record: phone model, Android version, CPU ABI (arm64-v8a expected for most current phones), NovaRun version and install result.
4. Grant only permissions required for the test. Do not grant unrelated accessibility, device-admin or special access.

## B. ZIP import and static preview

1. Import the trusted `clothing_store_nokshi_v1.5.3_professional.zip` project.
2. Confirm import completes without path traversal, duplicate-path or extraction error.
3. Open Preview.
4. Check storefront HTML, CSS, images and JavaScript assets; navigate between available storefront pages.
5. Close Preview, reopen it, and confirm it remains usable.

Expected: project imports and static storefront renders without a blank page or missing assets. Import must not automatically execute Node scripts, package lifecycle hooks or install dependencies.

## C. Node.js backend lifecycle (manual Start only)

1. Select the imported project and set the backend port to `3000`.
2. Tap Start explicitly. Confirm the ongoing NovaRun Node notification appears with a Stop action.
3. On the same phone, open `http://127.0.0.1:3000/healthz`.
4. Confirm the health response is successful and the storefront endpoint responds.
5. Tap Stop in NovaRun and also test the notification Stop action.
6. Confirm the endpoint no longer accepts a new connection.
7. Start again and confirm the service can restart after the previous Node process exited.
8. Background NovaRun for at least 5 minutes while the foreground notification is present; return and confirm status is accurate.

Expected: no false RUNNING state, no repeated startup-redelivery loop, logs remain bounded, and Stop ends the backend. Node.js Mobile does not support every desktop Node API (including child_process/cluster); do not treat a failure in those APIs as a general Node compatibility pass.

## D. Local Hosting and HTTPS/TLS LAN

1. Start Local Hosting with LAN sharing disabled first. Open the displayed localhost URL from NovaRun.
2. Stop the service and confirm the localhost endpoint closes.
3. Enable LAN HTTPS only on a trusted private Wi-Fi/hotspot. Keep LAN authentication enabled.
4. Start the server and record the displayed LAN URL, port and SHA-256 certificate fingerprint.
5. On a second device connected to the same network, open the LAN URL and complete authentication.
6. Compare the certificate fingerprint with NovaRun's displayed fingerprint before accepting/trusting the self-signed certificate.
7. Confirm the selected static project preview is served over HTTPS.
8. Stop the server and confirm the second device can no longer load the page.

Important: the current Node backend binds to loopback. LAN HTTPS serves the selected static preview; this test does not establish LAN reverse-proxy access to the Node backend.

## E. Report results

For each section mark PASS, FAIL or BLOCKED. Include the exact step, expected vs actual behavior, phone model, Android version and ABI. Attach screenshots or sanitized Logcat only; remove passwords, setup tokens, private project data and any authentication headers.

Do not report Emulator CI as physical-device acceptance. A successful device run should be recorded separately from CI.
