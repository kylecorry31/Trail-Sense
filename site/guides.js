const guides = [
    'tool_astronomy',
    'tool_augmented_reality',
    'tool_battery',
    'tool_bubble_level',
    'tool_cliff_height',
    'tool_climate',
    'tool_clinometer',
    'tool_clock',
    'tool_clouds',
    'tool_convert',
    'tool_flashlight',
    'tool_light_meter',
    'tool_lightning_strike_distance',
    'tool_metal_detector',
    'tool_notes',
    'tool_packing_lists',
    'tool_temperature_estimation',
    'tool_water_boil_timer',
    'tool_weather',
    'tool_whistle'
];

// Populate the guide list
const guideList = document.querySelector('#guide-list');
for (const guide of guides) {
    const guideItem = document.createElement('li');
    guideItem.innerHTML = `<a href="guide?id=${guide}">${guide.split('_').slice(1).map(w => w[0].toUpperCase() + w.slice(1)).join(' ')}</a>`;
    guideList.appendChild(guideItem);
}