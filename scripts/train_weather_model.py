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
history cut short and thinned, and with the reading noise, sea level calibration offset and occasional pressure jumps of
a phone. A difference older than the history uses the oldest reading. Averaging several networks,
two more years of the same stations, a history length input, a third layer and tanh were tested and were no better.

Each condition has an output for the next 3 hours and one for 3 to 12 hours, which the app shows as "within 3 hours"
and "in 3-12 hours". The next 3 hours includes weather already happening. The 3 to 12 hour output means the condition
starts then: it is not in the next 3 hours but is in 3 to 12 hours. For precipitation, storm, wind and thunderstorm
being in a window means any report of it there; for clear and overcast, most sky reports there. Splitting off weather
that is already happening was tested and made the next 3 hours worse, because pressure can't tell it from weather about
to start. Thunderstorm has a single output for the next 12 hours, because the network could not
tell whether thunder would come within 3 hours (held out HSS 0.02). Finer timing (1, 3, 12 and 24 hour onset windows) was tested and was no better than
always guessing the most common one.

Snow has an output for the next 12 hours and one for 12 to 24 hours: whether most precipitation reported there is
snow or ice, known only when there is precipitation. It decides rain or snow in place of a freezing temperature rule,
which scores about the same.

The forecast sensitivity setting changes how often precipitation, storm, wind and thunderstorm are forecast: High
forecasts each 1.5 times as often as Medium and Low 2/3 as often. That kept HSS within about 0.02 of Medium, while
shifting the odds by a fixed factor made rare conditions all but vanish on Low and show most of the time on High. Sky
and snow stay at Medium, because a higher sensitivity would make both clear and overcast more likely. The storm alert
is on the chance of a storm in the next 12 hours (the next 3 hours output plus the 3 to 12 hour output, which can't
both happen) with Medium maximizing F0.5, because an alert interrupts the user, and the storm sensitivity setting
scaling how often it fires the same way.

The clouds the user logged in the clouds tool are inputs, in two windows: the past hour, and 1 to 4 hours ago. Each
window has the sky cover of its latest log (clear, partly cloudy or mostly cloudy) and every cloud type logged in it.
The app maps cloud types to the cover they usually form, since layered clouds (stratus, nimbostratus, altostratus,
stratocumulus and cirrostratus) tend to cover most of the sky and the others tend to leave gaps. The stations stand in
for it with their sky report (clear for no clouds, partly cloudy for few or scattered and mostly cloudy for broken or
overcast) and the cloud types in their reports: cumulonimbus from METAR cloud layers, every type from Canadian METAR
remarks and from the ISD cloud layer groups. US stations only report cumulonimbus. Most users don't log clouds, so
each window is shown for only some samples. A log 1 to 4 hours old with nothing newer kept most of the gain of a log
from the past hour; with both, the older log added nothing.

The network also forecasts from logged clouds alone, with has_pressure 0 and the pressure inputs 0, for a user who logs
clouds without a pressure history. It trains on copies of samples with a logged sky and no pressure, which left the
forecast with pressure and no logged clouds as good as before. This replaced the app's cloud rules. On reports with
cloud types, the rules scored a pooled HSS of 0.12 for precipitation in the next 3 hours (the network without pressure
0.40), 0.26 for overcast (0.76), 0.48 for clear (0.63), 0.26 for storm (0.28), 0.08 for wind (0.05) and about 0 for
the 12 to 24 hour forecast (0.27 to 0.38).

A network for the actual current temperature and the next 24 hours' low and high, from the pressure, the historic
daily low and high and the other inputs, was tested and was only about 0.25 C better than the historic temperatures
for the current temperature and high and no better for the low, so the app doesn't use one. Gradient boosting with
no size limit did no better, so the inputs don't hold more, and without pressure it gained about a third as much.

Leaving the pressure tide in was tested and lowered HSS, most for the next 3 hours.

Decoupled weight decay kept every hidden unit alive but scored worse at every strength, and smaller layers scored no
better, so the network is trained with L2 and the units it shrinks to nothing are pruned.

