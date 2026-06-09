# 🔥 FF Sens Pro — Complete Android Studio Project

**AI-Powered Free Fire Sensitivity Calculator**
Calculates perfect sensitivity based on your real device hardware. No root, no hacks, 100% Play Store safe.

---

## 📁 Project Structure

```
FFSensPro/
├── build.gradle                          ← Root gradle
├── app/
│   ├── build.gradle                      ← App gradle (dependencies)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ffsens/pro/
│       │   ├── data/
│       │   │   └── DeviceDataCollector.kt  ← Reads ALL device specs
│       │   ├── logic/
│       │   │   └── SensitivityEngine.kt    ← Core calculation engine
│       │   └── ui/
│       │       ├── SplashActivity.kt
│       │       ├── MainActivity.kt
│       │       └── ResultActivity.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_splash.xml
│           │   ├── activity_main.xml
│           │   ├── activity_result.xml
│           │   └── item_sensitivity.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           ├── drawable/
│           │   ├── ic_logo.xml
│           │   ├── ic_device.xml
│           │   ├── ic_back.xml
│           │   ├── glow_circle.xml
│           │   ├── bg_icon_btn.xml
│           │   ├── badge_success.xml
│           │   ├── badge_warning.xml
│           │   └── badge_info.xml
│           ├── anim/
│           │   ├── fade_in_scale.xml
│           │   ├── slide_up.xml
│           │   ├── slide_up_delay.xml
│           │   ├── slide_in_right.xml
│           │   ├── slide_out_left.xml
│           │   ├── slide_in_left.xml
│           │   └── slide_out_right.xml
│           └── font/
│               └── FONTS_README.txt      ← Instructions to add Orbitron font
```

---

## ⚙️ SETUP STEPS IN ANDROID STUDIO

### Step 1 — Open Project
1. Open Android Studio
2. File → Open → Select the `FFSensPro` folder
3. Wait for Gradle sync to finish

### Step 2 — Add Orbitron Font (REQUIRED)
1. Go to https://fonts.google.com/specimen/Orbitron
2. Download the font family
3. From the ZIP, copy:
   - `Orbitron-Regular.ttf` → rename to `orbitron_regular.ttf`
   - `Orbitron-Bold.ttf`    → rename to `orbitron_bold.ttf`
4. Paste both files into: `app/src/main/res/font/`
5. Create `app/src/main/res/font/orbitron_regular.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
    <font android:fontStyle="normal" android:fontWeight="400"
          android:font="@font/orbitron_regular"/>
</font-family>
```
6. Create `app/src/main/res/font/orbitron_bold.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
    <font android:fontStyle="normal" android:fontWeight="700"
          android:font="@font/orbitron_bold"/>
</font-family>
```

### Step 3 — Add Launcher Icon
In Android Studio:
- Right-click `res` → New → Image Asset
- Icon Type: Launcher Icons (Adaptive and Legacy)
- Use the `ic_logo.xml` drawable or your own design
- Foreground: orange crosshair, Background: black

### Step 4 — Gradle Sync
- Click "Sync Now" if prompted, or Build → Rebuild Project

### Step 5 — Run / Build APK
- To test: Run → Run 'app' (on device or emulator)
- To build for Play Store: Build → Generate Signed Bundle/APK → Android App Bundle (.aab)

---

## 📲 WHAT THE APP DOES (100% SAFE, NO ROOT)

### Device Data Collected (read-only, no modification):
| Data | Source |
|------|--------|
| Brand & Model | `android.os.Build` |
| CPU ABI, cores, max frequency | `/sys/devices/system/cpu/` |
| Total RAM & available RAM | `ActivityManager.MemoryInfo` |
| Screen W×H pixels | `DisplayMetrics.getRealMetrics` |
| Screen DPI (x and y) | `DisplayMetrics.xdpi / ydpi` |
| Screen size in inches | Calculated from diagonal pixels ÷ avg DPI |
| Refresh rate (Hz) | `Display.getRefreshRate()` |
| Android version & SDK | `Build.VERSION` |
| Free Fire version | `PackageManager.getPackageInfo` |
| Free Fire install source | Detects if from Play Store or sideloaded |
| FF data/obb folders | Checks `/sdcard/Android/data|obb/com.dts.*` |

### Sensitivity Engine Formula:
The engine uses a **weighted multi-factor calibration**:
- **DPI normalization** → lower sens for higher DPI screens
- **Screen size factor** → bigger screens need higher sens
- **Refresh rate factor** → 90/120/165Hz gets tuned boost
- **RAM factor** → low RAM devices get slightly adjusted values
- **CPU performance score** → fast CPUs allow tighter calibration
- **Physical precision** → X/Y DPI ratio affects aim uniformity
- **Aspect ratio** → 18:9, 20:9 screens get tuned differently

### Output Range: 100–200 (matches Free Fire's sensitivity slider)
- General, Red Dot, 2x Scope, 4x Scope, Sniper (8x), Free Recoil
- Look Joystick, Fire Button

---

## 🔍 FREE FIRE VERSION CHECK
The app checks for:
- `com.dts.freefireth` (Free Fire Global)
- `com.dts.freefiremax` (Free Fire MAX)
- `com.dts.freefiremaxth` (Free Fire MAX TH)

It also:
- ✅ Shows version name & code
- ⚠️ Warns if installed from unofficial source (modded APK detection)
- ✅ Verifies data folder and OBB folder presence

---

## 🏪 PLAY STORE PUBLISHING CHECKLIST

### Before publishing:
- [ ] Change `applicationId` in `app/build.gradle` to your own (e.g., `com.yourname.ffsens`)
- [ ] Increment `versionCode` for each update
- [ ] Create a keystore: Build → Generate Signed Bundle
- [ ] Add Privacy Policy (required by Play Store — mention you read device specs)
- [ ] Add app screenshots (1080x1920 recommended)
- [ ] Target API 34 ✅ (already set)

### Privacy Policy template note:
> "FF Sens Pro reads device hardware information (CPU, RAM, screen specs) to calculate personalized sensitivity settings. No data is uploaded, stored externally, or shared with third parties."

---

## 🎨 Design Theme
- **Primary**: `#FF6B00` (Free Fire orange)
- **Background**: `#0A0A0A` (near black)
- **Cards**: `#1A1A1A` (dark gray)
- **Font**: Orbitron (futuristic gaming font)
- **Style**: Dark gamer UI with flame accents

---

## 📌 KNOWN ITEMS TO CUSTOMIZE
1. Replace `ic_launcher` with your actual app icon
2. Add your social media handle in the splash screen or footer
3. Optionally add AdMob banner ads (add `com.google.android.gms:play-services-ads` dependency)
4. Add a share button to share settings to WhatsApp/Instagram

---

*Built with ❤️ for the Free Fire community. 100% safe, no root, no hacks — just pure calibration science.*
