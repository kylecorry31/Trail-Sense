# Trail Sense AI Tool Skills

## Avalanche Risk Check
Skill ID: avalanche_risk_check
Name zh: 雪崩风险检查
Needs: avalanche, avalanche risk, snow slope, snowpack, slope angle, terrain trap, recent snow, wind loading, warming, 雪崩, 雪崩风险, 积雪, 坡度, 雪坡, 风吹雪, 地形陷阱
Summary: Use several Trail Sense tools to collect clues about avalanche terrain, weather, and route context, then explain that Trail Sense cannot determine snowpack stability or avalanche safety.
Summary zh: 组合测斜仪、天气、云、温度预估、地图和导航，收集雪崩地形、天气和路线线索，并说明 Trail Sense 不能判断雪层稳定性或保证安全。
Tools: 11, 20, 23, 22, 47, 6
Steps: 1. Open Clinometer and measure the slope you plan to cross, holding the phone aligned with the fall line. Slopes around 30-45 degrees are common avalanche terrain, especially near 35-40 degrees, but angle alone cannot prove safety. 2. Open Weather and check pressure trend, storm alerts, recent worsening weather, wind, and precipitation clues. Falling pressure, active storms, strong wind, new snow, or rapid warming increase concern. 3. Open Clouds and identify or log current clouds; growing storm clouds or rapidly changing sky support a more conservative decision. 4. Open Temperature Estimation and compare your current elevation with the slope or pass elevation; warming above freezing, rapid temperature change, or freeze-thaw conditions can increase instability. 5. Open Map and Navigation to confirm elevation, route, slope aspect if known, exposure, and whether the route crosses gullies, bowls, convex rolls, or terrain traps. 6. Recommend checking an official avalanche forecast and avoiding the slope when observations are uncertain or several concern signals are present.
Steps zh: 1. 打开「测斜仪」，沿坡面最大下滑方向贴近或对准坡面测量坡角。约 30-45 度的坡面属于常见雪崩地形，35-40 度附近尤其需要谨慎，但坡角本身不能证明安全。2. 打开「天气」，查看气压趋势、风暴提示、天气是否变坏、风和降雪线索。气压下降、正在风暴、强风、新雪或快速升温都会提高风险关注。3. 打开「云」，识别或记录当前云况；积雨云、快速发展云系或天空快速变化，说明应更保守。4. 打开「温度预估」，比较当前位置和目标坡面/山口海拔的温度变化；升温到 0 度以上、快速变温、冻融循环都可能增加不稳定性。5. 打开「地图」和「导航」，确认位置、海拔、路线、坡向线索，以及是否穿越沟槽、碗状地形、凸坡、坡脚堆积区等地形陷阱。6. 如果多个信号同时出现，或无法确认雪况，应建议查看官方雪崩预报、现场观察报告，并选择绕行或返回。
Interpretation: Treat the result as concern signals, not a safe/unsafe verdict. Higher concern if the slope is around 30-45 degrees, recent or ongoing storm signs exist, pressure is falling, wind or new snow may be loading the slope, temperature is rapidly warming or crossing freezing, or the route enters terrain traps. Lower concern only means fewer warning signs were found, not that travel is safe.
Interpretation zh: 只能把结果当作“风险关注信号”，不能当作安全/不安全结论。以下情况应提高风险关注：坡角约 30-45 度；最近或正在出现风暴信号；气压下降；强风或新雪可能让坡面加载；温度快速升高或跨过 0 度；路线进入沟槽、碗状地形、凸坡下方、坡脚堆积区等地形陷阱。如果这些信号较少，只能说关注较低，不能说可以安全通行。
Caveats: Avalanche decisions are safety-critical. Trail Sense only provides supporting observations and cannot evaluate snowpack stability, weak layers, recent loading, terrain traps, group spacing, rescue readiness, or human factors. Never answer that a slope is safe. Prefer conservative language: concern is lower/higher based on clues, but official avalanche forecasts, training, local observations, beacon/probe/shovel, and field judgment are required.
Caveats zh: 雪崩判断是安全关键决策。Trail Sense 只能提供辅助观察，不能评估雪层稳定性、弱层、近期加载、地形陷阱、队伍间距、救援准备或人为因素。不要回答“这个坡是安全的”。应使用保守表达：根据线索风险关注较低/较高，但仍需要官方雪崩预报、专业训练、当地观察、雪崩信标/探杆/铲子和现场判断。
Sample prompts: Am I in avalanche terrain? | 我现在是否有雪崩风险？ | Which tools should I use before crossing this snow slope?

