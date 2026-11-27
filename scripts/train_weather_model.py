"""
Fits the weather forecast network (WeatherNetworkCoefficients.kt) to the weather benchmark stations.

1. Download the stations: python3 scripts/download_weather_benchmark.py
2. Export the app's climate temperatures for them:
   .agents/skills/verify-trail-sense/scripts/tsctl test unit '*ClimateFeatureExport*'
3. Fit and write the network: python3 scripts/train_weather_model.py

The forecast is one network with a sigmoid output per condition, so each condition gets its own probability. Each
condition's yes or no threshold maximizes HSS on stations the network was not trained on (5 folds by site), and those
held out predictions are also what the script reports: HSS for the yes or no forecast and the Brier skill score for the
probability, relative to always forecasting the station's own observed rate (above 0 means more skill than knowing the
local climate). The Brier skill is the median station, because a station that rarely sees a condition swings the mean.

Each condition has an output for the next 3 hours and one for 3 to 12 hours, which the app shows as "within 3 hours"
and "in 3-12 hours". The next 3 hours includes weather already happening. The 3 to 12 hour output means the condition
starts then: it is not in the next 3 hours but is in 3 to 12 hours. For precipitation, storm, wind and thunderstorm
being in a window means any report of it there; for clear and overcast, most sky reports there. Splitting off weather
that is already happening was tested and made the next 3 hours worse, because pressure can't tell it from weather about
to start. Thunderstorm has a single output for the next 12 hours, because the network could not
tell whether thunder would come within 3 hours (held out HSS 0.02). Finer timing (1, 3, 12 and 24 hour onset windows) was tested and was no better than
always guessing the most common one.

The network size, L2 penalty and epochs scored best on held out sites and held out continents. Early stopping on held
out sites stopped within a few epochs, with worse held out probabilities, so a fixed number of epochs is used instead.
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
MINIMUM_FIT_READINGS = 3
# WHY: Consecutive hours are nearly identical, so every third hour keeps the fit fast without losing information
SAMPLE_HOURS = 3


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
            t, p = hourly[k]
            if k - keys[0] < 24 or t + timedelta(hours=24) > last or t.hour % SAMPLE_HOURS != 0:
                continue
            past = [(h, hourly[k - h][1]) for h in range(1, 49) if (k - h) in hourly]
            old_enough = [x for x in past if x[0] >= MINIMUM_CHANGE_HOURS]
            if not old_enough:
                continue

            def change(window):
                """Must match WeatherModel.getPressureChange"""
                recent = [(0, p)] + [x for x in past if x[0] <= window]
                if len(recent) >= MINIMUM_FIT_READINGS and recent[-1][0] >= MINIMUM_CHANGE_HOURS:
                    ages = np.array([h for h, _ in recent], float)
                    values = np.array([v for _, v in recent])
                    return -np.polyfit(ages, values, 1)[0] * window
                hours, value = min(old_enough, key=lambda x: abs(x[0] - window))
                return (p - value) * window / hours

            window48 = [p] + [v for _, v in past]
            i0 = bisect.bisect_left(times, t)
            i3, i12, i24 = (bisect.bisect_right(times, t + timedelta(hours=h)) for h in (3, 12, 24))
            low, high = daily_temperatures[(sid, t.date().isoformat())]
            rows.append(dict(
                station=sid, t=t, pressure=p, change_3h=change(3), change_12h=change(12), change_24h=change(24),
                range_48h=max(window48) - min(window48),
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


def features(rows):
    """Must match WeatherModel.getFeatures"""
    column = lambda name: np.array([r[name] for r in rows], float)
    pressure_from_standard = column("pressure") - STANDARD_PRESSURE_HPA
    change_3h, change_12h, change_24h = column("change_3h"), column("change_12h"), column("change_24h")
    low, high = column("low"), column("high")
    month_precipitation = column("precipitation_mm")
    annual_precipitation = column("annual_precipitation_mm")
    solar_hour = column("solar_hour")
    return {
        "pressure_from_standard": pressure_from_standard,
        "change_3h": change_3h,
        "change_12h": change_12h,
        "change_24h": change_24h,
        "range_48h": column("range_48h"),
        "solar_hour_sine": np.sin(2 * np.pi * solar_hour / 24),
        "solar_hour_cosine": np.cos(2 * np.pi * solar_hour / 24),
        "pressure_from_standard_squared": pressure_from_standard ** 2,
        "change_3h_squared": change_3h ** 2,
        "change_12h_squared": change_12h ** 2,
        "change_24h_squared": change_24h ** 2,
        "pressure_from_standard_x_change_12h": pressure_from_standard * change_12h,
        "pressure_from_standard_x_change_3h": pressure_from_standard * change_3h,
        "change_3h_x_change_12h": change_3h * change_12h,
        "fall_3h": np.minimum(change_3h, 0),
        "fall_12h": np.minimum(change_12h, 0),
        "mean_temperature": (low + high) / 2,
        "high_temperature": high,
        "temperature_range": high - low,
        "log_monthly_precipitation": np.log1p(month_precipitation),
        "monthly_precipitation_share": month_precipitation * 12 / np.maximum(annual_precipitation, 1),
        "log_annual_precipitation": np.log1p(annual_precipitation),
    }


CORE = ["pressure_from_standard", "change_3h", "change_12h", "change_24h", "range_48h",
        "solar_hour_sine", "solar_hour_cosine"]
PRESSURE_CURVES = ["pressure_from_standard_squared", "change_3h_squared", "change_12h_squared", "change_24h_squared",
                   "pressure_from_standard_x_change_12h", "pressure_from_standard_x_change_3h",
                   "change_3h_x_change_12h", "fall_3h", "fall_12h"]
CLIMATE = ["mean_temperature", "high_temperature", "temperature_range",
           "log_monthly_precipitation", "monthly_precipitation_share", "log_annual_precipitation"]
FEATURES = CORE + PRESSURE_CURVES + CLIMATE
CONDITIONS = [f"{name}_{window}" for window in ("next_3h", "3h_to_12h")
              for name in ("precipitation", "storm", "wind", "clear", "overcast")] + [
    "thunderstorm_next_12h", "precipitation_12h_to_24h", "clear_12h_to_24h", "overcast_12h_to_24h"]
HIDDEN_UNITS = 32
L2_PENALTY = 1e-3
EPOCHS = 60
BATCH_SIZE = 512
LEARNING_RATE = 3e-3
FOLDS = 5
SEED = 0
# WHY: Hurricane event stations repeat the site of a normal station, so they must be held out together
SITES = {"MSY_IDA_2021": "MSY"}


class Network:
    """A ReLU hidden layer and a sigmoid output per condition, matching Sol's NeuralNetwork relu and sigmoid layers"""

    def __init__(self, mean, scale, hidden_weights, hidden_bias, output_weights, output_bias):
        self.mean, self.scale = mean, scale
        self.hidden_weights, self.hidden_bias = hidden_weights, hidden_bias
        self.output_weights, self.output_bias = output_weights, output_bias

    def predict(self, X):
        hidden = np.maximum((X - self.mean) / self.scale @ self.hidden_weights + self.hidden_bias, 0)
        return 1 / (1 + np.exp(-(hidden @ self.output_weights + self.output_bias)))

    def raw_layers(self):
        """The layers with the standardization folded into the hidden layer, so the network takes raw inputs"""
        hidden_weights = self.hidden_weights / self.scale[:, None]
        hidden_bias = self.hidden_bias - (self.mean / self.scale) @ self.hidden_weights
        return (hidden_weights, hidden_bias), (self.output_weights, self.output_bias)


