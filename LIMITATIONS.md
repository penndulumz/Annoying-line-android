# AnnoyingLine: Overlay Limitations & Troubleshooting

Because this application relies on system-level overlays (`TYPE_APPLICATION_OVERLAY`), it is subject to strict Android security and battery management policies. This document explains known limitations and troubleshooting steps.

## System UI Restrictions

### 1. Lock Screen
- Android **hides all non-system overlays** on the lock screen to prevent attackers from spoofing unlock prompts.
- The line will disappear when the screen is locked and reappear immediately after unlocking.

### 2. Notification Shade
- Pulling down the notification shade or quick settings places the system UI above `TYPE_APPLICATION_OVERLAY`.
- The line will appear to slide behind the notification shade.

### 3. System Permission Dialogs (Tapjacking Protection)
- When a system permission dialog (e.g., "Allow app to use location"), biometric prompt, or package installer appears, Android enforces `setHideOverlayWindows(true)`.
- The line will temporarily vanish while the dialog is on-screen.

### 4. Gesture Navigation
- Our overlay uses `FLAG_NOT_TOUCHABLE`, so it does not intercept your touches.
- However, if the line is positioned extremely close to the screen edges, some strict OEM skins might flag it as interfering with gesture navigation. If you experience issues with back gestures, try nudging the line position slightly inward.

## Android 13+ Hardening

- **Restricted Settings:** If you install this app via an APK (sideloading) on Android 13 or higher, the system will block the "Display over other apps" permission by default.
  - **Fix:** Go to Android Settings → Apps → AnnoyingLine. Tap the three-dot menu in the top right and select **"Allow restricted settings"**. Then, you can grant the overlay permission.
- **Android 14+ Foreground Services:** The app uses the `specialUse` foreground service type to comply with new background execution limits.
- **Android 15+:** The system now strictly requires an active, visible overlay window for an app to start a foreground service from the background (e.g., on boot or after an update).

## OEM Battery Restrictions (Samsung, Xiaomi, etc.)

Many manufacturers aggressively kill background services to save battery. **Samsung** is particularly notorious for this. If the AnnoyingLine disappears after a few hours or fails to start on boot:

### Samsung Devices
1. Go to **Settings → Battery and device care → Battery**.
2. Tap **Background usage limits**.
3. Add AnnoyingLine to the **"Never sleeping apps"** list.
4. Go to **Settings → Apps → AnnoyingLine → Battery** and select **"Unrestricted"**.

### Xiaomi / MIUI / HyperOS
1. Go to **Settings → Apps → Manage apps → AnnoyingLine**.
2. Turn on **Autostart**.
3. Tap **Battery saver** and set it to **No restrictions**.

### OnePlus / OxygenOS
1. Go to **Settings → Battery → Battery optimization**.
2. Find AnnoyingLine and select **Don't optimize**.
3. (Older versions): Lock the app in the recent apps menu.

For more detailed device-specific guides, visit [Don't Kill My App!](https://dontkillmyapp.com/).
