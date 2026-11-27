"""
Fits the learned weather forecast (LearnedWeatherCoefficients.kt) to the weather benchmark stations.

1. Download the stations: python3 scripts/download_weather_benchmark.py
2. Export the app's climate temperatures for them:
   .agents/skills/verify-trail-sense/scripts/tsctl test unit '*ClimateFeatureExport*'
3. Fit and write the coefficients: python3 scripts/train_weather_model.py [--evaluate]

--evaluate also scores the model on each station after fitting it without that station, which takes a few minutes.
"""
import bisect
import csv
import gzip
import math
import os
import sys
from datetime import datetime, timedelta

import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), "..")
RESOURCES = os.path.join(ROOT, "app/src/test/resources/weather_benchmark")
CLIMATE_FEATURES = os.path.join(ROOT, "app/build/reports/weather-benchmark/climate_features.csv")
OUTPUT = os.path.join(ROOT, "app/src/main/java/com/kylecorry/trail_sense/tools/weather/domain/forecasting/learned/LearnedWeatherCoefficients.kt")
PRECIPITATION_CODES = ("RA", "SN", "DZ", "PL", "GR", "GS", "UP", "SG", "IC")
STANDARD_PRESSURE_HPA = 1013
# WHY: Consecutive hours are nearly identical, so every third hour keeps the fit fast without losing information
SAMPLE_HOURS = 3


def tide(t, latitude, longitude):
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

        keys = sorted(hourly)
        last = times[-1]
        for k in keys:
            t, p = hourly[k]
            if k - keys[0] < 24 or t + timedelta(hours=24) > last or t.hour % SAMPLE_HOURS != 0:
                continue
            past = [(h, hourly[k - h][1]) for h in range(1, 49) if (k - h) in hourly]
            old_enough = [x for x in past if x[0] >= 2]
            if not old_enough:
                continue

            def change(window):
                hours, value = min(old_enough, key=lambda x: abs(x[0] - window))
                return (p - value) * window / hours

            window48 = [p] + [v for _, v in past]
            i0 = bisect.bisect_left(times, t)
            i6, i12, i24 = (bisect.bisect_right(times, t + timedelta(hours=h)) for h in (6, 12, 24))
            low, high = daily_temperatures[(sid, t.date().isoformat())]
            rows.append(dict(
                station=sid, t=t, pressure=p, change_3h=change(3), change_12h=change(12), change_24h=change(24),
                range_48h=max(window48) - min(window48),
                low=low, high=high, precipitation_mm=precipitation[t.month], annual_precipitation_mm=annual_precipitation,
                solar_hour=(t.hour + t.minute / 60 + longitude / 15) % 24,
                precipitation=any(wet[i0:i12]), storm=any(storm[i0:i12]), wind=any(windy[i0:i12]),
                thunderstorm=any(thunder[i0:i12]), clear=majority(clear[i0:i6]), overcast=majority(overcast[i0:i6]),
                later_precipitation=any(wet[i12:i24]), later_clear=majority(clear[i12:i24]),
                later_overcast=majority(overcast[i12:i24]),
            ))
    return rows


def features(rows):
    """Must match LearnedWeatherModel.getFeatures"""
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
# WHY: Each condition keeps only the input groups that improved it on held out stations and regions, the rest made it worse or no better
INPUTS = {
    "precipitation": CORE + CLIMATE,
    "storm": CORE + PRESSURE_CURVES + CLIMATE,
    "wind": CORE,
    "thunderstorm": CORE + PRESSURE_CURVES + CLIMATE,
    "clear": CORE,
    "overcast": CORE,
    "later_precipitation": CORE,
    "later_clear": CORE,
    "later_overcast": CORE + PRESSURE_CURVES + CLIMATE,
}
CONDITIONS = list(INPUTS)


def fit(X, y, ridge=1e-3, iterations=12):
    mean = X.mean(0)
    scale = X.std(0) + 1e-9
    Z = np.hstack([np.ones((len(X), 1)), (X - mean) / scale])
    w = np.zeros(Z.shape[1])
    for _ in range(iterations):
        probability = 1 / (1 + np.exp(-Z @ w))
        gradient = Z.T @ (probability - y) / len(y) + ridge * np.r_[0, w[1:]]
        hessian = (Z.T * (probability * (1 - probability))) @ Z / len(y) + ridge * np.eye(len(w))
        w -= np.linalg.solve(hessian, gradient)
    return w, mean, scale


def predict(model, X):
    w, mean, scale = model
    return 1 / (1 + np.exp(-(np.hstack([np.ones((len(X), 1)), (X - mean) / scale]) @ w)))


def hss(predicted, observed):
    a = np.sum(predicted & observed)
    b = np.sum(predicted & ~observed)
    c = np.sum(~predicted & observed)
    d = np.sum(~predicted & ~observed)
    n = a + b + c + d
    expected = ((a + c) * (a + b) + (c + d) * (b + d)) / n
    return (a + d - expected) / (n - expected) if n != expected else 0.0


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


def fit_condition(X, observed):
    model = fit(X, observed.astype(float))
    return model, best_threshold(predict(model, X), observed)


def kotlin_floats(values):
    return "floatArrayOf(" + ", ".join(f"{v:.6g}f" for v in values) + ")"


def camel_case(name):
    head, *rest = name.split("_")
    return head + "".join(word.capitalize() for word in rest)


def main():
    rows = load_rows()
    stations = np.array([r["station"] for r in rows])
    terms = features(rows)
    blocks = []
    for condition in CONDITIONS:
        names = INPUTS[condition]
        X = np.column_stack([terms[n] for n in names])
        valid = np.array([r[condition] is not None for r in rows])
        observed = np.array([bool(r[condition]) for r in rows])
        (w, mean, scale), threshold = fit_condition(X[valid], observed[valid])
        blocks.append(f"""    val {camel_case(condition)} = LearnedCondition(
        listOf(
{",\n".join(f'            "{n}"' for n in names)}
        ),
        BinaryLogisticClassifier(
            {kotlin_floats(mean)},
            {kotlin_floats(scale)},
            {w[0]:.6g}f,
            {kotlin_floats(w[1:])},
            {threshold:.6g}f
        )
    )""")
        if "--evaluate" in sys.argv:
            predicted = np.zeros(len(rows), bool)
            for station in sorted(set(stations)):
                train = valid & (stations != station)
                model, station_threshold = fit_condition(X[train], observed[train])
                predicted[stations == station] = predict(model, X[stations == station]) >= station_threshold
            per_station = [hss(predicted[valid & (stations == s)], observed[valid & (stations == s)])
                           for s in sorted(set(stations)) if (valid & (stations == s)).sum() > 100]
            print(f"{condition}: held out station mean HSS {np.mean(per_station):.3f}, pooled "
                  f"{hss(predicted[valid], observed[valid]):.3f}", flush=True)

    with open(OUTPUT, "w") as f:
        f.write(f"""package com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned

import com.kylecorry.sol.math.classifiers.BinaryLogisticClassifier

// WHY: Generated by scripts/train_weather_model.py, do not edit by hand
internal object LearnedWeatherCoefficients {{
{chr(10).join(blocks)}
}}
""")
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
