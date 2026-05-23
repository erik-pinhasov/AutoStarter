# AutoStarter

A minimal, open-source Android app for car stereo head units that automatically launches apps in sequence when the device boots or a Bluetooth device connects.

No internet permission. No data collection. No analytics. 6 permissions total.

<img width="203" height="376" alt="Screenshot 2026-05-23 044807" src="https://github.com/user-attachments/assets/877713b8-5fba-4813-98d6-b86f6ea96d67" />
<img width="200" height="376" alt="Screenshot_2026-05-23-04-37-27-430_com miui global packageinstaller" src="https://github.com/user-attachments/assets/6bee19d1-fd08-4309-b760-2e160d592c53" />
<img width="200" height="376" alt="Screenshot_2026-05-23-04-38-23-208_com autostarter" src="https://github.com/user-attachments/assets/ecd315d5-b491-4c7d-9692-0db8f0273674" />
<img width="200" height="376" alt="Screenshot_2026-05-23-04-38-30-278_com autostarter" src="https://github.com/user-attachments/assets/7b6f8b9a-ed0d-482d-b8c5-a99f15145013" />

## Is this safe?

| Check | How to verify |
|---|---|
| **APK built from source** | Every release is compiled by GitHub Actions from this code. Click **Actions** tab to see the full public build log |
| **No INTERNET permission** | Read `AndroidManifest.xml` — or run `aapt d permissions AutoStarter.apk` |
| **No network code** | The CI security scan checks every build and fails if any network code is found |
| **VirusTotal scan** | Each release includes a VirusTotal report link |
| **Build it yourself** | See build instructions below |

## Features

- Configurable queue of apps to launch automatically in sequence
- Trigger on device boot (`BOOT_COMPLETED`, `QUICKBOOT_POWERON`)
- Trigger on Bluetooth connection (target specific MAC address or any paired device)
- Configurable app close behaviors: keep running, close immediately, or close after N seconds (3–60s)
- Drag-and-drop/button-based queue reordering
- Background service operation

## Permissions

| Permission | Why |
|---|---|
| `RECEIVE_BOOT_COMPLETED` | Detect device boot sequence completion |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | Detect incoming Bluetooth connections |
| `QUERY_ALL_PACKAGES` | Retrieve list of installed applications for queue selection |
| `KILL_BACKGROUND_PROCESSES` | Execute the closing sequence for queued applications |
| `FOREGROUND_SERVICE` | Maintain service priority during queue execution |

## Setup

1. Download APK from [Releases](../../releases)
2. Install, open AutoStarter
3. Toggle the master enable switch to "On"
4. Select your trigger mode (Boot, Bluetooth, or Both) and select your target Bluetooth device if applicable
5. Tap "Add App" to build your launch sequence
6. Tap the close behavior on each item (e.g., "Keep running") to configure delays/auto-kill
7. **Exclude AutoStarter from Android battery optimization** so the system doesn't kill the background receivers

## How it works

Uses a `BroadcastReceiver` listening for system boot and Bluetooth state changes. When triggered, it spawns an `AppLauncherService` that iterates through a JSON array saved in `SharedPreferences`. It fires standard package launch intents, uses standard Android `Handler` delays, and calls `ActivityManager.killBackgroundProcesses()` to close apps if configured.

## Building from source

```bash
sudo apt install android-sdk-platform-23 dalvik-exchange aapt zipalign apksigner default-jdk

ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
mkdir -p build/gen build/classes

aapt package -f -m -S res -J build/gen -M AndroidManifest.xml -I $ANDROID_JAR
javac -source 1.8 -target 1.8 -bootclasspath $ANDROID_JAR -classpath $ANDROID_JAR \
  -d build/classes build/gen/com/autostarter/R.java src/com/autostarter/*.java
dx --dex --output=build/classes.dex build/classes/
aapt package -f -S res -M AndroidManifest.xml -I $ANDROID_JAR -F build/app.apk
(cd build && aapt add app.apk classes.dex)
zipalign -f 4 build/app.apk build/app-aligned.apk
apksigner sign --ks your-key.jks --out build/AutoStarter.apk build/app-aligned.apk
