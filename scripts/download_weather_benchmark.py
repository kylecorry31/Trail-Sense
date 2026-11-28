"""
Downloads hourly airport observations used by WeatherForecastBenchmarkTest:
- US stations from the Iowa Environmental Mesonet (ASOS), which report sea level pressure to 0.1 hPa
- International stations from NOAA's Integrated Surface Database (ISD) on AWS

Usage: python3 scripts/download_weather_benchmark.py [ID ...]

With ids, only those series are downloaded and the rest of the data is kept.
"""
import csv
import gzip
import io
import os
import re
import sys
import time
import urllib.parse
import urllib.request

YEAR_START = "2025-12-01"
YEAR_END = "2026-11-28"
ISD_YEAR = 2025

# WHY: Left out stations whose reports can't label the weather: VHHH, ZBAA and WSSS report nearly one sky cover all
# year, NZQN reports no sky, and HKJK and HTKJ rarely report precipitation for how much falls there
# id, source, station, start, end, description
SERIES = [
    ("PVD", "asos", "PVD", YEAR_START, YEAR_END, "Providence, RI: coastal fronts, nor'easters"),
    ("BUF", "asos", "BUF", YEAR_START, YEAR_END, "Buffalo, NY: lake-effect snow, wind"),
    ("MSP", "asos", "MSP", YEAR_START, YEAR_END, "Minneapolis, MN: continental winter, summer storms"),
    ("SEA", "asos", "SEA", YEAR_START, YEAR_END, "Seattle, WA: marine fronts, drizzle"),
    ("SFO", "asos", "SFO", YEAR_START, YEAR_END, "San Francisco, CA: dry summer, fog, winter storms"),
    ("OKC", "asos", "OKC", YEAR_START, YEAR_END, "Oklahoma City, OK: plains thunderstorms"),
    ("DEN", "asos", "DEN", YEAR_START, YEAR_END, "Denver, CO: high elevation, snow"),
    ("PHX", "asos", "PHX", YEAR_START, YEAR_END, "Phoenix, AZ: desert, monsoon, dust"),
    ("MSY", "asos", "MSY", YEAR_START, YEAR_END, "New Orleans, LA: Gulf storms, hurricanes"),
    ("MIA", "asos", "MIA", YEAR_START, YEAR_END, "Miami, FL: subtropical convection, hurricanes"),
    ("TJSJ", "asos", "TJSJ", YEAR_START, YEAR_END, "San Juan, PR: tropical trade winds"),
    ("PHNL", "asos", "PHNL", YEAR_START, YEAR_END, "Honolulu, HI: tropical trade winds"),
    ("PGUM", "asos", "PGUM", YEAR_START, YEAR_END, "Guam: tropical, typhoons"),
    ("NSTU", "asos", "NSTU", YEAR_START, YEAR_END, "Pago Pago, American Samoa: southern hemisphere tropics"),
    ("PANC", "asos", "PANC", YEAR_START, YEAR_END, "Anchorage, AK: high latitude, snow"),
    ("PABR", "asos", "PABR", YEAR_START, YEAR_END, "Utqiagvik, AK: arctic"),
    ("MSY_IDA_2021", "asos", "MSY", "2021-08-18", "2021-09-03", "Hurricane Ida landfall near New Orleans"),
    ("RSW_IAN_2022", "asos", "RSW", "2022-09-18", "2022-10-03", "Hurricane Ian landfall at Fort Myers"),
    ("TPA_MILTON_2024", "asos", "TPA", "2024-09-30", "2024-10-13", "Hurricane Milton landfall near Tampa"),
    ("EGLL", "isd", "03772099999", None, None, "London, UK: maritime fronts"),
    ("LEMD", "isd", "08221099999", None, None, "Madrid, Spain: Mediterranean plateau"),
    ("ESSA", "isd", "02460099999", None, None, "Stockholm, Sweden: northern Europe, snow"),
    ("BIKF", "isd", "04018016201", None, None, "Keflavik, Iceland: subpolar North Atlantic storms"),
    ("LGAV", "isd", "16716199999", None, None, "Athens, Greece: Mediterranean, dry summer"),
    ("UUEE", "isd", "27515599999", None, None, "Moscow, Russia: continental, cold winter"),
    ("RJTT", "isd", "47671099999", None, None, "Tokyo, Japan: East Asian monsoon, typhoons"),
    ("VIDP", "isd", "42181099999", None, None, "Delhi, India: South Asian monsoon"),
    ("OERK", "isd", "40437099999", None, None, "Riyadh, Saudi Arabia: hot desert"),
    ("HECA", "isd", "62366099999", None, None, "Cairo, Egypt: hot desert"),
    ("YSSY", "isd", "94767099999", None, None, "Sydney, Australia: southern east coast"),
    ("YMML", "isd", "94866099999", None, None, "Melbourne, Australia: southern fronts"),
    ("NZCH", "isd", "93780099999", None, None, "Christchurch, New Zealand: southern westerlies"),
    ("FACT", "isd", "68816099999", None, None, "Cape Town, South Africa: winter rain"),
    ("SAEZ", "isd", "87576099999", None, None, "Buenos Aires, Argentina: southern plains storms"),
    ("SCEL", "isd", "85574099999", None, None, "Santiago, Chile: dry summer, winter fronts"),
    ("DNMM", "isd", "65201099999", None, None, "Lagos, Nigeria: West African monsoon"),
    ("SBEG", "isd", "82111099999", None, None, "Manaus, Brazil: Amazon rainforest"),
    ("YPDN", "isd", "94120099999", None, None, "Darwin, Australia: tropical monsoon, cyclones"),
    ("FIMP", "isd", "61990099999", None, None, "Mauritius: southern Indian Ocean trade winds, cyclones"),
    ("JAC", "asos", "JAC", YEAR_START, YEAR_END, "Jackson Hole, WY: Tetons, mountain snow"),
    ("ASE", "asos", "ASE", YEAR_START, YEAR_END, "Aspen, CO: high Rockies valley"),
    ("FLG", "asos", "FLG", YEAR_START, YEAR_END, "Flagstaff, AZ: high plateau, monsoon thunderstorms"),
    ("TVL", "asos", "TVL", YEAR_START, YEAR_END, "South Lake Tahoe, CA: Sierra Nevada snow"),
    ("BZN", "asos", "BZN", YEAR_START, YEAR_END, "Bozeman, MT: northern Rockies"),
    ("AVL", "asos", "AVL", YEAR_START, YEAR_END, "Asheville, NC: Appalachian ridges, summer storms"),
    ("UIL", "asos", "UIL", YEAR_START, YEAR_END, "Quillayute, WA: Olympic coastal rainforest"),
    ("PAJN", "asos", "PAJN", YEAR_START, YEAR_END, "Juneau, AK: temperate rainforest, fjords"),
    ("PAFA", "asos", "PAFA", YEAR_START, YEAR_END, "Fairbanks, AK: subarctic interior"),
    ("LSZS", "isd", "06630499999", None, None, "Samedan, Switzerland: high Alpine valley"),
    ("LOWI", "isd", "11120099999", None, None, "Innsbruck, Austria: Alpine valley, foehn"),
    ("LFLS", "isd", "07486099999", None, None, "Grenoble, France: western Alps foothills"),
    ("EGPE", "isd", "03059099999", None, None, "Inverness, UK: Scottish Highlands"),
    ("ENTC", "isd", "01025099999", None, None, "Tromso, Norway: arctic coast mountains"),
    ("CYXY", "isd", "71964099999", None, None, "Whitehorse, Canada: Yukon subarctic"),
    ("CYYC", "isd", "71877099999", None, None, "Calgary, Canada: Rockies foothills, chinooks"),
    ("CYYZ", "isd", "71624099999", None, None, "Toronto, Canada: Great Lakes"),
    ("CYWG", "isd", "71852099999", None, None, "Winnipeg, Canada: continental prairie"),
    ("CYHZ", "isd", "71395099999", None, None, "Halifax, Canada: Atlantic coastal storms"),
    ("CYVR", "isd", "71892099999", None, None, "Vancouver, Canada: Pacific coast fronts"),
    ("CYZV", "isd", "71811099999", None, None, "Sept-Iles, Canada: Gulf of St. Lawrence"),
    ("VNKT", "isd", "44454099999", None, None, "Kathmandu, Nepal: Himalayan foothills, monsoon"),
    ("SLLP", "isd", "85201099999", None, None, "La Paz, Bolivia: Andean altiplano"),
    ("SPZO", "isd", "84686099999", None, None, "Cusco, Peru: Andes, wet season storms"),
    ("SEQM", "isd", "84072599999", None, None, "Quito, Ecuador: equatorial Andes"),
    ("SAZS", "isd", "87765099999", None, None, "Bariloche, Argentina: Patagonian Andes"),
    ("YMHB", "isd", "94975099999", None, None, "Hobart, Australia: Tasmanian westerlies"),
    ("VTBS", "isd", "48459099999", None, None, "Bangkok, Thailand: Southeast Asian monsoon"),
    ("RPLL", "isd", "98429099999", None, None, "Manila, Philippines: typhoons, monsoon"),
    ("WIII", "isd", "96749099999", None, None, "Jakarta, Indonesia: equatorial convection"),
    ("VOMM", "isd", "43279099999", None, None, "Chennai, India: northeast monsoon, cyclones"),
    ("GOOY", "isd", "61641099999", None, None, "Dakar, Senegal: Sahel coast, dust"),
    ("FKKD", "isd", "64910099999", None, None, "Douala, Cameroon: equatorial West Africa, heavy rain"),
    ("HAAB", "isd", "63450099999", None, None, "Addis Ababa, Ethiopia: tropical highlands"),
    ("MROC", "isd", "78762099999", None, None, "San Jose, Costa Rica: Central American wet season"),
    ("MMMX", "isd", "76679399999", None, None, "Mexico City, Mexico: high tropical plateau, afternoon storms"),
    ("SKBO", "isd", "80222099999", None, None, "Bogota, Colombia: equatorial Andes plateau"),
    ("YBCS", "isd", "94287099999", None, None, "Cairns, Australia: tropical cyclones"),
    ("NFFN", "isd", "91680099999", None, None, "Nadi, Fiji: South Pacific tropics, cyclones"),
    ("FMMI", "isd", "67083099999", None, None, "Antananarivo, Madagascar: southern tropical highlands"),
    ("FQMA", "isd", "67341099999", None, None, "Maputo, Mozambique: southern Africa coast, cyclones"),
    ("FAPE", "isd", "68842099999", None, None, "Port Elizabeth, South Africa: southern coast, cut-off lows"),
    ("SBGR", "isd", "83075099999", None, None, "Sao Paulo, Brazil: subtropical summer storms"),
    ("SBPA", "isd", "83971099999", None, None, "Porto Alegre, Brazil: southern Brazil fronts"),
    ("SUMU", "isd", "86580099999", None, None, "Montevideo, Uruguay: pamperos, Rio de la Plata"),
    ("SCCI", "isd", "85934099999", None, None, "Punta Arenas, Chile: Patagonian westerlies"),
    ("SAWH", "isd", "87938099999", None, None, "Ushuaia, Argentina: subantarctic"),
    ("YBBN", "isd", "94578099999", None, None, "Brisbane, Australia: subtropical east coast storms"),
    ("YPPH", "isd", "94610099999", None, None, "Perth, Australia: Mediterranean winter fronts"),
    ("NZAA", "isd", "93119099999", None, None, "Auckland, New Zealand: subtropical North Island"),
]

