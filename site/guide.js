const baseUrl = "https://raw.githubusercontent.com/kylecorry31/Trail-Sense/main/app/src/main/res/raw"

/**
 * Converts markdown to HTML. Supports the following markdown:
 * - Headers
 * - Bold
 * - Italic
 * - Links
 * - Unordered lists
 * - Ordered lists
 * @param {string} markdown 
 * @returns {string} HTML
 */
function toHTML(markdown) {
    let html = markdown;
    // Headers
    html = html.replace(/^# (.*)$/gm, "<h1>$1</h1>");
    html = html.replace(/^## (.*)$/gm, "<h2>$1</h2>");
    html = html.replace(/^### (.*)$/gm, "<h3>$1</h3>");
    html = html.replace(/^#### (.*)$/gm, "<h4>$1</h4>");
    html = html.replace(/^##### (.*)$/gm, "<h5>$1</h5>");
    html = html.replace(/^###### (.*)$/gm, "<h6>$1</h6>");
    // Bold
    html = html.replace(/\*\*(.*)\*\*/gm, "<strong>$1</strong>");
    // Italic
    html = html.replace(/\*(.*)\*/gm, "<em>$1</em>");
    // Links
    html = html.replace(/\[(.*)\]\((.*)\)/gm, "<a href='$2'>$1</a>");

    // Unordered lists
    lines = html.split("\n");
    let isInUnorderedList = false;
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        const isListItem = line.startsWith("* ") || line.startsWith("- ");
        if (isListItem) {
            line = line.replace(/^(\*|-) (.*)$/gm, "<li>$2</li>");
            if (!isInUnorderedList) {
                line = "<ul>" + line;
                isInUnorderedList = true;
            }
        } else if (isInUnorderedList) {
            line = "</ul>" + line;
            isInUnorderedList = false;
        }
        lines[i] = line;
    }

    // Ordered lists
    let isInOrderedList = false;
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        const isListItem = line.match(/^\d+\. /gm);
        if (isListItem) {
            line = line.replace(/^(\d+)\. (.*)$/gm, "<li>$2</li>");
            if (!isInOrderedList) {
                line = "<ol>" + line;
                isInOrderedList = true;
            }
        } else if (isInOrderedList) {
            line = "</ol>" + line;
            isInOrderedList = false;
        }
        lines[i] = line;
    }

    html = lines.join("\n");

    // Remove script tags
    html = html.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, "");

    // If there are multiple newlines, replace them with a br except when before a header
    html = html.replace(/([^>])\n\n([^<])/gm, "$1<br><br>$2");

    return html;
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