# Nayan Kalpande Appreciation Society

Android app that monitors the US and NSE paper-trading bots
([kalpandeRepoEmaUs](https://github.com/AdityaKalpandeEren/kalpandeRepoEmaUs),
[kalpandeRepoEmaNse](https://github.com/AdityaKalpandeEren/kalpandeRepoEmaNse)):
today's and all-time P&L, every model and strategy by day, week, month and year,
open positions, bot health, outage notifications and a home-screen widget.
All figures are paper trades. Internal code name: AlgoPulse (`com.kalpande.algopulse`).

## How the data gets to the phone

```
production workflow (US/NSE Alert Scan)          unchanged: paper trading + Telegram
  │ saves state caches every run
  ▼
App Data workflow (.github/workflows/app-data.yml in each bot repo)
  │ starts after every production run (workflow_run)
  │ + every 10 min from cron-job.org (safety net: GitHub's own triggers are unreliable)
  │ READS the changed caches + production run history; never saves, trades or alerts
  ▼
release `dashboard-data` → dashboard.json.gz (public URL, no token on the phone)
  ▼
app: refreshes while open, checks in the background, alerts when a bot stops publishing
```

## Layout

| Path | What |
|---|---|
| `android/` | The app (Kotlin, Jetpack Compose, Material 3) |
| `pipeline/export.py` | Exporter: state files → one JSON. Standard library only |
| `pipeline/us-app-data.yml`, `pipeline/nse-app-data.yml` | The App Data workflow for each bot repo |

The bot repos carry copies: `pipeline/export.py` → `app_data/export.py`, and
`pipeline/<market>-app-data.yml` → `.github/workflows/app-data.yml`. Edit the
copies here, then copy them to both bot repos.

## cron-job.org safety net (one job per bot repo)

| Field | Value |
|---|---|
| URL | `https://api.github.com/repos/AdityaKalpandeEren/<repo>/actions/workflows/app-data.yml/dispatches` |
| Method | POST |
| Headers | `Authorization: Bearer <that repo's cron-job.org token>`, `Accept: application/vnd.github+json` |
| Body | `{"ref":"main"}` |
| Schedule | Every 10 minutes |

The token needs Actions: Read and write on that repo, the same as the scan job's token.
A `204` response means the run was started.

## Build

Needs JDK 17 and Android SDK 35.

```bash
cd android
./gradlew :app:testDebugUnitTest      # unit + screenshot tests
./gradlew :app:recordRoborazziDebug   # re-render screenshots to app/build/outputs/roborazzi
./gradlew :app:assembleRelease        # app/build/outputs/apk/release/app-release.apk
```

Release builds are signed with the machine's debug key unless
`~/.android/algopulse-release.properties` names a keystore (`storeFile`,
`storePassword`, `keyAlias`, `keyPassword`). Updates must be signed with the
same key as the installed app.