MAX_PRESSURE_JUMP_HPA = 10.0
METERS_PER_SECOND_TO_KNOTS = 1.94384
SKY_ORDER = ["CLR", "SKC", "NSC", "FEW", "SCT", "BKN", "OVC", "VV"]

root = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..")
output_dir = os.path.join(root, "app", "src", "test", "resources", "weather_benchmark")


def download_asos(station, start, end):
    y1, m1, d1 = start.split("-")
    y2, m2, d2 = end.split("-")
    params = [("station", station), ("year1", y1), ("month1", m1), ("day1", d1),
              ("year2", y2), ("month2", m2), ("day2", d2), ("tz", "Etc/UTC"),
              ("format", "onlycomma"), ("latlon", "yes"), ("elev", "yes"), ("missing", "M"), ("trace", "T"),
              ("report_type", "3"), ("report_type", "4")]
    params += [("data", d) for d in ("tmpc", "relh", "mslp", "p01m", "wxcodes", "gust", "sknt",
                                     "skyc1", "skyc2", "skyc3", "skyc4", "metar")]
    url = "https://mesonet.agron.iastate.edu/cgi-bin/request/asos.py?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=600) as response:
        rows = list(csv.DictReader(io.StringIO(response.read().decode())))
    clean = lambda v: "" if v in ("M", "T") else v
    observations = []
    for row in rows:
        layers = [row[k] for k in ("skyc1", "skyc2", "skyc3", "skyc4") if row[k] in SKY_ORDER]
        observations.append({
            "time": row["valid"].replace(" ", "T") + ":00Z",
            "pressure": clean(row["mslp"]),
            "temperature": clean(row["tmpc"]),
            "humidity": clean(row["relh"]),
            "precipitation": clean(row["p01m"]),
            "weather": clean(row["wxcodes"]),
            "gust": clean(row["gust"]),
            "wind": clean(row["sknt"]),
            "sky": max(layers, key=SKY_ORDER.index) if layers else "",
            "clouds": " ".join(metar_cloud_types(row["metar"])) if row["metar"] != "M" else "",
        })
    return observations, (rows[0]["lat"], rows[0]["lon"], rows[0]["elevation"])


