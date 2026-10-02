# Heat Lab 3.0 validation

Validated 2026-10-02. This records an independent Java immersed-layer variant, not parity with a Julia execution or physical cooking measurements.

## Numerical tests

`bash scripts/test.sh`: **122,974 assertions passed** with the numerical sources compiled into the APK. Most assertions check individual cells/markers, not independent experiments. Tests cover regularized-delta moments, normal spread/interpolation adjointness, closed surface normals, mask solve, compatible uniform equilibrium, insulated uniform equilibrium, pan/air temperature bounds for the default case, exact flip events, and energy accounting including the numerical box term. Invalid coefficients and unordered schedules are rejected.

The flip test uses 17.123 and 40.987 s. At the first event, every masked field value exactly matches a run with no flip, demonstrating instantaneous field continuity. Contact weights move from negative to positive body z; subsequent heating differs. The second event returns the original contacting face.

## Independent Robin sphere comparison

Sphere radius 0.02 m, initial 0, ambient 1, diffusivity 1 m²/s, Fourier number 0.1. No pan. Eigenvalues solve `1 − λ cot(λ) = Bi`; the analytical temperature is the radial eigenfunction series. RMS is evaluated in the same physical central region `r ≤ R/2`, normalized by the unit imposed temperature difference.

| Bi | 12 cells/diameter | 20 cells/diameter | 32 cells/diameter |
|---|---:|---:|---:|
| 0.1 | 0.001589921 | 0.001429894 | 0.001105138 |
| 1 | 0.006995353 | 0.007525442 | 0.006082069 |
| 10 | 0.060376336 | 0.029123096 | 0.015649625 |

The finer result improves in all three cases, but Bi=1 is **not monotonic** across these grids. These numbers do not establish a convergence order or an error bound for the steak. Surface-trace calibration, finite auxiliary domain, surface quadrature and time step all contribute error.

## Default steak

Default 12-cell thickness, 87,912 volume cells, 1,748 surface markers; 600 s duration, flip at 300 s. With 20 output intervals:

- Resolved-core minimum: 50.495093 °C; maximum: 120.140292 °C; mean: 80.830348 °C.
- Numerical box exchange: −2458.485994 J.
- Energy accounting residual: −1.10e−10 J.

The last number accounts for the box term and is not a physical conservation/accuracy claim. The nonzero box exchange is a limitation of the finite auxiliary-domain discretization. The UI explicitly shows it. Core statistics omit the regularized two-cell surface band.

Additional 10-output-interval sensitivity runs:

| Cells/thickness | Core min °C | Core max °C | Core mean °C | Numerical box J |
|---|---:|---:|---:|---:|
| 8 | 47.8735 | 101.468 | 81.5428 | −1523.38 |
| 12 | 50.4953 | 120.142 | 80.8312 | −2458.52 |
| 16 | 47.5102 | 132.954 | 80.1418 | −2760.16 |

The core region changes with resolution and its extrema are not converged. Do not interpret the default minimum as a calibrated cooking prediction. Shorter steps at output times explain small differences between the table, tests and UI run.

## Android package

The SDK 35 direct build passed APK v2/v3 signature verification. Version code 3 / version name 3.0 uses the same local development key as the previous package. Removed plate source and launcher; the build cleans class/dex folders so obsolete classes cannot remain in the APK.

Android 15 x86_64 Pixel 6 emulator: in-place install succeeded, launcher opens directly to steak simulation, new pan/air/contact/flip controls render, default run reaches 600 s with Side B down and one flip. Displayed minimum is 50.49 °C, pan power +44.0 W, air power −10.7 W. Emulator testing does not establish performance on a physical Android phone.

Boundary CSV (1,748 markers) and history CSV (101 frames) were exported through the Android document picker and read back. Every exported flux matches the configured Robin law; area-integrated pan/air fluxes match history powers. World y/z signs reflect the flip, the 300 s event is present, and all accounting residuals are below 1e−6 J. AndroidRuntime log contained no crash. APK inspection confirms the sole launcher is SteakActivity and removed plate classes are absent.
