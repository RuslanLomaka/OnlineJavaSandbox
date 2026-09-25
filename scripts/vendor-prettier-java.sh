#!/usr/bin/env bash
# Reproducibly vendors an in-browser Java formatter: Prettier (standalone
# build) + prettier-plugin-java, bundled into plain ES modules under
# static/vendor/. The editor formats code entirely in the browser; there is
# no server-side formatting.
#
# prettier-plugin-java is pinned to 2.8.1, the newest release whose parser
# (java-parser) is pure JavaScript. 2.9+ parse with tree-sitter WebAssembly,
# which would require loosening the CSP with 'wasm-unsafe-eval'.
#
# Needs Node/npm only when run (esbuild is fetched by npx); nothing is added
# to the project build.
# Usage: scripts/vendor-prettier-java.sh
set -euo pipefail

PRETTIER_VERSION="3.9.9"
PLUGIN_VERSION="2.8.1"
ESBUILD_VERSION="0.25.10"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/src/main/resources/static/vendor/prettier-java-$PRETTIER_VERSION-$PLUGIN_VERSION"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cd "$WORK"
npm init -y >/dev/null
npm install --no-audit --no-fund --silent \
    "prettier@$PRETTIER_VERSION" "prettier-plugin-java@$PLUGIN_VERSION"

rm -rf "$TARGET"
mkdir -p "$TARGET"
cp node_modules/prettier/standalone.mjs "$TARGET/prettier-standalone.mjs"
npx --yes "esbuild@$ESBUILD_VERSION" node_modules/prettier-plugin-java/dist/index.js \
    --bundle --format=esm --platform=browser --minify --legal-comments=eof \
    --outfile="$TARGET/prettier-plugin-java.mjs" --log-level=warning
cp node_modules/prettier/LICENSE "$TARGET/LICENSE-prettier"
cp node_modules/prettier-plugin-java/LICENSE "$TARGET/LICENSE-prettier-plugin-java"

echo "Vendored Prettier $PRETTIER_VERSION + prettier-plugin-java $PLUGIN_VERSION into $TARGET ($(du -sh "$TARGET" | cut -f1))"
