#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/ios-tests
javac --release 17 -d build/ios-tests app/src/main/java/org/heatlab/SteakGeometry.java app/src/main/java/org/heatlab/ImmersedSteakSolver.java tests/ios/Reference.java
java -cp build/ios-tests org.heatlab.Reference build/ios-tests/reference.bin
node tests/ios/solver.test.cjs
node tests/ios/export.test.cjs
node tests/ios/i18n.test.cjs
for file in ios-web/dist/*.js; do node --check "$file"; done