Early stopping on held out sites stopped within a few epochs, with worse held out probabilities, so a fixed number of
epochs is used instead.
"""
import bisect
import csv
import gzip
import math
import os
import sys
from multiprocessing import Pool
from datetime import datetime, timedelta

import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), "..")
RESOURCES = os.path.join(ROOT, "app/src/test/resources/weather_benchmark")
CLIMATE_FEATURES = os.path.join(ROOT, "app/build/reports/weather-benchmark/climate_features.csv")
OUTPUT = os.path.join(ROOT, "app/src/main/java/com/kylecorry/trail_sense/tools/weather/domain/forecasting/model/WeatherNetworkCoefficients.kt")
GOLDEN = os.path.join(ROOT, "app/src/test/resources/weather_benchmark/weather_network_golden.csv")
PRECIPITATION_CODES = ("RA", "SN", "DZ", "PL", "GR", "GS", "UP", "SG", "IC")
FROZEN_CODES = ("SN", "SG", "PL", "IC")
STANDARD_PRESSURE_HPA = 1013
MINIMUM_CHANGE_HOURS = 2
# WHY: Consecutive hours are nearly identical, so every third hour keeps the fit fast without losing information
SAMPLE_HOURS = 3
LOGGED_SKY_WINDOWS = {"recent": (0, 1), "earlier": (1, 4)}
OBSERVED_SKY_SHARE = 0.3
NO_PRESSURE_SHARE = 0.5
NO_PRESSURE_SKY_SHARE = 0.6
SKY_COVERS = {"CLR": "clear", "SKC": "clear", "NSC": "clear", "FEW": "partly_cloudy", "SCT": "partly_cloudy",
              "BKN": "mostly_cloudy", "OVC": "mostly_cloudy", "VV": "mostly_cloudy"}
SKY_COVER_NAMES = ("clear", "partly_cloudy", "mostly_cloudy")
CLOUD_TYPES = ("Cirrus", "Cirrocumulus", "Cirrostratus", "Altocumulus", "Altostratus", "Nimbostratus", "Stratocumulus",
               "Stratus", "Cumulus", "Cumulonimbus")
HISTORY_HOURS = 48
DIFFERENCE_HOURS = [1, 2, 3, 4, 6, 8, 10, 12, 15, 18, 21, 24, 30, 36, 42, 48]
PARTIAL_HISTORY_HOURS = [3, 4, 6, 9, 12, 18, 24, 36, 48]
PARTIAL_HISTORY_MISSING = [0, 0, 0.2, 0.4, 0.6]
READING_NOISE_HPA = 0.15
CALIBRATION_OFFSET_HPA = 1.5
MAX_CALIBRATION_OFFSET_HPA = 4
JUMP_CHANCE = 0.2
JUMP_HPA = (0.5, 2)
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


def mostly_frozen(wet, frozen):
    """Whether most precipitation reports are snow or ice, or None when there are none"""
    return majority([f for w, f in zip(wet, frozen) if w] or [None])


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
        times, wet, frozen, storm, windy, thunder, clear, overcast = [], [], [], [], [], [], [], []
        hourly = {}
        for o in observations:
            t = datetime.fromisoformat(o["time"].replace("Z", "+00:00"))
            codes = [c for c in o["weather"].split() if not c.startswith("VC")]
            is_thunder = any("TS" in c for c in codes)
            times.append(t)
            wet.append(number(o["precipitation_mm"]) >= 0.25 or any(w in c for c in codes for w in PRECIPITATION_CODES))
            thunder.append(is_thunder)
            frozen.append(any(f in c for c in codes for f in FROZEN_CODES))
            storm.append(is_thunder or number(o["gust_kt"]) >= 35 or number(o["precipitation_mm"]) >= 6)
            windy.append(number(o["gust_kt"]) >= 25 or number(o["wind_kt"]) >= 20)
            clear.append(o["sky_cover"] in ("CLR", "SKC", "NSC", "FEW") if o["sky_cover"] else None)
            overcast.append(o["sky_cover"] in ("BKN", "OVC", "VV") if o["sky_cover"] else None)
            if o["pressure_hpa"]:
                hourly[round(t.timestamp() / 3600)] = (t, float(o["pressure_hpa"]) - tide(t, latitude, longitude))

        sky_reports = [(t, o["sky_cover"], set(o["cloud_types"].split())) for t, o in zip(times, observations)
                       if o["sky_cover"] or o["cloud_types"]]
        sky_times = [t for t, _, _ in sky_reports]
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
                **{f"{window}_sky": logged_sky(sky_reports, sky_times, t, *hours)
                   for window, hours in LOGGED_SKY_WINDOWS.items()},
                **{f"{name}_next_3h": any(flags[i0:i3]) for name, flags in events},
                **{f"{name}_3h_to_12h": not any(flags[i0:i3]) and any(flags[i3:i12]) for name, flags in events},
                **{f"{name}_next_3h": majority(flags[i0:i3]) for name, flags in skies},
                **{f"{name}_3h_to_12h": starts_later(majority(flags[i0:i3]), majority(flags[i3:i12])) for name, flags in skies},
                thunderstorm_next_12h=any(thunder[i0:i12]), precipitation_12h_to_24h=any(wet[i12:i24]), clear_12h_to_24h=majority(clear[i12:i24]),
                overcast_12h_to_24h=majority(overcast[i12:i24]),
                snow_next_12h=mostly_frozen(wet[i0:i12], frozen[i0:i12]),
                snow_12h_to_24h=mostly_frozen(wet[i12:i24], frozen[i12:i24]),
            ))
    return rows


def logged_sky(reports, report_times, t, newest_hours, oldest_hours):
    """The sky reported at least newest_hours and less than oldest_hours before t as (the latest sky cover, every cloud
    type), or None"""
    start = bisect.bisect_right(report_times, t - timedelta(hours=oldest_hours))
    end = bisect.bisect_right(report_times, t - timedelta(hours=newest_hours))
    window = reports[start:end]
    if not window:
        return None
    covers = [cover for _, cover, _ in window if cover in SKY_COVERS]
    return covers[-1] if covers else None, set().union(*(types for _, _, types in window))


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


def perturbed_histories(histories, rng):
    """Each history as a phone might record it: reading noise, a sea level calibration offset and sometimes a jump in
    pressure, such as from a missed elevation change"""
    count = len(histories)
    noise = rng.normal(0, READING_NOISE_HPA, histories.shape)
    offset = np.clip(rng.normal(0, CALIBRATION_OFFSET_HPA, count), -MAX_CALIBRATION_OFFSET_HPA, MAX_CALIBRATION_OFFSET_HPA)
    jump = (rng.random(count) < JUMP_CHANCE) * rng.uniform(*JUMP_HPA, count) * rng.choice([-1, 1], count)
    jump_age = rng.integers(1, histories.shape[1], count)
    ages = np.arange(histories.shape[1])
    step = np.where(ages[None, :] >= jump_age[:, None], jump[:, None], 0)
    return histories + noise + offset[:, None] + step


def observed_skies(rows):
    """For each window in LOGGED_SKY_WINDOWS: the index of the sky cover in SKY_COVER_NAMES (-1 when unknown) and
    whether each of CLOUD_TYPES was seen"""
    skies = {}
    for window in LOGGED_SKY_WINDOWS:
        logged = [r[f"{window}_sky"] for r in rows]
        covers = np.array([SKY_COVER_NAMES.index(SKY_COVERS[sky[0]]) if sky and sky[0] else -1 for sky in logged])
        types = np.array([[sky is not None and name in sky[1] for name in CLOUD_TYPES] for sky in logged], bool)
        skies[window] = (covers, types)
    return skies


def no_skies(count):
    return {window: (np.full(count, -1), np.zeros((count, len(CLOUD_TYPES)), bool)) for window in LOGGED_SKY_WINDOWS}


def subset_skies(skies, mask):
    return {window: (covers[mask], types[mask]) for window, (covers, types) in skies.items()}


def has_logged_sky(skies):
    return np.any([(covers >= 0) | types.any(1) for covers, types in skies.values()], axis=0)


def without_pressure(rows, histories, skies, rng):
    """Copies of rows with a logged sky but no pressure history, as when the user only logs clouds. Each keeps at least
    one of its logged windows."""
    chosen = has_logged_sky(skies) & (rng.random(len(rows)) < NO_PRESSURE_SHARE)
    shown = hide_skies(subset_skies(skies, chosen), rng, NO_PRESSURE_SKY_SHARE)
    kept = has_logged_sky(shown)
    indices = np.flatnonzero(chosen)[kept]
    terms = features([rows[i] for i in indices], histories[indices], subset_skies(shown, kept), has_pressure=False)
    return np.column_stack([terms[n] for n in FEATURES]), indices


def hide_skies(skies, rng, share=OBSERVED_SKY_SHARE):
    """Each window is shown on its own, as a user might log the sky once or several times"""
    hidden = {}
    for window, (covers, types) in skies.items():
        shown = rng.random(len(covers)) < share
        hidden[window] = (np.where(shown, covers, -1), types & shown[:, None])
    return hidden


def features(rows, histories, skies, has_pressure=True):
    """Must match WeatherModel.getFeatures. Without pressure, the pressure features are 0."""
    column = lambda name: np.array([r[name] for r in rows], float)
    solar_hour = column("solar_hour")
    latitude = column("latitude")
    season = 2 * np.pi * (column("day_of_year") + np.where(latitude < 0, SOUTHERN_SEASON_OFFSET_DAYS, 0)) / DAYS_PER_YEAR
    differences = pressure_differences(histories) * has_pressure
    return {
        "pressure_from_standard": (histories[:, 0] - STANDARD_PRESSURE_HPA) * has_pressure,
        **{f"pressure_difference_{h}h": differences[:, i] for i, h in enumerate(DIFFERENCE_HOURS)},
        "has_pressure": np.full(len(rows), float(has_pressure)),
        "solar_hour_sine": np.sin(2 * np.pi * solar_hour / 24),
        "solar_hour_cosine": np.cos(2 * np.pi * solar_hour / 24),
        "season_sine": np.sin(season),
        "season_cosine": np.cos(season),
        "absolute_latitude": np.abs(latitude),
        "low_temperature": column("low"),
        "high_temperature": column("high"),
        "log_monthly_precipitation": np.log1p(column("precipitation_mm")),
        "log_annual_precipitation": np.log1p(column("annual_precipitation_mm")),
        **{f"{window}_{name}_sky": (covers == i).astype(float)
           for window, (covers, _) in skies.items() for i, name in enumerate(SKY_COVER_NAMES)},
        **{f"{window}_{name.lower()}": types[:, i].astype(float)
           for window, (_, types) in skies.items() for i, name in enumerate(CLOUD_TYPES)},
    }


DIFFERENCES = [f"pressure_difference_{h}h" for h in DIFFERENCE_HOURS]
FEATURES = ["pressure_from_standard"] + DIFFERENCES + ["has_pressure"] + [
    "solar_hour_sine", "solar_hour_cosine", "season_sine", "season_cosine", "absolute_latitude",
    "low_temperature", "high_temperature", "log_monthly_precipitation", "log_annual_precipitation"] + [
    f"{window}_{name}" for window in LOGGED_SKY_WINDOWS
    for name in [f"{cover}_sky" for cover in SKY_COVER_NAMES] + [name.lower() for name in CLOUD_TYPES]]
CONDITIONS = [f"{name}_{window}" for window in ("next_3h", "3h_to_12h")
              for name in ("precipitation", "storm", "wind", "clear", "overcast")] + [
    "thunderstorm_next_12h", "precipitation_12h_to_24h", "clear_12h_to_24h", "overcast_12h_to_24h",
    "snow_next_12h", "snow_12h_to_24h"]
SENSITIVITY_RATES = {"low": 2 / 3, "high": 1.5}
SENSITIVE_CONDITIONS = [f"{name}_{window}" for window in ("next_3h", "3h_to_12h")
                        for name in ("precipitation", "storm", "wind")] + [
    "thunderstorm_next_12h", "precipitation_12h_to_24h"]
STORM_ALERT_BETA = 0.5
HIDDEN_LAYERS = [32, 32]
L2_PENALTY = 1e-3
PRUNE_WEIGHT = 1e-4
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

    def pruned(self):
        """The network without the hidden units L2 shrank to nothing, so the app doesn't compute them"""
        layers = list(self.layers)
        for i in range(len(layers) - 1, 0, -1):
            weights, bias = layers[i]
            kept = np.abs(weights).max(1) > PRUNE_WEIGHT
            layers[i] = (weights[kept], bias)
            previous_weights, previous_bias = layers[i - 1]
            layers[i - 1] = (previous_weights[:, kept], previous_bias[kept])
        return Network(self.mean, self.scale, layers)

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


