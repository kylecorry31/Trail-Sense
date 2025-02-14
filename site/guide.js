const baseUrl = "https://raw.githubusercontent.com/kylecorry31/Trail-Sense/main/app/src/main/res/raw"

/**
 * Converts markdown to HTML.
 * @param {string} markdown 
 * @returns {string} HTML
 */
function toHTML(markdown) {
    const converter = new showdown.Converter();
    return converter.makeHtml(markdown);
}

function loadGuide(guide) {
    // Update the URL
    const params = new URLSearchParams(window.location.search);
    params.set('id', guide);
    window.history.replaceState({}, '', `${location.pathname}?${params}`);

    // Get the title from the ID
    const sliceAmount = guide.startsWith('survival') ? 2 : 1;
    const title = guide.split('_').slice(sliceAmount).map(w => w[0].toUpperCase() + w.slice(1)).join(' ');

    // Load the markdown
    return fetch(`${baseUrl}/guide_${guide}.md`)
        .then(response => response.text())
        .then(text => {
            let html = toHTML(`# ${title}${guide === "tool_survival_guide" ? "\n[View survival guide online](survival_guide)\n" : ""}\n${text}`);
            // Shift all the headers down by one h1 -> h2, h2 -> h3, etc.
            html = html.replace(/<h(\d).*>(.*)<\/h(\d)>/gm, (match, p1, p2, p3) => `<h${parseInt(p1) + 1}>${p2}</h${parseInt(p1) + 1}>`);
            // Resolve all images
            html = html.replaceAll('file:///android_asset/', 'https://raw.githubusercontent.com/kylecorry31/Trail-Sense/main/app/src/main/assets/');

            document.querySelector('#guide-text').innerHTML = html;
        });
}

// Try to load the guide from the URL
const params = new URLSearchParams(window.location.search);
const guide = params.get('id');
if (guide) {
    loadGuide(guide);
}