# Weather Forecast

The weather forecast is a neural network trained on up to a year of hourly observations from 63 airports around the world, plus three hurricane landfalls. It runs on the device and doesn't need the Internet. The training script is `scripts/train_weather_model.py`.

## Inputs

- **Pressure**: The sea level pressure minus 1013 hPa, and its change over the past 1, 2, 3, 4, 6, 8, 10, 12, 15, 18, 21, 24, 30, 36, 42 and 48 hours. The daily pressure tide is removed first.
- **Has pressure**: 1 if there is a recent pressure history, otherwise 0. Without one, the pressure inputs are 0.
- **Time**: The solar time of day and the time of year, each as a sine and cosine. The time of year is shifted by half a year in the southern hemisphere.
- **Location**: The absolute latitude.
- **Climate**: The historic low and high temperatures for the day, and the log of the historic monthly and annual precipitation.
- **Logged clouds**: For the past hour and for 1 to 4 hours ago, the sky cover of the latest log (clear, partly cloudy or mostly cloudy) and each cloud type logged.

## Outputs

Each output is the probability of a condition:

- Precipitation, storm, wind, clear and overcast in the next 3 hours.
- Precipitation, storm, wind, clear and overcast starting in 3 to 12 hours.
- Thunderstorm in the next 12 hours.
- Precipitation, clear and overcast in 12 to 24 hours.
- Snow (most precipitation is snow or ice) in the next 12 hours and in 12 to 24 hours.

A condition is forecast when its probability is above a threshold that maximizes HSS. The forecast sensitivity setting scales how often precipitation, storm, wind and thunderstorm are forecast.

## Architecture

- 53 inputs, standardized.
- Two hidden layers of 28 and 19 ReLU units.
- 16 sigmoid outputs.

It is trained with Adam on the cross entropy of each output, with L2 regularization. Training includes copies of each sample with a shorter, thinned and noisy pressure history, as a phone records it, and copies with no pressure history.

## Accuracy

Heidke skill score (HSS) at stations held out of training (5 folds by station), pooled over all stations. 1 is perfect and 0 is no better than chance. Logged clouds are stood in for by each station's sky report and the cloud types it reports. Snow is only scored when there is precipitation.

| Condition                    | Pressure, no clouds | Pressure and clouds | Clouds, no pressure |
|------------------------------|---------------------|---------------------|---------------------|
| Precipitation, next 3 h      | 0.26                | 0.41                | 0.38                |
| Storm, next 3 h              | 0.14                | 0.26                | 0.19                |
| Wind, next 3 h               | 0.21                | 0.19                | 0.04                |
| Clear, next 3 h              | 0.28                | 0.71                | 0.70                |
| Overcast, next 3 h           | 0.26                | 0.78                | 0.78                |
| Precipitation, 3 to 12 h     | 0.09                | 0.11                | 0.08                |
| Storm, 3 to 12 h             | 0.11                | 0.11                | 0.09                |
| Wind, 3 to 12 h              | 0.13                | 0.13                | 0.09                |
| Clear, 3 to 12 h             | 0.07                | 0.11                | 0.08                |
| Overcast, 3 to 12 h          | 0.05                | 0.15                | 0.12                |
| Thunderstorm, next 12 h      | 0.18                | 0.22                | 0.23                |
| Precipitation, 12 to 24 h    | 0.27                | 0.31                | 0.28                |
| Clear, 12 to 24 h            | 0.27                | 0.44                | 0.43                |
| Overcast, 12 to 24 h         | 0.27                | 0.41                | 0.41                |
| Snow, next 12 h              | 0.73                | 0.73                | 0.73                |
| Snow, 12 to 24 h             | 0.73                | 0.72                | 0.73                |
