# Heat Lab 3.0 for Android

Offline, native Android **3D steak heat simulation** with an adjustable rounded shape, pan contact, air convection and timed flipping. All computation runs on the phone. No Julia installation, server, account or network permission is needed.

## Install

Copy `artifacts/heat-lab-debug.apk` to an **Android 8.0 or newer** phone and open it. Allow installation from the app opening the APK if Android prompts. This is a locally signed development APK. The included build retains the previous signing key for an in-place update; settings start with the new boundary defaults.

## Use

1. Open **Setup**. Set initial, pan and air temperatures; contact and air heat-transfer coefficients; contact-band depth; and comma-separated flip times in seconds (blank means none).
2. Adjust shape, resolution, duration and material properties. **Apply 3D case** clears results and creates the new grid.
3. Tap **Run 3D**. Drag the mesh, select XY/XZ/YZ slices, or use **Show coldest slice**. Amber indicates pan contact; teal indicates exposed surface.
4. Export **Core CSV**, **History**, **Boundary CSV** or **Mesh STL** through Android's file picker.

Default: nominal 120 × 80 × 25 mm rounded steak, 5 °C initially, pan 180 °C, air 25 °C, pan coefficient 500 W/(m² K), air coefficient 15 W/(m² K), 4 mm contact band, one flip at 300 s, 600 s duration. These are illustrative inputs, not measured material or contact properties. **Oven only** disables contact and uses air at 180 °C with coefficient 25 W/(m² K).

Each flip rotates the steak 180° about its length (x axis). The opposite body face contacts the pan. Temperature stays attached to the steak and is continuous at the flip. The mesh changes orientation; slices and cold-point coordinates remain in body coordinates. Pan and ambient temperatures remain constant. Timesteps land exactly on flip events.

The contact fraction is `w = smooth(1 − d/contactDepth) smooth(−nz_world)`, with `d` measured upward from the lowest point and clamped cubic smoothstep. This approximates a compliant contact patch; it does not simulate deformation. A zero contact band disables contact. At each marker the inward flux is

```text
q_in = w hc (Tpan − Ts) + (1 − w) ha (Tair − Ts)
```

The partially contacting region interpolates contact and convection. There is no radiation, evaporation, crust, fluid flow, fat/bone interface or food-safety/doneness prediction.

Applied settings persist. Switching apps pauses the run; results are lost after process destruction. Export captures the displayed snapshot. Core extrema/means exclude a band within two grid spacings of the surface; grey slice cells mark that band. They are not full-volume extrema or averages. A reported cold cell is the numerical minimum in this resolved core.

## Numerical method

This is an independent Java **regularized immersed-layer variant**, inspired by JuliaIBPM, not a port or execution of its Julia packages. A closed quadrilateral superellipsoid mesh supplies area-weighted normals to a separate Cartesian volume grid.

```text
u_t = alpha (L u + D R_n S) + R(q_in / (rho cp))
I u = (I H) S
L H = −D R_n 1
T = Tinitial + u/H    (resolved interior)
```

`u` is masked temperature excess and `S` is the interior surface excess, with a zero auxiliary exterior trace. `R`/`I` are four-point regularized-delta spread/interpolation; `D R_n` is a staggered-face double layer. A rank-one compatible normal-operator correction makes `R_n 1 = −G H`, preserving uniform equilibrium and adjoint consistency. The surface trace is reconstructed locally using `Iu/(IH)` and the Robin flux evaluated from it. This calibrated closure is **not the reference Julia Neumann-constraint algorithm** and numerical parity is not claimed.

Explicit Euler evaluates all terms at the same old time with
`dt <= 0.4 / (6 alpha/h² + 2 beta_max/h)`, `beta_max = max(hc,ha)/(rho cp)` for enabled boundaries. This conservative step heuristic is not a maximum-principle guarantee for arbitrary geometry/inputs. Approximately 100 display frames add shortened steps at output times; pause/resume or changed output intervals can slightly change results.

The finite auxiliary box can exchange numerical energy. Diagnostics separately report masked energy, integrated physical boundary input, numerical box exchange and their accounting residual. A small accounting residual only verifies bookkeeping; **box exchange remains a numerical error**, not physical heat loss. See `VALIDATION.md` for analytical error and limitations.

Default resolution is 12 cells through thickness. Compare resolutions before interpreting results. Limits: 250,000 cells, 4,000 markers, 20,000 estimated time steps. Coarse-grid refinement is not always monotonic; no accuracy claim is made for a real steak.

## Exports

CSV files start with `#` metadata; read with `pandas.read_csv(path, comment='#')`. Coordinates use metres; STL uses millimetres and body coordinates. Boundary CSV includes body/world positions, marker area, contact fraction, reconstructed surface temperature and inward heat flux. History includes core extrema/mean, cold-point body coordinates, flip count, instantaneous pan/air power, masked energy, cumulative boundary input, box exchange and accounting residual. A positive flux/power means heating.

## Build and verify

Use JDK 17 or 21, Android SDK platform 35, build-tools 35.0.0 and `zip`. Put the JDK `bin` on PATH:

```bash
export ANDROID_HOME=/path/to/android-sdk
bash scripts/test.sh
bash scripts/build-apk.sh
```

The build cleans compiled classes, uses aapt2/javac/D8/zipalign/apksigner, and writes the APK plus checksum under `artifacts/`. Keep `build/apk/debug.jks` to preserve update compatibility; it is not in the source archive.

Alternatively open in Android Studio with Gradle 8.11.1, Android Gradle Plugin 8.9.2 and JDK 17/21, then run `gradle :app:assembleDebug`. No wrapper is included. The distributed APK uses the direct SDK build.

Sources: `SteakGeometry.java` (geometry), `ImmersedSteakSolver.java` (numerics), `SteakActivity.java` (native UI), all under `app/src/main/java/org/heatlab/`. `tests/ImmersedSteakSolverTest.java` checks the numerical sources compiled into Android.

## iPhone version

`ios-web/` contains the Chinese iPhone home-screen web app (PWA), ported from the same numerical model. It is not an IPA. See `ios-web/README.md` for installation, local preview and validation limits. Run `bash scripts/test-ios.sh` with Java and Node on PATH for cross-platform tests.
