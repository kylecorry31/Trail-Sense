import re
import subprocess
import sys
from datetime import datetime, timedelta, timezone

DEFAULT_PACKAGE = "com.kylecorry.trail_sense"
LOGCAT_TIME_PATTERN = r"(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})"


def adb(*args, text=True):
    try:
        result = subprocess.run(["adb", *args], capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        sys.exit(e.stderr.decode(errors="replace").strip() or f"adb {' '.join(args)} failed")
    if not text:
        return result.stdout
    return result.stdout.decode(errors="replace").replace("\r", "")


def device_now():
    return datetime.strptime(adb("shell", "date", "+%Y-%m-%d_%H:%M:%S").strip(), "%Y-%m-%d_%H:%M:%S")


def device_timezone():
    offset = adb("shell", "date", "+%z").strip()
    sign = -1 if offset.startswith("-") else 1
    return timezone(sign * timedelta(hours=int(offset[1:3]), minutes=int(offset[3:5])))


def get_uid(package):
    for line in adb("shell", "pm", "list", "packages", "-U", package).splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[0] == f"package:{package}":
            return parts[1].removeprefix("uid:")
    return None


def add_window_arguments(parser):
    parser.add_argument("--hours", type=int, help="How far back to look when --start is not set (default: 24)")
    parser.add_argument("--start", help='Start of the time window, as "YYYY-MM-DD HH:MM" or "HH:MM" (device time)')
    parser.add_argument("--end", help='End of the time window, as "YYYY-MM-DD HH:MM" or "HH:MM" (default: now)')


def has_window_arguments(args):
    return args.hours is not None or args.start is not None or args.end is not None


def format_window_arguments(since, until):
    return f'--start "{since:%Y-%m-%d %H:%M:%S}" --end "{until:%Y-%m-%d %H:%M:%S}"'


def get_window(args, now):
    """Returns the (since, until) datetimes selected by the arguments from add_window_arguments."""
    until = parse_window_time(args.end, now) if args.end else now
    since = parse_window_time(args.start, now) if args.start else now - timedelta(hours=args.hours or 24)
    if since >= until:
        sys.exit("The start of the time window must be before the end.")
    return since, until


def parse_window_time(value, now):
    for time_format in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"):
        try:
            return datetime.strptime(value, time_format)
        except ValueError:
            pass
    try:
        time = datetime.strptime(value, "%H:%M").time()
    except ValueError:
        sys.exit(f'Invalid time "{value}". Use "YYYY-MM-DD HH:MM" or "HH:MM".')
    # A bare time means its most recent occurrence
    dt = datetime.combine(now.date(), time)
    return dt - timedelta(days=1) if dt > now else dt


def parse_month_day_time(value, now):
    # Logs omit the year, so assume the most recent year that doesn't put the time in the future
    dt = datetime.strptime(f"{now.year}-{value}", "%Y-%m-%d %H:%M:%S.%f")
    if dt > now + timedelta(days=1):
        dt = dt.replace(year=now.year - 1)
    return dt


def logcat_lines(now, since, *buffers):
    """Yields (time, line) for logcat lines at or after since from the given buffers."""
    args = ["logcat", "-d", "-v", "threadtime", "-T", since.strftime("%m-%d %H:%M:%S.000")]
    for buffer in buffers:
        args += ["-b", buffer]
    for line in adb(*args).splitlines():
        match = re.match(LOGCAT_TIME_PATTERN, line)
        if match:
            yield parse_month_day_time(match.group(1), now), line


def parse_relative_offset(value):
    parts = {unit: int(amount) for amount, unit in re.findall(r"(\d+)(ms|d|h|m|s)", value)}
    return timedelta(
        days=parts.get("d", 0),
        hours=parts.get("h", 0),
        minutes=parts.get("m", 0),
        seconds=parts.get("s", 0),
        milliseconds=parts.get("ms", 0),
    )


def battery_history(now):
    """Yields (time, battery level or None, body) for each `dumpsys batterystats --history` entry."""
    # Older Android versions print times as offsets from a TIME: anchor, which only has second precision
    relative_anchor = None
    for line in adb("shell", "dumpsys", "batterystats", "--history").splitlines():
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
        level = re.match(r"^(\d{3})\b", body)
        yield dt, int(level.group(1)) if level else None, body


def format_time(dt):
    return dt.strftime("%m-%d %H:%M:%S.%f")[:-3] if dt else "-"


def format_duration(delta):
    seconds = delta.total_seconds()
    if seconds < 1:
        return f"{round(seconds * 1000)}ms"
    if seconds < 60:
        return f"{seconds:.1f}s"
    minutes, seconds = divmod(int(seconds), 60)
    hours, minutes = divmod(minutes, 60)
    return f"{hours}h{minutes:02d}m{seconds:02d}s" if hours else f"{minutes}m{seconds:02d}s"
