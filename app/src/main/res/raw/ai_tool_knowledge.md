# Trail Sense AI Tool Knowledge

## 1. Flashlight
Tool ID: 1
Needs: light, torch, strobe, SOS, screen light, visual emergency signal, 手电筒, 照明, 闪光, 求救信号
Where: Open Flashlight from Tools; can also be used as a quick action or widget.
How: Choose flashlight, strobe, SOS, or screen mode; adjust settings such as auto-off or volume-button toggle if needed.
Values: The mode controls the light pattern; SOS is a distress pattern; screen mode uses the display instead of the camera flash.
Caveats: Requires camera flash for torch modes; carry a real flashlight as backup.
Related: Whistle, Signal Finder

## 2. Whistle
Tool ID: 2
Needs: whistle, loud signal, SOS sound, emergency alert, 哨子, 声音信号, 求救, 警报
Where: Open Whistle from Tools; can also be used as a quick action.
How: Select whistle, SOS, help, or another signal and play it at high volume.
Values: The selected signal controls the sound pattern; SOS is the international distress rhythm.
Caveats: Phone speakers are limited; turn volume up or use an external speaker and carry a real whistle.
Related: Flashlight

## 3. Ruler
Tool ID: 3
Needs: measure small objects, map distance, length, inches, centimeters, scale, 测量, 尺子, 长度, 距离, 比例尺
Where: Open Ruler from Tools.
How: Place the object along the on-screen ruler; change units or direction; calibrate if the screen scale is off.
Values: Displayed length is the physical distance in the selected unit; map measuring uses the configured map scale.
Caveats: Accuracy depends on screen calibration and correct map scale.
Related: Convert

## 4. Pedometer
Tool ID: 4
Needs: steps, walking distance, speed, odometer, hike tracking, 步数, 计步, 行走距离, 速度, 徒步
Where: Open Pedometer from Tools; can also be used as a quick action or widget.
How: Start tracking and calibrate step length for better distance and speed estimates.
Values: Steps come from the step sensor; distance is based on steps and stride; speed is derived from movement over time.
Caveats: Requires a step sensor for full function; calibration strongly affects distance.
Related: Paths, Navigation

## 5. Cliff Height
Tool ID: 5
Needs: cliff height, drop height, estimate vertical distance, 悬崖高度, 高度, 垂直距离, 落差
Where: Open Cliff Height from Tools.
How: Follow the tool's estimation flow and enter or measure the requested inputs.
Values: The result is an estimated height based on measured inputs such as angle, distance, or timing.
Caveats: Estimate only; do not use for dangerous climbing or rescue decisions without proper gear.
Related: Clinometer

## 6. Navigation
Tool ID: 6
Needs: compass, navigate, bearing, heading, GPS, current location, speed, elevation, 导航, 指南针, 方位角, 航向, 当前位置, 海拔, 速度
Where: Open Navigation from Tools.
How: Use the compass for orientation; tap the navigation button to choose a beacon; tap the compass to set a bearing; inspect location, elevation, and speed panels.
Values: Bearing is direction in degrees; distance is distance to target; ETA is estimated travel time; elevation change compares current elevation to target; accuracy and satellites describe GPS quality.
Caveats: Compass needs calibration; GPS and elevation can be inaccurate; carry a physical compass and map.
Related: Beacons, Paths, Map, Declination

## 7. Beacons
Tool ID: 7
Needs: save location, waypoint, marker, point of interest, navigate back, 保存位置, 路标, 标记点, 兴趣点, 返回
Where: Open Beacons from Tools; can also be used as a quick action or widget.
How: Tap + to create or import a beacon; tap a beacon to view, edit, share, or navigate to it.
Values: Coordinates identify the saved place; distance and bearing show where it is relative to you; elevation and notes add context.
Caveats: GPS accuracy affects saved and current positions; organize and control visibility with groups.
Related: Navigation, Map, QR Code Scanner

