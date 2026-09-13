# Lists location listener registrations for an app on the connected device.
# Each registration shows when it started, when its first location arrived, and when it was removed.
# Newer Android versions (seen on 14+) have a location service event log, which holds a fixed number of events.
# Older versions (seen on 10) don't, so registrations come from logcat, which may only reach back a few hours.

import argparse
import re
from datetime import timedelta

from android_logs import (
    DEFAULT_PACKAGE,
    adb,
    add_window_arguments,
    battery_history,
    device_now,
    format_duration,
    format_time,
    get_window,
    logcat_lines,
    parse_month_day_time,
)

EVENT_PATTERN = re.compile(r"^\s*(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}): (.*)$")
PROVIDER_EVENT_PATTERN = re.compile(
    r"^(\w+ provider stationary/idle (?:un)?throttled"
    r"|location power save mode changed to \w+"
    r"|\w+ provider \[u\d+\] (?:enabled|disabled))"
)


def short_request(request):
    # Request[@+1m0s0ms HIGH_ACCURACY, WorkSource{...}] -> every 1m HIGH_ACCURACY
    # Request[ACCURACY_FINE gps requested=+20ms fastest=+20ms] -> every 20ms ACCURACY_FINE
    request = re.sub(r"^Request\[(.*)\]$", r"\1", request)
    request = re.sub(r",?\s*WorkSource\{[^}]*\}", "", request)

    def compact_interval(value):
        units = re.findall(r"(\d+)(ms|d|h|m|s)", value)
        return "every " + ("".join(f"{amount}{unit}" for amount, unit in units if amount != "0") or "0ms")

    if requested := re.search(r"requested=\+?(\S+)", request):
        quality = request.split()[0]
        return f"{compact_interval(requested.group(1))} {quality}"
    return re.sub(r"@\+(\S+)", lambda m: compact_interval(m.group(1)), request)


def new_registration(provider, registration_id, request, start):
    return {
        "provider": provider,
        "id": registration_id,
        "request": request,
        "start": start,
        "first_location": None,
        "locations": 0,
        "end": None,
    }


def read_event_log(package, now):
    """Reads the location service event log. Returns None when the device doesn't have one."""
    dump = adb("shell", "dumpsys", "location")
    if not re.search(r"^\s*Event Log:", dump, re.MULTILINE):
        return None

    client = rf"\d+/{re.escape(package)}(?:\[[^\]]*\])?/[0-9A-Fa-f]+"
    add_pattern = re.compile(rf"^(\w+) provider \+registration ({client}) -> (.*)$")
    remove_pattern = re.compile(rf"^(\w+) provider -registration ({client})$")
    deliver_pattern = re.compile(rf"^(\w+) provider delivered location\[(\d+)\] to ({client})$")

    registrations = []
    open_registrations = {}
    provider_events = []
    earliest = None
    earliest_delivery = None

    for line in dump.splitlines():
        match = EVENT_PATTERN.match(line)
        if not match:
            continue
        dt = parse_month_day_time(match.group(1), now)
        text = match.group(2)
        earliest = earliest or dt

        if added := add_pattern.match(text):
            registration = new_registration(added.group(1), added.group(2).rsplit("/", 1)[1], added.group(3), dt)
            registrations.append(registration)
            open_registrations[(added.group(1), added.group(2))] = registration
        elif removed := remove_pattern.match(text):
            registration = open_registrations.pop((removed.group(1), removed.group(2)), None)
            if registration is None:
                registration = new_registration(
                    removed.group(1), removed.group(2).rsplit("/", 1)[1], "(registered before the log starts)", None
                )
                registrations.append(registration)
            registration["end"] = dt
        elif " provider delivered location[" in text:
            earliest_delivery = earliest_delivery or dt
            delivered = deliver_pattern.match(text)
            registration = delivered and open_registrations.get((delivered.group(1), delivered.group(3)))
            if registration:
                registration["locations"] += int(delivered.group(2))
                registration["first_location"] = registration["first_location"] or dt
        elif provider_event := PROVIDER_EVENT_PATTERN.match(text):
            provider_events.append((dt, provider_event.group(1)))

    return {
        "registrations": registrations,
        "provider_events": provider_events,
        "log_name": "the location event log",
        "log_start": earliest,
        # Deliveries before the first logged one are missing
        "locations_complete_since": earliest_delivery,
        "locations_note": None if earliest_delivery else "no location deliveries are logged, so FIRST LOCATION and LOCATIONS show ?",
    }


