# carCheer (加油记账)

An Android app focused on **fuel tracking and consumption analysis** — log every refuel, calculate consumption automatically, and understand your vehicle costs through charts.

[中文说明](README.md)

## Features

### 📝 Refuel Records
- Log date & time, station, fuel type, amount, volume, unit price, odometer, and full-tank flag
- Segment consumption is settled automatically between consecutive full-tank fills; the first tank is marked "not settled"
- Filter records by vehicle, year, and data type

### 🕘 History
- Time range filter: this month / last 3 months / last half year, or a custom start/end date
- Data type filter: all / highest price / lowest price / highest amount — matched records are highlighted and tagged "Top / Low"
- Summary card: fill count, total amount, total volume, weighted average consumption, highest/lowest price with dates, and highest single fill amount with its date
- One-tap switch between list and stats views

### 📊 Statistics
- Overview card: total fills, total spent (tap to view the amount in Chinese capital numerals), total volume, weighted average consumption
- Latest unit price vs. previous price with a rise/fall indicator
- Consumption trend line chart (this month / 3 months / half year / 1 year)
- Monthly summary: amount, volume, and distance per month

### ⚙️ Settings
- Multi-vehicle management with a default vehicle
- Data management: CSV export / import via the system file picker, and clear-all
- Theme color switcher and in-app Chinese / English language switch

## Screenshots

| Records | History list | History stats |
|---|---|---|
| ![Records](docs/screenshots/home.png) | ![History list](docs/screenshots/history_list.png) | ![History stats](docs/screenshots/history_stats.png) |

| Statistics | Settings |
|---|---|
| ![Statistics](docs/screenshots/stats.png) | ![Settings](docs/screenshots/mine.png) |

## Tech Stack & Architecture

- **Language / SDK levels**: Java, minSdk 24 (Android 7.0), targetSdk 34 (Android 14)
- **Architecture**: MVVM — View (Fragment/Activity) → ViewModel (LiveData) → Repository → DAO (Room)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) 2.6.1 with exported schema files (`app/schemas/`) and migration support
- **UI**: [Material Components](https://github.com/material-components/material-components-android) + ViewBinding; a single Activity with bottom-navigation fragments
- **Charts**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for the consumption trend line chart
- **File access**: Storage Access Framework (SAF) for CSV export/import — no storage permission required
- **Localization**: AppCompat per-app locales for in-app Chinese / English switching
- **Unit tests**: the statistics core (`StatsCalculator`, `FuelCalculator`, etc.) is plain Java, covered by 33 JUnit test cases

```
app/src/main/java/com/carcheer/app/
├── data/
│   ├── dao/            # Room DAOs (records, vehicles)
│   ├── entity/         # Entities: RefuelRecord, Vehicle
│   └── AppDatabase.java
├── ui/
│   ├── history/        # History (list/stats toggle)
│   ├── mine/           # Settings, vehicle & data management
│   ├── record/         # Record list, add/edit screen
│   ├── stats/          # Statistics (overview / trend chart / monthly)
│   └── vehicle/        # Vehicle CRUD
├── util/               # Pure-Java calculators (fuel, stats, ranges, RMB uppercase, CSV)
├── viewmodel/          # ViewModels (LiveData)
└── ui/MainActivity.java
```

## Build & Run

Requirements: JDK 17, Android Gradle Plugin 8.2.2 (Gradle 8.2+), Android SDK 34.

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew test

# Signed release build: create keystore.properties in the project root (do not commit):
#   storeFile=...
#   storePassword=...
#   keyAlias=...
#   keyPassword=...
./gradlew assembleRelease
```

Alternatively, open the project in Android Studio, sync, and hit Run.

## License

This project is released under the [MIT License](LICENSE).
