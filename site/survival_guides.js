const guides = [
  "survival_chapter_overview",
  "survival_chapter_medical",
  "survival_chapter_water",
  "survival_chapter_food",
  "survival_chapter_fire",
  "survival_chapter_shelter_and_clothing",
  "survival_chapter_navigation",
  "survival_chapter_weather",
  "survival_chapter_crafting"
];

// Populate the guide list
const guideList = document.querySelector("#guide-list");
for (const guide of guides) {
  const guideItem = document.createElement("a");
  guideItem.innerText = guide
    .split("_")
    .slice(2)
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(" ");
  guideItem.classList.add("guide-list-item");
  guideItem.href = `guide?id=${guide}`;
  guideList.appendChild(guideItem);
}
