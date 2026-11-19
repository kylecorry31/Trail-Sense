"""Generate the wood board tiles used by the website (site/src/assets/images/wood.webp, wood-paint-dark.webp,
wood-paint-light.webp).

The tile holds four boards, each 160 CSS px tall (drawn at 2x), with fine, gently waving grain lines
and thin seams. Each line is a sum of waves that complete a whole number of cycles across the tile, so
the tile repeats sideways with no visible seam, and every board has its own lines so the repeat is
hard to spot. The board height must match the seam spacing used by the site's CSS.

wood.webp is the boards themselves. The two paint tiles are the same grain and seams, for painted areas
that show the boards through their paint without changing the paint's colors: light marks on black to
blend over dark paint (the hero's hills, the treeline and the footer), and dark marks on white to blend
over light paint (the sky and the header).

Requires: numpy, Pillow

Usage: python3 scripts/site_art/generate_wood.py [--output PATH] [--dark-paint-output PATH] [--light-paint-output PATH]
"""

import argparse
import os

import numpy as np
from PIL import Image

IMAGES_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "site", "src", "assets", "images")
DEFAULT_OUTPUT = os.path.join(IMAGES_DIR, "wood.webp")
DEFAULT_DARK_PAINT_OUTPUT = os.path.join(IMAGES_DIR, "wood-paint-dark.webp")
DEFAULT_LIGHT_PAINT_OUTPUT = os.path.join(IMAGES_DIR, "wood-paint-light.webp")

# Device pixels: 1434 x 160 CSS px per board.
WIDTH, BOARD_HEIGHT = 2868, 320
BASE = np.array([200, 177, 138.0])  # Wood Carved, slightly darker
LINE_RATIO = 0.94  # grain lines and seams as a fraction of the board's color
SEAM_RATIO = 0.883
# The paint tiles carry the boards' texture without their color. Dark paint gets light marks wherever the
# board is darker than average (it can only be lightened), light paint gets dark marks.
SEAM_PAINT = 0.025  # how dark a seam is as a fraction of the board, in the paint tiles
DARK_PAINT_GAIN = 400
DARK_PAINT_TINT = np.array([1.0, 0.83, 0.54])
LIGHT_PAINT_GAIN = 0.75
LIGHT_PAINT_TINT = np.array([1.0, 1.1, 1.5])
TONE = 0.028  # how much the boards' brightness drifts along the grain
PORE_THRESHOLD = 2.0  # how rare the dark pores are (in standard deviations)
PORE_DEPTH = 0.12
SEAM_PX = 2  # on each side of a board edge
LINE_WIDTH = 1.5
LINES = 13
# How far the grain wanders from straight. Every line in a board follows the same sweep, a little
# stronger or weaker, so neighboring lines stay roughly parallel like real grain.
SWEEP = 7.0
RIPPLE = 1.5
# (seed, brightness) for each board in the tile. Only the boards themselves vary in brightness; paint tiles
# must not change the paint's colors.
BOARDS = [(1, 1.0), (2, 0.988), (3, 1.01), (4, 0.994)]

ROWS = np.arange(BOARD_HEIGHT)[:, None] + 0.5
PHASE = np.arange(WIDTH)[None, :] / WIDTH * 2 * np.pi


def draw_board(seed):
    """Returns the board's grain coverage and seam mask."""
    rng = np.random.default_rng(seed)
    lines = np.zeros((BOARD_HEIGHT, WIDTH))

    sweep = (
        np.sin(rng.integers(1, 3) * PHASE + rng.uniform(0, 2 * np.pi))
        + 0.4 * np.sin(rng.integers(3, 5) * PHASE + rng.uniform(0, 2 * np.pi))
    ) * SWEEP

    margin = SEAM_PX + 14
    spacing = (BOARD_HEIGHT - 2 * margin) / LINES
    for i in range(LINES):
        center = margin + (i + 0.5) * spacing + rng.uniform(-3, 3)
        ripple = RIPPLE * np.sin(rng.integers(4, 9) * PHASE + rng.uniform(0, 2 * np.pi))
        wave = sweep * rng.uniform(0.85, 1.15) + ripple
        # Lines thin out and strengthen along their length.
        strength = np.clip(
            0.75 + 0.35 * np.sin(rng.integers(1, 4) * PHASE + rng.uniform(0, 2 * np.pi)), 0.4, 1.0
        )
        coverage = np.clip(LINE_WIDTH / 2 + 0.5 - np.abs(ROWS - (center + wave)), 0, 1) * strength
        lines = np.maximum(lines, coverage)

    seams = np.broadcast_to((ROWS < SEAM_PX) | (ROWS > BOARD_HEIGHT - SEAM_PX), lines.shape)
    return lines, seams


def streaks(shape, rng, length, thickness):
    """Noise stretched along the boards: streaks about `length` px long and `thickness` px thick, with unit
    spread. It wraps in both directions, like the tile."""
    spectrum = np.fft.fft2(rng.standard_normal(shape))
    along = np.fft.fftfreq(shape[1])[None, :] * length
    across = np.fft.fftfreq(shape[0])[:, None] * thickness
    noise = np.fft.ifft2(spectrum * np.exp(-(along**2) - across**2)).real
    return noise / noise.std()


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--output", default=DEFAULT_OUTPUT)
    parser.add_argument("--dark-paint-output", default=DEFAULT_DARK_PAINT_OUTPUT)
    parser.add_argument("--light-paint-output", default=DEFAULT_LIGHT_PAINT_OUTPUT)
    args = parser.parse_args()

    drawn = [draw_board(seed) for seed, _ in BOARDS]
    lines = np.concatenate([board[0] for board in drawn], axis=0)
    seams = np.concatenate([board[1] for board in drawn], axis=0)
    brightness = np.repeat([brightness for _, brightness in BOARDS], BOARD_HEIGHT)[:, None, None]

    # Weathered boards: soft tone variation along the grain, sparse dark pores, and the grain lines
    rng = np.random.default_rng(5)
    tone = 1 + TONE * (0.6 * streaks(lines.shape, rng, 250, 3) + 0.4 * streaks(lines.shape, rng, 700, 14))
    pores = np.clip(streaks(lines.shape, rng, 90, 0.9) - PORE_THRESHOLD, 0, 1)
    texture = tone * (1 - PORE_DEPTH * pores) * (1 + (LINE_RATIO - 1) * lines)
    texture[seams] = (tone * SEAM_RATIO)[seams]
    boards = np.clip(BASE * (brightness * texture[..., None]), 0, 255)

    darkness = 1 - texture / texture[~seams].mean()
    darkness[seams] = SEAM_PAINT
    dark_paint = np.clip(np.abs(darkness), 0, 0.08)[..., None] * DARK_PAINT_GAIN * DARK_PAINT_TINT
    light_paint = 255 * (1 - LIGHT_PAINT_GAIN * np.clip(darkness, 0, 0.1)[..., None] * LIGHT_PAINT_TINT)

    for path, tile in [
        (args.output, boards),
        (args.dark_paint_output, dark_paint),
        (args.light_paint_output, light_paint),
    ]:
        image = Image.fromarray(np.clip(tile, 0, 255).astype(np.uint8), "RGB")
        image.save(path, lossless=True, method=6)
        print(f"Wrote {os.path.normpath(path)} ({tile.shape[1]}x{tile.shape[0]})")


if __name__ == "__main__":
    main()
