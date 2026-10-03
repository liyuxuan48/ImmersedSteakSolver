# Constrained 3D immersed-layer heat solver

The web solver now solves for the surface temperature jump instead of assigning it from a mask-normalized temperature sample. The old calibration and its rank-one normal-operator correction have been removed. The Android APK has not been changed.

This is an independent 3D Robin extension of the [JuliaIBPM immersed-layer Neumann formulation](https://juliaibpm.github.io/ImmersedLayers.jl/stable/manual/heatconduction-neumann/), using the same single-layer, double-layer and corrected normal-gradient structure. It is not a direct port of Julia's complete time integrator, and no numerical parity with a Julia run is claimed.

## Unknowns, signs and units

All temperatures below are departures from the initial reference temperature T0. The auxiliary exterior starts at zero departure. The outward normal points out of the steak. The Cartesian grid has spacing Δx; facet i has area A_i. Surface quantities use the symmetric scaling s_i = sqrt(A_i / Δx³).

- u: regularized two-sided grid temperature departure.
- S: scaled interior-minus-exterior temperature jump. This is the negative of the temperature-jump convention in the linked reference.
- G and D: staggered gradient and divergence, with D = -Gᵀ including the outer-box faces.
- R and E = Rᵀ: scalar spreading and sampling.
- N and Nᵀ: normal-vector spreading and sampling.
- α = k/(ρ cp); β_i = h_i/(ρ cp).
- e_i = s_i (T_environment,i - T0).

Four-point delta weights and surface-area scaling are used consistently in each transpose pair. Define scaled interior and exterior traces:

```
τ_i = E u + S/2
τ_e = E u - S/2
f_i = β_i (e_i - τ_i)
f_e = -β_e τ_e
```

Here f_i is physical inward flux divided by ρ cp and multiplied by s_i. f_e is the auxiliary exterior flux in the reference normal convention. The physical temperature shown on the surface is T0 + τ_i/s_i. Pan selection still uses outward unit normals within 15° of vertically downward; flipping reverses the relevant world normal. Other facets receive air convection.

## Coupled boundary equation

At every initialized, advanced or flipped state, the solver satisfies:

```
u_t = α D(G u + N S) + R(f_i + f_e)
2 α Nᵀ(G u + N S) = f_i - f_e
```

Substitution of the Robin laws gives the surface system:

```
[α NᵀN + diag((β_i + β_e)/4)] S
  = β_i e_i/2 + (β_e - β_i) E u/2 - α NᵀG u
```

The sparse Gram matrix NᵀN is assembled once from overlapping kernel supports. A diagonally preconditioned conjugate-gradient solve uses the previous jump as an initial guess. The final true residual is recomputed; non-convergence raises an error, with no fallback to the old closure. The algebraic tolerance is max(1e-13, 1e-10 times the right-hand-side norm).

The displayed physical residual is independently reconstructed from the corrected normal gradient and both fluxes:

```
max_i |(ρ cp/s_i) [2 α Nᵀ(G u + N S) - f_i + f_e]_i|  [W/m²]
```

Setting β_e = 0 recovers the zero-exterior-flux Neumann constraint. Both sides insulated additionally sets β_i = 0. This limit is available through `exteriorRobinFactor: 0` for numerical testing, but is not the application's default.

## Why the default auxiliary exterior is Robin

Using zero exterior flux with the existing dense 3D markers produced large oscillations in the solved surface traces at the sharp pan/air transition. Small constraint residuals did not prevent those artifacts. The default uses a homogeneous auxiliary Robin condition with β_e = 50 α/Δx, or h_e = 50 k/Δx. An exactly zero exterior field satisfies this condition; it does not represent another physical heat-transfer mechanism acting on the steak.

This choice is a numerical auxiliary-domain treatment, not the two-Neumann-boundary example unchanged. It improves conditioning and suppresses the observed oscillations without clipping temperatures or changing the 15° pan mask. Finite-grid sensitivity and auxiliary exchange still need to be examined. The parameter is recorded in the configuration; tests cover factors 25, 50 and 100.

## Time integration and stability

The volume equation uses explicit Euler. Diffusion, both layers and the boundary solution are all evaluated at the same state. A new surface system is solved after every update, including at an exact flip time. The grid field and each cell's peak-temperature history remain continuous at a flip; the boundary jump and flux may change immediately.

For homogeneous environments, the eliminated spatial operator is dissipative. Its quadratic form is the minimum over S of

```
α ||G u + N S||² + ||sqrt(β_i)(E u + S/2)||²
                     + ||sqrt(β_e)(E u - S/2)||².
```

Choosing S = 2 E u gives an upper bound independent of β_e. Bounds on the scalar and normal Gram norms produce a conservative explicit time-step restriction. The earlier physical diffusion/transfer restriction is retained when smaller. Time steps are also truncated at flip and output times.

## Field display and energy accounting

Surface temperature comes from the solved two-sided trace. Interior slice values continue to divide the regularized field by the discrete interior mask. This is a display reconstruction, not the boundary closure; near-boundary values remain less accurate. Core statistics exclude two grid spacings near the surface. Cookedness records the maximum reconstructed interior temperature at every time step.

Pan/air powers and boundary CSV fluxes contain physical interior transfer only. The volume equation also includes the auxiliary exterior layer flux. Its accumulated contribution is included with outer-box exchange in the existing auxiliary-domain energy column. Thus a small accounting residual does not demonstrate small auxiliary-domain error or correct physical temperatures.

## Verified tests and results

Run `bash scripts/test-ios.sh` for the test suite and `node tests/ios/constraint-report.cjs` for the default-run sensitivity report.

Tests check the Gram matrix against independently applied spreading/sampling, both transpose identities, physical Robin flux exports, independently reconstructed boundary residuals before and after flips, the exterior-Neumann limit, homogeneous dissipation, uniform equilibrium, time-step sensitivity, energy accounting, peak-temperature retention, translations and exports.

Sphere test: R = 0.02 m, α = 1 m²/s, Fourier number 0.1, unit imposed temperature difference. All resolutions are sampled in the same physical region r ≤ R/2. Errors are dimensionless RMS against the Robin eigenfunction series.

| Bi | 12 cells across diameter | 24 cells | 32 cells |
|---|---:|---:|---:|
| 0.1 | 0.003599 | 0.001731 | 0.001301 |
| 1 | 0.032942 | 0.015116 | 0.011190 |
| 10 | 0.134708 | 0.056519 | 0.040559 |

The constrained method is **not uniformly more accurate on coarse grids** than the former calibrated closure. In particular, its 12-cell Bi=10 error is substantial. Refinement reduces all three benchmark errors, but this is not full steak validation.

Default 600 s steak, 12 cells through thickness, one flip at 300 s:

| Auxiliary factor | Core mean (°C) | Surface minimum over run (°C) | Maximum constraint residual (W/m²) |
|---|---:|---:|---:|
| 25 | 78.0683 | 4.7253 | 1.37e-4 |
| 50 (default) | 78.1124 | 4.9390 | 2.32e-4 |
| 100 | 78.1349 | 4.9909 | 5.25e-4 |

At factor 50, physical boundary input was 47,272.95 J and auxiliary-domain exchange was 7,324.69 J (about 15.5% of physical input). This is a significant finite-grid limitation, despite an accounting residual below 1e-6 J. The surface minimum shows a small undershoot below the initial 5°C, which is reported rather than clamped. Halving the time step in the 8-cell, 30 s double-flip case changed core values by at most 0.3711°C.

These are implementation and limited numerical-convergence checks. No physical cooking-time accuracy, Julia parity, Safari performance, or food-safety validity is established by them.