## 8. Offline Maps
Tool ID: 8
Needs: offline map, photo map, trail map, PDF map, Mapsforge, imported map, 离线地图, 照片地图, 导入地图, 纸质地图, PDF地图
Where: Open Offline Maps from Tools; can also be used as a quick action.
How: Tap + and import a file, camera photo, blank map, or group; calibrate image/PDF maps with two known points.
Values: Calibration aligns the map to real coordinates; scale enables distance, beacon, path, and elevation overlays; visibility controls map layers.
Caveats: Maps are not bundled; non-geospatial images need calibration; poor calibration misplaces overlays.
Related: Map, Map Layers, Beacons, Paths

## 9. Paths
Tool ID: 9
Needs: record hike, backtrack, breadcrumb, route, trail, GPX, path, 记录路线, 轨迹, 回溯, 面包屑, 小路, 路径
Where: Open Paths from Tools; can also be used as a quick action or widget.
How: Use Backtrack to record automatically or tap + to create or import a path; tap a path for overview and actions.
Values: Distance is path length; waypoints are recorded points; elevation profile shows ascent/descent; average speed and ETA depend on recorded movement.
Caveats: GPS quality and recording frequency affect accuracy and battery use.
Related: Navigation, Map, Pedometer

## 10. Triangulate Location
Tool ID: 10
Needs: triangulate, find position from bearings, locate distant object, 三角定位, 方位定位, 定位远处目标, 交会
Where: Open Triangulate Location from Tools.
How: Enter two known locations and bearings, then view the estimated intersection on the map.
Values: The intersection is the estimated position; bearings are sight lines from known points.
Caveats: Small bearing or location errors can create large position errors; use well-spaced references.
Related: Navigation, Map

## 11. Clinometer
Tool ID: 11
Needs: angle, slope, incline, avalanche risk, tree height, distance by angle, 坡度, 倾角, 斜坡, 坡面, 测角, 角度, 雪崩, 树高
Where: Open Clinometer from Tools.
How: Hold the phone aligned with the slope or sightline and choose the relevant calculator mode.
Values: Angle is inclination in degrees; slope may be grade or percent; height and distance estimates are derived from angle and known distance or height.
Caveats: Requires calibrated orientation sensors; avalanche guidance is a rough signal, not a safety guarantee.
Related: Bubble Level, Cliff Height

## 12. Bubble Level
Tool ID: 12
Needs: level, flat surface, tilt, tent platform, stove setup, 水平, 水平仪, 平面, 倾斜, 帐篷平台, 炉具
Where: Open Bubble Level from Tools.
How: Place the phone on the surface and watch the bubble or angle indicator.
Values: Centered bubble means approximately level; angle indicates tilt away from level.
Caveats: Depends on accelerometer calibration and phone case or edge shape.
Related: Clinometer

## 13. Clock
Tool ID: 13
Needs: GPS time, clock sync, digital clock, GPS时间, 时钟, 校时, 数字时钟
Where: Open Clock from Tools.
How: View GPS time or digital time and use sync information if available.
Values: GPS time comes from satellites; offset or sync quality indicates time reliability.
Caveats: GPS time requires satellite data and may be poor indoors.
Related: Astronomy

## 14. Astronomy
Tool ID: 14
Needs: sunrise, sunset, moonrise, moonset, moon phase, meteor shower, eclipse, sun direction, 日出, 日落, 月出, 月落, 月相, 流星雨, 日食, 月食, 太阳方向
Where: Open Astronomy from Tools; can also provide alerts and widgets.
How: Inspect sun and moon cards, charts, event lists, 3D view, or configure alerts.
Values: Rise and set times show horizon crossings; azimuth is compass direction; altitude is angle above horizon; phase and illumination describe the moon.
Caveats: Calculations depend on location and time; terrain can hide actual sunrise or sunset.
Related: Navigation, Turn Back, Solar Panel Aligner

## 15. Water Boil Timer
Tool ID: 15
Needs: boil water, water purification, safe drinking water timer, 烧水, 煮沸, 净水, 饮用水, 安全饮水
Where: Open Water Boil Timer from Tools.
How: Use current or entered elevation and start the timer after water reaches a rolling boil.
Values: Boil time is the recommended duration adjusted for elevation.
Caveats: Guidance only; use proper water treatment and follow local health guidance when possible.
Related: Survival Guide

