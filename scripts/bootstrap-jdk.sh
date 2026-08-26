#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JDK_DIR="$ROOT/.jdk"
BOOTSTRAP_DIR="$ROOT/.bootstrap"
ARCHIVE="$BOOTSTRAP_DIR/temurin-jdk25.tar.gz"
TEMP_JDK="$ROOT/.jdk.tmp.$$"
URL='https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4.1_1.tar.gz'
SHA256='dbb698396d478e7fa2b1e50f4103324b2a99b90569ee27c33f2261f9215cf41e'

is_java_25() {
    [ -x "$1/bin/java" ] || return 1
    JAVA_VERSION=$("$1/bin/java" -version 2>&1) || return 1
    case "$JAVA_VERSION" in
        *'version "25"'*|*'version "25.'*) return 0 ;;
        *) return 1 ;;
    esac
}

if is_java_25 "$JDK_DIR"; then
    printf '%s\n' "$JDK_DIR"
    exit 0
fi

case "$(uname -s):$(uname -m)" in
    Linux:x86_64|Linux:amd64) ;;
    *) echo 'Automatic JDK bootstrap supports Linux x86_64 only; install Java 25 and set JAVA_HOME.' >&2; exit 1 ;;
esac

cleanup() {
    rm -rf "$TEMP_JDK"
    rm -f "$ARCHIVE"
}
trap cleanup EXIT HUP INT TERM
mkdir -p "$BOOTSTRAP_DIR"
rm -rf "$TEMP_JDK"
mkdir "$TEMP_JDK"
if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 -o "$ARCHIVE" "$URL"
elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
else
    echo 'curl or wget is required to bootstrap Java 25.' >&2; exit 1
fi
printf '%s  %s\n' "$SHA256" "$ARCHIVE" | sha256sum -c - >&2
tar -xzf "$ARCHIVE" -C "$TEMP_JDK" --strip-components=1
if ! is_java_25 "$TEMP_JDK"; then
    echo 'Downloaded archive did not contain a working Java 25 JDK.' >&2
    exit 1
fi
rm -rf "$JDK_DIR"
mv "$TEMP_JDK" "$JDK_DIR"
printf '%s\n' "$JDK_DIR"
