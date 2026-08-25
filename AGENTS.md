# AGENTS.md

## Project overview

Android port of Fjällkartan, an offline-capable Nordic topographic map app. The app uses Jetpack Compose and MapLibre Native to display maps from Kartverket and Lantmäteriet, slope shading, route measurements, elevation profiles, place search, saved routes and pins, and downloadable offline regions.

The iOS project in `../fjallkartan` is the behavioral and content reference.

## Build environment

- Application ID: `fjallkartan.fjallkartan`
- Minimum SDK: 26
- Target/compile SDK: 37
- Java: 21 on the development machine; source compatibility is Java 17
- MapLibre: 13.5.1
- Supported ABIs: `arm64-v8a` and `x86_64`
- Emulator: API 37 arm64, 16 KB pages, AVD `Medium_Phone`

Common commands:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease :app:bundleRelease
python3 tools/verify_16kb_alignment.py app/build/outputs/apk/release/app-release-unsigned.apk
```

Use `$HOME/Library/Android/sdk/platform-tools/adb` when `adb` is not on `PATH`.

## Key files

| File | Purpose |
|---|---|
| `app/src/main/java/fjallkartan/Fjallkartan/MainActivity.kt` | Compose activity entry point |
| `app/src/main/java/fjallkartan/Fjallkartan/map/MapScreen.kt` | Main Compose UI, MapLibre lifecycle, controls, overlays, sheets, onboarding and review triggers |
| `app/src/main/java/fjallkartan/Fjallkartan/map/MapViewModel.kt` | App state and integration of measurement, elevation, search, saved data and offline regions |
| `app/src/main/java/fjallkartan/Fjallkartan/map/MapStyle.kt` | Raster style and tile-source configuration |
| `app/src/main/java/fjallkartan/Fjallkartan/map/KartverketTileProxy.kt` | Loopback style/tile server and Kartverket no-data rewriting |
| `app/src/main/java/fjallkartan/Fjallkartan/map/MeasurementViews.kt` | Screen-space drawing and custom two-finger map gestures |
| `app/src/main/java/fjallkartan/Fjallkartan/measurement/DistanceMeasurement.kt` | Geodesic route state, formatting and simplification |
| `app/src/main/java/fjallkartan/Fjallkartan/elevation/` | Elevation tile decoding, caching, resampling and profile UI |
| `app/src/main/java/fjallkartan/Fjallkartan/search/PlaceSearch.kt` | Bundled SQLite FTS4 place search |
| `app/src/main/java/fjallkartan/Fjallkartan/saved/` | JSON models, atomic persistence and saved-data UI |
| `app/src/main/java/fjallkartan/Fjallkartan/offline/OfflineRegionRepository.kt` | MapLibre offline packs plus elevation prefetch and recovery |
| `app/src/main/java/fjallkartan/Fjallkartan/offline/OfflineDownloadService.kt` | Foreground download notification |
| `app/src/main/java/fjallkartan/Fjallkartan/product/` | Legend, guide, About/debug UI, localization and review prompting |
| `tools/build_android_places_db.py` | Converts the canonical iOS FTS5 database to Android-compatible FTS4 |
| `tools/import_legend_assets.py` | Imports the 43 iOS legend symbols |
| `tools/import_localizations.py` | Imports ten translations from the iOS string catalogue |
| `tools/verify_16kb_alignment.py` | Checks ELF LOAD segments in an APK |
| `fastlane/Fastfile` | Google Play metadata, build, internal upload, promotion and pull lanes |
| `fastlane/metadata/android/` | Checked-in source of truth for Google Play listing copy |
| `tools/import_play_metadata.py` | Converts the iOS App Store copy into Google Play metadata |

## Architecture notes

### Map and tile proxy

- MapLibre loads its style from the loopback proxy at `127.0.0.1:8062`; offline source discovery does not work reliably from a `file://` style.
- The proxy binds only to IPv4 loopback.
- Kartverket cream no-data pixels are made transparent only at zoom 15 and above.
- Sparse slope-tile 404 responses become transparent PNG tiles so offline packs can complete.
- Preserve upstream 429 and 5xx statuses as retryable failures.
- MapLibre routinely cancels tile requests, so proxy response writes must tolerate broken pipes.