## 16. Tides
Tool ID: 16
Needs: high tide, low tide, tide table, ocean, coast, tide chart, 潮汐, 涨潮, 退潮, 高潮, 低潮, 海边
Where: Open Tides from Tools; can also use tide widgets and map layers.
How: Create a tide table from official high/low data or estimate tides, then view chart and nearby tide.
Values: High and low tide times are extrema; current tide height or phase is estimated between known points.
Caveats: Tide estimates can be dangerous if wrong; use official tide data for boating and coastal safety.
Related: Map

## 17. Battery
Tool ID: 17
Needs: battery, power, charging, time until dead, power saving, 电池, 电量, 充电, 省电, 续航
Where: Open Battery from Tools; can also be used as a quick action.
How: Inspect status, statistics, history, services, power saving, and system settings.
Values: Time remaining is estimated from recent usage; statistics show charge, drain, and battery state over time.
Caveats: Estimates change with usage, temperature, and Android battery reporting.
Related: Settings

## 18. Solar Panel Aligner
Tool ID: 18
Needs: align solar panel, sun angle, solar charging, 太阳能板, 对准太阳, 太阳角度, 充电
Where: Open Solar Panel Aligner from Tools.
How: Follow the direction and angle guidance to orient the panel toward the sun.
Values: Alignment guidance indicates direction and tilt that should improve solar exposure.
Caveats: Requires orientation sensors and clear sun exposure; shade and panel hardware matter.
Related: Astronomy

## 19. Light Meter
Tool ID: 19
Needs: light meter, lux, brightness, flashlight beam distance, 照度计, 勒克斯, 亮度, 光束距离
Where: Open Light Meter from Tools.
How: Follow the beam distance test or light measurement instructions.
Values: Beam distance estimates usable flashlight throw; light readings are approximate sensor readings.
Caveats: Requires supported sensors; environment and device hardware affect results.
Related: Flashlight

## 20. Weather
Tool ID: 20
Needs: weather forecast, barometer, pressure, storm alert, rain, humidity, weather monitor, 天气, 天气预报, 气压计, 气压, 暴风雨, 下雨, 湿度, 天气监测
Where: Open Weather from Tools; configure alerts in Settings > Weather; can also be a quick action or widget.
How: Enable the weather monitor to record pressure history; inspect prediction, pressure chart, temperature, humidity, clouds, and front.
Values: Falling pressure can mean worsening weather; rising pressure suggests clearing; pressure tendency is change over time; prediction combines pressure, climate normals, and logged clouds.
Caveats: Requires barometer for core features; forecast is a best guess and not an Internet forecast; trust visible weather and seek shelter if needed.
Related: Clouds, Climate, Temperature Estimation

## 21. Climate
Tool ID: 21
Needs: historical weather, climate normals, average temperature, precipitation, seasonal conditions, 气候, 历史天气, 平均温度, 降水, 季节
Where: Open Climate from Tools.
How: Change date or location to view historical climate data.
Values: Temperature and precipitation are historical averages; classification and ecology summarize typical patterns.
Caveats: Normals are not current weather and can differ during storms or unusual years.
Related: Weather

## 22. Temperature Estimation
Tool ID: 22
Needs: estimate temperature at elevation, altitude temperature, lapse rate, 温度估算, 海拔温度, 高度温度, 递减率
Where: Open Temperature Estimation from Tools.
How: Enter current and target elevation and temperature if prompted.
Values: Estimated temperature changes with elevation using a lapse-rate model.
Caveats: Rough estimate only; weather fronts, shade, wind, and terrain can dominate.
Related: Weather, Climate

## 23. Clouds
Tool ID: 23
Needs: identify clouds, cloud type, cloud forecast, sky classification, rain signs, 云, 云识别, 云类型, 天空, 下雨迹象
Where: Open Clouds from Tools; can also be a quick action.
How: Log a cloud manually or use automatic identification; use results with the Weather tool.
Values: Cloud type indicates likely weather pattern; logged clouds can inform weather prediction and front detection.
Caveats: Automatic identification may be wrong; use visible sky judgment.
Related: Weather

