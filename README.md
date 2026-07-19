# DeRaeve Fish Count — Android 13+

An unofficial Android client for selected public Alaska Department of Fish and Game fish-count projects.

## Current test build

- Android 13 minimum (`minSdk 33`), Android 16 target (`targetSdk 36`).
- Official ADF&G JSON export endpoint with a conservative HTML-table fallback.
- Room cache, transactional update storage, first-sync baseline protection, numeric fingerprints, and durable notification deduplication.
- Battery-conscious WorkManager synchronization with network constraints and exponential backoff.
- Alaska-time reporting, manual refresh, notification permission controls, grouped alerts, quiet-hour handling, project muting, and deep links.
- Settings-based notification simulations for new counts, revised counts, and multiple-location updates.
- Seven-day, 14-day, 30-day, and season chart ranges with previous-year comparison.
- Local-only followed-project and preference storage. No account or analytics SDK is included.

## Important limitations

The ADF&G export is a public endpoint but not a documented application API. The parser accepts several common object and array shapes and falls back to the official HTML table. Source-format changes can still require an app update. Counts do not guarantee fish at a particular fishing spot or fishing success.

The first successful sync establishes a baseline and intentionally does not announce historical records. A later changed official record is eligible for notification after it has been committed to Room.

## Build

Use Android Studio with JDK 17 and Android SDK 36, or run:

```bash
gradle testDebugUnitTest assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
