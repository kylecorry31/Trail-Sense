import argparse
import copy
import json
import math
import os
import struct
import zipfile
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torch import nn
from torch.nn import functional as F
from torch.nn.utils.fusion import fuse_conv_bn_eval


INPUT_SIZE = 128
INPUT_CHANNELS = 3
OUTPUTS = 10
# Input channels, output channels, kernel size, stride, groups.
CONVOLUTIONS = ((3, 8, 3, 2, 1), (8, 12, 3, 2, 1),
                (12, 20, 3, 2, 1), (20, 32, 3, 2, 1))
FEATURE_CHANNELS = CONVOLUTIONS[-1][1]
WEIGHT_COUNT = sum(outputs * (inputs // groups) * kernel ** 2 + outputs
                   for inputs, outputs, kernel, _, groups in CONVOLUTIONS)
WEIGHT_COUNT += OUTPUTS * FEATURE_CHANNELS * 2 + OUTPUTS
MODEL_MAGIC = b"TSCL"
MODEL_VERSION = 3
BATCH_SIZE = 32

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
]


# CCSN's Ct category is excluded: the output contains only the ten cloud genera.
CCSN_CLASSES = ("Ci", "Cc", "Cs", "As", "Ac", "Ns", "Sc", "Cu", "St", "Cb")


class CloudCNN(nn.Module):
    def __init__(self):
        super().__init__()
        layers = []
        for inputs, outputs, kernel, stride, groups in CONVOLUTIONS:
            layers.extend([
                nn.Conv2d(inputs, outputs, kernel, stride=stride, padding=kernel // 2,
                          groups=groups, bias=False),
                nn.BatchNorm2d(outputs),
                nn.ReLU(),
            ])
        self.features = nn.Sequential(*layers)
        self.classifier = nn.Linear(FEATURE_CHANNELS * 2, OUTPUTS)

    def forward(self, images):
        values = self.features(images)
        features = torch.cat((values.mean(dim=(2, 3)), values.amax(dim=(2, 3))), dim=1)
        # CrossEntropyLoss consumes logits; inference applies softmax.
        return self.classifier(features)


def inference_model(model):
    # Folding removes batch normalization's inference operations and parameters.
    result = copy.deepcopy(model).eval()
    layers = list(result.features)
    result.features = nn.Sequential(*[
        layer
        for index in range(0, len(layers), 3)
        for layer in (fuse_conv_bn_eval(layers[index], layers[index + 1]), nn.ReLU())
    ])
    return result


