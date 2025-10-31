import sys
import os
import numpy as np
from PIL import Image
from scipy import ndimage


def load(image_path):
    img = Image.open(image_path)
    img = img.convert("RGB")
    img_rgb = np.array(img) / 255.0
    return img_rgb


def save_image(img_array, file_name, mode="RGB"):
    img_pil = Image.fromarray((img_array * 255).astype(np.uint8), mode)
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), file_name)
    img_pil.save(output_path, quality=90)

def blur(img, radius):
    return ndimage.gaussian_filter(img, sigma=(radius, radius, 0))

def get_cloud_coverage(image, sky):
    blue_ratio = image[..., 2] / (image[..., 0] + image[..., 1] + 1 / 255)
    save_image(blue_ratio, "blue_ratio.jpg", mode="L")
    cloud_coverage_img = 1 - np.clip(np.abs(0.5 - blue_ratio) * 2, 0, 1)
    cloud_coverage_img[~sky] = 0
    cloud_coverage_img[cloud_coverage_img > 0.8] = 1
    save_image(cloud_coverage_img, "cloud_coverage.jpg", mode="L")

    cloud_coverage = np.sum(cloud_coverage_img * sky) / (np.sum(sky) + 1e-6)
    clear_sky = np.sum((cloud_coverage_img * sky) < 0.2) / (np.sum(sky) + 1e-6)
    return cloud_coverage, clear_sky

def get_cloud_roughness(image, sky):
    blue = image[..., 2]
    block_size = 5
    
    local_contrast = np.zeros_like(blue)
    half_block = block_size // 2
    
    for i in range(half_block, blue.shape[0] - half_block):
        for j in range(half_block, blue.shape[1] - half_block):
            block = blue[i-half_block:i+half_block+1, j-half_block:j+half_block+1]
            local_contrast[i, j] = np.max(block) - np.min(block)
    
    local_contrast[~sky] = 0
    save_image(local_contrast, "gradient_magnitude.jpg", mode="L")
    roughness = np.sum(local_contrast) * 10 / (np.sum(sky) + 1e-6)
    return roughness


# def get_cloud_roughness(image, sky):
#     kernel = np.array([[1, 0, -1], [2, 0, -2], [1, 0, -1]])
#     blue_channel = image[..., 2]
#     blue_channel = ndimage.gaussian_filter(blue_channel, sigma=1)
#     sobel_x = ndimage.convolve(blue_channel, kernel)
#     sobel_y = ndimage.convolve(blue_channel, kernel.T)
#     gradient_magnitude = np.sqrt(sobel_x**2 + sobel_y**2)
#     gradient_magnitude[~sky] = 0
#     save_image(gradient_magnitude, "gradient_magnitude.jpg", mode="L")
#     roughness = np.sum(gradient_magnitude) * 10 / np.sum(sky)
#     return roughness

# def get_cloud_roughness(image, sky):
#     blue = image[..., 2]
#     blurred1 = ndimage.gaussian_filter(blue, sigma=3)
#     blurred2 = ndimage.gaussian_filter(blue, sigma=1)
#     dog = np.clip(blurred2 - blurred1, 0, 1)
#     dog[~sky] = 0
#     save_image(dog, "gradient_magnitude.jpg", mode="L")
#     roughness = np.sum(dog) * 100 / (np.sum(sky) + 1e-6)
#     return roughness


def get_sky_mask(image):
    luminance = image[..., 2]
    stdev = np.std(luminance)

    blue_ratio = image[..., 2] / (image[..., 0] + image[..., 1] + 1 / 255)
    save_image(blue_ratio, "blue_ratio.jpg", mode="L")

    save_image(luminance, "luminance.jpg", mode="L")

    # TODO: This should be more robust to different lighting conditions
    # print(stdev)
    if stdev < 0.15:
        objects = np.zeros_like(image[..., 0], dtype=bool)
    else:
        objects = luminance < 0.15

    # Dilate the objects
    width = image.shape[1]
    objects = ndimage.binary_dilation(objects, iterations=int(width * 0.01))

    sky = ~objects

    classification_img = np.zeros_like(image)
    classification_img[..., 0] = sky * blue_ratio
    classification_img[..., 1] = objects
    classification_img[..., 2] = sky
    save_image(
        classification_img,
        "combined_classification.jpg",
        mode="RGB",
    )
    return sky


print(sys.argv[1])
image = load(sys.argv[1])
aspect = image.shape[1] / image.shape[0]
new_width = 400
new_height = 400

pil_image = Image.fromarray((image * 255).astype(np.uint8))
if aspect > 1:
    new_height = int(new_height / aspect)
    pil_image = pil_image.resize((new_width, new_height), Image.BICUBIC)
else:
    new_width = int(new_width * aspect)
    pil_image = pil_image.resize((new_width, new_height), Image.BICUBIC)
image = np.array(pil_image) / 255.0
image = blur(image, radius=1)
sky = get_sky_mask(image)
cloud_coverage, clear_sky = get_cloud_coverage(image, sky)
roughness = get_cloud_roughness(image, sky)
print(f"Coverage: {(cloud_coverage * 100):.2f}%")
print(f"Clear: {(clear_sky * 100):.2f}%")
print(f"Roughness: {(roughness * 100):.2f}%")