def best_f_beta_threshold(probability, observed, beta):
    order = np.argsort(-probability)
    hits = np.cumsum(observed[order])
    precision = hits / np.arange(1, len(observed) + 1)
    recall = hits / max(observed.sum(), 1)
    f = (1 + beta ** 2) * precision * recall / np.maximum(beta ** 2 * precision + recall, 1e-9)
    return probability[order][int(np.argmax(f))]


def threshold_for_share(probability, share):
    """The threshold that forecasts the given share of the samples"""
    return np.quantile(probability, 1 - min(share, 1))


def sensitivity_thresholds(probability, known, medium):
    """For each sensitivity, thresholds that forecast each sensitive condition its rate times as often as Medium, with a
    3 to 12 hour output's threshold picked after that sensitivity's next 3 hours thresholds decide when it is used"""
    medium_forecast = forecast(probability, medium)
    column = {c: j for j, c in enumerate(CONDITIONS)}
    levels = {"medium": medium}
    for level, rate in SENSITIVITY_RATES.items():
        thresholds = medium.copy()
        for condition in [c for c in SENSITIVE_CONDITIONS if "3h_to_12h" not in c]:
            j = column[condition]
            share = medium_forecast[known[:, j], j].mean() * rate
            thresholds[j] = threshold_for_share(probability[known[:, j], j], share)
        gates = later_gates(probability, thresholds)
        for condition in [c for c in SENSITIVE_CONDITIONS if "3h_to_12h" in c]:
            j = column[condition]
            gated = known[:, j] & gates[j]
            share = medium_forecast[known[:, j], j].sum() * rate / max(gated.sum(), 1)
            thresholds[j] = threshold_for_share(probability[gated, j], share)
        levels[level] = thresholds
    return levels


