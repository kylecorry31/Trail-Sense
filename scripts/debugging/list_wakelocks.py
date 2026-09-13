# Lists wakelocks held by an app on the connected device, with start and end times.
# Newer Android versions (seen on 14+) have a power manager wake lock log, a small buffer that often only covers the last few minutes to hours.
# Some older Samsung devices (seen on Android 10) instead log each released wakelock and how long it was held to the logcat events buffer.

import argparse
import re
import sys
from collections import defaultdict, deque
from datetime import timedelta

from android_logs import (
    DEFAULT_PACKAGE,
    adb,
    add_window_arguments,
    device_now,
    format_duration,
    format_time,
    get_uid,
    get_window,
    logcat_lines,
    parse_month_day_time,
)

# Examples:
#   09-12 19:21:49.052 - 10340 - ACQ com.example.Service (partial)
#   09-12 15:37:01.969 - 10276 (com.example) - ACQ *alarm* (partial)
WAKE_LOCK_LOG_PATTERN = re.compile(r"^\s*(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) - (\d+)(?: \([^)]*\))? - (ACQ|REL) (.*?)\s*$")
# Only acquire events end with the wakelock flags, such as "(partial)"
FLAGS_PATTERN = re.compile(r"^(.*) \(([^)]*)\)$")
# WorkManager job tags end with a long, repetitive scheduler and service name
WORK_MANAGER_JOB_SUFFIX = re.compile(r"#?@androidx\.work\.systemjobscheduler@.*$")


def short_tag(tag, package):
    tag = WORK_MANAGER_JOB_SUFFIX.sub("", tag)
    if tag.startswith(f"{package}."):
        tag = tag.rsplit(".", 1)[1]
    return tag


def read_wake_lock_log(uid, now):
    """Pairs acquire and release events from the power manager wake lock log. Returns (wakelocks, earliest entry)."""
    entries = set()
    earliest = None
    for line in adb("shell", "dumpsys", "power").splitlines():
        match = WAKE_LOCK_LOG_PATTERN.match(line)
        if not match:
            continue
        dt = parse_month_day_time(match.group(1), now)
        earliest = dt if earliest is None else min(earliest, dt)
        if match.group(2) != uid:
            continue
        action, tag, flags = match.group(3), match.group(4), ""
        if action == "ACQ" and (with_flags := FLAGS_PATTERN.match(tag)):
            tag, flags = with_flags.groups()
        # A set because some Android versions print the same event in more than one log section
        entries.add((dt, action, tag, flags))

    wakelocks = []
    held = defaultdict(deque)
    for dt, action, tag, flags in sorted(entries):
        if action == "ACQ":
            wakelock = {"tag": tag, "flags": flags, "start": dt, "end": None}
            wakelocks.append(wakelock)
            held[tag].append(wakelock)
        elif held[tag]:
            held[tag].popleft()["end"] = dt
        else:
            wakelocks.append({"tag": tag, "flags": "", "start": None, "end": dt})
    return wakelocks, earliest


def read_samsung_event_log(package, now, since):
    """Reads released partial wakelocks from Samsung's power_partial_wake_state events. Returns (wakelocks, earliest entry)."""
    # Example: power_partial_wake_state: [5142,com.example.Service:com.example]
    pattern = re.compile(rf"power_partial_wake_state: \[(\d+),(.*):{re.escape(package)}\]\s*$")
    wakelocks = []
    earliest = None
    for dt, line in logcat_lines(now, since, "events"):
        earliest = earliest or dt
        if match := pattern.search(line):
            held = timedelta(milliseconds=int(match.group(1)))
            wakelocks.append({"tag": match.group(2), "flags": "partial", "start": dt - held, "end": dt})
    return wakelocks, earliest


def main():
    parser = argparse.ArgumentParser(description="List wakelocks for an app on the connected device.")
    parser.add_argument("package", nargs="?", default=DEFAULT_PACKAGE)
    add_window_arguments(parser)
    args = parser.parse_args()

    uid = get_uid(args.package)
    if uid is None:
        sys.exit(f"{args.package} is not installed on the connected device.")

    now = device_now()
    since, until = get_window(args, now)

    wakelocks, log_start = read_wake_lock_log(uid, now)
    samsung_wakelocks, samsung_log_start = read_samsung_event_log(args.package, now, since)

    notes = []
    if log_start:
        # The log drops old events as it fills, so a few old entries don't mean the period after them is complete
        notes.append(f"the wake lock log's oldest entry is {format_time(log_start)}, but it is a small buffer and may be missing events")
    if samsung_wakelocks:
        wakelocks += samsung_wakelocks
        notes.append("some wakelocks come from Samsung's release log, so their start is calculated from how long they were held")
        if samsung_log_start > since:
            notes.append(f"the logcat events buffer only goes back to {format_time(samsung_log_start)}")
    if not log_start and not samsung_wakelocks:
        notes.append("this device has no wake lock log that this script can read")

    wakelocks = [w for w in wakelocks if (w["end"] or now) >= since and (w["start"] or w["end"]) <= until]

    for note in notes:
        print(f"Note: {note}")
    print()

    print(f"Wakelocks for {args.package} (uid {uid})")
    if not wakelocks:
        print("None found")
        return

    print(f"{'START':<18}  {'END':<18}  {'DURATION':>10}  {'TYPE':<30}  TAG")
    for w in sorted(wakelocks, key=lambda w: w["start"] or w["end"]):
        duration = format_duration(w["end"] - w["start"]) if w["start"] and w["end"] else "?"
        start = format_time(w["start"]) if w["start"] else "not logged"
        end = format_time(w["end"]) if w["end"] else "not logged"
        print(f"{start:<18}  {end:<18}  {duration:>10}  {w['flags']:<30}  {short_tag(w['tag'], args.package)}")


if __name__ == "__main__":
    main()
