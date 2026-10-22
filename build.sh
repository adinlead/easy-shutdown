#!/usr/bin/env bash
# EasyShutdown Linux build script: compile and package fat-jar
# Artifact: target/EasyShutdown-1.0-SNAPSHOT.jar (run with java -jar, requires JRE 17+)

set -e
cd "$(dirname "$0")"

# --- locate java: PATH first, then JAVA_HOME fallback ---
if ! command -v java >/dev/null 2>&1; then
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
    else
        echo "[ERROR] java not found in PATH and JAVA_HOME is not set correctly." >&2
        echo "        Install JDK 17+ first (e.g. sudo apt install openjdk-17-jdk)" >&2
        exit 1
    fi
fi

echo "[1/2] Cleaning previous build..."
./mvnw -q clean

echo "[2/2] Compiling and packaging..."
./mvnw -q -DskipTests package

echo ""
echo "[OK] Build succeeded: target/EasyShutdown-1.0-SNAPSHOT.jar"