def storm_chance(probability):
    """The chance of a storm in the next 12 hours: the two storm outputs can't both happen"""
    column = {c: j for j, c in enumerate(CONDITIONS)}
    return np.clip(probability[:, column["storm_next_3h"]] + probability[:, column["storm_3h_to_12h"]], 0, 1)


def storm_happened(Y):
    column = {c: j for j, c in enumerate(CONDITIONS)}
    return (Y[:, column["storm_next_3h"]] > 0) | (Y[:, column["storm_3h_to_12h"]] > 0)


def storm_alert_thresholds(probability, Y):
    chance = storm_chance(probability)
    medium = best_f_beta_threshold(chance, storm_happened(Y), STORM_ALERT_BETA)
    medium_share = (chance >= medium).mean()
    levels = {"medium": medium}
    for level, rate in SENSITIVITY_RATES.items():
        levels[level] = threshold_for_share(chance, medium_share * rate)
    return levels


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


def kotlin_thresholds(levels, j=None):
    value = lambda level: levels[level] if j is None else levels[level][j]
    return f"SensitivityThresholds({value('low'):.7g}f, {value('medium'):.7g}f, {value('high'):.7g}f)"


def write_kotlin(network, thresholds, alert_thresholds):
    hidden_layer_sizes = [len(bias) for _, bias in network.layers[:-1]]
    indent = "        "
    layer = lambda weights, bias: (f"    NeuralNetwork.LayerWeights(\n{indent}{kotlin_matrix(weights.T, indent)},\n"
                                   f"{indent}{kotlin_matrix(bias[:, None], indent)}\n    )")
    with open(OUTPUT, "w") as f:
        f.write(f"""package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.classifiers.NeuralNetwork

// WHY: Generated by scripts/train_weather_model.py, do not edit by hand
internal enum class WeatherOutput(val thresholds: SensitivityThresholds) {{
{",\n".join(f"    {camel_case(c, True)}({kotlin_thresholds(thresholds, j)})" for j, c in enumerate(CONDITIONS))}
}}

internal object WeatherNetworkCoefficients {{
    val stormAlertThresholds = {kotlin_thresholds(alert_thresholds)}

    val hiddenLayerSizes = listOf({", ".join(str(n) for n in hidden_layer_sizes)})

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
        writer = csv.writer(f, lineterminator="\n")
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


def held_out_predictions(fold_networks, X, fold):
    probability = np.zeros((len(X), len(CONDITIONS)))
    for k, fold_network in enumerate(fold_networks):
        probability[fold == k] = fold_network.predict(X[fold == k])
    return probability


def report(name, held_out, Y, known, fold, stations):
    predicted = np.zeros(Y.shape, bool)
    for k in range(FOLDS):
        other = fold != k
        predicted[fold == k] = forecast(held_out[fold == k], best_thresholds(held_out[other], Y[other], known[other]))
    print(name, flush=True)
    for j, condition in enumerate(CONDITIONS):
        valid, observed = known[:, j], Y[:, j] > 0
        evaluated = [valid & (stations == s) for s in sorted(set(stations)) if (valid & (stations == s)).sum() > 100]
        station_hss = [hss(predicted[m, j], observed[m]) for m in evaluated]
        station_bss = [brier_skill_score(held_out[m, j], observed[m]) for m in evaluated]
        print(f"  {condition}: held out station mean HSS {np.mean(station_hss):.3f}, pooled "
              f"{hss(predicted[valid, j], observed[valid]):.3f}; station median Brier skill "
              f"{np.median(station_bss):.3f}", flush=True)


def report_with_thresholds(name, probability, held_out, Y, known, fold, rows):
    """Pooled scores of probability on rows, forecast with the thresholds the app uses, which are chosen on the held
    out forecasts with pressure of the other folds"""
    predicted = np.zeros((rows.sum(), len(CONDITIONS)), bool)
    row_fold = fold[rows]
    for k in range(FOLDS):
        other = fold != k
        predicted[row_fold == k] = forecast(probability[row_fold == k],
                                            best_thresholds(held_out[other], Y[other], known[other]))
    print(name, flush=True)
    for j, condition in enumerate(CONDITIONS):
        valid = known[rows, j]
        observed = Y[rows, j][valid] > 0
        hits = (predicted[valid, j] & observed).sum()
        print(f"  {condition}: pooled HSS {hss(predicted[valid, j], observed):.3f}, POD "
              f"{hits / max(observed.sum(), 1):.2f}, FAR {1 - hits / max(predicted[valid, j].sum(), 1):.2f}", flush=True)


def report_sensitivity(held_out, Y, known, fold):
    """Each sensitivity's forecasts and storm alerts, with thresholds picked without the reported fold"""
    print("Sensitivity (share of forecasts showing it, POD, FAR)", flush=True)
    levels = ("low", "medium", "high")
    predicted = {level: np.zeros(Y.shape, bool) for level in levels}
    alerted = {level: np.zeros(len(Y), bool) for level in levels}
    for k in range(FOLDS):
        other = fold != k
        medium = best_thresholds(held_out[other], Y[other], known[other])
        thresholds = sensitivity_thresholds(held_out[other], known[other], medium)
        alert_thresholds = storm_alert_thresholds(held_out[other], Y[other])
        for level in levels:
            predicted[level][fold == k] = forecast(held_out[fold == k], thresholds[level])
            alerted[level][fold == k] = storm_chance(held_out[fold == k]) >= alert_thresholds[level]

    def scores(forecasted, observed):
        hits = np.sum(forecasted & observed)
        return (f"{forecasted.mean():.3f}, POD {hits / max(observed.sum(), 1):.2f}, "
                f"FAR {1 - hits / max(forecasted.sum(), 1):.2f}")

    for condition in SENSITIVE_CONDITIONS:
        j = CONDITIONS.index(condition)
        valid = known[:, j]
        print(f"  {condition}: " + "; ".join(
            f"{level} {scores(predicted[level][valid, j], Y[valid, j] > 0)}" for level in levels), flush=True)
    print("  storm alert: " + "; ".join(f"{level} {scores(alerted[level], storm_happened(Y))}" for level in levels))


def report_snow_rule(rows, Y, known):
    """The rule the network replaced: snow when the climate's mean daily temperature is at or below freezing"""
    mean_temperature = np.array([(r["low"] + r["high"]) / 2 for r in rows])
    for condition in ("snow_next_12h", "snow_12h_to_24h"):
        j = CONDITIONS.index(condition)
        valid = known[:, j]
        print(f"  {condition} freezing rule: pooled HSS {hss(mean_temperature[valid] <= 0, Y[valid, j] > 0):.3f}")


def report_dead_units(network, X):
    values = (X - network.mean) / network.scale
    for layer, (weights, bias) in enumerate(network.layers[:-1]):
        values = np.maximum(values @ weights + bias, 0)
        active = (values > 0).mean(0)
        print(f"  hidden layer {layer + 1}: {np.sum(active < 0.01)} of {len(active)} units active on under 1% of samples")


def main():
    should_write = "--no-write" not in sys.argv
    rows = load_rows()
    stations = np.array([r["station"] for r in rows])
    sites = np.array([SITES.get(s, s) for s in stations])
    histories = np.array([r["history"] for r in rows])
    unlogged = no_skies(len(rows))
    sky_rng = np.random.default_rng(SEED)
    skies = observed_skies(rows)
    terms = features(rows, histories, unlogged)
    X = np.column_stack([terms[n] for n in FEATURES])
    sky_terms = features(rows, histories, skies)
    X_sky = np.column_stack([sky_terms[n] for n in FEATURES])
    known = np.column_stack([[r[c] is not None for r in rows] for c in CONDITIONS])
    Y = np.column_stack([[bool(r[c]) for r in rows] for c in CONDITIONS]).astype(float)

    order = np.random.default_rng(SEED).permutation(sorted(set(sites)))
    fold_of_site = {site: i % FOLDS for i, site in enumerate(order)}
    fold = np.array([fold_of_site[site] for site in sites])

    rng = np.random.default_rng(SEED)
    partial, usable = partial_histories(histories, rng)
    augmented = perturbed_histories(partial[usable], rng)
    augmented_terms = features([r for r, u in zip(rows, usable) if u], augmented, hide_skies(subset_skies(skies, usable), sky_rng))
    hidden_sky_terms = features(rows, histories, hide_skies(skies, sky_rng))
    X_without_pressure, without_pressure_rows = without_pressure(rows, histories, skies, np.random.default_rng(SEED + 2))
    X_train = np.vstack([np.column_stack([hidden_sky_terms[n] for n in FEATURES]),
                         np.column_stack([augmented_terms[n] for n in FEATURES]), X_without_pressure])
    Y_train = np.vstack([Y, Y[usable], Y[without_pressure_rows]])
    known_train = np.vstack([known, known[usable], known[without_pressure_rows]])
    fold_train = np.concatenate([fold, fold[usable], fold[without_pressure_rows]])
    train_masks = [fold_train != k for k in range(FOLDS)] + [np.ones(len(X_train), bool)]
    with Pool() as pool:
        *fold_networks, network = pool.map(
            train_fold, [(X_train, Y_train, known_train.astype(float), m) for m in train_masks])
    unpruned = network
    network = network.pruned()

    perturbed_terms = features(rows, perturbed_histories(histories, np.random.default_rng(SEED + 1)), unlogged)
    X_perturbed = np.column_stack([perturbed_terms[n] for n in FEATURES])

    held_out = held_out_predictions(fold_networks, X, fold)
    report("Station readings", held_out, Y, known, fold, stations)
    report("Station readings with phone noise, calibration offset and jumps",
           held_out_predictions(fold_networks, X_perturbed, fold), Y, known, fold, stations)
    has_sky = has_logged_sky(skies)
    report("Station readings with a logged sky, only where there is one",
           held_out_predictions(fold_networks, X_sky[has_sky], fold[has_sky]), Y[has_sky], known[has_sky],
           fold[has_sky], stations[has_sky])
    sky_rows = [r for r, h in zip(rows, has_sky) if h]
    no_pressure_terms = features(sky_rows, histories[has_sky], subset_skies(skies, has_sky), has_pressure=False)
    X_no_pressure = np.column_stack([no_pressure_terms[n] for n in FEATURES])
    report_with_thresholds("Logged sky without pressure, with the thresholds for forecasts with pressure",
                           held_out_predictions(fold_networks, X_no_pressure, fold[has_sky]), held_out, Y, known,
                           fold, has_sky)
    sky_types = np.any([types.any(1) for _, types in skies.values()], axis=0)
    report_with_thresholds("Logged cloud types without pressure, with the thresholds for forecasts with pressure",
                           held_out_predictions(fold_networks, X_no_pressure[sky_types[has_sky]], fold[sky_types]),
                           held_out, Y, known, fold, sky_types)
    report_sensitivity(held_out, Y, known, fold)
    report_snow_rule(rows, Y, known)
    report_dead_units(network, X)
    print(f"  pruned to {[len(bias) for _, bias in network.layers[:-1]]} hidden units, changing probabilities by at most "
          f"{np.abs(network.predict(X_perturbed) - unpruned.predict(X_perturbed)).max():.1e}")

    if should_write:
        medium = best_thresholds(held_out, Y, known)
        write_kotlin(network, sensitivity_thresholds(held_out, known, medium), storm_alert_thresholds(held_out, Y))
        write_golden(network, np.vstack([X_sky[:len(X_sky) // 2], X_no_pressure[:len(X_no_pressure) // 2]]))
        print(f"Wrote {OUTPUT} and {GOLDEN}")


if __name__ == "__main__":
    main()
