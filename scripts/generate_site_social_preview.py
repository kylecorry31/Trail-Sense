"""Generate the social media link preview image (site/src/assets/images/social-preview.png) and the
Google Play feature graphic (fastlane/metadata/android/en-US/images/featureGraphic.png).

Both images are the site's topo background fading out toward the edges, with the app logo and name
centered on top. Run scripts/generate_site_topo.py first if the topo background changes.

Requires: ImageMagick (the `convert` and `identify` commands) and the FreeSans Bold font

Usage: python3 scripts/generate_site_social_preview.py [--social-output PATH] [--feature-output PATH]
"""

import argparse
import os
import subprocess
import tempfile

ROOT = os.path.join(os.path.dirname(__file__), "..")
IMAGES_DIR = os.path.join(ROOT, "site", "src", "assets", "images")
TOPO = os.path.join(IMAGES_DIR, "topo.svg")
LOGO = os.path.join(IMAGES_DIR, "logo.png")
DEFAULT_SOCIAL_OUTPUT = os.path.join(IMAGES_DIR, "social-preview.png")
DEFAULT_FEATURE_OUTPUT = os.path.join(
    ROOT, "fastlane", "metadata", "android", "en-US", "images", "featureGraphic.png"
)

# Open Graph / Twitter large image cards.
SOCIAL_SIZE = (1200, 630)
# Google Play feature graphic.
FEATURE_SIZE = (1024, 500)

# Layout values are defined for a canvas of this height and scaled to the others.
REFERENCE_HEIGHT = 630
LOGO_SIZE = 250
LOGO_OFFSET_Y = -80
# The name's baseline relative to the vertical center of the canvas.
NAME_BASELINE_OFFSET_Y = 152
NAME_SIZE = 88
NAME_FONT = "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf"
# Colors match --ink, --ink-text and --accent in site/src/assets/css/styles.css.
BACKGROUND = "#0b0e14"
NAME_COLOR = "#eceef2"
NAME_ACCENT_COLOR = "#ff6d00"
TOPO_OPACITY = 0.24
PALETTE_COLORS = 96


def convert(*args):
    subprocess.run(["convert", *args], check=True)


def text_width(text, font_size):
    """Returns the rendered width of the text in pixels."""
    result = subprocess.run(
        ["convert", "-font", NAME_FONT, "-pointsize", str(font_size), f"label:{text}",
         "-format", "%w", "info:"],
        check=True, capture_output=True, text=True,
    )
    return int(result.stdout)


def name_annotations(width, baseline_y, font_size):
    """Draws "Trail" in the text color and "Sense" in the accent color, centered as one name."""
    total = text_width("Trail Sense", font_size)
    trail_x = (width - total) // 2
    sense_x = trail_x + total - text_width("Sense", font_size)
    return [
        "-font", NAME_FONT, "-pointsize", str(font_size),
        "-fill", NAME_COLOR, "-annotate", f"+{trail_x}+{baseline_y}", "Trail",
        "-fill", NAME_ACCENT_COLOR, "-annotate", f"+{sense_x}+{baseline_y}", "Sense",
    ]


def generate(output, width, height):
    size = f"{width}x{height}"
    scale = height / REFERENCE_HEIGHT
    logo_size = round(LOGO_SIZE * scale)
    logo_offset_y = round(LOGO_OFFSET_Y * scale)
    name_size = round(NAME_SIZE * scale)
    baseline_y = height // 2 + round(NAME_BASELINE_OFFSET_Y * scale)

    with tempfile.TemporaryDirectory() as tmp:
        topo = os.path.join(tmp, "topo.png")
        vignette = os.path.join(tmp, "vignette.png")
        mask = os.path.join(tmp, "mask.png")
        lines = os.path.join(tmp, "lines.png")

        # Scale the topo to cover the canvas, cropping the overflow.
        convert(
            "-background", "none", "-density", "150", TOPO,
            "-resize", f"{size}^", "-gravity", "center", "-extent", size,
            topo,
        )
        convert("-size", size, "radial-gradient:gray90-gray15", vignette)

        # Fade the lines toward the edges by scaling the topo's alpha with the vignette.
        convert(
            topo, "-alpha", "extract", "-evaluate", "multiply", str(TOPO_OPACITY),
            vignette, "-compose", "multiply", "-composite", mask,
        )
        convert(
            "-size", size, "xc:white", "-alpha", "off",
            mask, "-compose", "copy-opacity", "-composite", lines,
        )

        convert(
            "-size", size, f"xc:{BACKGROUND}",
            lines, "-compose", "over", "-composite",
            "(", LOGO, "-resize", f"{logo_size}x{logo_size}", ")",
            "-gravity", "center", "-geometry", f"+0{logo_offset_y:+d}",
            "-compose", "over", "-composite",
            "+gravity",
            *name_annotations(width, baseline_y, name_size),
            "-colors", str(PALETTE_COLORS), "-dither", "None", "-strip",
            f"PNG8:{output}",
        )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--social-output", default=DEFAULT_SOCIAL_OUTPUT)
    parser.add_argument("--feature-output", default=DEFAULT_FEATURE_OUTPUT)
    args = parser.parse_args()
    generate(args.social_output, *SOCIAL_SIZE)
    generate(args.feature_output, *FEATURE_SIZE)
