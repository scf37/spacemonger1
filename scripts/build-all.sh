#!/bin/bash

#!/bin/bash
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Error: Please provide version as an argument."
    exit 1
fi
VER=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."
rm -rf "$ROOT/dist/$VER"
cd $ROOT
./gradlew clean
./scripts/linux.sh $VER
./scripts/windows.sh $VER
./scripts/macos.sh $VER