## Storm Check
Skill ID: storm_check
Name zh: 暴风雨检查
Needs: storm, severe weather, thunder, lightning, falling pressure, dark clouds, weather worsening, 暴风雨, 雷暴, 闪电, 气压下降, 天气变坏
Summary: Combine pressure trend, visible clouds, lightning distance, and current navigation context to judge whether to seek shelter or change plans.
Summary zh: 结合气压趋势、云况、闪电距离和当前位置，判断是否需要寻找避险处或改变计划。
Tools: 20, 23, 24, 6, 38
Steps: Use Weather to inspect pressure trend and alerts. Use Clouds to identify cloud type and log current sky conditions. Use Lightning Strike Distance if there is lightning or thunder. Use Navigation for current location and speed. Use Turn Back if daylight or return timing is the limiting factor.
Steps zh: 打开「天气」查看气压趋势和警报；打开「云」识别云型并记录天空状况；如果有闪电或雷声，使用「闪电距离」估算远近；用「导航」确认位置和移动速度；如果日照或返程时间受限，使用「折返」。
Interpretation: Increase concern if pressure is falling, clouds are building rapidly, lightning distance is decreasing, thunder is audible, or the route has no fast shelter option.
Interpretation zh: 如果气压下降、云快速发展、闪电距离变近、已经听到雷声，或当前路线没有快速避险点，应提高风险关注并优先避险。
Caveats: If thunder is audible or weather is clearly worsening, prioritize shelter and real-world judgment over app estimates.
Caveats zh: 如果已经听到雷声或天气明显变坏，应优先寻找避险处，不要等待应用估算给出“安全”结论。
Sample prompts: Is a storm coming? | 现在是不是要下暴雨？ | Which tools help decide when to turn back?

## Navigate Back Safely
Skill ID: navigate_back_safely
Name zh: 安全返程
Needs: get back, return to camp, lost, route back, navigation, beacon, path, offline map, 返程, 迷路, 回营地, 回到起点, 离线地图
Summary: Use saved locations, recorded paths, map layers, and turn-back timing to help the user return safely.
Summary zh: 使用已保存位置、路径记录、地图图层和折返时间，帮助用户更安全地返回。
Tools: 6, 7, 9, 47, 8, 38
Steps: Use Navigation to follow a bearing or saved beacon. Use Beacons for known locations. Use Paths or Backtrack if a route was recorded. Use Map and Offline Maps to inspect terrain and route context. Use Turn Back for time-based return decisions.
Steps zh: 用「导航」跟随方位或已保存信标；用「信标」查找营地、停车点或目标位置；如果记录过路线，用「路径」或 Backtrack 返程；用「地图」和「离线地图」查看地形和路线背景；用「折返」处理天黑前返回或定时返程。
Interpretation: Favor the route with known waypoints, recorded path evidence, better map context, enough daylight, and acceptable GPS/compass accuracy. If sensors disagree or the route crosses unsafe terrain, stop and reassess.
Interpretation zh: 优先选择有已知信标、有路径记录、有地图背景、日照时间足够且 GPS/指南针精度可接受的路线。如果传感器互相矛盾，或返程路线穿越危险地形，应停下重新判断。
Caveats: GPS and compass accuracy can degrade. Carry a physical map and compass, and avoid relying on a single sensor.
Caveats zh: GPS 和指南针精度可能下降。应携带实体地图和指南针，不要只依赖一个传感器。
Sample prompts: How do I get back to camp? | 我迷路了应该用哪些工具？ | Which tools help me retrace my route?

## Cold Elevation Planning
Skill ID: cold_elevation_planning
Name zh: 高海拔低温规划
Needs: cold, elevation temperature, freezing, hypothermia, mountain weather, gear, temperature drop, 低温, 海拔温度, 失温, 结冰, 保暖装备
Summary: Estimate how temperature may change with elevation and pair it with weather, climate, and packing checks.
Summary zh: 估算海拔变化带来的温度变化，并结合天气、气候和装备清单做低温准备。
Tools: 22, 20, 21, 27, 14
Steps: Use Temperature Estimation for a target elevation estimate. Use Weather for current trends. Use Climate for normal seasonal expectations. Use Packing Lists to check insulation, rain protection, and emergency gear. Use Astronomy for daylight and night timing.
Steps zh: 用「温度预估」估算目标海拔温度；用「天气」查看当前趋势；用「气候」了解季节常态；用「装备清单」检查保暖、防雨和应急装备；用「天文」确认日照和夜间时间。
Interpretation: Increase concern if the target elevation estimate is near or below freezing, weather is worsening, daylight is limited, precipitation or wind is likely, or the packing list lacks insulation and emergency gear.
Interpretation zh: 如果目标海拔温度接近或低于 0 度、天气变坏、日照不足、可能有降水或大风，或装备清单缺少保暖和应急装备，应提高低温风险关注。
Caveats: Elevation estimates are rough. Wind, precipitation, shade, exposure, and storms can make conditions much colder than the estimate.
Caveats zh: 海拔温度估算很粗略。风、降水、阴影、暴露地形和风暴都可能让体感远低于估算。
Sample prompts: How cold will it be at the summit? | 上山会不会很冷？ | Which tools help me pack for cold weather?
