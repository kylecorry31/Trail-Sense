# Summarizes the backtrack points an app recorded on the connected device, the gaps between them, and battery use.
# Without a time window, it summarizes the last backtrack session and prints the window for the other debugging scripts.
# Reading the app's database and settings requires a debuggable build (run-as).

import argparse
import re
import sqlite3
import statistics
import sys
import tempfile
import xml.etree.ElementTree as ElementTree
from datetime import datetime, timedelta
from pathlib import Path

from android_logs import (
    DEFAULT_PACKAGE,
    adb,
    add_window_arguments,
    battery_history,
    device_now,
    device_timezone,
    format_duration,
    format_time,
    format_window_arguments,
    get_window,
    has_window_arguments,
)

DATABASE_FILES = ["trail_sense", "trail_sense-wal", "trail_sense-shm"]
DEFAULT_INTERVAL = timedelta(minutes=1)


def run_as_cat(package, path):
    return adb("exec-out", "run-as", package, "cat", path, text=False)


def read_preferences(package):
    xml = run_as_cat(package, f"shared_prefs/{package}_preferences.xml")
    try:
        root = ElementTree.fromstring(xml)
    except ElementTree.ParseError:
        sys.exit(f"Could not read the settings for {package}. Is it a debuggable build?\n{xml.decode(errors='replace')[:200]}")
    return {element.get("name"): element.get("value", element.text) for element in root}


def copy_database(package, directory):
    for name in DATABASE_FILES:
        data = run_as_cat(package, f"databases/{name}")
        if name == "trail_sense" and not data.startswith(b"SQLite format 3"):
            sys.exit(f"Could not read the database for {package}. Is it a debuggable build?\n{data.decode(errors='replace')[:200]}")
        if data.startswith(b"run-as:") or b"No such file" in data[:200]:
            continue
        (directory / name).write_bytes(data)
    return sqlite3.connect(directory / "trail_sense")


def to_datetime(millis, tz):
    """Converts epoch millis to a naive datetime in the device's timezone, matching the log times."""
    return datetime.fromtimestamp(millis / 1000, tz).replace(tzinfo=None)


def to_millis(dt, tz):
    return dt.replace(tzinfo=tz).timestamp() * 1000


def get_last_session(connection, preferences, interval, now, tz):
    """Returns (path id, since, until) for the most recent backtrack path, or None when there are no backtrack points."""
    # Turning backtrack on starts a new path, so the most recent backtrack path is the last session
    path_id = preferences.get("last_backtrack_path_id")
    if path_id is None:
        row = connection.execute(
            "SELECT pathId FROM waypoints JOIN paths ON paths._id = waypoints.pathId "
            "WHERE paths.temporary = 1 ORDER BY createdOn DESC LIMIT 1"
        ).fetchone()
        if row is None:
            return None
        path_id = row[0]

    first, last = connection.execute(
        "SELECT MIN(createdOn), MAX(createdOn) FROM waypoints WHERE pathId = ?", (int(path_id),)
    ).fetchone()
    if first is None:
        return None
    # The service starts about one interval before its first point
    since = to_datetime(first, tz) - interval
    is_recording = preferences.get("pref_backtrack_enabled") == "true"
    until = now if is_recording else to_datetime(last, tz)
    return int(path_id), since.replace(microsecond=0), until.replace(microsecond=0) + timedelta(seconds=1)


def print_points(connection, path_id, interval, since, until, tz):
    rows = connection.execute(
        "SELECT createdOn FROM waypoints WHERE pathId = ? AND createdOn BETWEEN ? AND ? ORDER BY createdOn",
        (path_id, to_millis(since, tz), to_millis(until, tz)),
    ).fetchall()
    times = [to_datetime(row[0], tz) for row in rows]

    print(f"Points: {len(times)}")
    if not times:
        return
    span = times[-1] - times[0]
    expected = int(span / interval) + 1
    print(f"First: {format_time(times[0])}")
    print(f"Last:  {format_time(times[-1])}")
    print(f"Span:  {format_duration(span)}, which would be about {expected} points at the {format_duration(interval)} interval")

    gaps = [b - a for a, b in zip(times, times[1:])]
    if not gaps:
        return
    seconds = sorted(gap.total_seconds() for gap in gaps)
    percentile_90 = seconds[min(len(seconds) - 1, int(len(seconds) * 0.9))]
    print(
        f"Time between points: min {format_duration(timedelta(seconds=seconds[0]))}, "
        f"median {format_duration(timedelta(seconds=statistics.median(seconds)))}, "
        f"90th percentile {format_duration(timedelta(seconds=percentile_90))}"
    )

    long_gaps = [(a, b) for a, b in zip(times, times[1:]) if b - a > interval * 2]
    print()
    print(f"Gaps longer than {format_duration(interval * 2)}")
    if not long_gaps:
        print("None")
        return
    print(f"{'FROM':<18}  {'TO':<18}  {'LENGTH':>9}  {'MISSED POINTS':>13}")
    for a, b in long_gaps:
        missed = max(round((b - a) / interval) - 1, 0)
        print(f"{format_time(a):<18}  {format_time(b):<18}  {format_duration(b - a):>9}  {missed:>13}")


