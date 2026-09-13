# Lists the app's own log file (cache/log.txt) from the connected device, such as GPS filter decisions and timeouts.
# The file keeps about the last 256 KB of logs. Reading it requires a debuggable build (run-as).

import argparse
import re
import sys
from collections import Counter
from datetime import datetime

from android_logs import DEFAULT_PACKAGE, adb, add_window_arguments, device_now, device_timezone, format_time, get_window

LOG_FILE = "cache/log.txt"
# Example: 2026-09-12T23:54:49.582Z AccuracyFilterGPSModule		[D] Reject: 36.4m > 32.0m
ENTRY_PATTERN = re.compile(r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z) (\S*)\t\t\[([A-Z])\] (.*)$")


def parse_entries(text, tz):
    """Returns entries as (device time, level, tag, message lines). Lines before the first timestamp are dropped."""
    entries = []
    for line in text.splitlines():
        match = ENTRY_PATTERN.match(line)
        if match:
            utc = datetime.fromisoformat(match.group(1).replace("Z", "+00:00"))
            entries.append((utc.astimezone(tz).replace(tzinfo=None), match.group(3), match.group(2), [match.group(4)]))
        elif entries and line.strip():
            # Stack traces continue on the lines after their entry
            entries[-1][3].append(line)
    return entries


def message_kind(message):
    """Groups messages that differ only in their numbers or quoted times."""
    message = re.sub(r"\d{4}-\d{2}-\d{2}T[\d:.]+Z", "<time>", message)
    return re.sub(r"-?\d+(?:\.\d+)?", "N", message)


def main():
    parser = argparse.ArgumentParser(description="List the app's log file from the connected device.")
    parser.add_argument("package", nargs="?", default=DEFAULT_PACKAGE)
    parser.add_argument("--tag", help="Only show entries with this tag")
    add_window_arguments(parser)
    args = parser.parse_args()

    now = device_now()
    tz = device_timezone()
    since, until = get_window(args, now)

    text = adb("exec-out", "run-as", args.package, "cat", LOG_FILE)
    if text.startswith("run-as:") or "No such file" in text[:200]:
        sys.exit(f"Could not read {LOG_FILE} for {args.package}. Is it a debuggable build?\n{text[:200]}")

    all_entries = parse_entries(text, tz)
    entries = [e for e in all_entries if since <= e[0] <= until and (args.tag is None or e[2] == args.tag)]

    if all_entries and all_entries[0][0] > since:
        print(f"Note: the log file only goes back to {format_time(all_entries[0][0])}")
    print("Note: the app only logs unusual GPS events, so no entries during a period means nothing unusual was logged")
    print()

    print(f"App log for {args.package}")
    if not entries:
        print("None found")
        return

    print(f"{'COUNT':>5}  {'TAG':<28}  MESSAGE")
    counts = Counter((tag, message_kind(lines[0])) for _, _, tag, lines in entries)
    for (tag, kind), count in counts.most_common():
        print(f"{count:>5}  {tag:<28}  {kind}")

    print()
    for dt, level, tag, lines in entries:
        print(f"{format_time(dt)}  {level}  {tag:<28}  {lines[0]}")
        for line in lines[1:]:
            print(f"{'':<18}     {'':<28}  {line}")


if __name__ == "__main__":
    main()
