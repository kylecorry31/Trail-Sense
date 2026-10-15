"""
Replaces the elevation of every point in a GPX file with the USGS 3DEP elevation at that point.

Useful for creating a reference ("source of truth") elevation profile for a recorded path, since GPS
and barometer elevations are noisy. Only covers the United States.

Usage: python scripts/fill_usgs_elevations.py input.gpx output.gpx
"""
import argparse
import json
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

ENDPOINT = "https://epqs.nationalmap.gov/v1/json?x={lon}&y={lat}&wkid=4326&units=Meters&includeDate=false"
POINT_TAGS = ("trkpt", "rtept", "wpt")
RETRIES = 4


def get_elevation(lat: str, lon: str) -> float:
    url = ENDPOINT.format(lat=lat, lon=lon)
    for _ in range(RETRIES):
        try:
            with urllib.request.urlopen(url, timeout=30) as response:
                return float(json.load(response)["value"])
        except (OSError, ValueError, KeyError):
            pass
    raise SystemExit(f"Unable to get the elevation at {lat}, {lon} (is it in the United States?)")


def register_namespaces(path: str) -> None:
    # Keeps the original prefixes instead of ns0, ns1, ...
    for _, (prefix, uri) in ET.iterparse(path, events=("start-ns",)):
        ET.register_namespace(prefix, uri)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.strip().split("\n\n")[0])
    parser.add_argument("input", help="GPX file to read")
    parser.add_argument("output", help="GPX file to write")
    parser.add_argument("--workers", type=int, default=6, help="Number of parallel requests")
    args = parser.parse_args()

    register_namespaces(args.input)
    tree = ET.parse(args.input)
    root = tree.getroot()
    namespace = root.tag[: root.tag.index("}") + 1] if root.tag.startswith("{") else ""

    point_tags = {namespace + tag for tag in POINT_TAGS}
    points = [e for e in root.iter() if e.tag in point_tags]
    with ThreadPoolExecutor(args.workers) as executor:
        elevations = list(executor.map(lambda p: get_elevation(p.get("lat"), p.get("lon")), points))

    for point, elevation in zip(points, elevations):
        ele = point.find(namespace + "ele")
        if ele is None:
            ele = ET.Element(namespace + "ele")
            point.insert(0, ele)
        ele.text = str(round(elevation, 2))

    tree.write(args.output, encoding="UTF-8", xml_declaration=True)
    print(f"Wrote {len(points)} points to {args.output}")


if __name__ == "__main__":
    main()
