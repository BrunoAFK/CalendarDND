# Manual Updates (non-Store builds)

Manual update checks are enabled only for the `manual` flavor. The app pulls a small JSON file
from one or more URLs and decides how to notify based on semantic versioning.

## Update metadata JSON

Host a JSON file (GitHub Releases, your website, etc.) with this shape:

```json
{
  "releases": [
    {
      "versionName": "1.2.3",
      "versionCode": 123,
      "apkUrl": "https://example.com/CalendarDND-1.2.3.apk",
      "releaseNotes": "Optional multiline notes."
    }
  ]
}
```

Only `versionName` and `apkUrl` are required per release.

## Notification rules

- Major bump (1.x.x -> 2.x.x): in-app dialog only
- Minor bump (1.1.x -> 1.2.x): in-app dialog only
- Patch bump (1.2.2 -> 1.2.3): no notification

## Configure URLs

Update URLs are set in `app/build.gradle.kts` under the `manual` flavor:

```
buildConfigField(
    "String",
    "MANUAL_UPDATE_URLS",
    "\"https://github.com/BrunoAFK/CalendarDND/releases/latest/download/update.json,https://calendar-dnd.app/update.json\""
)
```

The app tries URLs in order and stops at the first valid response.

## Local release script

Use `scripts/release.sh` to create a release locally. It generates `update.json`, creates a tag,
pushes, creates a GitHub release, and uploads the APK + `update.json` assets.

Release notes live in `release-notes/<version>.md`. The script uses these files to build the
release notes and the update metadata list. The APK asset name is expected to be:

```
CalendarDND-<version>-manual.apk
```
