#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node tests/ios/solver.test.cjs
node tests/ios/flat-contact.test.cjs
node tests/ios/export.test.cjs
node tests/ios/i18n.test.cjs
for file in ios-web/dist/*.js; do node --check "$file"; done