def train(X, Y, known, seed=SEED):
    """Adam on the cross entropy of the known labels, with He initialization for the ReLU layer"""
    rng = np.random.default_rng(seed)
    mean, scale = X.mean(0), X.std(0) + 1e-9
    Z = (X - mean) / scale
    rate = np.clip((Y * known).sum(0) / np.maximum(known.sum(0), 1), 1e-3, 1 - 1e-3)
    params = [
        rng.normal(0, math.sqrt(2 / Z.shape[1]), (Z.shape[1], HIDDEN_UNITS)),
        np.zeros(HIDDEN_UNITS),
        rng.normal(0, math.sqrt(1 / HIDDEN_UNITS), (HIDDEN_UNITS, Y.shape[1])),
        np.log(rate / (1 - rate)),
    ]
    first_moment = [np.zeros_like(p) for p in params]
    second_moment = [np.zeros_like(p) for p in params]
    step = 0
    for _ in range(EPOCHS):
        order = rng.permutation(len(Z))
        for start in range(0, len(Z), BATCH_SIZE):
            batch = order[start:start + BATCH_SIZE]
            hidden_input = Z[batch] @ params[0] + params[1]
            hidden = np.maximum(hidden_input, 0)
            probability = 1 / (1 + np.exp(-(hidden @ params[2] + params[3])))
            output_delta = (probability - Y[batch]) * known[batch] / max(known[batch].sum(), 1)
            hidden_delta = (output_delta @ params[2].T) * (hidden_input > 0)
            gradients = [
                Z[batch].T @ hidden_delta + L2_PENALTY * params[0],
                hidden_delta.sum(0),
                hidden.T @ output_delta + L2_PENALTY * params[2],
                output_delta.sum(0),
            ]
            step += 1
            for i, gradient in enumerate(gradients):
                first_moment[i] = 0.9 * first_moment[i] + 0.1 * gradient
                second_moment[i] = 0.999 * second_moment[i] + 0.001 * gradient ** 2
                params[i] -= LEARNING_RATE * (first_moment[i] / (1 - 0.9 ** step)) / (
                        np.sqrt(second_moment[i] / (1 - 0.999 ** step)) + 1e-8)
    return Network(mean, scale, *params)


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
    return "floatArrayOf(" + ", ".join(f"{v:.7g}f" for v in values) + ")"


