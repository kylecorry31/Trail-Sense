# Lists wakelocks held by an app from the past day on the connected device, with start and end times.
# Data comes from the power manager wake lock log, a small fixed size buffer that often only covers the last few minutes to hours.

import argparse
import re
import sys
from collections import defaultdict, deque
from datetime import timedelta

from android_logs import adb, device_now, format_duration, format_time, parse_month_day_time

# Examples:
#   09-12 19:21:49.052 - 10340 - ACQ com.example.Service (partial)
#   09-12 15:37:01.969 - 10276 (com.example) - ACQ *alarm* (partial)
LOG_PATTERN = re.compile(r"^\s*(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) - (\d+)(?: \([^)]*\))? - (ACQ|REL) (.*?)\s*$")
# Only acquire events end with the wakelock flags, such as "(partial)"
FLAGS_PATTERN = re.compile(r"^(.*) \(([^)]*)\)$")
# WorkManager job tags end with a long, repetitive scheduler and service name
WORK_MANAGER_JOB_SUFFIX = re.compile(r"#?@androidx\.work\.systemjobscheduler@.*$")


def short_tag(tag, package):
    tag = WORK_MANAGER_JOB_SUFFIX.sub("", tag)
    if tag.startswith(f"{package}."):
        tag = tag.rsplit(".", 1)[1]
    return tag


def get_uid(package):
    for line in adb("shell", "pm", "list", "packages", "-U", package).splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[0] == f"package:{package}":
            return parts[1].removeprefix("uid:")
    return None


def main():
    parser = argparse.ArgumentParser(description="List wakelocks for an app on the connected device.")
    parser.add_argument("package", nargs="?", default="com.kylecorry.trail_sense")
    parser.add_argument("--hours", type=int, default=24, help="How far back to look (default: 24)")
    args = parser.parse_args()

    uid = get_uid(args.package)
    if uid is None:
        sys.exit(f"{args.package} is not installed on the connected device.")

    now = device_now()
    since = now - timedelta(hours=args.hours)

    entries = set()
    earliest = None
    for line in adb("shell", "dumpsys", "power").splitlines():
        match = LOG_PATTERN.match(line)
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

    wakelocks = [w for w in wakelocks if (w["end"] or now) >= since]

    if earliest:
        # The log drops old events as it fills, so a few old entries don't mean the period after them is complete
        print(f"Note: the wake lock log's oldest entry is {format_time(earliest)}, but it is a small buffer and may be missing events")
        print()

    print(f"Wakelocks for {args.package} (uid {uid})")
    if not wakelocks:
        print("None found")
        return

    print(f"{'START':<18}  {'END':<18}  {'DURATION':>10}  {'TYPE':<30}  TAG")
    for w in sorted(wakelocks, key=lambda w: w["start"] or w["end"]):
        if w["start"] and w["end"]:
            duration = format_duration(w["end"] - w["start"])
        elif w["start"]:
            duration = "?"
        else:
            duration = "?"
        start = format_time(w["start"]) if w["start"] else "not logged"
        end = format_time(w["end"]) if w["end"] else "not logged"
        print(f"{start:<18}  {end:<18}  {duration:>10}  {w['flags']:<30}  {short_tag(w['tag'], args.package)}")


if __name__ == "__main__":
    main()