def read_logcat(package, now, since):
    """Reads registrations from logcat for Android versions without the location event log."""
    request_pattern = re.compile(
        rf"LocationManagerService: request (\w+) (\w+) (Request\[.*\]) from {re.escape(package)}\(\d+ \w+\)\s*$"
    )
    remove_pattern = re.compile(r"LocationManagerService: remove (\w+)\s*$")
    # The GPS provider logs every fix it reports, but not which apps received it
    fix_pattern = re.compile(r"GnssLocationProvider: WakeLock acquired by sendMessage\(REPORT_LOCATION")
    power_pattern = re.compile(r"GnssLocationProvider: .*mDisableGpsForPowerManager = (true|false)")

    registrations = []
    open_registrations = {}
    provider_events = []
    earliest = None
    has_fix_logs = False
    gps_disabled = None

    for dt, line in logcat_lines(now, since, "main", "system"):
        earliest = earliest or dt
        if requested := request_pattern.search(line):
            registration = new_registration(requested.group(2), requested.group(1), requested.group(3), dt)
            registrations.append(registration)
            open_registrations[requested.group(1)] = registration
        elif removed := remove_pattern.search(line):
            registration = open_registrations.pop(removed.group(1), None)
            if registration:
                registration["end"] = dt
        elif fix_pattern.search(line):
            has_fix_logs = True
            for registration in open_registrations.values():
                if registration["provider"] == "gps":
                    registration["locations"] += 1
                    registration["first_location"] = registration["first_location"] or dt
        elif powered := power_pattern.search(line):
            disabled = powered.group(1) == "true"
            if disabled != gps_disabled:
                if gps_disabled is not None:
                    provider_events.append((dt, "gps provider disabled by power manager (Doze)" if disabled else "gps provider re-enabled by power manager"))
                gps_disabled = disabled

    if has_fix_logs:
        locations_note = (
            "this Android version doesn't log per-app deliveries, "
            "so FIRST LOCATION and LOCATIONS count GPS fixes reported while the listener was registered"
        )
    else:
        locations_note = "no GPS fix logs found, so FIRST LOCATION and LOCATIONS show ?"
    return {
        "registrations": registrations,
        "provider_events": provider_events,
        "log_name": "logcat",
        "log_start": earliest,
        "locations_complete_since": earliest if has_fix_logs else None,
        "locations_note": locations_note,
    }


def gps_on_time(now, since, until):
    """Returns how long the GPS hardware was on (for any app) between since and until, from battery stats."""

    def overlap(start, end):
        return max(min(end, until) - max(start, since), timedelta())

    total = None
    on_since = None
    for dt, _, body in battery_history(now):
        change = re.search(r"(?:^|\s)([+-])gps(?:\s|$)", body)
        if not change:
            continue
        total = total or timedelta()
        if change.group(1) == "+":
            on_since = on_since or dt
        elif on_since:
            total += overlap(on_since, dt)
            on_since = None
    if on_since:
        total += overlap(on_since, until)
    return total


def main():
    parser = argparse.ArgumentParser(description="List location registrations for an app on the connected device.")
    parser.add_argument("package", nargs="?", default=DEFAULT_PACKAGE)
    add_window_arguments(parser)
    args = parser.parse_args()

    now = device_now()
    since, until = get_window(args, now)

    result = read_event_log(args.package, now) or read_logcat(args.package, now, since)
    complete_since = result["locations_complete_since"]

    registrations = [r for r in result["registrations"] if (r["end"] or now) >= since and (r["start"] or r["end"]) <= until]
    provider_events = [e for e in result["provider_events"] if since <= e[0] <= until]

    notes = []
    if result["log_start"] and result["log_start"] > since:
        notes.append(f"{result['log_name']} only goes back to {format_time(result['log_start'])}")
    if complete_since and complete_since > max(since, result["log_start"]):
        notes.append(
            f"locations are only logged since {format_time(complete_since)}, so older registrations show ? or a minimum count like 25+"
        )
    if result["locations_note"]:
        notes.append(result["locations_note"])
    for note in notes:
        print(f"Note: {note}")
    if notes:
        print()

    print(f"Location registrations for {args.package}")
    if registrations:
        print(f"{'START':<18}  {'FIRST LOCATION':<18}  {'END':<18}  {'DURATION':>9}  {'LOCATIONS':>9}  {'PROVIDER':<8}  {'ID':<8}  REQUEST")
        for r in sorted(registrations, key=lambda r: r["start"] or r["end"]):
            if r["start"] and r["end"]:
                duration = format_duration(r["end"] - r["start"])
            elif r["start"]:
                duration = "active"
            else:
                duration = "?"
            # Locations before complete_since weren't logged, so counts for older registrations are lower bounds
            is_partial = complete_since is None or (r["start"] or since) < complete_since
            if is_partial:
                first_location = "?"
                locations = f"{r['locations']}+" if r["locations"] else "?"
            else:
                first_location = format_time(r["first_location"]) if r["first_location"] else "none"
                locations = str(r["locations"])
            print(
                f"{format_time(r['start']):<18}  {first_location:<18}  {format_time(r['end']):<18}  "
                f"{duration:>9}  {locations:>9}  {r['provider']:<8}  {r['id']:<8}  {short_request(r['request'])}"
            )
    else:
        print("None found")

    print()
    print("Provider events (all apps)")
    if provider_events:
        for dt, text in provider_events:
            print(f"{format_time(dt)}  {text}")
    else:
        print("None found")

    print()
    on_time = gps_on_time(now, since, until)
    window = until - since
    if on_time is None:
        print("GPS hardware on-time (all apps): unknown, battery stats has no GPS entries")
    else:
        percent = 100 * on_time / window
        print(f"GPS hardware on-time (all apps): {format_duration(on_time)} of {format_duration(window)} ({percent:.0f}%)")


if __name__ == "__main__":
    main()
