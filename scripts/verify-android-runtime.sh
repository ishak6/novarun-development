#!/bin/sh
# Executed as one script file because android-emulator-runner splits inline
# script input into independent shell invocations.
set -eu

ROOT="${GITHUB_WORKSPACE:-$(pwd)}"
cd "$ROOT"

REPORT_DIR="app/build/reports/novarun-runtime"
APK="app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
mkdir -p "$REPORT_DIR"

test -s "$APK"
test -s "$TEST_APK"

adb wait-for-device
BOOTED="$(adb shell getprop sys.boot_completed | tr -d '\r')"
if [ "$BOOTED" != "1" ]; then
  echo "Emulator is connected but Android has not completed booting (sys.boot_completed=$BOOTED)."
  exit 1
fi
adb shell settings put global package_verifier_enable 0 || true

echo "Installing app APK with non-streaming ADB..."
timeout 20m adb install --no-streaming -r "$APK"
echo "Installing instrumentation APK..."
timeout 10m adb install --no-streaming -r "$TEST_APK"

TEST_PACKAGE="com.mangalocalrunnerplus.test"
echo "Instrumentation package: $TEST_PACKAGE"

set +e
adb shell am instrument -w -r "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" > "$REPORT_DIR/instrumentation.txt" 2>&1
TEST_EXIT=$?
set -e
cat "$REPORT_DIR/instrumentation.txt"
adb logcat -d -t 5000 > "$REPORT_DIR/logcat.txt" || true

if [ "$TEST_EXIT" -ne 0 ] || ! grep -Eq 'OK.*[0-9]+ tests?' "$REPORT_DIR/instrumentation.txt" || grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=' "$REPORT_DIR/instrumentation.txt"; then
  echo "Android instrumentation tests did not pass."
  exit 1
fi

echo "All NovaRun ZIP runtime instrumentation tests passed."