### Measurement and elevation

- Distances are geodesic, not Mercator-space.
- Route strokes are simplified in screen space before conversion to coordinates.
- One finger draws; two fingers pan and zoom while measurement is active.
- Elevation tiles exist at z12 and encode `(R << 8 | G) - 32768`; alpha zero means no data.
- Profiles resample at 25 m and apply 4 m ascent/descent hysteresis.
- No-data gaps break elevation runs and must not be bridged.

### Search and persistence

- Android platform SQLite on the target emulator lacks FTS5, so the bundled database uses FTS4.
- Do not introduce a native SQLite dependency unless it is verified on 16 KB-page devices.
- Routes and pins are stored as one atomic JSON file per item.
- Coordinate JSON uses `latitude` and `longitude` for iOS compatibility.
- Saved data uses encrypted Android Auto Backup/device transfer. It intentionally does not implement live Google Drive or Firebase synchronization.

### Offline regions

- MapLibre downloads the style’s map resources for zooms 7 through 14.
- Elevation tiles are prefetched separately and merged into displayed progress.
- Explicit user-paused state is persisted separately from MapLibre’s native active state.
- Completed packs are inactive; do not interpret every inactive pack as paused.
- Incomplete, unpaused downloads resume after process restart.
- Elevation 404/410 responses create permanent no-data markers. Transient failures remain incomplete and retry.
- Delete elevation data only after MapLibre confirms pack deletion.

### Product and localization

- Product UI includes a six-page first-run guide, contextual hints, a searchable two-country legend, About/debug tools and Play in-app review prompting.
- Review eligibility requires at least three app opens plus either one completed offline region or three completed measurements of at least 500 m.
- Review prompts are limited to one per app version and at least 120 days apart.
- The iOS `Localizable.xcstrings` file is the translation source of truth.
- Supported languages are English, Danish, German, Spanish, Finnish, French, Italian, Norwegian Bokmål, Dutch, Swedish and Simplified Chinese.
- Finish replacing remaining hard-coded English UI strings before publishing.

## Release rules

- Keep the permanent application ID `fjallkartan.fjallkartan`.
- Keep the release 64-bit-only. The removed 32-bit MapLibre binaries use 4 KB ELF alignment.
- Every shipped native library must pass the 16 KB alignment script and Android `zipalign -c -P 16 4`.
- Release builds use R8 and resource shrinking.
- Use Google Play App Signing with a separate upload key.
- Google Play automation uses `GOOGLE_PLAY_JSON_KEY`, pointing to a service-account JSON file outside the repository.
- Signing values come only from these environment variables:
  - `FJALLKARTAN_KEYSTORE`
  - `FJALLKARTAN_STORE_PASSWORD`
  - `FJALLKARTAN_KEY_ALIAS`
  - `FJALLKARTAN_KEY_PASSWORD`
- Never commit keystores, passwords, API credentials or generated signing properties.
- Never push commits unless the user explicitly authorizes it.

Fastlane commands:

```sh
fastlane android check_metadata
fastlane android validate_credentials
fastlane android build
fastlane android metadata
fastlane android screenshots
fastlane android store
fastlane android internal
fastlane android promote from:internal to:production status:draft
fastlane android pull
```

## Change guidelines

- Preserve the iOS app’s behavior unless Android platform conventions require a deliberate difference.
- Keep map rendering updates source-driven; do not recreate style layers for routine state changes.
- Keep network and disk work off the main thread.
- Maintain process-restart behavior for offline downloads.
- Add targeted unit or instrumentation coverage for behavior changes.
- Validate map gestures and product UI on the API 37 arm64 emulator.
- Check portrait, landscape and enlarged font scale for UI changes.
