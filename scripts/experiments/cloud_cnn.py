import argparse
import math
import os
import struct
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torch import nn
from torch.nn import functional as F


INPUT_SIZE = 16
INPUT_CHANNELS = 3
OUTPUTS = 11
CONV1_CHANNELS = 12
CONV2_CHANNELS = 12
KERNEL_SIZE = 3
CONV1_SIZE = INPUT_SIZE - KERNEL_SIZE + 1
POOL1_SIZE = CONV1_SIZE // 2
CONV2_SIZE = POOL1_SIZE - KERNEL_SIZE + 1
POOL2_SIZE = CONV2_SIZE // 2
WEIGHT_COUNT = (
    CONV1_CHANNELS * INPUT_CHANNELS * KERNEL_SIZE * KERNEL_SIZE
    + CONV1_CHANNELS
    + CONV2_CHANNELS * CONV1_CHANNELS * KERNEL_SIZE * KERNEL_SIZE
    + CONV2_CHANNELS
    + OUTPUTS * CONV2_CHANNELS
    + OUTPUTS
)
MODEL_MAGIC = b"TSCL"
MODEL_VERSION = 1
BATCH_SIZE = 1

CLASS_NAMES = [
    "cirrus",
    "cirrocumulus",
    "cirrostratus",
    "altostratus",
    "altocumulus",
    "nimbostratus",
    "stratocumulus",
    "cumulus",
    "stratus",
    "cumulonimbus",
    "clear",
]


class CloudCNN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = nn.Conv2d(INPUT_CHANNELS, CONV1_CHANNELS, KERNEL_SIZE)
        self.conv2 = nn.Conv2d(CONV1_CHANNELS, CONV2_CHANNELS, KERNEL_SIZE)
        self.classifier = nn.Linear(CONV2_CHANNELS, OUTPUTS)
        for layer in (self.conv1, self.conv2, self.classifier):
            fan_in, _ = nn.init._calculate_fan_in_and_fan_out(layer.weight)
            limit = math.sqrt(6 / fan_in)
            nn.init.uniform_(layer.weight, -limit, limit)
            nn.init.zeros_(layer.bias)

    def forward(self, images):
        values = F.max_pool2d(F.relu(self.conv1(images)), kernel_size=2, stride=2)
        values = F.max_pool2d(F.relu(self.conv2(values)), kernel_size=2, stride=2)
        values = values.mean(dim=(2, 3))
        return self.classifier(values)


def load_dataset(dataset_dir):
    inputs = []
    labels = []
    for label, class_name in enumerate(CLASS_NAMES):
        class_dir = dataset_dir / class_name
        image_paths = sorted(
            path
            for path in class_dir.iterdir()
            if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
        )
        if not image_paths:
            raise ValueError(f"No training images found for class '{class_name}'")

        for image_path in image_paths:
            with Image.open(image_path) as image:
                image = image.convert("RGB").resize(
                    (INPUT_SIZE, INPUT_SIZE), Image.Resampling.BILINEAR
                )
                pixels = np.asarray(image, dtype=np.float32) / 255.0
                inputs.append(pixels.transpose(2, 0, 1))
                labels.append(label)

    return np.asarray(inputs, dtype=np.float32), np.asarray(labels, dtype=np.int64)


def predict(image, model):
    model.eval()
    image_tensor = torch.from_numpy(np.ascontiguousarray(image)).unsqueeze(0)
    with torch.no_grad():
        logits = model(image_tensor)
        probabilities = torch.softmax(logits, dim=1)
    return probabilities[0].cpu().numpy()