## 24. Lightning Strike Distance
Tool ID: 24
Needs: lightning distance, thunder delay, storm approaching, 闪电距离, 雷声延迟, 雷暴, 风暴接近
Where: Open Lightning Strike Distance from Tools.
How: Start timing at the lightning flash and stop when thunder is heard.
Values: Distance is computed from the delay between light and sound; increasing or decreasing distances indicate storm movement.
Caveats: If thunder is audible, lightning risk exists; seek shelter.
Related: Weather

## 25. Augmented Reality
Tool ID: 25
Needs: AR, camera overlay, beacons in real world, sun path, moon path, stars, compass grid, 增强现实, 相机叠加, 现实世界, 太阳路径, 月亮路径, 星星
Where: Open Augmented Reality from Tools.
How: Point the camera around you and enable the desired layers.
Values: Overlays show estimated real-world direction or position; reticle and grid help alignment.
Caveats: Requires camera, compass, location, and calibration; AR can drift, so verify with map or compass.
Related: Beacons, Navigation, Astronomy

## 26. Convert
Tool ID: 26
Needs: convert units, coordinates, distance, temperature, volume, weight, time, 单位换算, 坐标转换, 距离, 温度, 体积, 重量, 时间
Where: Open Convert from Tools; can also be a quick action.
How: Choose conversion type, enter a value, and select input/output units or coordinate formats.
Values: Output is the equivalent value in the target unit or coordinate format.
Caveats: Coordinate conversions require correct format and datum assumptions.
Related: Ruler, Settings

## 27. Packing Lists
Tool ID: 27
Needs: packing checklist, trip gear, inventory, pack weight, 打包清单, 装备, 行李, 背包重量
Where: Open Packing Lists from Tools.
How: Create a list, add items, mark packed, sort, export, or import.
Values: Pack weight sums item weights and quantities; packed state tracks what is ready.
Caveats: Weight is only as accurate as entered item data.
Related: Notes

## 28. Metal Detector
Tool ID: 28
Needs: detect metal, magnetic metal, iron, keys, knife, magnetometer, 金属探测, 磁性金属, 铁, 钥匙, 刀, 磁力计
Where: Open Metal Detector from Tools.
How: Calibrate, move the phone near objects, and adjust threshold or sensitivity.
Values: Readings represent magnetic field strength; spikes or direction changes suggest ferromagnetic metal nearby.
Caveats: Cannot reliably detect coins or jewelry; affected by magnets, cases, and environment.
Related: Sensors

## 29. White Noise
Tool ID: 29
Needs: white noise, sleep sound, relax, rest, sleep timer, 白噪音, 睡眠, 放松, 休息, 睡眠定时
Where: Open White Noise from Tools; can also be a quick action.
How: Choose a sound and optionally set a sleep timer.
Values: Sound selection controls audio texture; sleep timer stops playback after the set duration.
Caveats: Keep volume safe, especially with headphones.
Related: Clock

## 30. Notes
Tool ID: 30
Needs: notes, memo, journal, offline text, log, 笔记, 备忘录, 日志, 离线文本
Where: Open Notes from Tools; can also be a quick action.
How: Create, edit, delete, or share notes, including QR sharing.
Values: Notes are stored text; QR sharing encodes note content for another device to scan.
Caveats: Avoid storing sensitive data unless the device is protected.
Related: QR Code Scanner

## 31. QR Code Scanner
Tool ID: 31
Needs: scan QR code, barcode, open URL, scan location, share data, 扫描二维码, 条码, 链接, 位置, 分享数据
Where: Open QR Code Scanner from Tools.
How: Point the camera at a QR code and choose the action for the result.
Values: Result type determines action: URL opens a link, location can open or create location data, text is displayed or copied.
Caveats: Requires camera; only open trusted URLs and codes.
Related: Notes, Beacons