def kotlin_matrix(rows, indent):
    return "Matrix.create(\n" + indent + "    arrayOf(\n" + ",\n".join(
        indent + "        " + kotlin_floats(row) for row in rows) + "\n" + indent + "    )\n" + indent + ")"


def camel_case(name, capitalize=False):
    words = name.split("_")
    head = words[0].capitalize() if capitalize else words[0]
    return head + "".join(word.capitalize() for word in words[1:])


def write_kotlin(network, thresholds):
    (hidden_weights, hidden_bias), (output_weights, output_bias) = network.raw_layers()
    indent = "        "
    layer = lambda weights, bias: (f"NeuralNetwork.LayerWeights(\n{indent}{kotlin_matrix(weights.T, indent)},\n"
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
    const val HIDDEN_UNITS = {HIDDEN_UNITS}

    val features = listOf(
{",\n".join(f'        "{n}"' for n in FEATURES)}
    )

    val hidden = {layer(hidden_weights, hidden_bias)}

    val output = {layer(output_weights, output_bias)}
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


def main():
    rows = load_rows()
    stations = np.array([r["station"] for r in rows])
    sites = np.array([SITES.get(s, s) for s in stations])
    terms = features(rows)
    X = np.column_stack([terms[n] for n in FEATURES])
    known = np.column_stack([[r[c] is not None for r in rows] for c in CONDITIONS])
    Y = np.column_stack([[bool(r[c]) for r in rows] for c in CONDITIONS]).astype(float)

    order = np.random.default_rng(SEED).permutation(sorted(set(sites)))
    fold_of_site = {site: i % FOLDS for i, site in enumerate(order)}
    fold = np.array([fold_of_site[site] for site in sites])
    train_masks = [fold != k for k in range(FOLDS)] + [np.ones(len(rows), bool)]
    with Pool() as pool:
        *fold_networks, network = pool.map(train_fold, [(X, Y, known.astype(float), m) for m in train_masks])

    held_out = np.zeros(Y.shape)
    for k, fold_network in enumerate(fold_networks):
        held_out[fold == k] = fold_network.predict(X[fold == k])

    thresholds = []
    for j, condition in enumerate(CONDITIONS):
        valid, observed = known[:, j], Y[:, j] > 0
        threshold = best_threshold(held_out[valid, j], observed[valid])
        thresholds.append(threshold)
        evaluated = [valid & (stations == s) for s in sorted(set(stations)) if (valid & (stations == s)).sum() > 100]
        station_hss = [hss(held_out[m, j] >= threshold, observed[m]) for m in evaluated]
        station_bss = [brier_skill_score(held_out[m, j], observed[m]) for m in evaluated]
        brier = f"; station median Brier skill {np.median(station_bss):.3f}"
        print(f"{condition}: held out station mean HSS {np.mean(station_hss):.3f}, pooled "
              f"{hss(held_out[valid, j] >= threshold, observed[valid]):.3f}{brier}", flush=True)

    write_kotlin(network, thresholds)
    write_golden(network, X)
    print(f"Wrote {OUTPUT} and {GOLDEN}")


if __name__ == "__main__":
    main()
