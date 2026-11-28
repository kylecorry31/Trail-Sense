"""
Fits the weather forecast network (WeatherNetworkCoefficients.kt) to the weather benchmark stations.

1. Download the stations: python3 scripts/download_weather_benchmark.py
2. Export the app's climate temperatures for them:
   .agents/skills/verify-trail-sense/scripts/tsctl test unit '*ClimateFeatureExport*'
3. Fit and write the network: python3 scripts/train_weather_model.py

The forecast is one network with a sigmoid output per condition, so each condition gets its own probability. Each
condition's yes or no threshold maximizes HSS on stations the network was not trained on (5 folds by site). The app
only looks at a 3 to 12 hour output when the next 3 hours output says no, so those thresholds are tuned on that decision.
The script reports HSS for the yes or no forecast with thresholds picked without the reported fold, so picking them
doesn't flatter the score, and the Brier skill score for the probability, relative to always forecasting the station's
own observed rate (above 0 means more skill than knowing the local climate). The Brier skill is the median station,
because a station that rarely sees a condition swings the mean.

The inputs are the sea level pressure, its difference from each of several hours ago, the time of day and year,
latitude and the local climate, with no hand made trends or curves; two ReLU hidden layers learn those instead and
scored better than one layer fed the hand made ones. Stations report every hour, but the app may have only a few hours
of history, recorded at the user's chosen interval, so the network also trains on a copy of every sample with its
history cut short and thinned. A difference older than the history uses the oldest reading. Averaging several networks,
two more years of the same stations, a history length input, a third layer and tanh were tested and were no better.

Each condition has an output for the next 3 hours and one for 3 to 12 hours, which the app shows as "within 3 hours"
and "in 3-12 hours". The next 3 hours includes weather already happening. The 3 to 12 hour output means the condition
starts then: it is not in the next 3 hours but is in 3 to 12 hours. For precipitation, storm, wind and thunderstorm
being in a window means any report of it there; for clear and overcast, most sky reports there. Splitting off weather
that is already happening was tested and made the next 3 hours worse, because pressure can't tell it from weather about
to start. Thunderstorm has a single output for the next 12 hours, because the network could not
tell whether thunder would come within 3 hours (held out HSS 0.02). Finer timing (1, 3, 12 and 24 hour onset windows) was tested and was no better than
always guessing the most common one.

Early stopping on held out sites stopped within a few epochs, with worse held out probabilities, so a fixed number of
epochs is used instead.
"""
import bisect
import csv
import gzip
import math
import os
from multiprocessing import Pool
from datetime import datetime, timedelta

import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), "..")
RESOURCES = os.path.join(ROOT, "app/src/test/resources/weather_benchmark")
CLIMATE_FEATURES = os.path.join(ROOT, "app/build/reports/weather-benchmark/climate_features.csv")
OUTPUT = os.path.join(ROOT, "app/src/main/java/com/kylecorry/trail_sense/tools/weather/domain/forecasting/model/WeatherNetworkCoefficients.kt")
GOLDEN = os.path.join(ROOT, "app/src/test/resources/weather_benchmark/weather_network_golden.csv")
PRECIPITATION_CODES = ("RA", "SN", "DZ", "PL", "GR", "GS", "UP", "SG", "IC")
STANDARD_PRESSURE_HPA = 1013
MINIMUM_CHANGE_HOURS = 2
# WHY: Consecutive hours are nearly identical, so every third hour keeps the fit fast without losing information
SAMPLE_HOURS = 3
HISTORY_HOURS = 48
DIFFERENCE_HOURS = [1, 2, 3, 4, 6, 8, 10, 12, 15, 18, 21, 24, 30, 36, 42, 48]
PARTIAL_HISTORY_HOURS = [3, 4, 6, 9, 12, 18, 24, 36, 48]
PARTIAL_HISTORY_MISSING = [0, 0, 0.2, 0.4, 0.6]
SOUTHERN_SEASON_OFFSET_DAYS = 182.5
DAYS_PER_YEAR = 365.25


def tide(t, latitude, longitude):
    """Must match Sol's Meteorology.getPressureTide"""
    ut = t.timestamp() / 3600 * 15
    local = ut + longitude
    c, s = math.cos(math.radians(latitude)), math.sin(math.radians(latitude))
    sin = lambda degrees: math.sin(math.radians(degrees))
    return (1.16 * c ** 3 * sin(2 * local + 158) + 0.085 * (3 * s * s - 1) / 2 * sin(2 * ut + 118)
            + 0.593 * c ** 3 * sin(local + 12))


def number(value):
    return float(value) if value else 0.0


