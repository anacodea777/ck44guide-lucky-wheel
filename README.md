# Vòng quay may mắn — Lucky Wheel / ভাগ্যের চাকা

A simple Android spinning-wheel picker.

- Works fully offline: no internet, no permissions (the CI build fails if the APK requests any).
- Two languages: **English** and **বাংলা (Bengali)**. It follows the phone language, or you can switch from the ⋮ menu → Language.
- Edit the wheel's items (2–24, one per line), remove the winning item after a spin, or reset to the defaults.
- Items are saved on the device with SharedPreferences.

## Build

**Android Studio:** open this folder, let Gradle sync, then Run.

**GitHub Actions:** every push to `main` builds a debug APK. Download it from the
run's *Artifacts* section (`lucky-wheel-debug-apk`).

**Command line** (JDK 17 + Android SDK + Gradle 8.9):

```bash
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

| File | Purpose |
| --- | --- |
| `app/src/main/java/.../WheelView.kt` | Custom view that draws and animates the wheel |
| `app/src/main/java/.../MainActivity.kt` | Spin button, result dialog, item editor, language switch |
| `app/src/main/res/values/strings.xml` | English text |
| `app/src/main/res/values-bn/strings.xml` | Bengali text |

Tech: Kotlin, AndroidX AppCompat, Material 3. minSdk 26, targetSdk 34.
