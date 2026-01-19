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

cleanup() {
  rm -rf "$BUILD_ROOT/spacemonger1" || true
}
cleanup
trap cleanup EXIT

mkdir -p "$DIST_ROOT"

cd "$ROOT"
./gradlew shadowJar
cp "$ROOT/build/libs/spacemonger1-all.jar" "$DIST_ROOT/spacemonger1-$VER.jar"

./gradlew jar
jlink --add-modules java.base,java.desktop,java.prefs,spacemonger \
      --output "$BUILD_ROOT/spacemonger1" \
      --compress zip-9 \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --module-path "$ROOT/build/libs"
cp "$SCRIPT_DIR/linux_x64/spacemonger1" "$BUILD_ROOT/spacemonger1"

rm "$DIST_ROOT/spacemonger1-${VER}-linux_x64.zip" || true
7z a -tzip -mx=9 "$DIST_ROOT/spacemonger1-${VER}-linux_x64.zip" "$BUILD_ROOT/spacemonger1"
cleanup