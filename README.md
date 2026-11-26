# Godelin Browser

Godelin is an Android browser project by Mad-Compile.

## Current foundation

- Kotlin and Jetpack Compose interface
- Android WebView browsing engine
- Google search from the address bar
- Back, forward, reload, and home controls
- Dark interface
- Minimum Android version: Android 8.0 (API 26)

## Build

Open this repository in Android Studio, allow Gradle sync to finish, then run the `app` configuration on an Android device or emulator.

GitHub Actions builds a debug APK on pushes and pull requests to `main`. Download it from the workflow run's **Artifacts** section.

## Project status

Early development build. Bookmarks, history, tabs, downloads, private browsing, settings, and a full security/privacy review are planned for later milestones.

## License

See [LICENSE](LICENSE). Third-party dependencies remain under their respective licenses.

## Publisher and ownership

**Godelin** is developed and published by **Mad Compile™**, based in India.

Copyright (C) Mad Compile. All Rights Reserved.

Godelin is proprietary software. See [LICENSE](LICENSE) for the project terms. Third-party components retain their own licences.

## Signed releases and in-app updates

Stable releases are published as signed APKs on [GitHub Releases](https://github.com/Mad-Compile/Godelin/releases). The Android app checks that public release feed when **More options → Check for updates** is opened. A user chooses when to download and install; Android's package installer always presents the final installation confirmation.

### One-time signing-key setup (required before the first stable release)

The release signing key is permanent. Every future update must be signed with the same key. Losing it means existing stable installs cannot receive in-place updates. Keep offline backups and restrict access to the key and its passwords.

1. On a trusted local computer with JDK 17+, run:
   ```bash
   bash scripts/create-release-keystore.sh
   ```
   The script prompts for the keystore and key passwords and creates `private-signing/godelin-release.jks`. Store an offline backup before proceeding.
2. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret** and create these four repository secrets:
   - `GODELIN_RELEASE_KEYSTORE_BASE64` — the complete Base64 output printed by the script.
   - `GODELIN_RELEASE_STORE_PASSWORD` — the keystore password.
   - `GODELIN_RELEASE_KEY_ALIAS` — `godelin-release` (unless you chose another alias).
   - `GODELIN_RELEASE_KEY_PASSWORD` — the private-key password.
3. Do not add the JKS, Base64 string, or passwords to the repository, a workflow file, build log, or issue. The release workflow grants `contents: write` only so it can publish the release, and removes the temporary keystore from its runner after the job.
4. Create and push a version tag after the secrets are set:
   ```bash
   git tag v0.1.1
   git push origin v0.1.1
   ```
   The workflow builds the release variant, verifies the APK signature, creates a SHA-256 checksum, and attaches both files to the matching GitHub Release.

The first install signed with this key establishes Godelin's stable update identity. Existing development/debug APKs use a different signing certificate; uninstall those test builds before installing the first stable signed release. After that first stable install, same-key releases with higher version codes can update in place.

### Local signed release build

Set the same four `GODELIN_RELEASE_*` environment variables locally, point `GODELIN_RELEASE_STORE_FILE` to your JKS file, then run `gradle :app:assembleRelease`. A release build without those signing inputs is not a distributable signed release.
