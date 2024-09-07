from PIL import Image
import tkinter as tk
from tkinter import filedialog

# Choose the file
root = tk.Tk()
root.withdraw()
file_path = filedialog.askopenfilename()

image = Image.open(file_path)

image.thumbnail((1000, 300))
image.save("converted.webp", "WEBP", quality=75)