"""Generate the social media link preview image (site/src/assets/images/social-preview.png) and the
Google Play feature graphic (fastlane/metadata/android/en-US/images/featureGraphic.png).

Both images are the site's hero scene along the bottom with the app name centered in the sky above
it, in the same font and colors as the website's hero.

Requires: Pillow built with FreeType WOFF2 support. If optipng is installed, it is used to shrink
the output.

Usage: python3 scripts/generate_site_social_preview.py [--social-output PATH] [--feature-output PATH]
"""

import argparse
import os
import shutil
import subprocess

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
SITE_ASSETS_DIR = os.path.join(ROOT, "site", "src", "assets")
SCENE = os.path.join(SITE_ASSETS_DIR, "images", "hero-scene.webp")
NAME_FONT = os.path.join(SITE_ASSETS_DIR, "fonts", "NationalPark-ExtraBold.woff2")
DEFAULT_SOCIAL_OUTPUT = os.path.join(SITE_ASSETS_DIR, "images", "social-preview.png")
DEFAULT_FEATURE_OUTPUT = os.path.join(
    ROOT, "fastlane", "metadata", "android", "en-US", "images", "featureGraphic.png"
)

# Open Graph / Twitter large image cards.
SOCIAL_SIZE = (1200, 630)
# Google Play feature graphic.
FEATURE_SIZE = (1024, 500)

# Layout values are defined for a canvas of this width and scaled to the others.
REFERENCE_WIDTH = 1200
NAME_SIZE = 140
NAME_TRACKING = 0.02  # In em, matching the hero heading's letter-spacing.
NAME = "TRAIL SENSE"
# Match --sky and --text-on-sky in site/src/assets/css/styles.css.
SKY_COLOR = (255, 199, 117)
NAME_COLOR = (57, 47, 30)


def draw_name(image, sky_height, font_size):
    """Draws the name horizontally centered, and vertically centered in the sky above the scene."""
    font = ImageFont.truetype(NAME_FONT, font_size)
    tracking = font_size * NAME_TRACKING
    advances = [font.getlength(c) for c in NAME]
    total_width = sum(advances) + tracking * (len(NAME) - 1)
    _, top, _, bottom = font.getbbox(NAME)

    draw = ImageDraw.Draw(image)
    x = (image.width - total_width) / 2
    y = (sky_height - (bottom - top)) / 2 - top
    for char, advance in zip(NAME, advances):
        draw.text((x, y), char, font=font, fill=NAME_COLOR)
        x += advance + tracking


def generate(output, width, height):
    image = Image.new("RGBA", (width, height), SKY_COLOR + (255,))

    scene = Image.open(SCENE).convert("RGBA")
    scene = scene.resize((width, round(scene.height * width / scene.width)), Image.LANCZOS)
    scene_top = height - scene.height
    image.alpha_composite(scene, (0, scene_top))

    draw_name(image, scene_top, round(NAME_SIZE * width / REFERENCE_WIDTH))
    image.convert("RGB").save(output, optimize=True)

    if shutil.which("optipng"):
        subprocess.run(["optipng", "-quiet", "-o4", output], check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--social-output", default=DEFAULT_SOCIAL_OUTPUT)
    parser.add_argument("--feature-output", default=DEFAULT_FEATURE_OUTPUT)
    args = parser.parse_args()
    generate(args.social_output, *SOCIAL_SIZE)
    generate(args.feature_output, *FEATURE_SIZE)
