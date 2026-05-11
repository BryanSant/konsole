#!/usr/bin/env bash
# Run any konsole example app from the repo root.
#
#   ./demo.sh                  → list available demos
#   ./demo.sh PrideApp         → run PrideApp interactively
#   ./demo.sh Pride            → same (suffix optional)
#
# Why this script exists instead of `./gradlew :examples:runExample`:
# Gradle's JavaExec task captures stdin/stdout and does not pass the
# controlling terminal through to the child JVM. JLine FFM then reports
# `dumb` terminal type and size 0x0, breaking every interactive demo.
# This launcher uses Gradle only to compile + assemble the classpath, then
# `exec java` directly so the JVM inherits the calling shell's TTY.

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

# Compile so the runtime classpath references real .class files, then ask
# Gradle to print it (and the toolchain JDK launcher path). The cache is
# reused on repeat runs unless a .kt source is newer.
CLASSPATH_CACHE="examples/build/runtimeClasspath.txt"
LAUNCHER_CACHE="examples/build/javaLauncher.txt"
need_rebuild=0
if [ ! -f "$CLASSPATH_CACHE" ] || [ ! -f "$LAUNCHER_CACHE" ]; then
    need_rebuild=1
elif [ -n "$(find core/src rich/src textualize/src "$EXAMPLES_DIR" -newer "$CLASSPATH_CACHE" -name '*.kt' -print -quit 2>/dev/null)" ]; then
    need_rebuild=1
fi
if [ "$need_rebuild" = "1" ]; then
    ./gradlew --console=plain -q :examples:assemble
    ./gradlew --console=plain -q :examples:printRuntimeClasspath > "$CLASSPATH_CACHE"
    ./gradlew --console=plain -q :examples:printJavaLauncher    > "$LAUNCHER_CACHE"
fi

CLASSPATH="$(cat "$CLASSPATH_CACHE")"
JAVA_BIN="$(cat "$LAUNCHER_CACHE")"
MAIN_CLASS="tools.konsole.examples.${RESOLVED}Kt"

if [ ! -x "$JAVA_BIN" ]; then
    echo "demo.sh: toolchain java not found at '$JAVA_BIN'; falling back to PATH java." >&2
    JAVA_BIN="java"
fi

# Exec into java directly so the JVM inherits the calling shell's TTY.
# Using the toolchain JDK (Java 25) — JLine FFM requires Java 22+ and any
# older `java` on the user's PATH would fall back to a dumb terminal.
exec "$JAVA_BIN" \
    --enable-native-access=ALL-UNNAMED \
    -Dorg.jline.terminal.provider=ffm \
    -cp "$CLASSPATH" \
    "$MAIN_CLASS"