def majority(values):
    values = [v for v in values if v is not None]
    return (sum(values) * 2 > len(values)) if values else None


def starts_later(soon, later):
    return None if soon is None or later is None else (not soon and later)


def load_rows():
    daily_temperatures = {}
    for r in csv.DictReader(open(CLIMATE_FEATURES)):
        daily_temperatures[(r["id"], r["date"])] = (float(r["low_c"]), float(r["high_c"]))
    monthly_precipitation = {}
    for r in csv.DictReader(open(os.path.join(RESOURCES, "climate_normals.csv"))):
        monthly_precipitation.setdefault(r["id"], {})[int(r["month"])] = float(r["precipitation_mm"])

    rows = []
    for station in csv.DictReader(open(os.path.join(RESOURCES, "stations.csv"))):
        sid = station["id"]
        latitude, longitude = float(station["latitude"]), float(station["longitude"])
        precipitation = monthly_precipitation[sid]
        annual_precipitation = sum(precipitation.values())
        observations = list(csv.DictReader(gzip.open(os.path.join(RESOURCES, sid + ".csv.gz"), "rt")))
        times, wet, storm, windy, thunder, clear, overcast = [], [], [], [], [], [], []
        hourly = {}
        for o in observations:
            t = datetime.fromisoformat(o["time"].replace("Z", "+00:00"))
            codes = [c for c in o["weather"].split() if not c.startswith("VC")]
            is_thunder = any("TS" in c for c in codes)
            times.append(t)
            wet.append(number(o["precipitation_mm"]) >= 0.25 or any(w in c for c in codes for w in PRECIPITATION_CODES))
            thunder.append(is_thunder)
            storm.append(is_thunder or number(o["gust_kt"]) >= 35 or number(o["precipitation_mm"]) >= 6)
            windy.append(number(o["gust_kt"]) >= 25 or number(o["wind_kt"]) >= 20)
            clear.append(o["sky_cover"] in ("CLR", "SKC", "NSC", "FEW") if o["sky_cover"] else None)
            overcast.append(o["sky_cover"] in ("BKN", "OVC", "VV") if o["sky_cover"] else None)
            if o["pressure_hpa"]:
                hourly[round(t.timestamp() / 3600)] = (t, float(o["pressure_hpa"]) - tide(t, latitude, longitude))

        events = (("precipitation", wet), ("storm", storm), ("wind", windy))
        skies = (("clear", clear), ("overcast", overcast))
        keys = sorted(hourly)
        last = times[-1]
        for k in keys:
            t = hourly[k][0]
            if k - keys[0] < 24 or t + timedelta(hours=24) > last or t.hour % SAMPLE_HOURS != 0:
                continue
            history = np.array([hourly[k - h][1] if (k - h) in hourly else np.nan for h in range(HISTORY_HOURS + 1)])
            if not has_old_enough_reading(history):
                continue
            i0 = bisect.bisect_left(times, t)
            i3, i12, i24 = (bisect.bisect_right(times, t + timedelta(hours=h)) for h in (3, 12, 24))
            low, high = daily_temperatures[(sid, t.date().isoformat())]
            rows.append(dict(
                station=sid, t=t, history=history, latitude=latitude, day_of_year=t.timetuple().tm_yday,
                low=low, high=high, precipitation_mm=precipitation[t.month], annual_precipitation_mm=annual_precipitation,
                solar_hour=(t.hour + t.minute / 60 + longitude / 15) % 24,
                **{f"{name}_next_3h": any(flags[i0:i3]) for name, flags in events},
                **{f"{name}_3h_to_12h": not any(flags[i0:i3]) and any(flags[i3:i12]) for name, flags in events},
                **{f"{name}_next_3h": majority(flags[i0:i3]) for name, flags in skies},
                **{f"{name}_3h_to_12h": starts_later(majority(flags[i0:i3]), majority(flags[i3:i12])) for name, flags in skies},
                thunderstorm_next_12h=any(thunder[i0:i12]), precipitation_12h_to_24h=any(wet[i12:i24]), clear_12h_to_24h=majority(clear[i12:i24]),
                overcast_12h_to_24h=majority(overcast[i12:i24]),
            ))
    return rows


def has_old_enough_reading(history):
    return not np.all(np.isnan(history[MINIMUM_CHANGE_HOURS:]))


def pressure_differences(histories):
    """Must match WeatherModel.getPressureDifference"""
    ages = np.arange(histories.shape[1], dtype=float)
    differences = np.zeros((len(histories), len(DIFFERENCE_HOURS)))
    for i, history in enumerate(histories):
        present = ~np.isnan(history)
        differences[i] = history[0] - np.interp(DIFFERENCE_HOURS, ages[present], history[present])
    return differences


