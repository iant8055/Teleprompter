# 📱 Android Teleprompter

A specialized, lightweight teleprompter application designed for **Android devices**. This tool features a floating overlay service, allowing content creators to read scripts while simultaneously recording via the camera app or streaming live.

---

## ✨ Key Features
* **Floating Overlay:** Reads scripts over any other application (Camera, Zoom, YouTube, etc.).
* **Mirror Mode:** Flip text horizontally for use with professional teleprompter glass/hardware.
* **Hierarchical Organization:** Manage scripts via **Projects > Scenes > Scripts**.
* **Customizable Display:**
    * Adjustable Font Size (12sp - 72sp)
    * Variable Scroll Speed (1x - 10x)
    * Window Opacity / Transparency Control
    * High-Contrast Text Colors
* **Loop Mode:** Seamless continuous replay with END marker and configurable gaps.
* **Countdown Timer:** Film-leader style animation with beeps and ACTION tone. Choose from Off, 3, 5, or 10 seconds before scrolling begins.
* **Multi-Language Support:** Full app translation via Google Translate API — menus, buttons, and labels update automatically.
* **Redesigned Settings Panel:** Bottom sheet with real-time preview — adjust settings while the script scrolls live behind the panel.
* **Persistent Settings:** Automatically remembers your font, speed, color, and mirror preferences.
* **Offline Privacy:** All data is stored locally on the device (`JSON` format); no internet connection required.

---

## 📜 Version History

| Version | Date | Changes & Features |
| :--- | :--- | :--- |
| **v3.1** | Nov 23, 2026 | **16 KB Page-Size Compatibility:** Pinned `androidx.graphics:graphics-path` to 1.1.0 (built with NDK r27) to clear Play Console's "may crash on 16 KB devices" warning. **Faster Language Switching:** Translations for the app and intro slides are cached per language after the first load. **How To Refresh:** Reworded the Overview, Navigation (now includes the first-launch tip) and Countdown Timer sections. |
| **v3.0** | Aug 10, 2026 | **Settings Redesign:** Bottom sheet replaces floating card — grouped controls, horizontal sliders, single-row color bubbles, Save/Cancel with snapshot restore. Script scrolls live behind panel for real-time feedback. **How To Overhaul:** Accordion-style layout with 4 sections (Main Screen, Organization, Editing, Launching) replacing 10 flat items. **System Theme Support:** How To screen now follows system dark/light mode via `MaterialTheme`. **Multi-Language Support:** Full app translation via Google Translate API — select language from Settings; menus, buttons, and labels update automatically. |
| **v2.4** | Aug 9, 2026 | **Countdown Timer:** Film-leader style (rings, sweep, auto-fit numbers, ACTION tone — higher pitched, 1.5x duration). **Drag-Fix:** Play button flips correctly on manual scroll. **Seamless Loop:** END marker with configurable gaps. **Window Size:** Starting size fitted to phone screen. **Auto-Start:** Launch auto-starts countdown + scroll; countdown on both Launch and in-window play. |
| **v2.3** | Feb 7, 2026 | **Mirror Mode:** Added horizontal text flipping for teleprompter glass; moved toggle to Settings Header for better UX. |
| **v2.2** | Feb 7, 2026 | **Settings Persistence:** Added `SharedPreferences` logic to automatically save and load Font, Speed, Opacity, and Color settings. |
| **v2.1** | Feb 6, 2026 | **UI/UX Overhaul:** Updated file icons to high-res PNGs (120dp); added "Hamburger" menus for Rename/Delete/Description options. |
| **v2.0** | Feb 5, 2026 | **Stability Patch:** Applied `singleTask` launch mode to resolve navigation loops; fixed "back button" behavior. |
| **v1.9** | Feb 4, 2026 | **Help System:** Integrated How To screen and context-aware help button. |
| **v1.0 - v1.8** | Feb 2, 2026 | **Initial Release:** Core architecture development; implementation of Project/Scene/Script hierarchy; creation of Floating Overlay Service. |

---

## 🛠 Installation
1.  Download the latest `.apk` from the **Releases** section.
2.  Install on your Android device (ensure "Install from Unknown Sources" is enabled).
3.  Grant **"Display Over Other Apps"** permission when prompted upon first launch.

> ⚠️ **Required Permission:** This app must have permission to **Draw Over Other Apps** (also called "Appear on top") to display the teleprompter overlay over your camera or other apps. Without this permission the floating overlay will not function. You can grant it at any time via **Settings > Apps > Teleprompter > Display Over Other Apps**.

---

*Developed by Ian T / IanT8055 for the DIY & Content Creator Community.*
