#!/usr/bin/env bash

set -e

# Detect or locate gradle
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    # Fallback to downloading gradle wrapper if gradle binary not present in CI
    DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
    if [ ! -f "$WRAPPER_JAR" ]; then
        echo "Downloading gradle wrapper jar..."
        mkdir -p "$DIR/gradle/wrapper"
        curl -sLo "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar || true
    fi
    if [ -f "$WRAPPER_JAR" ]; then
        exec java -jar "$WRAPPER_JAR" "$@"
    else
        echo "Error: Gradle is not installed and wrapper jar could not be downloaded."
        exit 1
    fi
fi