## 32. Sensors
Tool ID: 32
Needs: sensor list, raw sensor values, GPS, altimeter, barometer, compass, accelerometer, 传感器, 原始数据, GPS, 高度计, 气压计, 指南针, 加速度计
Where: Open Sensors from Tools; can also be a quick action or widget.
How: View the sensor list and inspect individual sensor readings.
Values: Values are raw or processed device readings such as pressure, light, orientation, acceleration, GPS, or elevation.
Caveats: Units and availability depend on hardware; useful for troubleshooting.
Related: Diagnostics, Settings

## 33. Diagnostics
Tool ID: 33
Needs: troubleshoot, diagnose, test sensors, check permissions, app issues, 诊断, 排查问题, 测试传感器, 权限, 应用问题
Where: Open Diagnostics from Tools.
How: Run or inspect diagnostics for sensors, permissions, notifications, and services.
Values: Each diagnostic reports whether a dependency is healthy or needs action.
Caveats: Passing diagnostics does not guarantee perfect readings.
Related: Sensors, Settings

## 34. Settings
Tool ID: 34
Needs: configure units, calibrate sensors, privacy, theme, tools, backup, 设置, 配置, 单位, 校准传感器, 隐私, 主题, 备份
Where: Open Settings from Tools.
How: Choose a category such as Units, Sensors, Privacy, Tools, Theme, or Backup.
Values: Settings change how tools interpret and display units, north reference, sensor source, smoothing, offsets, and visibility.
Caveats: Incorrect calibration or settings can affect many tools.
Related: Diagnostics, Sensors

## 35. User Guide
Tool ID: 35
Needs: help, guide, manual, instructions, tutorial, how to use Trail Sense, 帮助, 指南, 手册, 教程, 怎么使用
Where: Open User Guide from Tools; can also be a quick action.
How: Search or select a guide for the relevant tool.
Values: Guide content explains usage, settings, accuracy, widgets, and related tools.
Caveats: Static documentation may not cover experimental or plugin-only tools.
Related: AI Assistant

## 36. Experimentation
Tool ID: 36
Needs: experiment, debug, sandbox, internal testing
Where: Only visible in debug builds.
How: Use only for internal experimental features.
Values: No stable user-facing result contract.
Caveats: Debug-only and experimental; do not recommend to regular users.
Related: Settings

## 37. Mirror Camera
Tool ID: 37
Needs: mirror, front camera, reflection, selfie view, 镜子, 前置相机, 反射, 自拍
Where: Open Mirror Camera from Tools.
How: Use the camera preview as a mirror.
Values: The displayed image is the camera preview.
Caveats: Requires camera; not a medical tool.
Related: Magnifier

## 38. Turn Back
Tool ID: 38
Needs: turn back reminder, return time, turnaround alarm, return before dark, 返回提醒, 折返时间, 返回闹钟, 天黑前返回
Where: Open Turn Back from Tools.
How: Choose a return time or return-before-dark behavior and start the alert.
Values: Alert timing is based on the target return time and local sunset context when applicable.
Caveats: Estimate depends on assumptions and user pace; leave margin.
Related: Astronomy, Paths

## 39. Local Messaging
Tool ID: 39
Needs: local message, communication, send text, receive text
Where: Visible only when the Trail Sense communications companion app is installed.
How: Open Local Messaging to launch the companion app messaging tool.
Values: Results are handled by the companion communications app.
Caveats: Experimental and plugin-only; no built-in guide in this repo.
Related: Local Talk

## 40. Local Talk
Tool ID: 40
Needs: local talk, voice communication, walkie talkie, radio, speak
Where: Visible only when the Trail Sense communications companion app is installed.
How: Open Local Talk to launch the companion app talk tool.
Values: Results are handled by the companion communications app.
Caveats: Experimental and plugin-only; no built-in guide in this repo.
Related: Local Messaging

## 41. Survival Guide
Tool ID: 41
Needs: survival guide, wilderness reference, bushcraft, emergency information, 生存指南, 野外生存, 应急信息, 户外参考
Where: Open Survival Guide from Tools; can also be a quick action.
How: Browse or search survival topics.
Values: Results are reference articles and steps, not live sensor readings.
Caveats: Educational only; follow real safety guidance and local emergency procedures.
Related: Field Guide, Water Boil Timer

