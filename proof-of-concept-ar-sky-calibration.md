# Proof of concept: AR calibration from the sky

The Augmented Reality tool calibrates its compass heading from what the camera sees:
- **automatically** whenever the sun or moon is in view,
- **on demand** from the sun or moon anywhere in frame, with no need to center it,
- **at night** from a single photo of the stars.

The existing "center the sun / moon" calibration is unchanged and still available.

It builds on the celestial navigation POC ([proof-of-concept-celestial-navigation.md](proof-of-concept-celestial-navigation.md)).

## Status

| Check | Result |
|---|---|
| Star heading, synthetic, with an 8° compass error and 0.5° gravity error | True heading to **0.006° median, 0.04° worst**, 30/30 identified |
| Sun detection, synthetic frame with glare on a bright sky | Within 1 px of the true center |
| Crescent moon, synthetic | Center pulled 2.7 px toward the lit side at a 6 px radius (≈0.15° on a typical preview) |
| False targets (elongated light, dim frame) | Rejected |
| Emulator | Menu, star photo and sun photo run, and fail gracefully indoors |
| Real sun, moon or night sky on a phone | **Not tested yet** |

For comparison, typical phone compass error is several degrees, and the existing calibration depends on how well the user centers the sun or moon in the reticle.

## Using it

The calibrate button (compass icon, top left) opens a menu:

1. **Center the sun / moon**: the existing flow.
2. **Photo: stars (night, hold still)**: takes a 0.5 s, ISO 1600 photo and identifies the stars at your GPS location. A dialog shows how far off the compass was, how many stars were detected and identified, the fit error, and the sensor's tilt vs. the stars' tilt.
3. **Photo: sun / moon anywhere in frame**: finds the disc in the current preview and calibrates from it. A dialog shows the correction, the tilt error and the disc size.
4. **Auto calibrate from sun / moon in frame: ON/OFF**: toggles automatic calibration. It's on by default.

**Automatic calibration** runs about once a second while the camera is on:
- It only looks when the sun (or the moon, when the sun is down) is predicted to be on screen, away from the edges.
- It needs two detections in a row, under 3 s apart and agreeing within 1°, before applying anything. A reflection or light that happens to pass the disc checks won't trigger it.
- It applies at most once every 10 s, and shows a toast with the correction, such as "Auto calibrated from the sun (+3.2°)".
- The first calibration goes through the normal path, which respects "Use gyroscope after calibration". Later ones adjust the existing calibration, so a failed attempt never wipes a good one.
- It's skipped while the time override (clock button) is active.
- Long-pressing the calibrate button still resets the calibration. That also re-arms the first-calibration path.

## How it works

**Sun / moon (`ARDiscCalibrator`, `BrightDiscFinder`)**
1. Predict where the body should be on screen using the AR view's current orientation (`toPixel`).
2. Grab the preview bitmap and search around that point. The search radius covers a 20° compass error.
3. Find the brightest pixel, flood-fill the bright core, and reject it if it's too dim, too low in contrast, not round, too sparse, or larger than about 4°.
4. Map the disc's centroid back to a direction with the AR view's own camera mapping (`toCoordinate`). The correction is the difference between that bearing and the body's true bearing.

Because the same mapping draws the overlays, after calibrating the drawn sun or moon sits on the real one, wherever it is in the frame. The old method only matched at the reticle.

**Stars (`ARStarCalibrator`)**
1. Switch the camera to manual exposure, take the photo, and record the compass heading and gravity during the exposure.
2. Detect the stars, then identify them with Sol's `Astronomy.getOrientationFromStars`. The location is known, so gravity's zenith must be within 5°.
3. Sol fits the camera-to-sky rotation from the identified stars. The camera axis (+z in Sol's camera frame) gives the true bearing, which is compared with the compass.

Gravity is only used to narrow the search, so its error doesn't affect the heading.

## Files

- `tools/augmented_reality/domain/calibration/BrightDiscFinder.kt`: disc detection (pure Kotlin, unit tested).
- `tools/augmented_reality/domain/calibration/ARDiscCalibrator.kt`: sun / moon calibrator.
- `tools/augmented_reality/domain/calibration/ARStarCalibrator.kt`: star photo calibrator.
- `tools/augmented_reality/ui/AugmentedRealityView.kt`: `recalibrate()` adjusts the calibration without resetting it, and `calibrationOffset` exposes it.
- `tools/augmented_reality/ui/AugmentedRealityFragment.kt`:
  - method picker
  - auto-calibration timer
  - result dialogs
  - camera started with image capture enabled for the star photo
- `tools/celestial_navigation/infrastructure/StarPhotoCapture.kt`: shared photo capture and lens model, also used by the celestial navigation tool.
- Tests: `BrightDiscFinderTest` and `CelestialNavigationSyntheticTest.starHeading`.

## Known issues

- **Overexposed preview after a star photo (emulator):** the preview stayed bright until the tool was reopened, and restarting the camera in place didn't help. CameraX seems to keep the manual exposure options on the cached camera. Real phones should ignore those once auto exposure is back on, but this is unverified.
- **Daytime detection** assumes the sun is saturated in the auto-exposed preview. Lens flare or a sun behind thin cloud could bias or block it. Thresholds are hard-coded:
  - sun: peak ≥ 245 and contrast ≥ 30
  - moon: peak ≥ 150 and contrast ≥ 80
- **The moon's phase** biases its center toward the lit side, up to about a quarter of its diameter (≈0.15° at a crescent). That's still far below compass error, but it could be removed by fitting the limb circle.
- **Heading only:** the AR view only supports a bearing offset. The star result also reports the tilt error. Correcting tilt would need a full rotation offset in the AR view.
- **Time skew:** the preview frame and the orientation are read a few milliseconds apart. That's fine while holding still, but swinging the phone during auto calibration could add a small error. The two-detection agreement check limits it.
- **Image capture:** enabling it on the AR camera adds a CameraX use case. Watch for preview resolution or performance changes on older devices.

## Next steps

- Test outdoors: sun at different altitudes, moon phases, and a star photo at night. Compare against a known landmark bearing.
- If the sun works well, consider calibrating whenever the sun is anywhere in frame by default and dropping the reticle method.
- Add a tilt (full rotation) offset to the AR view so star calibration can fix tilt too.
- Planets (Venus, Jupiter) could use the disc calibrator at dusk.