def partial_histories(histories, rng):
    """Each history cut to a random length and with a random share of readings missing, as the app might record it"""
    ages = np.arange(histories.shape[1])
    hours = rng.choice(PARTIAL_HISTORY_HOURS, len(histories))
    missing = rng.choice(PARTIAL_HISTORY_MISSING, len(histories))
    removed = (ages[None, :] > hours[:, None]) | (rng.random(histories.shape) < missing[:, None])
    removed[:, 0] = False
    partial = np.where(removed, np.nan, histories)
    return partial, np.array([has_old_enough_reading(h) for h in partial])


def features(rows, histories):
    """Must match WeatherModel.getFeatures"""
    column = lambda name: np.array([r[name] for r in rows], float)
    solar_hour = column("solar_hour")
    latitude = column("latitude")
    season = 2 * np.pi * (column("day_of_year") + np.where(latitude < 0, SOUTHERN_SEASON_OFFSET_DAYS, 0)) / DAYS_PER_YEAR
    differences = pressure_differences(histories)
    return {
        "pressure_from_standard": histories[:, 0] - STANDARD_PRESSURE_HPA,
        **{f"pressure_difference_{h}h": differences[:, i] for i, h in enumerate(DIFFERENCE_HOURS)},
        "solar_hour_sine": np.sin(2 * np.pi * solar_hour / 24),
        "solar_hour_cosine": np.cos(2 * np.pi * solar_hour / 24),
        "season_sine": np.sin(season),
        "season_cosine": np.cos(season),
        "absolute_latitude": np.abs(latitude),
        "low_temperature": column("low"),
        "high_temperature": column("high"),
        "log_monthly_precipitation": np.log1p(column("precipitation_mm")),
        "log_annual_precipitation": np.log1p(column("annual_precipitation_mm")),
    }


DIFFERENCES = [f"pressure_difference_{h}h" for h in DIFFERENCE_HOURS]
FEATURES = ["pressure_from_standard"] + DIFFERENCES + [
    "solar_hour_sine", "solar_hour_cosine", "season_sine", "season_cosine", "absolute_latitude",
    "low_temperature", "high_temperature", "log_monthly_precipitation", "log_annual_precipitation"]
CONDITIONS = [f"{name}_{window}" for window in ("next_3h", "3h_to_12h")
              for name in ("precipitation", "storm", "wind", "clear", "overcast")] + [
    "thunderstorm_next_12h", "precipitation_12h_to_24h", "clear_12h_to_24h", "overcast_12h_to_24h"]
HIDDEN_LAYERS = [32, 32]
L2_PENALTY = 1e-3
EPOCHS = 60
BATCH_SIZE = 512
LEARNING_RATE = 3e-3
FOLDS = 5
SEED = 0
# WHY: Hurricane event stations repeat the site of a normal station, so they must be held out together
SITES = {"MSY_IDA_2021": "MSY"}


class Network:
    """ReLU hidden layers and a sigmoid output per condition, matching Sol's NeuralNetwork relu and sigmoid layers"""

    def __init__(self, mean, scale, layers):
        self.mean, self.scale, self.layers = mean, scale, layers

    def predict(self, X):
        values = (X - self.mean) / self.scale
        for weights, bias in self.layers[:-1]:
            values = np.maximum(values @ weights + bias, 0)
        weights, bias = self.layers[-1]
        return 1 / (1 + np.exp(-(values @ weights + bias)))

    def raw_layers(self):
        """The layers with the standardization folded into the first layer, so the network takes raw inputs"""
        weights, bias = self.layers[0]
        first = (weights / self.scale[:, None], bias - (self.mean / self.scale) @ weights)
        return [first] + self.layers[1:]


