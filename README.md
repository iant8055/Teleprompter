### **Project Handover: Android Teleprompter Development**

**Introduction**
This project involves the development of a custom Android Teleprompter application using Kotlin and Jetpack Compose. The development hardware is an older laptop, requiring high efficiency and careful resource management during builds.

---

### **The Master Rule: Sequential Execution**
> **Mandatory Protocol:** You MUST provide only **one step at a time**. Do not generate multiple instructions or rationales in a single response. Wait for explicit confirmation that a step is completed before proceeding. Strictly follow all user-provided style and formatting instructions without deviation or summary.

---

### **Current Build Summary & Status**
* **Technical Issue:** The build process encountered a 30+ minute hang due to resource indexing conflicts after transitioning from `.jpg` to `.png` files.
* **Resolution Path:** Successfully performed "Invalidate Caches and Restart," followed by "Build > Clean Project," and a full system reboot to clear Gradle daemons.
* **Current State:** The project is cleaned and indexed. It is ready for the first **Run** command after the system restart.

---

### **Accomplishments & Version History**

| Version | Feature / Change | Description |
| :--- | :--- | :--- |
| **v1.0 - v1.8** | **Core Logic** | Implemented Project/Scene/Script hierarchy and Floating Overlay Service. |
| **v1.9** | **Help Integration** | Created `HowToActivity` and added a context-aware Help button. |
| **v2.0** | **Stability Fix** | Applied `singleTask` launch mode to `MainActivity` to resolve navigation loops. |
| **v2.1** | **UI/UX Overhaul** | Migrated to **PNG** assets; set icon sizes to **120dp**; implemented **Hamburger** menus; updated Help button to **Rounded Square FAB** with **"?"**. |

---

### **Technical Context for Implementation**
* **Main Activity:** Handles the grid view of projects and scenes.
* **Launch Mode:** `android:launchMode="singleTask"` in `AndroidManifest.xml`.
* **Resources:** Icons reside in `res/drawable` as PNG files.
* **UI Constants:** Icon size is **120dp**; Help button uses `RoundedCornerShape(12.dp)`.

---

### **Next Steps (The Morning Checklist)**
1.  **First Run:** Execute the green **Run** button and monitor for a successful build (estimated 9–16 minutes).
2.  **Visual Audit:**
    * Verify the **Help Button** is a rounded square with a large **"?"**.
    * Confirm all list icons are correctly rendered at **120dp**.
    * Verify card menus display **three horizontal lines (hamburger)**.
3.  **Navigation Check:** Confirm the "Back" arrow on the `HowToActivity` screen returns to the `MainActivity`.
4.  
