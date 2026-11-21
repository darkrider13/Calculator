<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Calculator app icon" width="120" height="120">

# Calculator

**A fast, dark, and beautifully minimal calculator for Android.**
Everyday math, scientific functions, unit and price conversion, age and BMI tools — all in one pocket-sized app.

![Platform](https://img.shields.io/badge/platform-Android%206.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Version](https://img.shields.io/badge/version-2.0-2F8408)
![Theme](https://img.shields.io/badge/theme-pure%20black-000000)

[**⬇️ Download the latest APK**](https://github.com/darkrider13/Calculator/releases/latest)

</div>

---

## ✨ Highlights

| | Feature | What it does |
|---|---|---|
| 🧮 | **Calculator** | Clean keypad with live expression and instant results |
| 🔬 | **Scientific mode** | Advanced functions one tap away |
| 🪟 | **Floating calculator** | Use the calculator in a small window on top of other apps |
| 🕘 | **History** | Every result is saved; tap **Use** to bring it back to the display |
| ✅ | **Verify** | Step through each number of a saved calculation, fix a wrong one, or remove it — the total updates automatically |
| ⚖️ | **Unit converter** | Weight and volume: gram, kilogram, milliliter, liter and poa |
| 🏷️ | **Price converter** | *Price for quantity* and *Quantity for money* — see what you pay or how much you get |
| 🎂 | **Age calculator** | Exact age in years, months and days, next birthday, day of birth and more |
| ⚕️ | **BMI calculator** | Enter height and weight to get your BMI and its category |
| 🔢 | **Numeral system** | Convert between Binary, Octal, Decimal and Hexadecimal |

## 🎨 Design

- **Pure black** theme that looks great on OLED screens and saves battery
- Rounded keys, soft green accents and large, readable numbers
- Smooth, lightweight animations and haptic feedback
- Built with **Jetpack Compose** — no heavy dependencies

## 📲 Installation

1. Open the [**Releases**](https://github.com/darkrider13/Calculator/releases/latest) page.
2. Download the latest `.apk` file.
3. Open it on your phone and allow *Install from this source* if Android asks.
4. Launch **Calculator** and enjoy.

> Updating? Just install the new APK over the old one — your data stays.

**Requirements:** Android 6.0 (API 23) or newer.

## 🔐 Permissions

| Permission | Why it is needed |
|---|---|
| Display over other apps | Shows the floating calculator window |
| Foreground service & notifications | Keeps the floating calculator running |
| Vibration | Key press feedback |
| Internet | Optional features that need a network connection |

## 🛠️ Build from source

```bash
git clone https://github.com/darkrider13/Calculator.git
cd Calculator
gradle assembleRelease
```

The APK is generated at `app/build/outputs/apk/release/`.
Every push to the repository also builds an APK automatically with GitHub Actions and publishes it under **Releases**.

## 🧰 Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Min SDK:** 23 · **Target SDK:** 34
- **CI/CD:** GitHub Actions

## 📁 Project structure

```
app/src/main/java/com/qalculator/app/
├── ui/       Screens, sheets, keypads and theme (Compose)
├── logic/    Calculation engine, converters, age and verify logic
└── ...       Activity, floating service, app state
```

## 🤝 Contributing

Found a bug or have an idea? Please [open an issue](https://github.com/darkrider13/Calculator/issues) — suggestions are always welcome.

---

<div align="center">

Made with ❤️ by [**darkrider13**](https://github.com/darkrider13)

</div>