def train(X, Y, known, seed=SEED):
    """Adam on the cross entropy of the known labels, with He initialization for the ReLU layers"""
    rng = np.random.default_rng(seed)
    mean, scale = X.mean(0), X.std(0) + 1e-9
    Z = (X - mean) / scale
    rate = np.clip((Y * known).sum(0) / np.maximum(known.sum(0), 1), 1e-3, 1 - 1e-3)
    sizes = [Z.shape[1]] + HIDDEN_LAYERS
    params = []
    for inputs, outputs in zip(sizes, sizes[1:]):
        params += [rng.normal(0, math.sqrt(2 / inputs), (inputs, outputs)), np.zeros(outputs)]
    params += [rng.normal(0, math.sqrt(1 / sizes[-1]), (sizes[-1], Y.shape[1])), np.log(rate / (1 - rate))]
    first_moment = [np.zeros_like(p) for p in params]
    second_moment = [np.zeros_like(p) for p in params]
    step = 0
    for _ in range(EPOCHS):
        order = rng.permutation(len(Z))
        for start in range(0, len(Z), BATCH_SIZE):
            batch = order[start:start + BATCH_SIZE]
            activations, pre_activations = [Z[batch]], []
            for i in range(0, len(params) - 2, 2):
                pre_activations.append(activations[-1] @ params[i] + params[i + 1])
                activations.append(np.maximum(pre_activations[-1], 0))
            probability = 1 / (1 + np.exp(-(activations[-1] @ params[-2] + params[-1])))
            delta = (probability - Y[batch]) * known[batch] / max(known[batch].sum(), 1)
            gradients = [None] * len(params)
            for layer in range(len(params) // 2 - 1, -1, -1):
                weights = params[2 * layer]
                gradients[2 * layer] = activations[layer].T @ delta + L2_PENALTY * weights
                gradients[2 * layer + 1] = delta.sum(0)
                if layer > 0:
                    delta = (delta @ weights.T) * (pre_activations[layer - 1] > 0)
            step += 1
            for i, gradient in enumerate(gradients):
                first_moment[i] = 0.9 * first_moment[i] + 0.1 * gradient
                second_moment[i] = 0.999 * second_moment[i] + 0.001 * gradient ** 2
                params[i] -= LEARNING_RATE * (first_moment[i] / (1 - 0.9 ** step)) / (
                        np.sqrt(second_moment[i] / (1 - 0.999 ** step)) + 1e-8)
    return Network(mean, scale, [(params[i], params[i + 1]) for i in range(0, len(params), 2)])


def hss(predicted, observed):
    a = np.sum(predicted & observed)
    b = np.sum(predicted & ~observed)
    c = np.sum(~predicted & observed)
    d = np.sum(~predicted & ~observed)
    n = a + b + c + d
    expected = ((a + c) * (a + b) + (c + d) * (b + d)) / n
    return (a + d - expected) / (n - expected) if n != expected else 0.0


def brier_skill_score(probability, observed):
    rate = observed.mean()
    reference = rate * (1 - rate)
    return 1 - np.mean((probability - observed) ** 2) / reference if reference > 0 else 0.0


def best_threshold(probability, observed):
    order = np.argsort(-probability)
    sorted_observed = observed[order]
    n = len(observed)
    positives = sorted_observed.sum()
    a = np.cumsum(sorted_observed)
    b = np.arange(1, n + 1) - a
    c = positives - a
    d = n - positives - b
    expected = ((a + c) * (a + b) + (c + d) * (b + d)) / n
    return probability[order][int(np.argmax((a + d - expected) / np.maximum(n - expected, 1e-9)))]


def train_fold(args):
    X, Y, known, train_mask = args
    return train(X[train_mask], Y[train_mask], known[train_mask])


def kotlin_floats(values):
    return "floatArrayOf(" + ", ".join(f"{v:.7g}f" if np.float32(v) != 0 else "0f" for v in values) + ")"


def kotlin_matrix(rows, indent):
    return "Matrix.create(\n" + indent + "    arrayOf(\n" + ",\n".join(
        indent + "        " + kotlin_floats(row) for row in rows) + "\n" + indent + "    )\n" + indent + ")"


def camel_case(name, capitalize=False):
    words = name.split("_")
    head = words[0].capitalize() if capitalize else words[0]
    return head + "".join(word.capitalize() for word in words[1:])


def write_kotlin(network, thresholds):
    indent = "        "
    layer = lambda weights, bias: (f"    NeuralNetwork.LayerWeights(\n{indent}{kotlin_matrix(weights.T, indent)},\n"
                                   f"{indent}{kotlin_matrix(bias[:, None], indent)}\n    )")
    with open(OUTPUT, "w") as f:
        f.write(f"""package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.classifiers.NeuralNetwork

// WHY: Generated by scripts/train_weather_model.py, do not edit by hand
internal enum class WeatherOutput(val threshold: Float) {{
{",\n".join(f"    {camel_case(c, True)}({t:.7g}f)" for c, t in zip(CONDITIONS, thresholds))}
}}

internal object WeatherNetworkCoefficients {{
    val hiddenLayerSizes = listOf({", ".join(str(n) for n in HIDDEN_LAYERS)})

    val features = listOf(
{",\n".join(f'        "{n}"' for n in FEATURES)}
    )

    val layers = listOf(
{",\n".join("    " + layer(w, b) for w, b in network.raw_layers())}
    )
}}
""")


def write_golden(network, X):
    """A few inputs and the probabilities the network gives, so a Kotlin test can check the exported network"""
    rng = np.random.default_rng(SEED)
    sample = X[rng.choice(len(X), 5, replace=False)]
    probability = network.predict(sample)
    with open(GOLDEN, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(FEATURES + CONDITIONS)
        for features, outputs in zip(sample, probability):
            writer.writerow([f"{v:.7g}" for v in features] + [f"{v:.7g}" for v in outputs])


def later_gates(probability, thresholds):
    """For each 3 to 12 hour output, the samples where the app looks at it: nothing predicted for the next 3 hours"""
    column = {c: j for j, c in enumerate(CONDITIONS)}
    below = lambda condition: probability[:, column[condition]] < thresholds[column[condition]]
    no_sky_soon = below("clear_next_3h") & below("overcast_next_3h")
    gates = {column[f"{name}_3h_to_12h"]: below(f"{name}_next_3h") for name in ("precipitation", "storm", "wind")}
    gates.update({column[f"{name}_3h_to_12h"]: no_sky_soon for name in ("clear", "overcast")})
    return gates


def best_thresholds(probability, Y, known):
    thresholds = np.array([best_threshold(probability[known[:, j], j], Y[known[:, j], j] > 0) for j in range(len(CONDITIONS))])
    for j, gate in later_gates(probability, thresholds).items():
        gated = np.where(gate, probability[:, j], -1.0)[known[:, j]]
        thresholds[j] = best_threshold(gated, Y[known[:, j], j] > 0)
    return thresholds


def forecast(probability, thresholds):
    predicted = probability >= thresholds
    for j, gate in later_gates(probability, thresholds).items():
        predicted[:, j] &= gate
    return predicted


def main():
    rows = load_rows()
    stations = np.array([r["station"] for r in rows])
    sites = np.array([SITES.get(s, s) for s in stations])
    histories = np.array([r["history"] for r in rows])
    terms = features(rows, histories)
    X = np.column_stack([terms[n] for n in FEATURES])
    known = np.column_stack([[r[c] is not None for r in rows] for c in CONDITIONS])
    Y = np.column_stack([[bool(r[c]) for r in rows] for c in CONDITIONS]).astype(float)

    order = np.random.default_rng(SEED).permutation(sorted(set(sites)))
    fold_of_site = {site: i % FOLDS for i, site in enumerate(order)}
    fold = np.array([fold_of_site[site] for site in sites])

    partial, usable = partial_histories(histories, np.random.default_rng(SEED))
    partial_terms = features([r for r, u in zip(rows, usable) if u], partial[usable])
    X_train = np.vstack([X, np.column_stack([partial_terms[n] for n in FEATURES])])
    Y_train, known_train = np.vstack([Y, Y[usable]]), np.vstack([known, known[usable]])
    fold_train = np.concatenate([fold, fold[usable]])
    train_masks = [fold_train != k for k in range(FOLDS)] + [np.ones(len(X_train), bool)]
    with Pool() as pool:
        *fold_networks, network = pool.map(
            train_fold, [(X_train, Y_train, known_train.astype(float), m) for m in train_masks])

    held_out = np.zeros(Y.shape)
    for k, fold_network in enumerate(fold_networks):
        held_out[fold == k] = fold_network.predict(X[fold == k])

    thresholds = best_thresholds(held_out, Y, known)
    predicted = np.zeros(Y.shape, bool)
    for k in range(FOLDS):
        other = fold != k
        predicted[fold == k] = forecast(held_out[fold == k], best_thresholds(held_out[other], Y[other], known[other]))

    for j, condition in enumerate(CONDITIONS):
        valid, observed = known[:, j], Y[:, j] > 0
        evaluated = [valid & (stations == s) for s in sorted(set(stations)) if (valid & (stations == s)).sum() > 100]
        station_hss = [hss(predicted[m, j], observed[m]) for m in evaluated]
        station_bss = [brier_skill_score(held_out[m, j], observed[m]) for m in evaluated]
        print(f"{condition}: held out station mean HSS {np.mean(station_hss):.3f}, pooled "
              f"{hss(predicted[valid, j], observed[valid]):.3f}; station median Brier skill "
              f"{np.median(station_bss):.3f}", flush=True)

    write_kotlin(network, thresholds)
    write_golden(network, X)
    print(f"Wrote {OUTPUT} and {GOLDEN}")


if __name__ == "__main__":
    main()
