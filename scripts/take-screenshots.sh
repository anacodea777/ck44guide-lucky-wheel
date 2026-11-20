#!/usr/bin/env bash
# Drives the debug APK on a running emulator and saves README screenshots.
# Usage: bash scripts/take-screenshots.sh [output-dir]
set -euo pipefail

PKG=com.vongquay.luckywheel
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=${1:-screenshots}
mkdir -p "$OUT"

adb wait-for-device
adb install -r "$APK"

# Instant window transitions, but keep animator scale at 1 so the wheel really spins.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 1

# Clean status bar.
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0930
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false

shot() { sleep "${2:-1}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "saved $1"; }

# Tap the centre of the view with the given resource id (or content-desc as fallback).
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb shell cat /sdcard/ui.xml > /tmp/ui.xml
  local node
  node=$(grep -o "<node [^>]*resource-id=\"$PKG:id/$1\"[^>]*>" /tmp/ui.xml | head -1 || true)
  if [ -z "$node" ] && [ -n "${2:-}" ]; then
    node=$(grep -o "<node [^>]*content-desc=\"$2\"[^>]*>" /tmp/ui.xml | head -1 || true)
  fi
  if [ -z "$node" ]; then echo "view $1 not found"; return 1; fi
  read -r x1 y1 x2 y2 < <(echo "$node" | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/')
  adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
}

launch() {
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/.MainActivity" >/dev/null
}

# ---- English ----
adb shell cmd locale set-app-locales "$PKG" --locales en
launch
shot 01_home_en 3

tap spinButton
shot 02_spinning_en 1.3
shot 03_result_en 7
adb shell input keyevent KEYCODE_BACK

tap action_edit "Edit items"
shot 04_edit_items_en 2
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input keyevent KEYCODE_BACK 2>/dev/null || true

# ---- Bengali ----
adb shell cmd locale set-app-locales "$PKG" --locales bn
launch
shot 05_home_bn 3

tap spinButton
shot 06_result_bn 8
adb shell input keyevent KEYCODE_BACK

demo exit
ls -l "$OUT"
