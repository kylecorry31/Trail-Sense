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
    const title = guide.split('_').slice(1).map(w => w[0].toUpperCase() + w.slice(1)).join(' ');

    // Load the markdown
    return fetch(`${baseUrl}/guide_${guide}.md`)
        .then(response => response.text())
        .then(text => {
            let html = toHTML(`# ${title}\n${text}`);
            // Shift all the headers down by one h1 -> h2, h2 -> h3, etc.
            html = html.replace(/<h(\d).*>(.*)<\/h(\d)>/gm, (match, p1, p2, p3) => `<h${parseInt(p1) + 1}>${p2}</h${parseInt(p1) + 1}>`);
            document.querySelector('#guide-text').innerHTML = html;
        });
}

// Try to load the guide from the URL
const params = new URLSearchParams(window.location.search);
const guide = params.get('id');
if (guide) {
    loadGuide(guide);
}