def train(inputs, labels, epochs, learning_rate, seed):
    torch.manual_seed(seed)
    random = np.random.default_rng(seed)
    model = CloudCNN()
    class_counts = np.bincount(labels, minlength=OUTPUTS)
    represented_classes = np.count_nonzero(class_counts)
    class_weights = np.ones(OUTPUTS, dtype=np.float32)
    for label, count in enumerate(class_counts):
        if count:
            class_weights[label] = np.clip(
                math.sqrt(len(labels) / (represented_classes * count)), 0.5, 4.0
            )

    class_weight_tensor = torch.from_numpy(class_weights)
    criterion = nn.CrossEntropyLoss(reduction="none")
    optimizer = torch.optim.SGD(model.parameters(), lr=learning_rate)
    input_tensor = torch.from_numpy(np.ascontiguousarray(inputs))
    label_tensor = torch.from_numpy(labels.astype(np.int64, copy=False))

    for epoch in range(epochs):
        model.train()
        order = random.permutation(len(labels))
        total_loss = 0.0
        for start in range(0, len(order), BATCH_SIZE):
            indexes = torch.from_numpy(order[start : start + BATCH_SIZE].copy())
            batch = input_tensor[indexes].clone()
            batch_labels = label_tensor[indexes]
            flips = torch.rand(batch.shape[0]) < 0.5
            batch[flips] = batch[flips].flip(-1)

            optimizer.zero_grad(set_to_none=True)
            loss = criterion(model(batch), batch_labels)
            loss = (loss * class_weight_tensor[batch_labels]).mean()
            loss.backward()
            optimizer.step()
            total_loss += loss.item() * len(batch_labels)

        report_interval = max(1, epochs // 10)
        if (epoch + 1) % report_interval == 0 or epoch == 0 or epoch + 1 == epochs:
            print(f"Epoch {epoch + 1}/{epochs}: loss={total_loss / len(labels):.4f}")
    return model


def split_dataset(labels, seed):
    random = np.random.default_rng(seed)
    training_indexes = []
    validation_indexes = []
    for label in range(OUTPUTS):
        indexes = np.flatnonzero(labels == label)
        random.shuffle(indexes)
        validation_count = max(1, int(len(indexes) * 0.2)) if len(indexes) > 1 else 0
        validation_indexes.extend(indexes[:validation_count])
        training_indexes.extend(indexes[validation_count:])
    random.shuffle(training_indexes)
    random.shuffle(validation_indexes)
    return np.asarray(training_indexes), np.asarray(validation_indexes)


def flatten_weights(model):
    parameters = (
        model.conv1.weight,
        model.conv1.bias,
        model.conv2.weight,
        model.conv2.bias,
        model.classifier.weight,
        model.classifier.bias,
    )
    values = np.concatenate(
        [parameter.detach().cpu().numpy().astype("<f4", copy=False).reshape(-1) for parameter in parameters]
    )
    if values.size != WEIGHT_COUNT:
        raise ValueError(f"Expected {WEIGHT_COUNT} weights, got {values.size}")
    return values


def save_weights_webp(weights, output):
    payload = MODEL_MAGIC + struct.pack("<BH", MODEL_VERSION, len(weights))
    payload += np.asarray(weights, dtype="<f4").tobytes()
    pixel_count = math.ceil(len(payload) / 3)
    width = 64
    height = math.ceil(pixel_count / width)
    pixels = np.zeros(width * height * 3, dtype=np.uint8)
    pixels[: len(payload)] = np.frombuffer(payload, dtype=np.uint8)
    image = Image.fromarray(pixels.reshape(height, width, 3), "RGB")
    image.save(output, format="WEBP", lossless=True, quality=100, method=6)

    with Image.open(output) as decoded:
        decoded_payload = np.asarray(decoded.convert("RGB"), dtype=np.uint8).reshape(-1)
        if decoded_payload[: len(payload)].tobytes() != payload:
            raise ValueError("Lossless WebP round-trip changed model bytes")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--epochs", type=int, default=800)
    parser.add_argument("--learning-rate", type=float, default=0.005)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument(
        "--dataset",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "app/src/androidTest/assets/clouds",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "app/src/main/assets/cloud_cnn_weights.webp",
    )
    args = parser.parse_args()

    torch.set_num_threads(1)
    inputs, labels = load_dataset(args.dataset)
    train_indexes, validation_indexes = split_dataset(labels, args.seed)
    validation_model = train(
        inputs[train_indexes],
        labels[train_indexes],
        args.epochs,
        args.learning_rate,
        args.seed,
    )
    correct = sum(
        int(np.argmax(predict(inputs[index], validation_model)) == labels[index])
        for index in validation_indexes
    )
    print(f"Validation accuracy: {correct / len(validation_indexes):.3f}")

    final_model = train(inputs, labels, args.epochs, args.learning_rate, args.seed)
    os.makedirs(args.output.parent, exist_ok=True)
    save_weights_webp(flatten_weights(final_model), args.output)
    print(f"Saved {WEIGHT_COUNT} lossless WebP weights to {args.output}")


if __name__ == "__main__":
    main()
