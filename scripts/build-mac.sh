#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd -- "$(dirname "$0")/.." && pwd)"
app_name="SpaceMonger1"
app_bundle="${app_name}.app"
app_path="$repo_root/$app_bundle"
build_dir="$repo_root/build"
classes_dir="$build_dir/classes"
jar_path="$build_dir/libs/spacemonger1.jar"
icon_source="$repo_root/src/main/resources/SpaceMonger.png"
icon_file="SpaceMonger1.icns"
runtime_dir="$build_dir/runtime-macos-arm64"
tmp_iconset=""
tmp_icns=""

require_cmd() { command -v "$1" >/dev/null 2>&1 || { echo "Missing required command: $1" >&2; exit 1; }; }

echo "Checking required tools..."
require_cmd java
require_cmd javac
require_cmd jar
require_cmd jlink
require_cmd sips
require_cmd python3
require_cmd /usr/libexec/java_home

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home)}"
java_version="$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F\" '/version/ {print $2}')"
java_major="${java_version%%.*}"
if [[ -z "$java_major" || "$java_major" -lt 25 ]]; then
  echo "Java 25+ required. Found: $java_version (JAVA_HOME=$JAVA_HOME)" >&2
  exit 1
fi

cleanup() {
  rm -rf "$runtime_dir" "$tmp_iconset" "$tmp_icns" "$classes_dir"
}
trap cleanup EXIT

echo "Cleaning old outputs..."
rm -rf "$app_path" "$runtime_dir"
mkdir -p "$classes_dir" "$build_dir/libs"

echo "Compiling sources..."
"$JAVA_HOME/bin/javac" -d "$classes_dir" $(find "$repo_root/src/main/java" -name '*.java')
cp -R "$repo_root/src/main/resources/." "$classes_dir/"
"$JAVA_HOME/bin/jar" --create --file "$jar_path" --main-class spacemonger1.App -C "$classes_dir" .

echo "Creating runtime image with jlink..."
"$JAVA_HOME/bin/jlink" \
  --module-path "$build_dir/libs:$JAVA_HOME/jmods" \
  --add-modules spacemonger,java.base,java.desktop,java.prefs,java.logging,java.xml \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "$runtime_dir"

echo "Preparing icon..."
if [[ ! -f "$icon_source" ]]; then
  echo "Icon not found at $icon_source" >&2
  exit 1
fi
tmp_iconset="$(mktemp -d "$build_dir/iconset.XXXXXX")"
tmp_icns="$(mktemp "$build_dir/SpaceMonger.XXXXXX.icns")"
for size in 16 32 64 128 256 512 1024; do
  sips -z "$size" "$size" "$icon_source" --out "$tmp_iconset/icon_${size}x${size}.png" >/dev/null
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

echo "Assembling app bundle..."
mkdir -p "$app_path/Contents/MacOS" "$app_path/Contents/Resources" "$app_path/Contents/Runtime"
cp "$jar_path" "$app_path/Contents/Resources/spacemonger1.jar"
cp "$tmp_icns" "$app_path/Contents/Resources/$icon_file"
cp -R "$runtime_dir/." "$app_path/Contents/Runtime/"

cat > "$app_path/Contents/MacOS/$app_name" <<'EOF'
#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/../Runtime/bin/java" \
  -Xdock:name="SpaceMonger1" \
  -Xdock:icon="$DIR/../Resources/SpaceMonger1.icns" \
  -Xms64m \
  -Xmx1024m \
  -jar "$DIR/../Resources/spacemonger1.jar"
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

echo "Computing final size..."
du -sh "$app_path"

echo "Done. App bundle at: $app_path"
