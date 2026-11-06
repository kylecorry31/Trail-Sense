"""Generate the contour-line background used by the website (site/src/assets/images/topo.svg).

The contours are drawn from public AWS Terrain Tiles (Terrarium encoding) and centered on the
highest point near the given location (Mount Washington by default).

Requires: numpy, matplotlib, Pillow

Usage: python3 scripts/generate_site_topo.py [--lat 44.2706] [--lon -71.3033] [--output PATH]
"""

import argparse
import math
import os
import tempfile
import urllib.request

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from PIL import Image

TILE_URL = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"
TILE_SIZE = 256
# Tiles on each side of the center tile; the grid must be larger than the crop.
TILE_RADIUS = 2

DEFAULT_OUTPUT = os.path.join(
    os.path.dirname(__file__), "..", "site", "src", "assets", "images", "topo.svg"
)


def tile_index(lat, lon, zoom):
    n = 2**zoom
    x = int((lon + 180) / 360 * n)
    y = int((1 - math.asinh(math.tan(math.radians(lat))) / math.pi) / 2 * n)
    return x, y


def load_elevation(lat, lon, zoom, cache_dir):
    """Returns a square elevation grid (meters) of tiles around the location."""
    cx, cy = tile_index(lat, lon, zoom)
    size = (2 * TILE_RADIUS + 1) * TILE_SIZE
    elevation = np.zeros((size, size))
    for row, y in enumerate(range(cy - TILE_RADIUS, cy + TILE_RADIUS + 1)):
        for col, x in enumerate(range(cx - TILE_RADIUS, cx + TILE_RADIUS + 1)):
            path = os.path.join(cache_dir, f"{zoom}_{x}_{y}.png")
            if not os.path.exists(path):
                urllib.request.urlretrieve(TILE_URL.format(z=zoom, x=x, y=y), path)
            rgb = np.asarray(Image.open(path).convert("RGB")).astype(float)
            tile = rgb[..., 0] * 256 + rgb[..., 1] + rgb[..., 2] / 256 - 32768
            elevation[
                row * TILE_SIZE : (row + 1) * TILE_SIZE,
                col * TILE_SIZE : (col + 1) * TILE_SIZE,
            ] = tile
    return elevation


def gaussian_blur(grid, sigma):
    radius = int(3 * sigma)
    kernel = np.exp(-0.5 * (np.arange(-radius, radius + 1) / sigma) ** 2)
    kernel /= kernel.sum()
    padded = np.pad(grid, radius, mode="edge")
    padded = np.apply_along_axis(lambda v: np.convolve(v, kernel, mode="valid"), 0, padded)
    return np.apply_along_axis(lambda v: np.convolve(v, kernel, mode="valid"), 1, padded)


def simplify(points, tolerance):
    """Ramer-Douglas-Peucker line simplification."""
    if len(points) < 3:
        return points
    start, end = points[0], points[-1]
    direction = end - start
    length = np.hypot(*direction)
    relative = points - start
    if length:
        distances = np.abs(direction[0] * relative[:, 1] - direction[1] * relative[:, 0]) / length
    else:
        distances = np.hypot(relative[:, 0], relative[:, 1])
    farthest = int(distances.argmax())
    if distances[farthest] <= tolerance:
        return np.array([start, end])
    left = simplify(points[: farthest + 1], tolerance)
    right = simplify(points[farthest:], tolerance)
    return np.vstack([left[:-1], right])


def build_svg(elevation, width, height, interval, index_every, sigma):
    peak_y, peak_x = np.unravel_index(elevation.argmax(), elevation.shape)
    left, top = peak_x - width // 2, peak_y - height // 2
    crop = gaussian_blur(elevation[top : top + height, left : left + width], sigma)

    levels = np.arange(np.ceil(crop.min() / interval) * interval, crop.max(), interval)
    xs, ys = np.meshgrid(np.arange(width), np.arange(height))
    fig, ax = plt.subplots()
    contours = ax.contour(xs, ys, crop, levels=levels)
    plt.close(fig)

    lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" '
        'preserveAspectRatio="xMidYMid slice" fill="none" stroke="#fff" '
        'stroke-linejoin="round" stroke-linecap="round">'
    ]
    for level, segments in zip(levels, contours.allsegs):
        path = ""
        for segment in segments:
            if len(segment) < 14:
                continue
            points = simplify(segment, 0.7)
            path += "M" + " ".join(f"{x:.1f} {y:.1f}" for x, y in points)
        if path:
            is_index = int(round(level / interval)) % index_every == 0
            lines.append(
                f'<path d="{path}" stroke-width="{2.2 if is_index else 1.0}" '
                f'stroke-opacity="{1 if is_index else 0.6}"/>'
            )
    lines.append("</svg>")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--lat", type=float, default=44.2706, help="Latitude near the peak")
    parser.add_argument("--lon", type=float, default=-71.3033, help="Longitude near the peak")
    parser.add_argument("--zoom", type=int, default=12, help="Tile zoom level")
    parser.add_argument("--width", type=int, default=960, help="Output width in pixels")
    parser.add_argument("--height", type=int, default=540, help="Output height in pixels")
    parser.add_argument("--interval", type=int, default=50, help="Contour interval in meters")
    parser.add_argument("--index-every", type=int, default=5, help="Emphasize every Nth contour")
    parser.add_argument("--smoothing", type=float, default=3, help="Gaussian sigma in pixels")
    parser.add_argument("--cache-dir", help="Directory for downloaded tiles (default: temporary)")
    parser.add_argument("--output", default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    with tempfile.TemporaryDirectory() as temp_dir:
        cache_dir = args.cache_dir or temp_dir
        os.makedirs(cache_dir, exist_ok=True)
        elevation = load_elevation(args.lat, args.lon, args.zoom, cache_dir)

    svg = build_svg(
        elevation, args.width, args.height, args.interval, args.index_every, args.smoothing
    )
    with open(args.output, "w") as output_file:
        output_file.write(svg)
    print(f"Wrote {os.path.normpath(args.output)} ({len(svg) / 1024:.0f} KB)")


if __name__ == "__main__":
    main()
