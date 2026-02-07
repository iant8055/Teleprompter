# 📱 Android Teleprompter (Foldable Edition)

A specialized, lightweight teleprompter application designed for the **Samsung Galaxy Z-Fold 4** and standard Android devices. This tool features a floating overlay service, allowing content creators to read scripts while simultaneously recording via the camera app or streaming live.

---

## ✨ Key Features
* **Floating Overlay:** Reads scripts over any other application (Camera, Zoom, YouTube, etc.).
* **Foldable Optimized:** Responsive UI that adapts to both folded and unfolded (tablet) modes.
* **Mirror Mode:** Flip text horizontally for use with professional teleprompter glass/hardware.
* **Hierarchical Organization:** Manage scripts via **Projects > Scenes > Scripts**.
* **Customizable Display:**
    * Adjustable Font Size (12sp - 72sp)
    * Variable Scroll Speed (1x - 10x)
    * Window Opacity / Transparency Control
    * High-Contrast Text Colors
* **Persistent Settings:** Automatically remembers your font, speed, color, and mirror preferences.
* **Offline Privacy:** All data is stored locally on the device (`JSON` format); no internet connection required.

---

## 📜 Version History

| Version | Date | Changes & Features |
| :--- | :--- | :--- |
| **v2.3** | Feb 7, 2026 | **Mirror Mode:** Added horizontal text flipping for teleprompter glass; moved toggle to Settings Header for better UX. |
| **v2.2** | Feb 7, 2026 | **Settings Persistence:** Added `SharedPreferences` logic to automatically save and load Font, Speed, Opacity, and Color settings. |
| **v2.1** | Feb 6, 2026 | **UI/UX Overhaul:** Updated file icons to high-res PNGs (120dp); added "Hamburger" menus for Rename/Delete/Description options. |
| **v2.0** | Feb 5, 2026 | **Stability Patch:** Applied `singleTask` launch mode to resolve navigation loops; fixed "back button" behavior. |
| **v1.9** | Feb 4, 2026 | **Help System:** Integrated `HowToActivity` screen and context-aware "?" Help button. |
| **v1.0 - v1.8** | Feb 2, 2026 | **Initial Release:** Core architecture development; implementation of Project/Scene/Script hierarchy; creation of Floating Overlay Service. |

---

## 🛠 Installation
1.  Download the latest `.apk` from the **Releases** section.
2.  Install on your Android device (ensure "Install from Unknown Sources" is enabled).
3.  Grant **"Display Over Other Apps"** permission when prompted upon first launch.

---

*Developed by Ian T / IanT8055 for the DIY & Content Creator Community.*