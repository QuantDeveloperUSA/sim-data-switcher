# SIM Data Switcher

A minimal Android app that lets you set any active eSIM as your **mobile data SIM** independently of your voice SIM — no root required.

Built for Samsung Galaxy Z Fold 7 (One UI 8 / Android 16), but works on any Android 8+ dual-SIM device.

---

## What it does

Opens a screen listing all your active SIMs. Tap one to make it the default data provider. Your voice SIM stays unchanged.

It uses a hidden system API (`SubscriptionManager.setDefaultDataSubId`) that requires one ADB permission grant — done once, permanently.

---

## Prerequisites

- A Windows/Mac/Linux PC with [ADB installed](https://developer.android.com/tools/releases/platform-tools)
- Android Studio (to build the APK), **or** just install the pre-built debug APK if available
- USB cable (or Wi-Fi ADB)

---

## Step 1 — Enable Developer Options on your phone

1. **Settings → About phone**
2. Tap **Build number** 7 times rapidly
3. Enter your PIN if prompted
4. Go back to **Settings → Developer options**
5. Enable **USB debugging**

---

## Step 2 — Build the APK

### Option A — Android Studio
1. Clone this repo:
   ```
   git clone https://github.com/QuantDeveloperUSA/sim-data-switcher.git
   ```
2. Open the folder in **Android Studio**
3. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Option B — Command line
```bash
git clone https://github.com/QuantDeveloperUSA/sim-data-switcher.git
cd sim-data-switcher
./gradlew assembleDebug          # Mac/Linux
gradlew.bat assembleDebug        # Windows PowerShell
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Step 3 — Install the APK on your phone

Connect your phone via USB, accept the "Allow USB debugging" prompt on the phone, then:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 4 — Grant the permission (one-time, permanent)

This is the critical step. The app needs `MODIFY_PHONE_STATE` which can only be granted via ADB:

```bash
adb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE
```

You only need to do this **once**. The permission persists across reboots.

---

## Step 5 — Use the app

1. Open **SIM Switcher** from your app drawer
2. Your active SIMs are listed (mint, usa urozet, etc.)
3. The currently active data SIM is highlighted in green with a ✓
4. **Tap any SIM** to switch data to it
5. The screen refreshes immediately confirming the change

Your voice/calls SIM (mint) remains unchanged.

---

## Troubleshooting

**"No active SIMs found"**
→ Make sure the SIMs you want are toggled ON in Settings → Connections → SIM manager

**"Error: permission denied"**
→ Re-run the ADB grant command in Step 4. Make sure USB debugging is enabled.

**App crashes on launch**
→ Ensure you're running Android 8.0 (API 26) or higher. Check with Settings → About phone → Android version.

**ADB not recognized in PowerShell**
→ Download [Platform Tools](https://developer.android.com/tools/releases/platform-tools), extract the zip, and run the commands from inside that folder:
```powershell
cd C:\path\to\platform-tools
.\adb.exe install ...
.\adb.exe shell pm grant ...
```

---

## How it works (technical)

Android exposes `SubscriptionManager.setDefaultDataSubId(int subId)` as a hidden API. It requires the `MODIFY_PHONE_STATE` permission, which is normally reserved for system/carrier apps. However, Android allows granting it via ADB on debug builds without root.

The app calls this method via reflection, meaning it works without any special build flags.

---

## Project structure

```
sim-data-switcher/
├── app/
│   ├── src/main/
│   │   ├── java/com/abfstech/simswitcher/
│   │   │   └── MainActivity.java       ← entire app, ~120 lines
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

No dependencies. No XML layouts. Pure Java, single activity.
