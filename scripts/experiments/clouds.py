import sys
import os
import numpy as np
from PIL import Image


def load(image_path):
    img = Image.open(image_path)
    img = img.convert('RGB')
    img_rgb = np.array(img) / 255.0
    return img_rgb

def save_image(img_array, file_name, mode="RGB"):
    img_pil = Image.fromarray(img_array, mode)
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), file_name)
    img_pil.save(output_path, quality=90)

def classify_pixels(img_rgb):    
    luminance = np.mean(img_rgb, axis=2)
    min_luminance = luminance.min()
    normalized_luminance = ((luminance - min_luminance) / (luminance.max() - min_luminance + 1e-6)) if min_luminance < 0.1 else luminance

    blue_ratio = img_rgb[..., 2] / (img_rgb[..., 0] + img_rgb[..., 1] + 1/255)
    nrbr = ((img_rgb[..., 2] - img_rgb[..., 0]) / (img_rgb[..., 2] + img_rgb[..., 0] + 1/255) + 1) / 2
    save_image((nrbr * 255).astype(np.uint8), "blue_ratio.jpg", mode='L')


    # sky = (blue_ratio < 0.1) & (percent_blue > 0.4) & (normalized_luminance > 0.05)
    # sky = (blue_ratio < 0.1) & (normalized_luminance > 0.1)
    # sky = (percent_blue > 0.3) & (normalized_luminance > 0.2)
    # sky = (nrbr < 0.9)# & (luminance > 0.1) 
    # objects = ~sky
    # clouds = ~objects

    all_sky = np.all(blue_ratio > 0.1)
    
    if all_sky:
        objects = np.zeros_like(img_rgb[..., 0], dtype=bool)
    else:
        objects = (nrbr >= 0.9) | (nrbr <= 0.1) | (normalized_luminance < 0.25)
    sky = ~objects
    clouds = sky

    classification_img = np.zeros_like(img_rgb)
    classification_img[..., 0] = clouds * blue_ratio
    classification_img[..., 1] = objects
    classification_img[..., 2] = sky
    save_image((classification_img * 255).astype(np.uint8), "combined_classification.jpg", mode='RGB')

    cloud_coverage_img = 1 - np.clip(np.abs(0.5 - blue_ratio) * 2, 0, 1)
    cloud_coverage_img[~clouds] = 0
    save_image((cloud_coverage_img * 255).astype(np.uint8), "cloud_coverage.jpg", mode='L')

    cloud_coverage = np.sum(cloud_coverage_img * clouds) / (np.sum(clouds) + 1e-6)
    print(f"  Estimated Cloud Coverage: {(cloud_coverage * 100):.2f}")

   
print(sys.argv[1])
img_rgb = load(sys.argv[1])
classify_pixels(img_rgb)