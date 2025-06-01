import pystache

renderer = pystache.Renderer()

def md_to_html(site_data, item_data, template):
    data = {}
    for key in site_data:
        data[key] = site_data[key]
    data['item'] = item_data
    return renderer.render(template, data)

def populate_html(site_data, template):
    return renderer.render(template, site_data)
