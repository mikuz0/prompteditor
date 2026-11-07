#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

JPACKAGE=/usr/lib/jvm/java-21-openjdk-amd64/bin/jpackage
JLINK=/usr/lib/jvm/java-21-openjdk-amd64/bin/jlink
if [ ! -x "$JPACKAGE" ]; then
    echo "ERROR: jpackage not found at $JPACKAGE"
    exit 1
fi
if [ ! -x "$JLINK" ]; then
    echo "ERROR: jlink not found at $JLINK"
    exit 1
fi
echo "Using jpackage: $($JPACKAGE --version)"
echo "Using jlink: $($JLINK --version)"

APP_NAME="prompteditor"
APP_VERSION="2.2.0"
MAIN_JAR="prompteditor-2.2.0.jar"
MAIN_CLASS="com.pandakind.prompteditor.PromptEditorAppKt"
INPUT_DIR="build/install/prompteditor/lib"
DEST_DIR="build/dist"
RUNTIME_DIR="build/jre-runtime"

if [ ! -d "$INPUT_DIR" ]; then
    echo "ERROR: $INPUT_DIR not found. Run ./gradlew installDist first."
    exit 1
fi

mkdir -p "$DEST_DIR"

# Step 1: Build custom JRE with jlink
echo "Creating custom JRE with jlink..."
rm -rf "$RUNTIME_DIR"
"$JLINK" \
    --module-path "$INPUT_DIR/javafx-base-21-linux.jar:$INPUT_DIR/javafx-graphics-21-linux.jar:$INPUT_DIR/javafx-controls-21-linux.jar" \
    --add-modules java.base,java.desktop,java.logging,java.sql,java.instrument,java.management,java.naming,java.scripting,java.security.sasl,java.xml,jdk.management,jdk.unsupported \
    --add-modules javafx.base,javafx.controls,javafx.graphics \
    --strip-java-debug-attributes \
    --strip-native-commands \
    --no-man-pages \
    --no-header-files \
    --output "$RUNTIME_DIR"

echo "Custom JRE created at $RUNTIME_DIR"
ls "$RUNTIME_DIR" | head -10

# Step 2: Package with jpackage using custom runtime
echo "Building .deb with jpackage..."
"$JPACKAGE" \
    --type deb \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input "$INPUT_DIR" \
    --main-jar "$MAIN_JAR" \
    --main-class "$MAIN_CLASS" \
    --runtime-image "$RUNTIME_DIR" \
    --linux-shortcut \
    --linux-package-name "$APP_NAME" \
    --linux-menu-group "Utility" \
    --linux-app-category "Utility" \
    --linux-deb-maintainer "you@example.com" \
    --linux-package-deps "libgtk-3-0, libxtst6, libxrender1" \
    --dest "$DEST_DIR"

echo "Built:"
ls -la "$DEST_DIR"/*.deb
