#!/usr/bin/env bash
# One-time setup: generate the OFFICIAL Gradle wrapper (gradlew, gradlew.bat,
# gradle/wrapper/gradle-wrapper.jar) for this project, then commit the results.
#
# Why this is a script and not committed files: the wrapper jar is a binary
# produced by Gradle itself and must not be hand-written or copied from an
# unknown source. It needs a real Gradle install (or Android Studio's bundled one).
#
# Usage (from the repository root):
#   scripts/bootstrap-gradle-wrapper.sh            # uses `gradle` on PATH
#   GRADLE_BIN=/path/to/gradle scripts/bootstrap-gradle-wrapper.sh
#
# The version below MUST match gradle/wrapper/gradle-wrapper.properties.
set -euo pipefail

GRADLE_VERSION="8.9"
GRADLE_BIN="${GRADLE_BIN:-gradle}"

cd "$(dirname "$0")/.."

if ! command -v "$GRADLE_BIN" >/dev/null 2>&1; then
  echo "ERROR: '$GRADLE_BIN' not found. Install Gradle $GRADLE_VERSION (or newer), e.g." >&2
  echo "  - macOS:   brew install gradle" >&2
  echo "  - Windows: choco install gradle   (or scoop install gradle)" >&2
  echo "  - Linux:   sdk install gradle $GRADLE_VERSION   (SDKMAN)" >&2
  echo "  - or point GRADLE_BIN at the Gradle bundled with Android Studio." >&2
  exit 1
fi

"$GRADLE_BIN" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin

chmod +x gradlew
echo
echo "Wrapper generated. Verify and commit:"
echo "  ./gradlew --version"
echo "  git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties"
echo "  git update-index --chmod=+x gradlew   # keeps the executable bit on Windows"
echo "  git commit -m 'Add Gradle wrapper'"