def isd_value(field, index, scale, missing):
    parts = field.split(",") if field else []
    if len(parts) <= index or parts[index].lstrip("+-") == missing:
        return None
    try:
        return int(parts[index]) / scale
    except ValueError:
        return None


# WHY: ISD stores WMO present weather codes; the benchmark reads METAR style codes
def isd_weather(row):
    codes = []
    manual = isd_value(row.get("MW1", ""), 0, 1, "99")
    if manual is not None:
        ww = int(manual)
        if ww == 17 or 91 <= ww <= 99:
            codes.append("TS")
        if 50 <= ww <= 59:
            codes.append("DZ")
        elif 60 <= ww <= 69 or 80 <= ww <= 84 or 91 <= ww <= 92:
            codes.append("RA")
        elif 70 <= ww <= 79 or 85 <= ww <= 88 or 93 <= ww <= 94:
            codes.append("SN")
    automated = isd_value(row.get("AW1", ""), 0, 1, "99")
    if automated is not None:
        ww = int(automated)
        if 90 <= ww <= 96:
            codes.append("TS")
        if 40 <= ww <= 49 or 60 <= ww <= 68 or 80 <= ww <= 84:
            codes.append("RA")
        elif 50 <= ww <= 58:
            codes.append("DZ")
        elif 70 <= ww <= 78 or 85 <= ww <= 87:
            codes.append("SN")
    return " ".join(dict.fromkeys(codes))


