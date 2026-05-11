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

# Resolve case-insensitively. Accept the bare name, or with common suffixes
# ("App", "Demo") tacked on. Prefer exact match; fall back to prefix match.
RESOLVED="$(list_demos | awk -v n="$NAME" '
    function ic(a, b) { return tolower(a) == tolower(b) }
    {
        if (ic($0, n) || ic($0, n "App") || ic($0, n "Demo")) { print; exit }
    }' || true)"

# If still nothing, accept any case-insensitive prefix.
if [ -z "$RESOLVED" ]; then
    RESOLVED="$(list_demos | awk -v n="$NAME" 'tolower($0) ~ "^" tolower(n) { print; exit }' || true)"
fi

if [ -z "$RESOLVED" ]; then
    echo "No demo matching '$NAME'." >&2
    echo "Available:" >&2
    list_demos | sed 's/^/  /' >&2
    exit 1
fi

exec ./gradlew --console=plain -q ":examples:runExample" -Pexample="$RESOLVED"
