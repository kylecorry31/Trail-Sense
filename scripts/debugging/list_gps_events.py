# Lists location listener registrations for an app from the past day on the connected device.
# Each registration shows when it started, when its first location was delivered, and when it was removed.
# Data comes from the location service event log, which holds a fixed number of events and may not cover the whole day.

import argparse
import re
from datetime import timedelta

from android_logs import adb, device_now, format_duration, format_time, parse_month_day_time

EVENT_PATTERN = re.compile(r"^\s*(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}): (.*)$")
PROVIDER_EVENT_PATTERN = re.compile(
    r"^(\w+ provider stationary/idle (?:un)?throttled"
    r"|location power save mode changed to \w+"
    r"|\w+ provider \[u\d+\] (?:enabled|disabled))"
)


def short_request(request):
    # Request[@+1m0s0ms HIGH_ACCURACY, WorkSource{...}] -> every 1m, HIGH_ACCURACY
    request = re.sub(r"^Request\[(.*)\]$", r"\1", request)
    request = re.sub(r",?\s*WorkSource\{[^}]*\}", "", request)

    def compact_interval(match):
        units = re.findall(r"(\d+)(ms|d|h|m|s)", match.group(1))
        return "every " + ("".join(f"{amount}{unit}" for amount, unit in units if amount != "0") or "0ms")

    return re.sub(r"@\+(\S+)", compact_interval, request)


def new_registration(provider, client, request, start):
    return {
        "provider": provider,
        "id": client.rsplit("/", 1)[1],
        "request": request,
        "start": start,
        "first_location": None,
        "locations": 0,
        "end": None,
    }


def main():
    parser = argparse.ArgumentParser(description="List location registrations for an app on the connected device.")
    parser.add_argument("package", nargs="?", default="com.kylecorry.trail_sense")
    parser.add_argument("--hours", type=int, default=24, help="How far back to look (default: 24)")
    args = parser.parse_args()

    now = device_now()
    since = now - timedelta(hours=args.hours)

    client = rf"\d+/{re.escape(args.package)}(?:\[[^\]]*\])?/[0-9A-Fa-f]+"
    add_pattern = re.compile(rf"^(\w+) provider \+registration ({client}) -> (.*)$")
    remove_pattern = re.compile(rf"^(\w+) provider -registration ({client})$")
    deliver_pattern = re.compile(rf"^(\w+) provider delivered location\[(\d+)\] to ({client})$")

    registrations = []
    open_registrations = {}
    provider_events = []
    earliest = None
    earliest_delivery = None

    for line in adb("shell", "dumpsys", "location").splitlines():
        match = EVENT_PATTERN.match(line)
        if not match:
            continue
        dt = parse_month_day_time(match.group(1), now)
        text = match.group(2)
        earliest = earliest or dt

        if added := add_pattern.match(text):
            registration = new_registration(added.group(1), added.group(2), added.group(3), dt)
            registrations.append(registration)
            open_registrations[(added.group(1), added.group(2))] = registration
        elif removed := remove_pattern.match(text):
            registration = open_registrations.pop((removed.group(1), removed.group(2)), None)
            if registration is None:
                registration = new_registration(removed.group(1), removed.group(2), "(registered before the log starts)", None)
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

    registrations = [r for r in registrations if (r["end"] or now) >= since]
    provider_events = [e for e in provider_events if e[0] >= since]

    notes = []
    if earliest and earliest > since:
        notes.append(f"the location event log only goes back to {format_time(earliest)}")
    if earliest_delivery is None:
        notes.append("no location deliveries are logged, so FIRST LOCATION and LOCATIONS show ?")
    elif earliest_delivery > max(earliest, since):
        notes.append(
            f"location deliveries are only logged since {format_time(earliest_delivery)}, "
            "so older registrations show ? or a minimum count like 25+"
        )
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
            # Deliveries before the first logged one are missing, so the counts for older registrations are lower bounds
            is_partial = earliest_delivery is None or (r["start"] or earliest) < earliest_delivery
            first_location = "?" if is_partial else format_time(r["first_location"]) if r["first_location"] else "none"
            if is_partial:
                locations = f"{r['locations']}+" if r["locations"] else "?"
            else:
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


if __name__ == "__main__":
    main()
