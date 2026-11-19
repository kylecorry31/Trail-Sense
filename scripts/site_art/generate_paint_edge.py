"""Generate the hand-painted lower edge of the website's painted areas (site/src/assets/images/paint-edge.webp).

The edge is a mask tile, 20 CSS px tall (drawn at 2x), that repeats sideways with no seam. A brush edge
isn't straight or jagged: it drifts slowly, with a small tremor from the hand, and the paint thins out over
its last few pixels so the boards show through in fine streaks along the grain.

Requires: numpy, Pillow

Usage: python3 scripts/site_art/generate_paint_edge.py [--output PATH]
"""

import argparse
import os

import numpy as np
from PIL import Image

DEFAULT_OUTPUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "..", "site", "src", "assets", "images", "paint-edge.webp"
)

# Device pixels: the tile is 1200 x 20 CSS px.
WIDTH, HEIGHT = 2400, 40
EDGE_ROW = 24  # where the paint ends on average
DRIFT = 5.0  # how far the edge wanders from that row, slowly
TREMOR = 0.9  # and the small wobble from the hand
FEATHER = 4.0  # how far the paint thins out at its edge
STREAK_LENGTH = 220  # the length of a streak in the thinning paint, in px
STREAK_DEPTH = 0.5  # how much the streaks break up the thinning paint


def periodic_wave(rng, cycles):
    """Smooth noise across the tile that finishes a whole number of cycles, so the tile repeats."""
    phase = np.arange(WIDTH) / WIDTH * 2 * np.pi
    wave = np.zeros(WIDTH)
    for cycle in cycles:
        wave += rng.uniform(0.5, 1.0) * np.sin(cycle * phase + rng.uniform(0, 2 * np.pi))
    return wave / np.abs(wave).max()


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--output", default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    rng = np.random.default_rng(8)
    edge = (
        EDGE_ROW
        + DRIFT * periodic_wave(rng, [3, 5, 8, 13])
        + TREMOR * periodic_wave(rng, [31, 47, 61, 83, 109])
    )

    spectrum = np.fft.fft2(rng.standard_normal((HEIGHT, WIDTH)))
    along = np.fft.fftfreq(WIDTH)[None, :] * STREAK_LENGTH
    across = np.fft.fftfreq(HEIGHT)[:, None] * 0.7
    streaks = np.fft.ifft2(spectrum * np.exp(-(along**2) - across**2)).real
    streaks /= streaks.std()

    rows = np.arange(HEIGHT)[:, None] + 0.5
    depth = (edge[None, :] - rows) / FEATHER
    alpha = np.clip(depth + STREAK_DEPTH * streaks * np.clip(1 - depth, 0, 1) * 0.5 + 0.5, 0, 1)
    alpha = alpha * alpha * (3 - 2 * alpha)

    tile = np.zeros((HEIGHT, WIDTH, 4), dtype=np.uint8)
    tile[..., 3] = np.round(alpha * 255)
    Image.fromarray(tile, "RGBA").save(args.output, lossless=True, method=6)
    print(f"Wrote {os.path.normpath(args.output)} ({WIDTH}x{HEIGHT})")


if __name__ == "__main__":
    main()
