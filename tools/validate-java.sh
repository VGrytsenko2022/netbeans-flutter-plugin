#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/target/manual-classes"
rm -rf "$OUT" && mkdir -p "$OUT"
find "$ROOT/flutter-core-api/src/main/java" "$ROOT/flutter-sdk/src/main/java" "$ROOT/flutter-project/src/main/java" "$ROOT/dart-analysis/src/main/java" "$ROOT/flutter-run/src/main/java" "$ROOT/flutter-designer/src/main/java" -name '*.java' > "$ROOT/target/sources.txt"
javac --release 21 -d "$OUT" @"$ROOT/target/sources.txt"
echo "Java core modules compile successfully."
