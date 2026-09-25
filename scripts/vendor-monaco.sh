#!/usr/bin/env bash
# Reproducibly vendors a pruned Monaco editor build into static/vendor/.
#
# Why vendored: Monaco loads many chunks and web workers at runtime, which a
# CDN <script integrity=...> (SRI) cannot cover. Serving them from our own
# origin keeps the strict CSP (script-src 'self' + two pinned CDNs).
#
# Why pruned: the Java editor never loads the TypeScript/CSS/HTML/JSON
# language workers (~18 MB) or UI translations, so they are removed.
#
# Usage: scripts/vendor-monaco.sh [version]   (default below)
set -euo pipefail

VERSION="${1:-0.56.0}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/src/main/resources/static/vendor/monaco-editor-$VERSION"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

curl -fsSL "https://registry.npmjs.org/monaco-editor/-/monaco-editor-$VERSION.tgz" \
    -o "$WORK/monaco.tgz"
tar -xzf "$WORK/monaco.tgz" -C "$WORK"

rm -rf "$TARGET"
mkdir -p "$TARGET"
cp -r "$WORK/package/min" "$TARGET/"
cp "$WORK/package/LICENSE" "$WORK/package/ThirdPartyNotices.txt" "$TARGET/"

VS="$TARGET/min/vs"
rm -f "$VS"/assets/{ts,css,html,json}.worker-*.js
rm -f "$VS"/language/*/*.worker.js
rm -rf "$VS/nls/lang"

# Give the editor worker a stable name; java-editor.js points
# MonacoEnvironment.getWorker at it (Monaco's own hashed worker URL doesn't
# resolve when it is loaded through the AMD loader).
mv "$VS"/assets/editor.worker-*.js "$VS/assets/editor.worker.js"

echo "Vendored monaco-editor $VERSION into $TARGET ($(du -sh "$TARGET" | cut -f1))"
