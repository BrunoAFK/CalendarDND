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
      "releaseNotes": "Optional multiline notes.",
      "sha256": "Optional SHA-256 hash for APK verification."
    }
  ]
}
```

Only `versionName` and `apkUrl` are required per release.
If present, `sha256` is used to verify the downloaded APK before install.

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

## Signature pinning (manual flavor)

Manual updates verify APK signatures against an allowlist of SHA-256 cert fingerprints.
Configure the allowlist with the `manualSignerSha256` Gradle property (comma-separated).

Get the SHA-256 fingerprint from a JKS keystore:

```
keytool -list -v -keystore /path/to/your.jks -alias your_alias
```

Or from a signed APK:

```
keytool -printcert -jarfile /path/to/CalendarDND-1.2.3-manual.apk
```

Use lowercase hex without colons. Example:

```
manualSignerSha256=aa6c3d794be571f320e938ee51ffcfb95181a1d79b616d324393df1b6f0b7f6b
```

Multiple fingerprints:

```
manualSignerSha256=firstsha256hex,secondsha256hex
```

Where to set it:
- `gradle.properties` (project or user-level) for a committed config, or
- pass it on the command line: `./gradlew assembleManualRelease -PmanualSignerSha256=...`

## Local release script

Use `scripts/release.sh` to create a release locally. It generates `update.json`, creates a tag,
pushes, creates a GitHub release, and uploads the APK + `update.json` assets.

Release notes live in `release-notes/<version>.md`. The script uses these files to build the
release notes and the update metadata list. The APK asset name is expected to be:

```
CalendarDND-<version>-manual.apk
```
