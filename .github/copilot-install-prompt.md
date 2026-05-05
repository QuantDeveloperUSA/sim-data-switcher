# SIM Data Switcher — Copilot Agent Install Prompt

Paste the block below into the VS Code Copilot Chat panel (Agent mode).
It will handle every step — prerequisites, build, install, and ADB permission grant — and stop to ask you if anything needs input.

---

## Prompt to paste into Copilot Chat

```
You are helping me build and install an Android app called SIM Data Switcher onto my Samsung Galaxy Z Fold 7, which is connected via USB.

Work through these steps in order. After each step, tell me what you did and whether it succeeded before moving on. If a step fails, show me the full error output and suggest a fix before continuing.

---

STEP 1 — Check prerequisites

Run the following commands and report the output of each:
  java -version
  git --version
  adb version

If any command is not found:
- Java missing: tell me to install it from https://adoptium.net and come back
- Git missing: tell me to install it from https://git-scm.com and come back
- ADB missing: tell me to download Platform Tools from https://developer.android.com/tools/releases/platform-tools, extract it to C:\platform-tools, and add it to PATH — or run adb.exe directly from that folder

Do not continue until all three are confirmed working.

---

STEP 2 — Clone the repo

Run:
  git clone https://github.com/QuantDeveloperUSA/sim-data-switcher.git
  cd sim-data-switcher

If the folder already exists, run:
  cd sim-data-switcher
  git pull

---

STEP 3 — Build the APK

Run:
  gradlew.bat assembleDebug

This will take 1–3 minutes on first run (Gradle downloads dependencies).

Watch the output and report:
- BUILD SUCCESSFUL → continue
- BUILD FAILED → show me the last 30 lines of output and stop

The APK will be at:
  app\build\outputs\apk\debug\app-debug.apk

Confirm the file exists after the build.

---

STEP 4 — Verify phone is connected

Run:
  adb devices

Expected output contains a line like:
  RFCY61X401P    device

If it shows "unauthorized": tell me to check my phone screen for a "Allow USB debugging?" dialog and tap Allow.
If it shows nothing: tell me to enable USB debugging in Settings → Developer options, and check the cable.
If adb is not found: remind me to add platform-tools to PATH or cd into C:\platform-tools first.

Do not continue until the device shows as "device" (not "unauthorized" or "offline").

---

STEP 5 — Install the APK

Run:
  adb install app\build\outputs\apk\debug\app-debug.apk

Expected: "Performing Streamed Install" then "Success"

If it fails with INSTALL_FAILED_ALREADY_EXISTS or similar, run:
  adb install -r app\build\outputs\apk\debug\app-debug.apk

Report the result.

---

STEP 6 — Grant the required permission

Run:
  adb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE

This command produces no output on success. If it outputs an error, show it to me.

Then verify the permission was granted:
  adb shell dumpsys package com.abfstech.simswitcher | findstr MODIFY_PHONE_STATE

Expected output contains: "granted=true"

Report what you see.

---

STEP 7 — Launch the app

Run:
  adb shell am start -n com.abfstech.simswitcher/.MainActivity

Then tell me:
- The app should now be open on my phone showing a list of active SIMs
- The currently active data SIM should have a green DATA badge
- Tapping a different SIM will switch mobile data to it while keeping voice on mint

---

STEP 8 — Done

Summarise:
- Which steps succeeded
- Whether the permission was confirmed granted
- Any warnings or issues I should know about

If anything failed, tell me exactly which step and what to do next.
```
