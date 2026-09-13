# Lists Doze events from the past day on the connected device.
# DOZE, SCREEN, and PLUG rows come from battery stats history, which usually covers the whole day.
# DEEP and LIGHT rows are the detailed Doze state machine steps from logcat, which may only reach back a few hours.

import argparse
import re
from datetime import datetime, timedelta

from android_logs import adb, device_now, format_time, parse_month_day_time

DEEP_STATES = ["ACTIVE", "INACTIVE", "IDLE_PENDING", "SENSING", "LOCATING", "IDLE", "IDLE_MAINTENANCE", "QUICK_DOZE_DELAY"]
LIGHT_STATES = {0: "ACTIVE", 1: "INACTIVE", 4: "IDLE", 5: "WAITING_FOR_NETWORK", 6: "IDLE_MAINTENANCE", 7: "OVERRIDE"}


def parse_relative_offset(value):
    parts = {unit: int(amount) for amount, unit in re.findall(r"(\d+)(ms|d|h|m|s)", value)}
    return timedelta(
        days=parts.get("d", 0),
        hours=parts.get("h", 0),
        minutes=parts.get("m", 0),
        seconds=parts.get("s", 0),
        milliseconds=parts.get("ms", 0),
    )


def battery_stats_events(history, now):
    events = []
    # Older Android versions print history times as offsets from a TIME: anchor instead of dates
    relative_anchor = None
    last = {}

    for line in history.splitlines():
        absolute = re.match(r"^\s*(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) (.*)$", line)
        relative = re.match(r"^\s*(0|\+[0-9dhms]+)\s+\(\d+\)\s+(.*)$", line)
        if absolute:
            dt = parse_month_day_time(absolute.group(1), now)
            body = absolute.group(2)
        elif relative:
            offset = parse_relative_offset(relative.group(1))
            body = relative.group(2)
            anchor = re.search(r"TIME:\s*(\d{4}-\d{2}-\d{2}-\d{2}-\d{2}-\d{2})", body)
            if anchor:
                relative_anchor = datetime.strptime(anchor.group(1), "%Y-%m-%d-%H-%M-%S") - offset
            if relative_anchor is None:
                continue
            dt = relative_anchor + offset
        else:
            continue

        values = {}
        if idle := re.search(r"\bdevice_idle=(\w+)", body):
            values["DOZE"] = idle.group(1)
        if screen := re.search(r"(?:^|\s)([+-])screen(?:\s|$)", body):
            values["SCREEN"] = "on" if screen.group(1) == "+" else "off"
        if plug := re.search(r"\bplug=(\w+)", body):
            values["PLUG"] = plug.group(1)

        for kind, value in values.items():
            if last.get(kind) != value:
                last[kind] = value
                events.append((dt, kind, value))

    return events


def logcat_events(log, now):
    events = []
    last_state = {}

    for line in log.splitlines():
        match = re.match(r"^(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}).*\bdevice_idle(_light)?: \[(\d+),(.*)\]\s*$", line)
        if not match:
            continue
        mode = "LIGHT" if match.group(2) else "DEEP"
        number = int(match.group(3))
        if mode == "DEEP":
            state = DEEP_STATES[number] if number < len(DEEP_STATES) else str(number)
        else:
            state = LIGHT_STATES.get(number, str(number))
        previous = last_state.get(mode)
        last_state[mode] = state
        transition = state if previous is None else f"{previous} -> {state}"
        events.append((parse_month_day_time(match.group(1), now), mode, f"{transition} ({match.group(4)})"))

    return events


def main():
    parser = argparse.ArgumentParser(description="List Doze events from the connected device.")
    parser.add_argument("--hours", type=int, default=24, help="How far back to look (default: 24)")
    args = parser.parse_args()

    now = device_now()
    since = now - timedelta(hours=args.hours)

    events = battery_stats_events(adb("shell", "dumpsys", "batterystats", "--history"), now)
    detailed_events = logcat_events(adb("logcat", "-b", "events", "-d", "-v", "threadtime", "-T", since.strftime("%m-%d %H:%M:%S.000")), now)
    events += detailed_events
    events.sort(key=lambda e: e[0])

    state_at_start = {kind: detail for dt, kind, detail in events if dt < since and kind in ("DOZE", "SCREEN", "PLUG")}
    events = [e for e in events if e[0] >= since]

    if not events and not state_at_start:
        print("No Doze events found in the retained logs.")
        return

    header = []
    if not detailed_events:
        header.append("Note: no detailed DEEP/LIGHT steps are in logcat, so only DOZE, SCREEN, and PLUG rows are shown")
    elif detailed_events[0][0] > since:
        header.append(f"Note: detailed DEEP/LIGHT steps are only available since {format_time(detailed_events[0][0])}")
    if state_at_start:
        summary = ", ".join(f"{kind.lower()}={detail}" for kind, detail in state_at_start.items())
        header.append(f"State at {since.strftime('%m-%d %H:%M:%S')}: {summary}")
    if header:
        print("\n".join(header) + "\n")

    for dt, kind, detail in events:
        print(f"{format_time(dt)}  {kind:<6}  {detail}")


if __name__ == "__main__":
    main()