def isd_sky(row):
    oktas = isd_value(row.get("GF1", ""), 0, 1, "99")
    if oktas is None:
        layers = [isd_value(row.get(f"GA{i}", ""), 0, 1, "99") for i in range(1, 7)]
        layers = [layer for layer in layers if layer is not None]
        oktas = max(layers) if layers else None
    if oktas is None:
        return ""
    oktas = int(oktas)
    if oktas == 0:
        return "CLR"
    if oktas <= 2:
        return "FEW"
    if oktas <= 4:
        return "SCT"
    if oktas <= 7:
        return "BKN"
    return "OVC" if oktas == 8 else "VV"


METAR_WEATHER = re.compile(
    r"^(\+|-)?(MI|PR|BC|DR|BL|SH|TS|FZ){0,2}(DZ|RA|SN|SG|IC|PL|GR|GS|UP|BR|FG|FU|VA|DU|SA|HZ|PY|PO|SQ|FC|SS|DS){0,3}$"
)
METAR_SKY = re.compile(r"^(FEW|SCT|BKN|OVC|VV)(\d{3}|///)")
METAR_LAYER_TYPE = re.compile(r"^(FEW|SCT|BKN|OVC|VV)(\d{3}|///)(CB|TCU)$")
# WHY: Canadian METAR remarks list each layer's cloud type and oktas, such as SC4AC2; towering cumulus and cumulus
# fractus are logged as cumulus in the app, and stratus fractus as stratus
CANADIAN_CLOUD_TYPES = {"CI": "Cirrus", "CC": "Cirrocumulus", "CS": "Cirrostratus", "AC": "Altocumulus",
                        "ACC": "Altocumulus", "AS": "Altostratus", "NS": "Nimbostratus", "SC": "Stratocumulus",
                        "ST": "Stratus", "SF": "Stratus", "CU": "Cumulus", "CF": "Cumulus", "TCU": "Cumulus",
                        "CB": "Cumulonimbus"}
CANADIAN_CLOUD_REMARK = re.compile(r"\b(TCU|ACC|CI|CC|CS|AC|AS|NS|SC|ST|SF|CU|CF|CB)(\d)")
ISD_CLOUD_TYPES = {"00": "Cirrus", "01": "Cirrocumulus", "02": "Cirrostratus", "03": "Altocumulus",
                   "04": "Altostratus", "05": "Nimbostratus", "06": "Stratocumulus", "07": "Stratus", "08": "Cumulus",
                   "09": "Cumulonimbus"}