## 42. Field Guide
Tool ID: 42
Needs: identify plant, animal, fungi, mushroom, wildlife, sightings, nature log, 识别植物, 动物, 真菌, 蘑菇, 野生动物, 观察记录
Where: Open Field Guide from Tools.
How: Browse built-in pages, create or edit pages, record sightings, and control map layer visibility.
Values: Sightings are saved observations; map layer visibility controls whether they appear on maps.
Caveats: Identification is not guaranteed; be careful with plants, animals, and mushrooms.
Related: Survival Guide, Map

## 43. Signal Finder
Tool ID: 43
Needs: find cell signal, tower, reception, network strength, cellular coverage, 找信号, 手机信号, 基站, 网络强度, 蜂窝覆盖
Where: Open Signal Finder from Tools.
How: Inspect signal readings, cell towers, and emergency-call information.
Values: Signal readings indicate cellular reception strength or availability; tower info helps choose a better location.
Caveats: Cell data varies by device and network; emergency availability is not guaranteed.
Related: Beacons, Map

## 44. Ballistics
Tool ID: 44
Needs: ballistics, bullet trajectory, firearm calculator, scope, energy, 弹道, 子弹轨迹, 枪械计算, 瞄准镜, 能量
Where: Open Ballistics from Tools.
How: Enter firearm, projectile, and scope parameters to calculate trajectory or energy.
Values: Drop, adjustment, and energy values are calculator outputs from entered parameters.
Caveats: Safety-critical; follow firearm laws and safe handling and verify with real ballistic data.
Related: Convert

## 45. Permits
Tool ID: 45
Needs: permits, license, pass, authorization, reservation
Where: Only visible in debug builds.
How: Currently a placeholder permit list.
Values: No stable user-facing values.
Caveats: Debug-only experimental tool; do not recommend to regular users yet.
Related: Settings

## 46. Declination
Tool ID: 46
Needs: magnetic declination, true north, magnetic north, compass correction, 磁偏角, 真北, 磁北, 指南针校正
Where: Open Declination from Tools.
How: Use current or chosen location to get the declination for a physical compass.
Values: Declination is the angle between true north and magnetic north.
Caveats: Declination varies by location and date.
Related: Navigation, Settings

## 47. Map
Tool ID: 47
Needs: map, terrain, topographic, elevation, contours, hillshade, beacons, paths, 地图, 地形, 地形图, 海拔, 等高线, 阴影, 坡度图层, 路标, 路径
Where: Open Map from Tools; can also be a widget.
How: Pan and zoom, use layers, long-press to create or navigate, measure distance, or inspect elevation.
Values: Scale shows map distance; measurements use selected map/data sources; layers show beacons, paths, tides, DEM overlays, and more.
Caveats: Not a replacement for dedicated maps or physical maps; needs loaded offline maps for base map content.
Related: Offline Maps, Beacons, Paths

## 48. Magnifier
Tool ID: 48
Needs: magnifier, magnify, zoom, enlarge, close-up text, camera zoom, 放大镜, 放大, 缩放, 近距离文字, 相机变焦
Where: Open Magnifier from Tools.
How: Adjust zoom, focus mode, torch, or freeze the image.
Values: Zoom is camera magnification; freeze captures the current frame for inspection.
Caveats: Requires camera; image quality depends on focus and light.
Related: Mirror Camera

## 49. AI Assistant
Tool ID: 49
Needs: AI help, ask Trail Sense, explain app tools, sensor interpretation, assistant, AI帮助, 询问Trail Sense, 解释工具, 传感器解读, 助手
Where: Open AI Assistant from Tools; configure in AI settings where available.
How: Ask a question, optionally from a tool context or with an attached photo.
Values: Responses are generated from local model instructions, provided tool knowledge, live context when available, and attached images.
Caveats: Advice is supplementary and can be wrong; verify safety-critical information.
Related: User Guide