def print_system_battery(now, since, until):
    readings = []
    for dt, level, body in battery_history(now):
        charge = re.search(r"\bcharge=(\d+)", body)
        if level is not None or charge:
            readings.append((dt, level, int(charge.group(1)) if charge else None))

    # History from before a battery stats reset can appear out of order
    readings.sort(key=lambda reading: reading[0])

    def reading_at(time):
        """The latest known level and charge at the time, each with when it was reported."""
        level = charge = None
        for dt, reading_level, reading_charge in readings:
            if dt > time:
                break
            if reading_level is not None:
                level = (dt, reading_level)
            if reading_charge is not None:
                charge = (dt, reading_charge)
        return level, charge

    print("System battery (battery stats)")
    start_level, start_charge = reading_at(since)
    end_level, end_charge = reading_at(until)
    if not start_level or not end_level:
        print("Unknown, battery stats doesn't cover the time window")
        return

    for label, time, level, charge in (("Start", since, start_level, start_charge), ("End", until, end_level, end_charge)):
        level_text = f"{level[1]}%" + (f" (reported {format_time(level[0])})" if time - level[0] > timedelta(minutes=5) else "")
        charge_text = f", charge {charge[1]} mAh (reported {format_time(charge[0])})" if charge else ""
        print(f"{label + ':':<6} {level_text}{charge_text}")

    hours = (until - since).total_seconds() / 3600
    change = f"Change: {end_level[1] - start_level[1]:+d}%"
    if start_charge and end_charge:
        used = start_charge[1] - end_charge[1]
        change += f", {-used:+d} mAh ({used / hours:.1f} mA average)"
    print(f"{change} over {format_duration(until - since)}")
    if start_charge and end_charge:
        print("Charge only updates occasionally, so mAh values can lag the level by several minutes")


def print_app_battery_log(connection, since, until, tz):
    rows = connection.execute(
        "SELECT time, percent, capacity, isCharging FROM battery WHERE time BETWEEN ? AND ? ORDER BY time",
        (to_millis(since, tz), to_millis(until, tz)),
    ).fetchall()
    print("App battery log")
    if not rows:
        print("None")
        return
    print(f"{'TIME':<18}  {'PERCENT':>7}  {'CAPACITY':>9}  CHARGING")
    for time, percent, capacity, is_charging in rows:
        print(f"{format_time(to_datetime(time, tz)):<18}  {percent:>6.0f}%  {capacity:>5.0f} mAh  {'yes' if is_charging else 'no'}")


def main():
    parser = argparse.ArgumentParser(description="Summarize backtrack points recorded on the connected device.")
    parser.add_argument("package", nargs="?", default=DEFAULT_PACKAGE)
    add_window_arguments(parser)
    args = parser.parse_args()

    now = device_now()
    tz = device_timezone()

    preferences = read_preferences(args.package)
    # The frequency preference is stored in milliseconds despite its name
    interval_millis = preferences.get("pref_backtrack_frequency_seconds")
    interval = timedelta(milliseconds=int(interval_millis)) if interval_millis else DEFAULT_INTERVAL

    with tempfile.TemporaryDirectory() as directory:
        connection = copy_database(args.package, Path(directory))
        try:
            session = get_last_session(connection, preferences, interval, now, tz)
            if session is None:
                sys.exit(f"{args.package} has no backtrack points.")
            path_id = session[0]
            if has_window_arguments(args):
                since, until = get_window(args, now)
                print(f"Backtrack path {path_id} for {args.package}, {format_time(since)} to {format_time(until)}")
            else:
                since, until = session[1], session[2]
                print(f"Last backtrack session (path {path_id}) for {args.package}, {format_time(since)} to {format_time(until)}")
                print(f"Window for the other debugging scripts: {format_window_arguments(since, until)}")
            minimum_distance = preferences.get("pref_backtrack_min_distance")
            print(
                f"Settings: interval {format_duration(interval)}, "
                f"minimum distance {minimum_distance + ' m' if minimum_distance else '10 m (default)'}, "
                f"keep device awake {'on' if preferences.get('pref_backtrack_keep_awake') == 'true' else 'off'}, "
                f"backtrack currently {'on' if preferences.get('pref_backtrack_enabled') == 'true' else 'off'}"
            )
            print()
            print_points(connection, path_id, interval, since, until, tz)
            print()
            print_system_battery(now, since, until)
            print()
            print_app_battery_log(connection, since, until, tz)
        finally:
            connection.close()


if __name__ == "__main__":
    main()