METAR_GUST = re.compile(r"^(\d{3}|VRB|///)\d{2,3}G(\d{2,3})(KT|MPS)$")
METAR_END = ("RMK", "TEMPO", "BECMG", "NOSIG", "FCST")


def parse_metar(text):
    """Returns the weather codes, sky cover and gust (kt) from the observed part of a METAR"""
    # WHY: ISD prefixes the raw report with a remark id and length, such as MET078METAR
    report = re.search(r"(METAR|SPECI) (.*)", text)
    if report is None:
        return None
    weather, layers, gust = [], [], None
    for token in report.group(2).replace("=", " ").split()[1:]:
        if token in METAR_END:
            break
        if token in ("CAVOK", "NSC", "NCD", "SKC", "CLR"):
            layers.append("CLR" if token in ("CAVOK", "SKC", "CLR") else "NSC")
            continue
        sky = METAR_SKY.match(token)
        if sky:
            layers.append(sky.group(1))
            continue
        wind = METAR_GUST.match(token)
        if wind:
            gust = int(wind.group(2)) * (METERS_PER_SECOND_TO_KNOTS if wind.group(3) == "MPS" else 1)
            continue
        if any(c.isdigit() for c in token) or "/" in token or len(token) < 2:
            continue
        if METAR_WEATHER.match(token) and token not in ("+", "-"):
            weather.append(token)
    return {
        "weather": " ".join(weather),
        "sky": max(layers, key=SKY_ORDER.index) if layers else "",
        "gust": "" if gust is None else round(gust, 1),
    }


def metar_cloud_types(text):
    """The cloud types in a METAR: cumulonimbus from its cloud layers, and every type from Canadian remarks"""
    report = re.search(r"(METAR|SPECI) (.*)", text)
    body = report.group(2) if report else text
    observed, _, remarks = body.partition(" RMK ")
    types = {"Cumulonimbus" for token in observed.split()
             if (layer := METAR_LAYER_TYPE.match(token)) and layer.group(3) == "CB"}
    types |= {CANADIAN_CLOUD_TYPES[code] for code, _ in CANADIAN_CLOUD_REMARK.findall(remarks)}
    return sorted(types)


def isd_cloud_types(row):
    layers = (row.get(f"GA{i}", "").split(",") for i in range(1, 7))
    return {ISD_CLOUD_TYPES[layer[4]] for layer in layers if len(layer) > 4 and layer[4] in ISD_CLOUD_TYPES}


def isd_pressure(row, use_sea_level_pressure):
    if use_sea_level_pressure:
        return isd_value(row.get("SLP", ""), 0, 10, "99999")
    return isd_value(row.get("MA1", ""), 0, 10, "99999")


