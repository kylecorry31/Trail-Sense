---
title: "Weather"
---

The Weather tool can be used to roughly predict the weather.

## Weather prediction
The weather prediction appears at the top of your screen. This does not use the Internet and factors in pressure history, historical (30-year average) temperatures, and the clouds from the Clouds tool. It also attempts to estimate when the weather change will occur, displayed beneath the prediction. The accuracy of the time frame depends on if the clouds indicate a weather front; if it's detected, an approximate time will be displayed, otherwise, it's more vague, like "soon" or "later."

If the weather monitor is on, it records pressure history automatically. Otherwise, pressure updates only occur when you open the Weather tool. For accuracy, it's best to record pressure every 30 minutes.

You can adjust prediction sensitivity in Settings > Weather > Forecast sensitivity. Higher sensitivity may detect more patterns but might yield more false predictions.

## Weather monitor
The weather monitor runs in the background to record pressure history. Enable it by clicking the start icon at the bottom-right. This causes a notification to appear with the current prediction and an option to stop the weather monitor. You can also stop it by clicking the stop icon in the bottom-right.

To change the recording frequency, click the time under the weather monitor label at the bottom left and enter a new interval.

For better accuracy, grant Trail Sense Location permission to determine your elevation via GPS. This helps convert pressure to sea level pressure, improving prediction accuracy.

In Settings > Weather, you can customize the notification to show:

- **Pressure**: Displays the current pressure.
- **Temperature**:  Displays the current temperature (refer to Thermometer guide for details).

You can also enable daily weather notifications in Settings > Weather > Daily weather notification. Without the weather monitor being active, you won't receive these notifications. The timing of this notification is configurable in Settings > Weather > Daily weather time.

You can disable weather monitor notifications in Android's notification settings for Trail Sense.

## Storm alert
If the weather monitor is active, Trail Sense can notify you of storms. Enable this in Settings > Weather > Storm alert. You can adjust the sensitivity in Settings > Weather > Storm sensitivity. Higher sensitivity might trigger false alerts, while lower sensitivity could miss some storms.

You can choose to use the alarm audio channel for this alert by enabling Settings > Weather > Use alarm for storm alert. This will play a sound as an alarm even if media and notifications are muted. You can choose to use the alarm only during the day by enabling Settings > Weather > Mute alarm at night. 

## Weather details
The Weather tool also provides more information about the current and predicted weather.

### Alerts
The Weather tool displays alerts when a storm is detected or historical temperatures suggest hot or cold conditions.

### Pressure
A pressure history chart is displayed at the top of the screen. Falling pressure may indicate an incoming storm, rising pressure suggests clearing weather, and steady pressure indicates no significant changes. The current pressure and its tendency (change over time) are listed below the chart.

The current pressure system is also displayed. Low pressure often means poor weather, while high pressure indicates fair conditions. Clicking on it provides a description of the pressure system.

You can change the history duration in Settings > Weather > "Pressure history".

### Temperature
The current temperature, sourced from historical data or the onboard thermometer, is shown in the weather list. Daily high/low temperatures are also provided using historical data. Clicking on the current temperature reveals a temperature history chart, while clicking on high/low temperatures displays a 24-hour temperature prediction chart based on historical temperatures.

### Humidity
If your device has a humidity sensor, the current humidity is displayed. Clicking on it reveals a humidity history chart.

### Clouds
The last logged cloud type appears in the weather list. Clicking on it provides a description of the cloud type.

### Weather front
The current weather front is displayed in the weather list. Fronts typically indicate weather changes and are often associated with precipitation and wind. Clicking on it provides a description of the weather front.

## Exporting weather data
You can export the recorded weather data to a CSV file with Settings > Weather > Export weather records.

## Quick action
You can enable the Weather Monitor quick action in the settings for the tab you want it on.

To use the quick action, tap the Weather Monitor quick action button to toggle it on or off.

## Accuracy
The weather prediction is a best guess using available sensor data and may not be accurate. If Trail Sense says it is going to be clear but you see what appears to be storm clouds rolling in, trust your instincts - sometimes storms roll in with gradual changes in pressure. Seek shelter if you sense a storm approaching, regardless of Trail Sense's prediction.

Climate normals (historic temperatures) are based on the 30-year average of the historic data and indicate what the weather usually is. The actual values may differ for a variety of reasons but should remain fairly close to the historic values. Large differences are normally due to a storm or other weather event.

Historic temperatures are estimated using a custom 30-year "climate normal" model derived from the NASA Global Modeling and Assimilation Office - MERRA-2 data (1991 - 2020).

## Widgets
The following widgets can be placed on your device's homescreen or viewed in-app:

- **Weather**: Shows the current weather and temperature prediction.
- **Pressure**: Shows the current pressure and tendency arrow.
- **Pressure chart**: The pressure chart. 
