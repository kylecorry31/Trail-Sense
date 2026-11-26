"""Train the Cloud Scanner model from the features the app computes on device.

Usage: python3 scripts/train-cloud-model.py [app/src/data/clouds.csv]
Requires numpy and scipy. See docs/development/cloud-model.md.
"""
import csv
import re
import sys

import numpy as np
from scipy.optimize import minimize

CLASSES = 11
L2 = 1e-3
FOLDS = 5
SEEDS = 10
MIN_CLASS_SIZE_FOR_F1 = 3


def load(path):
    rows = list(csv.reader(open(path)))
    files = [row[1].split("/")[-1] for row in rows]
    # WHY: The same photo filed under two genera has no single correct label.
    rows = [row for row, file in zip(rows, files) if files.count(file) == 1]
    labels = np.array([int(row[0]) for row in rows])
    features = np.array([[float(v) for v in row[2:]] for row in rows])
    groups = np.array([capture_date(row[1]) for row in rows])
    return labels, features, groups


def capture_date(name):
    # WHY: Burst photos taken seconds apart must stay on the same side of a train/test split.
    match = re.search(r"PXL_(\d{8})_", name)
    return match.group(1) if match else name


def softmax(z):
    e = np.exp(z - z.max(axis=1, keepdims=True))
    return e / e.sum(axis=1, keepdims=True)


def fit(x, y):
    """Returns weights (features + 1, CLASSES) for raw features, with standardization folded in."""
    mean = x.mean(axis=0)
    scale = np.where(x.std(axis=0) < 1e-9, 1, x.std(axis=0))
    xs = np.column_stack([(x - mean) / scale, np.ones(len(x))])
    onehot = np.eye(CLASSES)[y]
    shape = (xs.shape[1], CLASSES)

    def loss(flat):
        w = flat.reshape(shape)
        p = softmax(xs @ w)
        value = -np.mean(np.log(np.sum(p * onehot, axis=1) + 1e-12)) + L2 * np.sum(w[:-1] ** 2)
        grad = xs.T @ (p - onehot) / len(y)
        grad[:-1] += 2 * L2 * w[:-1]
        return value, grad.ravel()

    w = minimize(loss, np.zeros(np.prod(shape)), jac=True, method="L-BFGS-B", options={"maxiter": 500}).x.reshape(shape)
    weights = w[:-1] / scale[:, None]
    bias = w[-1] - (mean / scale) @ w[:-1]
    return np.vstack([weights, bias])


def predict(weights, x):
    return softmax(np.column_stack([x, np.ones(len(x))]) @ weights).argmax(axis=1)


def grouped_folds(labels, groups, seed):
    rng = np.random.default_rng(seed)
    unique = np.array(sorted(set(groups)))
    rng.shuffle(unique)
    members = {g: np.flatnonzero(groups == g) for g in unique}
    counts = np.zeros((FOLDS, CLASSES))
    assignment = {}
    for g in sorted(unique, key=lambda g: -len(members[g])):
        histogram = np.bincount(labels[members[g]], minlength=CLASSES)
        cost = (counts * histogram).sum(axis=1) + counts.sum(axis=1) * 1e-3 + rng.random(FOLDS) * 1e-6
        fold = int(np.argmin(cost))
        assignment[g] = fold
        counts[fold] += histogram
    return np.array([assignment[g] for g in groups])


def macro_f1(labels, predicted):
    scores = []
    for c in np.unique(labels):
        if np.sum(labels == c) < MIN_CLASS_SIZE_FOR_F1:
            continue
        tp = np.sum((predicted == c) & (labels == c))
        wrong = np.sum((predicted == c) != (labels == c))
        scores.append(2 * tp / (2 * tp + wrong) if tp else 0.0)
    return float(np.mean(scores))


def cross_validate(labels, features, groups):
    accuracy, f1 = [], []
    for seed in range(SEEDS):
        folds = grouped_folds(labels, groups, seed)
        predicted = np.zeros(len(labels), dtype=int)
        for fold in range(FOLDS):
            train, test = folds != fold, folds == fold
            predicted[test] = predict(fit(features[train], labels[train]), features[test])
        accuracy.append(np.mean(predicted == labels))
        f1.append(macro_f1(labels, predicted))
    return float(np.median(accuracy)), float(np.median(f1))


def main():
    labels, features, groups = load(sys.argv[1] if len(sys.argv) > 1 else "app/src/data/clouds.csv")
    print(f"{len(labels)} photos, {len(set(groups))} capture dates, per class {np.bincount(labels, minlength=CLASSES).tolist()}")
    accuracy, f1 = cross_validate(labels, features, groups)
    print(f"Grouped {FOLDS}-fold CV over {SEEDS} seeds: accuracy {accuracy:.3f}, macro F1 {f1:.3f}")
    weights = fit(features, labels)
    print(f"Training accuracy: {np.mean(predict(weights, features) == labels):.3f}")
    print("Weights for SoftmaxCloudClassifier.kt:")
    print("arrayOf(\n" + ",\n".join("    floatArrayOf(" + ", ".join(f"{v:.6g}f" for v in row) + ")" for row in weights) + "\n)")


if __name__ == "__main__":
    main()
