import csv
import datetime
import matplotlib.pyplot as plt
import numpy as np
from statsmodels.nonparametric.smoothers_lowess import lowess


def smooth(data):
    frac = 0.15
    if frac * len(data) > 10:
        frac = 10 / len(data)
    smoothed = lowess(data, np.arange(len(data)), frac=frac, return_sorted=False, it=1)
    return smoothed

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

samples = list(smooth(ys[:32]))
original_ys = ys[:]
ys = smooth(ys)
dys = smooth([ys[i + 1] - ys[i] for i in range(len(ys) - 1)])
ddys = smooth([dys[i + 1] - dys[i] for i in range(len(dys) - 1)])


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
        values.append(
            [values[-1][j + 1] - values[-1][j] for j in range(len(values[-1]) - 1)]
        )
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


def project_samples(samples, n, dx):
    # Continue to 1 sample past the peak. If there is no peak, then only return the next 5 samples
    return prediction(
        samples,
        n,
        order=4,
        smooth_fn=smooth,
        damping_factors=[1.0, 0.95, 0.9, 0.2, 0.2],
        limits=[[800, 1100], [-10, 10], [-5, 5], None, None],
    )

projected = project_samples(samples, len(xs) - len(samples), 1)  # xs[1] - xs[0])
samples = scale(samples, np.min(original_ys), np.max(original_ys))
projected = scale(projected, np.min(original_ys), np.max(original_ys))
original_ys = scale(original_ys)
ys = scale(ys)

plt.plot(xs[: len(samples)], samples, "o")
plt.plot(xs[len(samples) :], projected, "o")
plt.plot(xs, original_ys)
plt.plot(xs, ys)
plt.plot(xs[1:], dys)
plt.plot(xs[2:], ddys)
plt.show()
