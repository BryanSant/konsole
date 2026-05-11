#!/usr/bin/env bash
# Run any konsole example app from the repo root.
#
#   ./demo.sh                  → list available demos
#   ./demo.sh PrideApp         → run PrideApp interactively
#   ./demo.sh Pride            → same (suffix optional)
#
# Demos that need a real terminal (those using systemDriver) take over the
# screen and block until you quit them (usually 'q' or Ctrl+C).

set -euo pipefail
cd "$(dirname "$0")"

EXAMPLES_DIR="examples/src/main/kotlin/tools/konsole/examples"

list_demos() {
    find "$EXAMPLES_DIR" -maxdepth 1 -name '*.kt' -printf '%f\n' \
        | sed 's/\.kt$//' \
        | sort
}

if [ $# -eq 0 ]; then
    echo "Available demos:"
    list_demos | sed 's/^/  /'
    echo
    echo "Usage: $0 <DemoName>"
    echo "Example: $0 PrideApp"
    exit 0
fi

NAME="$1"

# Resolve case-insensitively, allow omitting the "App" suffix.
RESOLVED="$(list_demos | awk -v n="$NAME" '
    BEGIN { IGNORECASE = 1 }
    $0 == n          { print; exit }
    $0 == n "App"    { print; exit }
    tolower($0) == tolower(n)        { print; exit }
    tolower($0) == tolower(n) "app"  { print; exit }
' || true)"

if [ -z "$RESOLVED" ]; then
    echo "No demo matching '$NAME'." >&2
    echo "Available:" >&2
    list_demos | sed 's/^/  /' >&2
    exit 1
fi

exec ./gradlew --console=plain -q ":examples:runExample" -Pexample="$RESOLVED"
