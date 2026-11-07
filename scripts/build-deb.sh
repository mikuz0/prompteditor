#!/usr/bin/env bash
set -euo pipefail

# ─── JDK 21 явно ─────────────────────────────────────────────────
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

JPACKAGE=/usr/lib/jvm/java-21-openjdk-amd64/bin/jpackage
if [ ! -x "$JPACKAGE" ]; then
    echo "ERROR: jpackage not found at $JPACKAGE"
    exit 1
fi
echo "Using jpackage: $($JPACKAGE --version)"

APP_NAME="prompteditor"
APP_VERSION="2.2.0"
MAIN_JAR="prompteditor-2.2.0.jar"
MAIN_CLASS="com.pandakind.prompteditor.PromptEditorAppKt"
INPUT_DIR="build/install/prompteditor/lib"
DEST_DIR="build/dist"

# ─── Step 1: пересобрать jar и installDist, игнорируя кеш ────────
echo "=== Rebuilding application from source (ignoring cache) ==="
./gradlew --no-daemon clean jar installDist --rerun-tasks

# ─── Step 2: проверить, что jar свежий ───────────────────────────
echo "=== Verifying fresh jar ==="
JAR_PATH="$INPUT_DIR/$MAIN_JAR"
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: $JAR_PATH not found. installDist failed?"
    exit 1
fi
echo "Jar: $JAR_PATH"
echo "Size: $(stat -c%s "$JAR_PATH") bytes"
echo "Modified: $(stat -c%y "$JAR_PATH")"
echo "SHA256: $(sha256sum "$JAR_PATH")"

# ─── Step 3: очистить старый .deb ────────────────────────────────
echo "=== Cleaning old .deb ==="
mkdir -p "$DEST_DIR"
rm -f "$DEST_DIR"/*.deb

# ─── Step 4: jpackage (без ручного jlink) ────────────────────────
echo "=== Building .deb with jpackage ==="
"$JPACKAGE" \
    --type deb \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input "$INPUT_DIR" \
    --main-jar "$MAIN_JAR" \
    --main-class "$MAIN_CLASS" \
    --linux-shortcut \
    --linux-package-name "$APP_NAME" \
    --linux-menu-group "Utility" \
    --linux-app-category "Utility" \
    --linux-deb-maintainer "you@example.com" \
    --linux-package-deps "libgtk-3-0, libxtst6, libxrender1" \
    --dest "$DEST_DIR"

# ─── Step 5: добавить postinst/postrm (симлинк + меню) ───────────
DEB_FILE=$(ls "$DEST_DIR"/*.deb | head -n 1)
echo "=== Injecting postinst/postrm into $DEB_FILE ==="
TMP_DIR=$(mktemp -d)
dpkg-deb -R "$DEB_FILE" "$TMP_DIR"

cat > "$TMP_DIR/DEBIAN/postinst" <<'EOF'
#!/bin/sh
set -e
ln -sf /opt/prompteditor/bin/prompteditor /usr/bin/prompteditor
if [ -f /opt/prompteditor/lib/prompteditor-prompteditor.desktop ]; then
    cp /opt/prompteditor/lib/prompteditor-prompteditor.desktop /usr/share/applications/prompteditor.desktop
fi
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database /usr/share/applications || true
fi
exit 0
EOF

cat > "$TMP_DIR/DEBIAN/postrm" <<'EOF'
#!/bin/sh
set -e
rm -f /usr/bin/prompteditor
rm -f /usr/share/applications/prompteditor.desktop
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database /usr/share/applications || true
fi
exit 0
EOF

chmod 755 "$TMP_DIR/DEBIAN/postinst" "$TMP_DIR/DEBIAN/postrm"

# Пересобрать .deb
DEB_TMP="$DEB_FILE.tmp"
dpkg-deb -b "$TMP_DIR" "$DEB_TMP"
mv "$DEB_TMP" "$DEB_FILE"
rm -rf "$TMP_DIR"

# ─── Step 6: финальный отчёт ─────────────────────────────────────
echo "=== DONE ==="
ls -la "$DEST_DIR"/*.deb
echo "Size: $(du -h "$DEST_DIR"/*.deb | cut -f1)"
echo "Deb contains postinst: $(dpkg-deb -e "$DEB_FILE" /tmp/debchk 2>/dev/null && ls /tmp/debchk/ | grep -q postinst && echo YES || echo NO)"
echo ""
echo "To install:"
echo "  sudo dpkg -r prompteditor  # remove old, if any"
echo "  sudo dpkg -i $DEST_DIR/$(basename "$DEB_FILE")"
echo ""
echo "To verify:"
echo "  which prompteditor"
echo "  prompteditor"
echo "  # or via app menu (Super → prompteditor)"
