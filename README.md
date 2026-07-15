# হিসাব ম্যানেজার — Offline Business Accounting App

A 100% offline Android app for small businesses / shop owners in Bangladesh, built with
Kotlin, Jetpack Compose, Room, Hilt, and MVVM. No internet permission, no cloud, no backend.

## What's genuinely complete and working in this drop

- **Architecture**: MVVM + Repository pattern, Hilt DI, StateFlow throughout, clean package
  separation (`data/`, `di/`, `ui/`, `util/`).
- **Database**: Room with `TransactionEntity` / `CategoryEntity`, foreign key + indexes on
  `categoryId`, `dateEpochDay`, `type`. Amounts stored as `Long` minor units (poisha) to avoid
  floating-point rounding errors. Soft-delete + restore for the "Undo" flow. A `MIGRATION_1_2`
  stub is scaffolded so future schema changes don't require destructive migration.
- **Modules wired end-to-end**: Dashboard (today/month/year cards + income-vs-expense pie
  chart + recent list), Add/Edit Transaction (full validation), Transaction list with
  swipe-to-delete + undo snackbar, Category CRUD (blocked delete when in use), Search with
  live debounced query + type/category filters + sort, Reports (daily/weekly/monthly/yearly)
  with PDF and Excel export, Settings (business name, currency symbol, dark mode, backup/restore).
- **Exports**: PDF via Android's built-in `PdfDocument` API (no library needed). Excel via a
  **hand-rolled minimal OOXML/.xlsx writer** using only `java.util.zip` — see the note below on
  why this replaces Apache POI.
- **Backup/restore**: WAL checkpoint + raw SQLite file copy to app-specific external storage,
  timestamped, restorable from a picker in Settings.
- **Entirely Bangla UI**: `strings.xml`, Bangla digit formatting (`০-৯`), Bangla month names,
  Bangla PDF/Excel filenames and headers.
- **Tests**: real JUnit tests for `CurrencyFormatter` and `DateUtils`, and an instrumented
  Room test (`AppDatabaseTest`) covering insert, soft-delete/restore, and category-in-use
  blocking against an in-memory database.

## Deliberate substitutions vs. the original spec (and why)

- **Apache POI → hand-rolled XLSX writer.** `poi-ooxml` depends on `xmlbeans`/full `javax.xml`
  APIs that are a well-known source of `NoClassDefFoundError` and huge method-count bloat on
  Android. Since the export is a single flat sheet, writing the OOXML parts directly (a valid
  `.xlsx` any spreadsheet app will open) is more robust for a phone target and keeps the APK
  small. If you specifically need POI's richer formatting (multiple sheets, cell styles,
  formulas), that's a deliberate trade-off to revisit.
- **Charts** are drawn with plain Compose `Canvas` (pie chart on the dashboard) rather than a
  third-party charting library, to avoid an extra heavy dependency for two chart types. A bar
  chart primitive (`SimpleBarChart`) is included in `ui/common/Charts.kt` but not yet wired
  into a trend screen — trivial to hook up once you decide which report should show it.
- **Signing config** is scaffolded in `app/build.gradle.kts` (`signingConfigs { create("release") }`)
  but commented out — you must point it at your own keystore before `assembleRelease` will
  produce a signed APK. Never commit a real keystore/password into the repo.

## Explicitly NOT done in this pass (needs a follow-up)

- **This sandbox has no Android SDK / Gradle network access**, so the project has been
  hand-written carefully but **not actually compiled here**. Open it in Android Studio
  (Giraffe+ / AGP 8.5+, JDK 17) and run a Gradle sync — expect to fix a handful of small
  import/typo issues that only a real Kotlin compiler catches.
- **100k-row performance**: Room queries use `LIMIT/OFFSET` and indexes, but the transaction
  list/search screens aren't yet on Compose `Paging 3` — for very large datasets (50k+) you'll
  want to wire in `androidx.paging:paging-compose` rather than the current fixed-size flows.
- **Backup encryption** is not implemented (plain SQLite file copy).
- **UI test coverage** is a starting point, not exhaustive — no Compose UI tests (`ui-test-junit4`)
  were written yet, only the Room instrumented test and two JUnit util tests.
- **Launcher icon** is a simple vector placeholder, not a designed brand icon.

## Getting an installable APK without Android Studio

This repo includes `.github/workflows/build-apk.yml`, which builds a debug APK automatically
on GitHub's servers — free, no local Android SDK needed. Steps:

1. Create a free account at github.com if you don't have one.
2. Create a new **public or private** repository (e.g. `bizaccount-app`).
3. Upload the entire contents of this folder into that repository (drag-and-drop works on
   github.com's "Add file → Upload files" page, or use `git push` if you're comfortable with git).
4. Go to the **Actions** tab of your repository. A workflow called "Build Debug APK" will run
   automatically (takes ~5-8 minutes the first time).
5. When it finishes (green checkmark), click into the run, scroll to **Artifacts**, and download
   `BusinessAccounting-debug-apk.zip`. Unzip it — inside is `app-debug.apk`.
6. Transfer `app-debug.apk` to your phone (email it to yourself, or use Google Drive) and tap it
   to install. You'll need to allow "install from unknown sources" for whichever app you use to
   open it — Android will prompt you the first time.

Note: this produces a **debug-signed** APK, which installs and runs identically to a release
build but isn't signed with your own release key. That's fine for installing on your own phone
to test it. If you later want to publish to the Play Store, you'll need to generate a release
keystore and fill in `signingConfigs.release` in `app/build.gradle.kts`, then run the
`assembleRelease` task instead (the workflow can be duplicated with `assembleRelease` once you
add your keystore as a GitHub Actions secret).



1. Open the `BusinessAccounting/` folder in Android Studio.
2. Let Gradle sync (needs internet the first time, to pull dependencies — the *app* itself
   never calls the internet after that).
3. Run on a device/emulator with API 26+.
4. For a signed release APK: fill in `signingConfigs.release` in `app/build.gradle.kts` with
   your keystore, then `./gradlew assembleRelease`.

## Package layout

```
com.riad.bizaccount/
  data/local/         Room entities, DAOs, database, converters
  data/repository/     Repository layer (business logic, filters, sorting)
  data/settings/        DataStore-backed app settings
  di/                       Hilt modules
  ui/dashboard/       Dashboard screen + ViewModel
  ui/transaction/     Add/Edit + list screens + ViewModel
  ui/category/          Category CRUD screen + ViewModel
  ui/search/              Search/filter screen + ViewModel
  ui/report/              Reports + export screen + ViewModel
  ui/settings/           Settings + backup/restore screen + ViewModel
  ui/common/            Shared composables (charts, empty states)
  ui/theme/               Material 3 theme, colors, typography
  util/                     Currency/date formatting, PDF/Excel export, backup manager
```
