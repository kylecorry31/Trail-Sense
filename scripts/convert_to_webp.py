from PIL import Image
import tkinter as tk
from tkinter import filedialog
import matplotlib.pyplot as plt
import os
import cv2
import numpy as np

# Choose the file
root = tk.Tk()
root.withdraw()
file_path = filedialog.askopenfilename()

def convert_to_webp(file_path, quality):
    image = Image.open(file_path)
    image.thumbnail((1000, 300))
    image.save("converted.webp", "WEBP", quality=quality)
    size = os.path.getsize("converted.webp") / 1024
    return Image.open("converted.webp"), size


def botanical_sketch(image_path, output_path):
    # TODO: Accentuate the edges
    img = cv2.imread(image_path)

    # Resize image to fit within 1000x1000 while maintaining aspect ratio
    height, width = img.shape[:2]
    if height > width:
        height = 1000
        width = int(1000 / img.shape[0] * img.shape[1])
    else:
        width = 1000
        height = int(1000 / img.shape[1] * img.shape[0])
    img = cv2.resize(img, (width, height), interpolation=cv2.INTER_AREA)

    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    img_edge_preserved = cv2.edgePreservingFilter(
        img_rgb, flags=1, sigma_s=64, sigma_r=0.2
    )

    img_smoothed = cv2.bilateralFilter(
        img_edge_preserved, d=9, sigmaColor=75, sigmaSpace=75
    )

    Z = img_smoothed.reshape((-1, 3))
    Z = np.float32(Z)

    K = 64  # Number of colors
    criteria = (
        cv2.TermCriteria_EPS + cv2.TermCriteria_MAX_ITER,
        10,
        1.0,
    )
    _, labels, centers = cv2.kmeans(
        Z, K, None, criteria, 10, cv2.KMEANS_PP_CENTERS
    )
    centers = np.uint8(centers)
    img_quantized = centers[labels.flatten()].reshape(img_smoothed.shape)

    illustration = img_quantized.astype(np.uint8)
    illustration_bgr = cv2.cvtColor(illustration, cv2.COLOR_RGB2BGR)
    cv2.imwrite(output_path, illustration_bgr)

original = Image.open(file_path)
original_size = os.path.getsize(file_path) / 1024
original.thumbnail((1000, 300))

# Display the images side by side in a window
def update_quality(val):
    quality = int(val)
    converted = convert_to_webp(file_path, quality)
    ax[1].imshow(converted[0])
    ax[1].axis("off")
    ax[1].set_title(f"{quality}% ({converted[1]:.2f} KB)")
    fig.canvas.draw_idle()

fig, ax = plt.subplots(1, 2, figsize=(10, 5))
ax[0].imshow(original)
ax[0].axis("off")
ax[0].set_title(f"Original Image ({original_size:.2f} KB)")

quality = 75
converted = convert_to_webp(file_path, quality)
ax[1].imshow(converted[0])
ax[1].axis("off")
ax[1].set_title(f"{quality}% ({converted[1]:.2f} KB)")

slider_ax = plt.axes([0.25, 0.01, 0.50, 0.03], facecolor='lightgoldenrodyellow')
quality_slider = plt.Slider(slider_ax, 'Quality', 1, 100, valinit=quality)
quality_slider.on_changed(update_quality)

# TODO: Apply the botanical sketch effect to the image
# botanical_sketch(file_path, "botanical_sketch.jpg")

plt.show()
