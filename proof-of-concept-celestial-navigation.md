# Proof of concept: celestial navigation

Location from a photo of the night sky: detect stars, identify them, then use gravity to find the zenith among the stars. Nothing needs a compass, and nothing needs GPS except as a reference to compare against.

## Status

| Check | Result |
|---|---|
| Synthetic, perfect data | under 5 m |
| Synthetic, handheld, uncalibrated phone (0.3° gravity bias) | 33 km median, 100% solved |
| Synthetic, handheld, calibrated gravity (0.05° noise) | 5 km median |
| Synthetic, ground mode, two 1 h sessions rotated 180°, bias solved | 36 m median (17–109 m, 6/6 trials) |
| Rendered star images (PSF, noise, hot pixel, light) through the detector | 0.10–0.16 px centroids, ~all stars matched |
| Realistic Providence sky photos (shot + read noise, streetlight, lens error) through the full image pipeline | 31–83 m; the same pipeline also ran end to end on the emulator |
| Real night sky on a phone | **Not tested yet** |

The single-photo handheld result is limited by the phone's accelerometer tilt bias, not by the stars. Every 0.1° of tilt error is about 11 km. Ground mode with a 180° rotation cancels that bias, and that's where GPS-class numbers come from in the synthetic tests.

## Using it at night

The tool is under **Tools > Location > Celestial Navigation** (experimental). The photo goes black once a capture loads. **Camera** returns to the live view.

**Single photo (handheld)**
1. Brace the phone and point at a star-rich area at least 30° above the horizon. Avoid the moon.
2. **Capture**. With **Auto detect**, it solves immediately. With **Manual taps**, tap 4+ stars, then **Solve**. With **Auto + manual edit**, auto-detect runs and then taps add or remove stars.
3. To tap more easily, use the fullscreen button on the photo, pinch to zoom, drag to pan, and double-tap to reset. Taps snap to the brightest pixel within the tap radius, and tapping an existing star removes it.
4. The results list every method (from the **Method** dropdown) with its error against GPS. The top lines are the primary method, the mean of all photos so far, and **Bias solved**. Bias solved appears once photos span different phone orientations, so take a few with the phone rotated (portrait, upside down, landscape).

**Ground (continuous)**: the most accurate mode
1. Set mode to **Ground**, detection to **Auto detect**, then press **Start**.
2. Lay the phone screen-down on a flat, steady surface (the back camera faces the sky). The screen stays on.
3. After 30 frames (configurable) it **beeps twice**. Rotate the phone 180° in place. It sees the motion, waits to settle, and starts session 2.
4. It **long-beeps** when the reversal result's standard error drops below the target (50 m by default). Results show each session's mean, the mean of all frames, and the reversal (bias solved) result.

**Settings** (gear icon) are saved between launches. Ones likely to matter outdoors:
- **Exposure / ISO**: default 500 ms at ISO 1600. Try 250–1000 ms. Handheld much past 500 ms blurs.
- **Photos stacked per capture**: good for ground mode.
- **Detection threshold / binning**: lower the threshold for a dark sky, raise it in a city.
- **Use lens intrinsic calibration**: if the focal length looks wrong, turn it off to use the FOV. The solver also fits the focal length and distortion (f and k1 are shown in the results).
- **Initial guess**: set to Time zone to prove GPS isn't helping.
- **Vertical deflection**: corrects with the slope of the app's built-in 1° geoid by default, or None.

Every single-photo solve saves a stretched JPG and a JSON (stars, gravity, compass, time, GPS, results) to `Android/data/com.kylecorry.trail_sense/files/celestial_navigation/`. Pull those and I can replay them offline. Ground frames aren't saved yet.

### How long to leave it on the ground

Simulated, 20 trials each, with a typical phone accelerometer (150 µg/√Hz) averaged over each half session, one frame every ~5 s, and the phone rotated 180° halfway:

| Total time | Median | p90 | Worst |
|---|---|---|---|
| 1 min | 113 m | 253 m | 310 m |
| 2 min | 72 m | 199 m | 210 m |
| 5 min | 75 m | 105 m | 137 m |
| 30 min (8 trials) | 29 m | — | 55 m |

At these lengths the limit is gravity noise averaged over each half (≈0.0016° at 30 s, ≈0.0007° at 2.5 min). One minute gets a similar typical fix but a long tail. Outdoors, add deflection-model error (~30–60 m), real-lens effects and accelerometer drift.

## How it works

