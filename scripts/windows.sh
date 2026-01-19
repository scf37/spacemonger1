#!/bin/bash
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Error: Please provide version as an argument."
    exit 1
fi
VER=$1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."
DIST_ROOT="$ROOT/dist/$VER"
BUILD_ROOT="$ROOT/build"
JDK_JMODS_DIR="$HOME/dev/jdk-25-win/jmods"
if [ ! -d "$JDK_JMODS_DIR" ]; then
    echo "JDK jmods not found: $JDK_JMODS_DIR"
    exit 1
fi

cleanup() {
  rm -rf "$BUILD_ROOT/spacemonger1" || true
}
cleanup
trap cleanup EXIT

mkdir -p "$DIST_ROOT"

cd "$ROOT"
./gradlew jar
rm -rf "$DIST_ROOT/spacemonger1"
jlink --add-modules java.base,java.desktop,java.prefs,spacemonger \
      --output "$BUILD_ROOT/spacemonger1" \
      --compress zip-9 \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --module-path "$JDK_JMODS_DIR:$ROOT/build/libs"
cp "$SCRIPT_DIR/win_x64/spacemonger1.vbs" "$BUILD_ROOT/spacemonger1"
rm "$DIST_ROOT/spacemonger1-${VER}-win_x64.zip" || true
7z a -tzip -mx=9 "$DIST_ROOT/spacemonger1-${VER}-win_x64.zip" "$BUILD_ROOT/spacemonger1"