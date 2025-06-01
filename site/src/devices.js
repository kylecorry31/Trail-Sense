const baseUrl = "./devices.json";

const ratingRanks = {
  A: 5,
  B: 4,
  C: 3,
  D: 2,
  Unsupported: 1,
};

function loadDevices() {
  return fetch(baseUrl)
    .then((response) => response.json())
    .then((json) => {
      const devices = json.devices;
      const sortedDevices = [...devices].sort((a, b) => {
        if (ratingRanks[a.rating] > ratingRanks[b.rating]) {
          return -1;
        } else if (ratingRanks[a.rating] < ratingRanks[b.rating]) {
          return 1;
        } else {
          return a.name.localeCompare(b.name);
        }
      });
      const html = sortedDevices
        .map(
          (device) => `
                <details class="device">
                    <summary>${device.name} <span class="device-rating">${
            device.rating
          }</span></summary>
                    <ul>${device.features
                      .map((feature) => `<li>${feature}</li>`)
                      .join("")}</ul>
                </details>
            `
        )
        .join("");

      document.querySelector("#devices-list").innerHTML = html;
    })
    .catch(() => {
      document.querySelector("#devices-list").innerHTML =
        "Failed to load devices";
    });
}

loadDevices();
