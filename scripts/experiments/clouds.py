import sys
import os
import numpy as np
from PIL import Image
from scipy import ndimage
import json


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
            block = blue[
                i - half_block : i + half_block + 1, j - half_block : j + half_block + 1
            ]
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


def get_attributes(path):
    image = load(path)
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
    return {
        "coverage": float(cloud_coverage),
        "clear": float(clear_sky),
        "roughness": float(roughness),
    }


def classify(attributes):
    if attributes["clear"] > 0.9:
        return "clear"
    if attributes["clear"] < 0.1:
        # This is a stratiform cloud
        # TODO: Stratocumulus?
        if attributes['coverage'] > 0.9:
            return 'stratus'
        elif attributes['coverage'] > 0.75:
            return 'altostratus'
        else:
            return 'cirrostratus'
    # This is either a cirrus or cumulus cloud
    if attributes['roughness'] < 0.1:
        return "cirrus"
    return "cumulus"


def calculate_accuracy(confusion_matrix):
    total = 0
    correct = 0
    for true_label in confusion_matrix:
        for pred_label in confusion_matrix[true_label]:
            count = confusion_matrix[true_label][pred_label]
            total += count
            if true_label == pred_label:
                correct += count
    return correct / total if total > 0 else 0


def calculate_precision_recall_f1(confusion_matrix):
    labels = list(confusion_matrix.keys())
    precisions = []
    recalls = []
    f1s = []
    for label in labels:
        tp = confusion_matrix[label].get(label, 0)
        fp = sum(confusion_matrix[other].get(label, 0) for other in labels if other != label)
        fn = sum(confusion_matrix[label].get(other, 0) for other in labels if other != label)
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0
        f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0
        precisions.append(precision)
        recalls.append(recall)
        f1s.append(f1)
    macro_precision = sum(precisions) / len(precisions) if precisions else 0
    macro_recall = sum(recalls) / len(recalls) if recalls else 0
    macro_f1 = sum(f1s) / len(f1s) if f1s else 0
    return macro_precision, macro_recall, macro_f1


if len(sys.argv) > 1:
    attrs = get_attributes(sys.argv[1])
    classification = classify(attrs)
    print(f"Coverage: {(attrs["coverage"] * 100):.2f}%")
    print(f"Clear: {(attrs["clear"] * 100):.2f}%")
    print(f"Roughness: {(attrs["roughness"] * 100):.2f}%")
    print(f"Classification: {classification}")
else:
    cache = {}
    cache_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache.json")
    if os.path.exists(cache_file):
        with open(cache_file, "r") as f:
            cache = json.load(f)
    dataset_folder = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "../../app/src/androidTest/assets/clouds",
    )
    subfolders = [os.path.join(dataset_folder, f) for f in os.listdir(dataset_folder)]
    confusion_matrix = {}
    for folder in subfolders:
        folder_name = os.path.basename(folder)
        for file in os.listdir(folder):
            if file in cache:
                attrs = cache[file]
            else:
                path = os.path.join(folder, file)
                attrs = get_attributes(path)
                attrs["type"] = folder_name
                cache[file] = attrs
            print(attrs)
            classification = classify(attrs)
            if folder_name not in confusion_matrix:
                confusion_matrix[folder_name] = {}
            if classification not in confusion_matrix[folder_name]:
                confusion_matrix[folder_name][classification] = 0
            confusion_matrix[folder_name][classification] += 1

    with open(cache_file, "w") as f:
        json.dump(cache, f, indent=4)

    labels = sorted(confusion_matrix.keys())
    abbrev = {
        "altocumulus": "Ac",
        "altostratus": "As",
        "cirrocumulus": "Cc",
        "cirrostratus": "Cs",
        "cirrus": "Ci",
        "clear": "No",
        "cumulonimbus": "Cb",
        "cumulus": "Cu",
        "nimbostratus": "Ns",
        "stratocumulus": "Sc",
        "stratus": "St"
    }
    display_labels = [abbrev.get(label, label) for label in labels]
    max_label_len = max(len(dl) for dl in display_labels) if display_labels else 0
    max_count_len = max((len(str(count)) for true_label in confusion_matrix for pred_label in confusion_matrix[true_label] for count in [confusion_matrix[true_label][pred_label]]), default=0)
    header = " " * max_label_len + " | " + " | ".join(f"{dl:>{max_count_len}}" for dl in display_labels)
    print("Confusion Matrix:")
    print(header)
    print("-" * len(header))
    for true_label in labels:
        display_true = abbrev.get(true_label, true_label)
        row_counts = [str(confusion_matrix[true_label].get(pred_label, 0)) for pred_label in labels]
        row = f"{display_true:<{max_label_len}} | " + " | ".join(f"{count:>{max_count_len}}" for count in row_counts)
        print(row)

    accuracy = calculate_accuracy(confusion_matrix)
    precision, recall, f1 = calculate_precision_recall_f1(confusion_matrix)
    print(f"\nAccuracy: {accuracy:.4f}")
    print(f"Macro Precision: {precision:.4f}")
    print(f"Macro Recall: {recall:.4f}")
    print(f"Macro F1 Score: {f1:.4f}")
