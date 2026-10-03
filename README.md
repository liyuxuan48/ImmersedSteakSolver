# Heat Lab

[Live app](https://liyuxuan48.github.io/heat-lab/) · [Android download](https://github.com/liyuxuan48/ImmersedSteakSolver/releases/latest) · [MIT license](LICENSE)

**3D transient heat conduction in an adjustable steak**, with pan contact, air convection and scheduled flipping. Includes a native Android app and an iPhone-compatible home-screen web app. Computation runs locally on the device; no API key or simulation server is needed.

## Try it

- **iPhone / browser:** open the [live app](https://liyuxuan48.github.io/heat-lab/). In Safari, choose Share → Add to Home Screen. This is a PWA, not a native iOS IPA.
- **Android 8.0+:** download the development APK from [Releases](https://github.com/liyuxuan48/ImmersedSteakSolver/releases/latest), transfer it to your phone and open it. Rebuilding with your own signing key may require uninstalling the distributed app first.
- **Local web preview:** install Python 3, then run `python3 -m http.server 8765 --directory ios-web/dist` and open `http://localhost:8765`. HTTPS is required for installed/offline PWA use on a phone.

The Android interface is English. The web app supports English and Chinese: use the header language selector. It initially follows your browser language and remembers your choice on this device. Switching languages preserves edited settings and the current simulation. Settings stay on the current device. Switching away pauses a run; refreshing or process termination loses simulation results. Export CSV/STL to retain them. Initial web loading needs a connection; later offline availability depends on browser caching and storage eviction.

## Features

- Adjustable length, width, thickness, roundness and outline variation.
- Finite pan contact and air convection, with configurable temperatures and transfer coefficients.
- Multiple exact-time flip events; temperature remains continuous in body coordinates.
- Rotatable surface mesh, XY/XZ/YZ temperature slices and resolved-core cold-point location.
- Core temperature CSV, boundary flux CSV, history CSV and surface STL export.
- Separate diagnostics for boundary energy input and numerical auxiliary-box exchange.

## Numerical method and limitations

An independent Java implementation and Float64 JavaScript port use regularized immersed single and double layers on a Cartesian volume grid with a closed surface mesh. The surface temperature uses a calibrated discrete trace closure. This is inspired by [JuliaIBPM / ImmersedLayers.jl](https://github.com/JuliaIBPM/ImmersedLayers.jl) and [ComputationalHeatTransfer.jl](https://github.com/JuliaIBPM/ComputationalHeatTransfer.jl), but does not execute those packages and is not their reference Neumann-constraint algorithm. No numerical parity with Julia is claimed.

This is a coarse research model. Core statistics exclude a two-grid-spacing surface band. Grid refinement is not always monotonic, and the finite auxiliary box introduces numerical heat exchange. There is no evaporation, radiation, crust, fluid flow, deformation or food-safety prediction. Material and contact defaults are illustrative. See [Android method and build details](docs/ANDROID.md), [numerical validation](VALIDATION.md) and [web validation](ios-web/README.md).

## Build and test

Requirements: JDK 17 or 21, Node.js 22+ for the cross-platform tests. No npm dependencies.

```sh
bash scripts/test.sh
bash scripts/test-ios.sh
```

To build Android, install SDK platform 35, build-tools 35.0.0 and `zip`, then:

```sh
export ANDROID_HOME=/path/to/android-sdk
bash scripts/build-apk.sh
```

Output: `artifacts/heat-lab-debug.apk`. The script generates a local development key in `build/apk/debug.jks`; keep it private and retain it for compatible updates. Keys and generated builds are not tracked. Android Studio / Gradle is an alternative; this project has no Gradle wrapper.

## Repository layout

| Path | Purpose |
|---|---|
| `app/` | Native Android UI, geometry and Java solver |
| `ios-web/dist/` | Deployable static web app and JavaScript solver |
| `tests/` | Analytical, operator, cross-language and export checks |
| `scripts/` | Numerical tests and Android SDK build |
| `VALIDATION.md` | Results and accuracy limitations |

The existing live demo is deployed from the separate [`liyuxuan48.github.io` repository](https://github.com/liyuxuan48/liyuxuan48.github.io/tree/master/heat-lab). Updating this source repository does not automatically replace that demo.

## Contributing

Issues and pull requests are welcome. Include a reproducible case with geometry, material properties, boundary conditions, flip schedule and resolution. For numerical changes, run both test scripts and report analytical error and energy diagnostics; a visually plausible temperature map is not sufficient validation. Browser changes should be checked at phone width and preserve worker responsiveness and relative asset paths.

## License

[MIT](LICENSE). Scientific references above are acknowledgements, not endorsements. No Julia package source is bundled.

### Web surface views and normal-angle pan contact

The web app offers surface-temperature and boundary-condition views alongside the interior temperature slice. Surface colors use the reconstructed marker temperatures and a labeled scale that adapts each frame. Pan Robin conditions apply to facets whose outward unit normals lie within 15 degrees of vertically downward (inclusive), including qualifying rounded rim facets. All other facets use air convection; setting the pan coefficient to zero enables oven-only convection everywhere. Flips exchange the two planar faces.

The web geometry is a rounded body clipped by planar caps: its level function is `max((r^p + |0.8 z/c|^p)^(1/p), |z/c|)`, where `r` is the normalized variable-outline radius and `c` is half-thickness. The angle-selected contact area depends on mesh resolution. No contact-depth parameter is used. This web update differs from the existing Android APK, which retains the earlier contact-band geometry.

Temperature slices color every interior cell, including the near-boundary band. Core statistics continue to exclude the two-grid-spacing surface band; near-boundary values are more sensitive to reconstruction error.