def download_isd(station):
    url = f"https://noaa-global-hourly-pds.s3.amazonaws.com/{ISD_YEAR}/{station}.csv"
    with urllib.request.urlopen(url, timeout=600) as response:
        rows = list(csv.DictReader(io.StringIO(response.read().decode())))
    hours = lambda field, missing: len({row["DATE"][:13] for row in rows if isd_value(row.get(field, ""), 0, 10, missing) is not None})
    # WHY: METAR-only stations leave SLP missing and report the altimeter setting (QNH, usually whole hPa) instead
    use_sea_level_pressure = hours("SLP", "99999") >= 0.8 * hours("MA1", "99999")
    by_time = {}
    for row in rows:
        by_time.setdefault(row["DATE"], []).append(row)
    observations = []
    pressure_hours = set()
    for date in sorted(by_time):
        group = by_time[date]
        hour = date[:13]
        pressure = next((p for p in (isd_pressure(row, use_sea_level_pressure) for row in group) if p is not None), None)
        # WHY: Some ISD stations report every 10 minutes; the app records about every 30 minutes
        if pressure is not None and hour in pressure_hours:
            pressure = None
        if pressure is not None:
            pressure_hours.add(hour)
        first = lambda f: next((v for v in (f(row) for row in group) if v not in (None, "")), None)
        temperature = first(lambda row: isd_value(row.get("TMP", ""), 0, 10, "9999"))
        dew = first(lambda row: isd_value(row.get("DEW", ""), 0, 10, "9999"))
        humidity = ""
        if temperature is not None and dew is not None:
            humidity = round(100 * pow(10, 7.5 * dew / (237.3 + dew) - 7.5 * temperature / (237.3 + temperature)), 1)
        precipitation = first(lambda row: isd_value(row.get("AA1", ""), 1, 10, "9999")
                              if (isd_value(row.get("AA1", ""), 0, 1, "99") or 99) <= 1 else None)
        metar = first(lambda row: parse_metar(row.get("REM", "")))
        if metar is not None:
            weather, sky = metar["weather"], metar["sky"]
            gust = metar["gust"]
        else:
            weather = " ".join(dict.fromkeys(" ".join(isd_weather(row) for row in group).split()))
            sky = first(isd_sky) or ""
            gust = first(lambda row: isd_value(row.get("OC1", ""), 0, 10, "9999"))
            gust = "" if gust is None else round(gust * METERS_PER_SECOND_TO_KNOTS, 1)
        wind = first(lambda row: isd_value(row.get("WND", ""), 3, 10, "9999"))
        clouds = set()
        for row in group:
            clouds |= isd_cloud_types(row) | set(metar_cloud_types(row.get("REM", "")))
        observations.append({
            "time": date + "Z",
            "pressure": "" if pressure is None else f"{pressure:.1f}",
            "temperature": "" if temperature is None else temperature,
            "humidity": humidity,
            "precipitation": "" if precipitation is None else precipitation,
            "weather": weather,
            "gust": gust,
            "wind": "" if wind is None else round(wind * METERS_PER_SECOND_TO_KNOTS, 1),
            "sky": sky,
            "clouds": " ".join(sorted(clouds)),
        })
    first_row = rows[0]
    source = "SLP" if use_sea_level_pressure else "QNH"
    print(f"{station}: pressure from {source}")
    return observations, (first_row["LATITUDE"], first_row["LONGITUDE"], first_row["ELEVATION"])


def is_plausible_pressure(observations, i):
    # WHY: Some stations occasionally report sea level pressure tens of hPa off, which a phone barometer would not
    pressure = float(observations[i]["pressure"])
    if not 900 <= pressure <= 1080:
        return False
    neighbours = sorted(float(o["pressure"]) for o in observations[max(0, i - 6):i + 7]
                        if o is not observations[i] and o["pressure"] != "")
    if not neighbours:
        return True
    return abs(pressure - neighbours[len(neighbours) // 2]) <= MAX_PRESSURE_JUMP_HPA


selected_ids = set(sys.argv[1:])
unknown = selected_ids - {series[0] for series in SERIES}
if unknown:
    raise SystemExit(f"Unknown series: {', '.join(sorted(unknown))}")
selected = [series for series in SERIES if not selected_ids or series[0] in selected_ids]

os.makedirs(output_dir, exist_ok=True)
stations_path = os.path.join(output_dir, "stations.csv")
index = {}
if selected_ids and os.path.exists(stations_path):
    with open(stations_path) as f:
        index = {row[0]: row for row in list(csv.reader(f))[1:]}
else:
    for name in os.listdir(output_dir):
        if name.endswith(".csv.gz"):
            os.remove(os.path.join(output_dir, name))

for series_id, source, station, start, end, description in selected:
    if source == "asos":
        observations, (latitude, longitude, elevation) = download_asos(station, start, end)
        # WHY: The Mesonet rejects rapid repeated requests
        time.sleep(5)
    else:
        observations, (latitude, longitude, elevation) = download_isd(station)
    if not observations:
        raise RuntimeError(f"No observations for {series_id}")
    removed = 0
    with gzip.open(os.path.join(output_dir, f"{series_id}.csv.gz"), "wt", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["time", "pressure_hpa", "temperature_c", "humidity_percent", "precipitation_mm",
                         "weather", "gust_kt", "wind_kt", "sky_cover", "cloud_types"])
        for i, observation in enumerate(observations):
            pressure = observation["pressure"]
            if pressure != "" and not is_plausible_pressure(observations, i):
                pressure = ""
                removed += 1
            writer.writerow([observation["time"], pressure, observation["temperature"], observation["humidity"],
                             observation["precipitation"], observation["weather"], observation["gust"],
                             observation["wind"], observation["sky"], observation["clouds"]])
    index[series_id] = [series_id, latitude, longitude, elevation, description]
    print(f"{series_id}: {len(observations)} observations, {removed} implausible pressures removed")

with open(stations_path, "w", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(["id", "latitude", "longitude", "elevation_m", "description"])
    writer.writerows(index[series[0]] for series in SERIES if series[0] in index)
