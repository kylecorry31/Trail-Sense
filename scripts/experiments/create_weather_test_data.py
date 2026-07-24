import urllib.request
import urllib.parse
import json
import csv

"""
Open-Meteo data from:


Zippenfenig, P. (2023). Open-Meteo.com Weather API [Computer software]. Zenodo. https://doi.org/10.5281/ZENODO.7970649

Hersbach, H., Bell, B., Berrisford, P., Biavati, G., Horányi, A., Muñoz Sabater, J., Nicolas, J., Peubey, C., Radu, R., Rozum, I., Schepers, D., Simmons, A., Soci, C., Dee, D., Thépaut, J-N. (2023). ERA5 hourly data on single levels from 1940 to present [Data set]. ECMWF. https://doi.org/10.24381/cds.adbb2d47

Muñoz Sabater, J. (2019). ERA5-Land hourly data from 2001 to present [Data set]. ECMWF. https://doi.org/10.24381/CDS.E2161BAC

Schimanke S., Ridal M., Le Moigne P., Berggren L., Undén P., Randriamampianina R., Andrea U., Bazile E., Bertelsen A., Brousseau P., Dahlgren P., Edvinsson L., El Said A., Glinton M., Hopsch S., Isaksson L., Mladek R., Olsson E., Verrelle A., Wang Z.Q. (2021). CERRA sub-daily regional reanalysis data for Europe on single levels from 1984 to present [Data set]. ECMWF. https://doi.org/10.24381/CDS.622A565A
"""


def request_url(url, params):
    if params:
        url = url + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url) as response:
        return response.read().decode("utf-8")


def get_historical_weather(latitude, longitude, start_date, end_date):
    url = "https://archive-api.open-meteo.com/v1/archive"
    params = {
        "latitude": latitude,
        "longitude": longitude,
        "start_date": start_date,
        "end_date": end_date,
        "hourly": ",".join(
            [
                "temperature_2m",
                "relative_humidity_2m",
                "weather_code",
                "cloud_cover_low",
                "cloud_cover_mid",
                "cloud_cover_high",
                "precipitation",
                "pressure_msl",
                "wind_speed_10m",
            ]
        ),
    }
    response = request_url(url, params)
    return json.loads(response)["hourly"]


locations = [
    (52.52, 13.41, "2025-10-03", "2025-10-05"),  # Rain storm
    (52.52, 13.41, "2025-09-28", "2025-09-29"),  # Clear
    (41.9, -71.7, "2025-09-25", "2025-09-27"),  # Rain storm
    (41.9, -71.7, "2025-09-30", "2025-10-01"),  # Windy
    (41.9, -71.7, "2025-02-08", "2025-02-10"),  # Snow
]

data = []
for lat, lon, start, end in locations:
    weather_data = get_historical_weather(lat, lon, start, end)
    for i in range(len(weather_data["time"])):
        row = [
            lat,
            lon,
            start,
            end,
            weather_data["time"][i],
            weather_data["temperature_2m"][i],
            weather_data["relative_humidity_2m"][i],
            weather_data["weather_code"][i],
            weather_data["cloud_cover_low"][i],
            weather_data["cloud_cover_mid"][i],
            weather_data["cloud_cover_high"][i],
            weather_data["precipitation"][i],
            weather_data["pressure_msl"][i],
            weather_data["wind_speed_10m"][i],
        ]
        data.append(row)
    i += 1

with open("weather_test_data.csv", "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(
        [
            "latitude",
            "longitude",
            "start_date",
            "end_date",
            "time",
            "temperature_2m",
            "relative_humidity_2m",
            "weather_code",
            "cloud_cover_low",
            "cloud_cover_mid",
            "cloud_cover_high",
            "precipitation",
            "pressure_msl",
            "wind_speed_10m",
        ]
    )
    writer.writerows(data)
