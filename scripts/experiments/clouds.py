import sys
import os
import numpy as np
from PIL import Image
from scipy import ndimage


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
    luminance = img_rgb[..., 2]
    stdev = np.std(luminance)

    blue_ratio = img_rgb[..., 2] / (img_rgb[..., 0] + img_rgb[..., 1] + 1/255)
    save_image((blue_ratio * 255).astype(np.uint8), "blue_ratio.jpg", mode='L')

    save_image((luminance * 255).astype(np.uint8), "luminance.jpg", mode='L')

    # TODO: This should be more robust to different lighting conditions
    print(stdev)
    if stdev < 0.15:
        objects = np.zeros_like(img_rgb[..., 0], dtype=bool)
    else:
        objects = (luminance < 0.15)
    
    # Dilate the objects
    width = img_rgb.shape[1]
    objects = ndimage.binary_dilation(objects, iterations=int(width * 0.01))
    
    sky = ~objects
    clouds = sky

    classification_img = np.zeros_like(img_rgb)
    classification_img[..., 0] = clouds * blue_ratio
    classification_img[..., 1] = objects
    classification_img[..., 2] = sky
    save_image((classification_img * 255).astype(np.uint8), "combined_classification.jpg", mode='RGB')

    cloud_coverage_img = 1 - np.clip(np.abs(0.5 - blue_ratio) * 2, 0, 1)
    cloud_coverage_img[~clouds] = 0
    cloud_coverage_img[cloud_coverage_img > 0.8] = 1
    save_image((cloud_coverage_img * 255).astype(np.uint8), "cloud_coverage.jpg", mode='L')

    cloud_coverage = np.sum(cloud_coverage_img * clouds) / (np.sum(clouds) + 1e-6)
    print(f"  Estimated Cloud Coverage: {(cloud_coverage * 100):.2f}")

   
print(sys.argv[1])
img_rgb = load(sys.argv[1])
classify_pixels(img_rgb)