import csv
import datetime
import matplotlib.pyplot as plt
import scipy
import numpy as np
from scipy.integrate import solve_ivp
from statsmodels.nonparametric.smoothers_lowess import lowess
import random


def smooth(data, frac = 0.15):
    if frac * len(data) > 10:
        frac = 10 / len(data)
    smoothed = lowess(data, np.arange(len(data)), frac=frac, return_sorted=False, it=1)
    return smoothed


def derivative(data):
    return [data[i + 1] - data[i] for i in range(len(data) - 1)]


def add_noise(data, noise_level):
    noisy_data = []
    for value in data:
        noise = random.gauss(0.0, noise_level)
        noisy_data.append(value + noise)
    return noisy_data

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

index = random.choice(range(12, len(xs) - 3))
samples = list(smooth(add_noise(ys[:index], 1), 0.4))
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


def prediction_taylor_series(
    samples,
    n,
    order=3,
    smooth_fn=None,
    damping_factors=None,
    offsets=None,
    limits=None,
):
    values = [samples[:]]
    for i in range(order):
        values.append(derivative(values[-1]))
        if smooth_fn:
            values[-1] = list(smooth_fn(values[-1]))
        if offsets:
            for j in range(len(values[-1])):
                values[-1][j] += offsets[i + 1]
    
    y0 = np.array([values[i][-1] for i in range(len(values))])
    
    def ode_system(t, y):
        dydt = np.zeros_like(y)
        for i in range(len(y) - 1):
            dydt[i] = y[i + 1]
        dydt[-1] = 0  # Highest order derivative is constant
        return dydt
    
    t_span = (0, n)
    t_eval = np.arange(0, n, 1)
    
    solution = solve_ivp(ode_system, t_span, y0, t_eval=t_eval, method='RK45')
    
    predictions = solution.y[0, :]
    
    return list(predictions)


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
                order=2,
                smooth_fn=smooth,
                # damping_factors=[
                #     1.0,
                #     1.0,
                #     1.0,
                #     0.01
                # ],
                # TODO: This should just be part of the "smooth" function (rename to map)
                offsets = [
                    0.0,
                    random_value(0.0, 0.2, -1.0, 1.0),
                    random_value(0.0, 0.2, -0.5, 0.5),
                    # random_value(0.0, 0.03, -0.03, 0.03)
                ],
                limits=[[800, 1100], [-10, 10], [-5, 5], None],
            )
        )
    predictions = []
    upper = []
    lower = []
    for i in range(n):
        values = [all_predictions[j][i] for j in range(ensemble)]
        predictions.append(np.median(values))
        upper.append(np.percentile(values, 95))
        lower.append(np.percentile(values, 5))
    return predictions, upper, lower


projected, upper, lower = project_samples(samples, len(xs) - len(samples), 1)  # xs[1] - xs[0])
samples = scale(samples, np.min(original_ys), np.max(original_ys))

max_projection = 12

projected = scale(projected, np.min(original_ys), np.max(original_ys))[:max_projection]
upper = scale(upper, np.min(original_ys), np.max(original_ys))[:max_projection]
lower = scale(lower, np.min(original_ys), np.max(original_ys))[:max_projection]
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
    print(np.max(abs(last_dir)))
    plt.plot(xs[i + 1 :], last_dir)
plt.show()
