#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf build/tests
mkdir -p build/tests
javac --release 17 -d build/tests app/src/main/java/org/heatlab/SteakGeometry.java app/src/main/java/org/heatlab/ImmersedSteakSolver.java tests/ImmersedSteakSolverTest.java
java -cp build/tests org.heatlab.ImmersedSteakSolverTest
