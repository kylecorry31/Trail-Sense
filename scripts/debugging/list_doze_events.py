# Lists Doze events on the connected device.
# DOZE, SCREEN, and PLUG rows come from battery stats history, which usually covers the whole day.
# DEEP and LIGHT rows are the detailed Doze state machine steps from logcat, which may only reach back a few hours.

import argparse
import re

from android_logs import add_window_arguments, battery_history, device_now, format_time, get_window, logcat_lines

DEEP_STATES = ["ACTIVE", "INACTIVE", "IDLE_PENDING", "SENSING", "LOCATING", "IDLE", "IDLE_MAINTENANCE", "QUICK_DOZE_DELAY"]
LIGHT_STATES = {0: "ACTIVE", 1: "INACTIVE", 4: "IDLE", 5: "WAITING_FOR_NETWORK", 6: "IDLE_MAINTENANCE", 7: "OVERRIDE"}


def battery_stats_events(now):
    events = []
    last = {}
    for dt, _, body in battery_history(now):
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


def logcat_events(now, since):
    """Returns the detailed Doze steps and the time of the oldest line in the logcat events buffer."""
    events = []
    last_state = {}
    earliest = None
    for dt, line in logcat_lines(now, since, "events"):
        earliest = earliest or dt
        match = re.search(r"\bdevice_idle(_light)?: \[(\d+),(.*)\]\s*$", line)
        if not match:
            continue
        mode = "LIGHT" if match.group(1) else "DEEP"
        number = int(match.group(2))
        if mode == "DEEP":
            state = DEEP_STATES[number] if number < len(DEEP_STATES) else str(number)
        else:
            state = LIGHT_STATES.get(number, str(number))
        previous = last_state.get(mode)
        last_state[mode] = state
        transition = state if previous is None else f"{previous} -> {state}"
        events.append((dt, mode, f"{transition} ({match.group(3)})"))
    return events, earliest


def main():
    parser = argparse.ArgumentParser(description="List Doze events from the connected device.")
    add_window_arguments(parser)
    args = parser.parse_args()

    now = device_now()
    since, until = get_window(args, now)

    detailed_events, logcat_start = logcat_events(now, since)
    events = sorted(battery_stats_events(now) + detailed_events, key=lambda e: e[0])

    state_at_start = {kind: detail for dt, kind, detail in events if dt < since and kind in ("DOZE", "SCREEN", "PLUG")}
    events = [e for e in events if since <= e[0] <= until]
    detailed_events = [e for e in detailed_events if since <= e[0] <= until]

    if not events and not state_at_start:
        print("No Doze events found in the retained logs.")
        return

    header = []
    if logcat_start is None or logcat_start > until:
        header.append("Note: logcat has no events for this time window, so only DOZE, SCREEN, and PLUG rows are shown")
    elif logcat_start > since:
        header.append(f"Note: detailed DEEP/LIGHT steps are only available since {format_time(logcat_start)}")
    if "DOZE" not in {kind for _, kind, _ in events} | set(state_at_start):
        header.append("Note: battery stats has no Doze entries for this time window")
    if state_at_start:
        summary = ", ".join(f"{kind.lower()}={detail}" for kind, detail in state_at_start.items())
        header.append(f"State at {since.strftime('%m-%d %H:%M:%S')}: {summary}")
    if header:
        print("\n".join(header) + "\n")

    for dt, kind, detail in events:
        print(f"{format_time(dt)}  {kind:<6}  {detail}")


if __name__ == "__main__":
    main()
