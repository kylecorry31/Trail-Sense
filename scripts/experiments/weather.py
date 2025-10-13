import csv
import datetime
import matplotlib.pyplot as plt
import scipy
import numpy as np
from statsmodels.nonparametric.smoothers_lowess import lowess
import random


def smooth(data):
    frac = 0.15
    if frac * len(data) > 10:
        frac = 10 / len(data)
    smoothed = lowess(data, np.arange(len(data)), frac=frac, return_sorted=False, it=1)
    return smoothed


def derivative(data):
    return [data[i + 1] - data[i] for i in range(len(data) - 1)]


def integral(initial_value, values, dt, damping_factor=1.0, limit=None):
    result = []
    current = initial_value
    for i in range(len(values)):
        current += values[i] * dt
        current *= damping_factor
        if limit is not None:
            current = min(max(current, limit[0]), limit[1])
        result.append(current)
    return result


# Load weather_test_data.csv
with open("weather_test_data.csv", "r") as csvfile:
    reader = csv.DictReader(csvfile)
    weather_data = [
        [datetime.datetime.fromisoformat(row["time"] + "Z"), float(row["pressure_msl"])]
        for row in reader
    ]

start = weather_data[0][0]
xs = [(t - start).total_seconds() / 3600 for t, _ in weather_data]
ys = [p for _, p in weather_data]

samples = list(smooth(ys[:16]))
original_ys = ys[:]
ys = smooth(ys)


def scale(a, a_min=None, a_max=None):
    a = np.asarray(a, dtype=float)
    if a_min is None:
        a_min = np.min(a)
    if a_max is None:
        a_max = np.max(a)
    if a_max == a_min:
        return np.zeros_like(a)
    return 2 * (a - a_min) / (a_max - a_min) - 1


def project_next(dx, position, velocity, acceleration):
    new_position = position + velocity * dx + 0.5 * acceleration * dx * dx
    new_velocity = velocity + acceleration * dx
    # print(position, velocity, acceleration)
    return new_position, new_velocity


def factorial(n):
    if n == 0:
        return 1
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


def prediction_taylor_series(
    samples,
    n,
    order=3,
    smooth_fn=None,
    damping_factors=None,
    limits=None,
):
    values = [samples[:]]
    for i in range(order):
        values.append(derivative(values[-1]))
        if smooth_fn:
            values[-1] = list(smooth_fn(values[-1]))
    predictions = []
    for _ in range(n):
        coefs = [values[i][-1] for i in range(len(values))]
        next_coefs = []
        for i in range(len(coefs)):
            next_value = coefs[i]
            for j in range(i + 1, len(coefs)):
                next_value += coefs[j] * 1 / factorial(j - i)
            next_coefs.append(float(next_value))
        if damping_factors:
            for i in range(len(next_coefs)):
                next_coefs[i] *= damping_factors[i]
        if limits:
            for i in range(len(next_coefs)):
                limit = limits[i]
                if (
                    limit is not None
                    and next_coefs[i] >= limit[0]
                    and next_coefs[i] <= limit[1]
                ):
                    next_coefs[i] = min(max(next_coefs[i], limit[0]), limit[1])
        predictions.append(next_coefs[0])
        for i in range(len(values)):
            values[i].append(next_coefs[i])
    return predictions


def prediction(
    samples,
    n,
    order=3,
    smooth_fn=None,
    damping_factors=None,
    limits=None,
):
    values = [samples[:]]
    for i in range(order):
        values.append(derivative(values[-1]))
        if smooth_fn:
            values[-1] = list(smooth_fn(values[-1]))
    predictions = [[0] * n]

    for i in range(order + 1):
        index = order - i
        predictions.append(
            integral(
                values[index][-1],
                predictions[-1],
                1,
                damping_factors[index] if damping_factors is not None else 1.0,
                limits[index] if limits is not None else None,
            )
        )
    return predictions[-1]


def random_value(center, deviation, minimum, maximum):
    value = random.gauss(center, deviation)
    return min(max(value, minimum), maximum)


def project_samples(samples, n, dx):
    all_predictions = []
    ensemble = 100
    for i in range(ensemble):
        all_predictions.append(
            prediction_taylor_series(
                samples,
                n,
                order=3,
                smooth_fn=smooth,
                damping_factors=[
                    1.0,
                    random_value(1.0, 0.1, 0.8, 1.0),
                    random_value(0.9, 0.5, 0.0, 1.0),
                    random_value(0.3, 0.5, 0.0, 0.8)
                ],
                limits=[[800, 1100], [-10, 10], [-5, 5], None],
            )
        )
    predictions = []
    upper = []
    lower = []
    for i in range(n):
        values = [all_predictions[j][i] for j in range(ensemble)]
        # TODO: Confidence interval
        predictions.append(np.mean(values))
        upper.append(np.percentile(values, 95))
        lower.append(np.percentile(values, 5))
    return predictions, upper, lower


projected, upper, lower = project_samples(samples, len(xs) - len(samples), 1)  # xs[1] - xs[0])
samples = scale(samples, np.min(original_ys), np.max(original_ys))
projected = scale(projected, np.min(original_ys), np.max(original_ys))[:12]
upper = scale(upper, np.min(original_ys), np.max(original_ys))[:12]
lower = scale(lower, np.min(original_ys), np.max(original_ys))[:12]
unscaled_ys = ys
original_ys = scale(original_ys)
ys = scale(ys)

plt.plot(xs[: len(samples)], samples, "o")
# confidence interval
plt.fill_between(xs[len(samples) : len(samples) + len(projected)], lower, upper, color="gray", alpha=0.5)
plt.plot(xs[len(samples) : len(samples) + len(projected)], projected, "o")
plt.plot(xs, original_ys)
plt.plot(xs, ys)
last_dir = unscaled_ys
for i in range(3):
    last_dir = smooth(derivative(last_dir))
    plt.plot(xs[i + 1 :], last_dir)
plt.show()
