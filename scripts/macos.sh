#!/bin/bash
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Error: Please provide version as an argument."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."
DIST_ROOT="$ROOT/dist/$1"
BUILD_ROOT="$ROOT/build"
tmp_iconset="$BUILD_ROOT/iconset"
tmp_icns="$BUILD_ROOT/SpaceMonger1.icns"
app_name=SpaceMonger1
app_path="$BUILD_ROOT/spacemonger1.app"

cleanup() {
  rm -rf "$BUILD_ROOT/spacemonger1" || true
  rm -rf "$tmp_icns" || true
  rm -rf "$tmp_iconset" || true
  rm -rf "$app_path" || true
}
trap cleanup EXIT

mkdir -p "$DIST_ROOT"

require_cmd() { command -v "$1" >/dev/null 2>&1 || { echo "Missing required command: $1" >&2; exit 1; }; }

echo "Checking required tools..."
require_cmd java
require_cmd javac
require_cmd jar
require_cmd jlink
require_cmd python3
require_cmd convert
require_cmd 7z

cd "$ROOT"
./gradlew jar

build_image() {
  ARCH="$1"
  JDK_JMODS_DIR="$2"

  if [ ! -d "$JDK_JMODS_DIR" ]; then
      echo "JDK jmods not found: $JDK_JMODS_DIR"
      exit 1
  fi

  cleanup

  mkdir -p "$tmp_iconset"

  icon_source="$ROOT/src/main/resources/SpaceMonger_hres.png"
  if [[ ! -f "$icon_source" ]]; then
    echo "Icon not found at $icon_source" >&2
    exit 1
  fi

  for size in 16 32 64 128 256 512 1024; do
    convert "$icon_source" -resize "${size}x${size}^" -gravity center "$tmp_iconset/icon_${size}x${size}.png"
  done
  python3 - "$tmp_iconset" "$tmp_icns" <<'PY'
import pathlib, struct, sys
iconset, out = map(pathlib.Path, sys.argv[1:3])
entries = [
    ("icp4",  "icon_16x16.png"),
    ("ic11",  "icon_32x32.png"),   # 16@2x
    ("icp5",  "icon_32x32.png"),
    ("ic12",  "icon_64x64.png"),   # 32@2x
    ("icp6",  "icon_64x64.png"),
    ("ic07",  "icon_128x128.png"),
    ("ic13",  "icon_256x256.png"), # 128@2x
    ("ic08",  "icon_256x256.png"),
    ("ic14",  "icon_512x512.png"), # 256@2x
    ("ic09",  "icon_512x512.png"),
    ("ic10",  "icon_1024x1024.png"), # 512@2x
]
def chunk(tag, data):
    return tag.encode("ascii") + struct.pack(">I", len(data) + 8) + data
chunks = [chunk(tag, (iconset / name).read_bytes()) for tag, name in entries]
total = 8 + sum(len(c) for c in chunks)
with out.open("wb") as f:
    f.write(b"icns")
    f.write(struct.pack(">I", total))
    for part in chunks:
        f.write(part)
PY

  jlink --add-modules java.base,java.desktop,java.prefs,spacemonger \
        --output "$BUILD_ROOT/spacemonger1" \
        --compress zip-9 \
        --strip-debug \
        --no-header-files \
        --no-man-pages \
        --module-path "$JDK_JMODS_DIR:$ROOT/build/libs"


  mkdir -p "$app_path/Contents/MacOS" "$app_path/Contents/Resources" "$app_path/Contents/Runtime"
  cp "$tmp_icns" "$app_path/Contents/Resources/SpaceMonger1.icns"
  cp -R "$BUILD_ROOT/spacemonger1/." "$app_path/Contents/Runtime/"

  cat > "$app_path/Contents/MacOS/$app_name" <<'EOF'
#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/../Runtime/bin/java" \
  -Xdock:name="SpaceMonger1" \
  -Xdock:icon="$DIR/../Resources/SpaceMonger1.icns" \
  -XX:+UseSerialGC \
  --enable-native-access=spacemonger \
  -m spacemonger/spacemonger1.App
EOF
chmod +x "$app_path/Contents/MacOS/$app_name"

cat > "$app_path/Contents/Info.plist" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
 "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleExecutable</key>
  <string>SpaceMonger1</string>
  <key>CFBundleIdentifier</key>
  <string>org.spacemonger1.app</string>
  <key>CFBundleName</key>
  <string>SpaceMonger1</string>
  <key>CFBundlePackageType</key>
  <string>APPL</string>
  <key>CFBundleIconFile</key>
  <string>SpaceMonger1.icns</string>
</dict>
</plist>
EOF

  rm "$DIST_ROOT/spacemonger1-${1}-macos_$ARCH.zip" || true
  7z a -tzip -mx=9 "$DIST_ROOT/spacemonger1-${1}-macos_$ARCH.zip" "$app_path"
}

build_image "x64" "$HOME/dev/jdk-25-macos_x64/Contents/Home/jmods"
build_image "aarch64" "$HOME/dev/jdk-25-macos_aarch64/Contents/Home/jmods"