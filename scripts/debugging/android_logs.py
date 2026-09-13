import subprocess
import sys
from datetime import datetime, timedelta


def adb(*args):
    try:
        result = subprocess.run(["adb", *args], capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        sys.exit(e.stderr.decode(errors="replace").strip() or f"adb {' '.join(args)} failed")
    return result.stdout.decode(errors="replace").replace("\r", "")


def device_now():
    return datetime.strptime(adb("shell", "date", "+%Y-%m-%d_%H:%M:%S").strip(), "%Y-%m-%d_%H:%M:%S")


def parse_month_day_time(value, now):
    # Logs omit the year, so assume the most recent year that doesn't put the time in the future
    dt = datetime.strptime(f"{now.year}-{value}", "%Y-%m-%d %H:%M:%S.%f")
    if dt > now + timedelta(days=1):
        dt = dt.replace(year=now.year - 1)
    return dt


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