1. **Capture**: CameraX still with manual exposure, ISO, focus at infinity and optional stacking. Gravity is averaged over the exposure (handheld) or over the whole session (ground).
2. **Detect** (`StarDetector`): 2×2 binning, box-filter background subtraction, a *local* noise threshold, peak finding, rejection of hot pixels and large blobs, then an intensity-weighted centroid.
3. **Identify** (Sol `Astronomy.identifyStars`): every pair of the 10 brightest detections is compared against catalog pairs (stars plus planets) by angular separation, allowing for focal-length uncertainty. Each pair gives a rotation. Rotations that put gravity's "up" more than 30° from the guess's zenith are rejected. The one that lines up the most other stars wins. Only angles between stars matter, so the compass is never used. Sol then refines the focal length, distortion k1 and the rotation.
4. **Camera frame**: Sol's camera frame is x right, y down, looking down +z. Device gravity (x, y, z) becomes (x, −y, −z), and compass device → ENU becomes `deviceToEnu × diag(1, −1, −1)` (`CameraFrame`).
5. **Locate**, by the method dropdown:
   - **Gravity zenith (photo)**: Sol `Astronomy.getLocationFromStars(image, …)` finds where gravity's "up" is the zenith, refitting the lens at the solved location.
   - **Star altitudes (gravity)**: altitudes from gravity for each identified star, then Sol's least-squares altitude fix.
   - **Altitude + compass azimuth**: the compass is also used for azimuths.
   - **Plate solve from sensors**: every detection becomes an altitude / azimuth from the sensors (or gravity alone), then Sol's `plateSolve` and `getLocationFromStars`.

   Ground mode always uses gravity zenith. Each frame is identified once with `getOrientationFromStars` at the best known location. As the session's gravity average improves, it's re-located with a single zenith step (`zenithStep`), with no re-identification.
6. **Combine** (`FixCombiner`, in Trail Sense): rejects outliers per session, then takes the mean. `meanWithDeviceBias` solves location plus a gravity error fixed in the phone's frame (accelerometer bias, camera-to-IMU misalignment). The error is observable once frames span different phone orientations. This is the generalized 180° reversal.
7. **Deflection** (Sol `Geophysics.getVerticalDeflectionModel` / `getGeodeticLocation`, via `DeflectionModels`): gravity gives the *astronomic* position, which is converted to geodetic. The built-in geoid is sampled up front because Sol's geoid lookup can't suspend.

## Findings

- **Lens refinement must happen at the solved location.** See Ported to Sol below.
- **Sol's quad plate solver misidentifies most stars.** On perfect synthetic data it got 2 of ~15 right, which probably explains the earlier branches' "varying luck". In the POC its output is only used as candidates and re-verified geometrically.
- **Sol's `getLocationFromStars` (altitudes only) sometimes diverges.** Its p90 error is thousands of km even on perfect data. It should probably start from a trilateration result and reject solutions on the wrong branch.
- **The detector must use local noise.** A single global noise level let a streetlight's glow produce dozens of false stars that crowded out real ones. That fixture went from 2,552 km to 83 m after the change.
- **Compass error doesn't matter with this approach**, and the rest of the accuracy comes down to the gravity vector. Long averages remove noise, and rotating the phone removes bias.
- The lens self-calibration recovered the generator's f = 3126 px / k1 = 0.015 exactly from the fixtures.

## Vertical deflection

Gravity gives the *astronomic* position. It's converted to geodetic with φ = Φ − ξ and λ = Λ − η / cos φ, where the deflection (ξ, η) comes from the slope of the app's built-in 1° geoid. That captures only the broad part of the deflection. Local effects from nearby terrain (often 100–300 m, more in mountains) remain. A high-resolution grid (GEOID18 cut to Rhode Island) was tried and then dropped in favor of the built-in geoid. The sign convention was verified end to end before it was dropped.

The AR tool doesn't need this. Deflection is a few arcseconds, roughly 1000× smaller than the AR tool's compass and sensor errors.

## AR tool calibration

See [proof-of-concept-ar-sky-calibration.md](proof-of-concept-ar-sky-calibration.md).

## Sol 19.3.0

Trail Sense now uses Sol `19.3.0`. Star identification, the gravity zenith fix, the orientation fit, the plate solver, the altitude-only fix, the camera model, rotations and vertical deflection all come from Sol. Trail Sense keeps photo capture, star detection, gravity tracking, combining fixes across frames, and the UI.

What Sol 19.3.0 changed for this work:
- `Astronomy.plateSolve` matches by pair separations and verifies every identification. `numNeighbors` is removed.
- `Astronomy.getLocationFromStars(List<StarReading>)` uses a least-squares fit for altitudes only.
- New: `Astronomy.identifyStars`, `getLocationFromStars(SkyImage, …)` and `getOrientationFromStars`.
- New: `Geophysics.getGeodeticLocation`, `getAstronomicLocation` and `getVerticalDeflectionModel`.

## Testing

```bash
scripts/run-unit-tests.sh "*CelestialNavigationSyntheticTest"
```

Prints error tables per scenario (~4 min). To make phone or emulator sky photos over Providence and load them in the app with a long press on **Capture** (set the deflection setting to None, since the photos simulate none):

```bash
CELESTIAL_FIXTURES=/tmp/celestial-fixtures ./gradlew testDebugUnitTest --tests "*CelestialNavigationSyntheticTest.generateDeviceFixtures" --rerun
```

```bash
adb push /tmp/celestial-fixtures/. /sdcard/Android/data/com.kylecorry.trail_sense/files/celestial_navigation/fixtures/
```

## Not done / next steps

- No real night test yet. Expect surprises from real lens distortion (may need k2), rolling shutter, and devices that ignore manual exposure.
- Ground frames aren't saved for offline replay, and a long ground session keeps every frame's stars in memory, which is fine for hours.
- The motion detector uses a simple threshold. A bump that doesn't change the phone's orientation keeps the old session.
- The handheld "Bias solved" result needs photos at quite different phone orientations, and it's untested on real data.
- The JVM tests don't cover the camera's intrinsic-calibration path.
