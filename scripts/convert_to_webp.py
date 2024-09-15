from PIL import Image
import tkinter as tk
from tkinter import filedialog
import matplotlib.pyplot as plt
import os

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

plt.show()