def load_dataset(dataset_path, input_size=INPUT_SIZE):
    inputs = []
    labels = []
    with zipfile.ZipFile(dataset_path) as archive:
        for label, category in enumerate(CCSN_CLASSES):
            paths = sorted(name for name in archive.namelist()
                           if name.startswith(f"CCSN_v2/{category}/")
                           and Path(name).suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
                           and not Path(name).name.startswith("._"))
            if not paths:
                raise ValueError(f"No training images found for class '{CLASS_NAMES[label]}'")
            print(f"{CLASS_NAMES[label]}: {len(paths)} images")
            for path in paths:
                with archive.open(path) as stream, Image.open(stream) as image:
                    image = image.convert("RGB").resize(
                        (input_size, input_size), Image.Resampling.BILINEAR
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


def augment(images):
    batch = images.clone()
    count = len(batch)
    flips = torch.rand(count) < 0.5
    batch[flips] = batch[flips].flip(-1)
    # A translated, rotated crop retaining 70-100% of the image's width/height.
    angle = (torch.rand(count) - 0.5) * 0.5
    scale = 0.7 + 0.3 * torch.rand(count)
    transform = torch.zeros(count, 2, 3)
    transform[:, 0, 0] = scale * angle.cos()
    transform[:, 1, 1] = scale * angle.cos()
    transform[:, 0, 1] = -scale * angle.sin()
    transform[:, 1, 0] = scale * angle.sin()
    transform[:, :, 2] = (torch.rand(count, 2) - 0.5) * 0.2
    grid = F.affine_grid(transform, batch.shape, align_corners=False)
    batch = F.grid_sample(batch, grid, align_corners=False, padding_mode="border")
    mean = batch.mean(dim=(2, 3), keepdim=True)
    batch = (batch - mean) * (0.8 + 0.4 * torch.rand(count, 1, 1, 1)) + mean
    batch *= 0.8 + 0.4 * torch.rand(count, 1, 1, 1)
    return (batch + torch.randn_like(batch) * 0.015).clamp(0, 1)


def evaluate(model, inputs, labels):
    model.eval()
    tensor = torch.from_numpy(np.ascontiguousarray(inputs))
    with torch.no_grad():
        logits = torch.cat([model(tensor[start:start + BATCH_SIZE])
                            for start in range(0, len(tensor), BATCH_SIZE)])
    predictions = logits.argmax(dim=1).numpy()
    confusion = np.zeros((OUTPUTS, OUTPUTS), dtype=np.int64)
    np.add.at(confusion, (labels, predictions), 1)
    return float(np.mean(predictions == labels)), confusion


def train(inputs, labels, epochs, learning_rate, seed, validation=None, stop_epoch=None):
    torch.manual_seed(seed)
    random = np.random.default_rng(seed)
    model = CloudCNN()
    optimizer = torch.optim.AdamW(model.parameters(), lr=learning_rate, weight_decay=0.01)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(
        optimizer, epochs, eta_min=learning_rate / 30
    )
    input_tensor = torch.from_numpy(np.ascontiguousarray(inputs))
    label_tensor = torch.from_numpy(labels.astype(np.int64, copy=False))
    best_accuracy = -1.0
    best_epoch = 0
    best_state = None
    history = []
    training_epochs = epochs if stop_epoch is None else stop_epoch

    for epoch in range(training_epochs):
        model.train()
        order = random.permutation(len(labels))
        total_loss = 0.0
        for start in range(0, len(order), BATCH_SIZE):
            indexes = torch.from_numpy(order[start:start + BATCH_SIZE].copy())
            batch = augment(input_tensor[indexes])
            batch_labels = label_tensor[indexes]
            optimizer.zero_grad(set_to_none=True)
            loss = F.cross_entropy(model(batch), batch_labels, label_smoothing=0.05)
            loss.backward()
            optimizer.step()
            total_loss += loss.item() * len(batch_labels)
        scheduler.step()

        if (epoch + 1) % 10 == 0 or epoch + 1 == training_epochs:
            record = {"epoch": epoch + 1, "loss": total_loss / len(labels)}
            message = f"Epoch {epoch + 1}/{training_epochs}: loss={record['loss']:.4f}"
            if validation is not None:
                accuracy, _ = evaluate(model, *validation)
                record["validation_accuracy"] = accuracy
                message += f", validation accuracy={accuracy:.4f}"
                if accuracy > best_accuracy:
                    best_accuracy = accuracy
                    best_epoch = epoch + 1
                    best_state = copy.deepcopy(model.state_dict())
            history.append(record)
            print(message, flush=True)

    if best_state is not None:
        model.load_state_dict(best_state)
    return model, best_epoch if validation is not None else training_epochs, history


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
    parameters = inference_model(model).parameters()
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
    parser.add_argument("--epochs", type=int, default=120)
    parser.add_argument("--learning-rate", type=float, default=0.003)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument(
        "--dataset",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "CCSN.zip",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "app/src/main/assets/cloud_cnn_weights.webp",
    )
    parser.add_argument("--report", type=Path, help="Optional JSON training/validation report")
    args = parser.parse_args()
    if args.epochs <= 0 or args.learning_rate <= 0:
        parser.error("epochs and learning-rate must be positive")

    torch.set_num_threads(2)
    inputs, labels = load_dataset(args.dataset)
    train_indexes, validation_indexes = split_dataset(labels, args.seed)
    validation_model, best_epoch, history = train(
        inputs[train_indexes], labels[train_indexes], args.epochs,
        args.learning_rate, args.seed,
        validation=(inputs[validation_indexes], labels[validation_indexes]),
    )
    accuracy, confusion = evaluate(
        validation_model, inputs[validation_indexes], labels[validation_indexes]
    )
    print(f"Best validation accuracy: {accuracy:.4f} (epoch {best_epoch})")
    # Use the selected duration while preserving the validation run's LR schedule.
    final_model, _, _ = train(
        inputs, labels, args.epochs, args.learning_rate, args.seed, stop_epoch=best_epoch
    )
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps({
            "seed": args.seed, "epochs": args.epochs, "learning_rate": args.learning_rate,
            "selected_epoch": best_epoch, "validation_accuracy": accuracy,
            "training_images": len(train_indexes), "validation_images": len(validation_indexes),
            "deployment_images": len(labels), "classes": CLASS_NAMES,
            "confusion_matrix": confusion.tolist(), "history": history,
            "input_size": INPUT_SIZE, "exported_parameters": WEIGHT_COUNT,
            "convolutions": CONVOLUTIONS,
            "balanced_accuracy": float(np.mean(confusion.diagonal() / confusion.sum(axis=1))),
        }, indent=2) + "\n")
    os.makedirs(args.output.parent, exist_ok=True)
    save_weights_webp(flatten_weights(final_model), args.output)
    print(f"Saved {WEIGHT_COUNT} lossless WebP weights to {args.output}")


if __name__ == "__main__":
    main()
