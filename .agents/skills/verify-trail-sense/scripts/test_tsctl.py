import argparse
import sys
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))

import cli  # noqa: E402
from core import (  # noqa: E402
    BASE_PACKAGE, PROTECTED_PACKAGE, Failed, expired, Policy, Refused, UsageError, apply_pref_edits, check_dry_run,
    classify, decide, flatten_layout, parse_flash_units, parse_focused_package, parse_installed_info, parse_prefs,
    parse_tool_ids, parse_torch_state, prune_state, resolve_pref_key, resolve_tool, serialize_prefs, shard, validate_grant,
)

PHONE = "R5CT123ABC"


def gate(policy, *, emulator=False, touched=(BASE_PACKAGE,), dry_run=False, approved=None, granted=False):
    decide(policy, is_emulator=emulator, serial="emulator-5554" if emulator else PHONE, touched=list(touched),
           dry_run=dry_run, approved=approved, action="test", granted=granted)


class GateTest(unittest.TestCase):
    destructive = Policy(risk="destructive", dry_run=True)
    destructive_app_data = Policy(risk="destructive", dry_run=True, app_data=True)
    act = Policy(risk="act")

    def test_emulator_needs_no_approval_even_for_destructive(self):
        gate(self.destructive_app_data, emulator=True)

    def test_phone_refuses_unapproved_action_and_destructive(self):
        for policy in (self.act, self.destructive):
            with self.assertRaises(Refused):
                gate(policy)

    def test_phone_allows_inspect_without_approval(self):
        gate(Policy(risk="inspect"))

    def test_approval_must_name_this_serial(self):
        gate(self.destructive, approved=PHONE)
        with self.assertRaises(Refused):
            gate(self.destructive, approved="emulator-5554")

    def test_full_control_grant_replaces_approval(self):
        gate(self.destructive_app_data, granted=True)

    def test_grant_does_not_cover_ungrantable_commands(self):
        with self.assertRaises(Refused):
            decide(self.destructive, is_emulator=False, serial=PHONE, touched=[BASE_PACKAGE], dry_run=False,
                   approved=None, action="adb", granted=True, grantable=False)

    def test_protected_package_refused_on_phone_even_when_approved_or_granted(self):
        for policy in (Policy(risk="inspect"), self.act, self.destructive):
            with self.assertRaises(Refused):
                gate(policy, touched=(PROTECTED_PACKAGE,), approved=PHONE, granted=True)

    def test_protected_package_allowed_on_emulator(self):
        gate(self.destructive, emulator=True, touched=(PROTECTED_PACKAGE,))

    def test_protected_package_in_raw_adb_args_or_focused_window_is_refused(self):
        with self.assertRaises(Refused):
            gate(self.destructive, touched=(BASE_PACKAGE, f"shell pm clear {PROTECTED_PACKAGE}"), approved=PHONE)

    def test_dry_run_skips_approval_only_without_app_data(self):
        gate(self.destructive, dry_run=True)
        with self.assertRaises(Refused):
            gate(self.destructive_app_data, dry_run=True)

    def test_emulator_only_command_fails_on_phone(self):
        with self.assertRaises(Failed):
            gate(Policy(risk="act", emulator_only=True), approved=PHONE)

    def test_dry_run_rejected_for_acting_command_without_dry_run_mode(self):
        with self.assertRaises(UsageError):
            check_dry_run(self.act, True, "app launch")
        check_dry_run(Policy(risk="inspect"), True, "tools")

    def test_grant_validation(self):
        validate_grant("com.kylecorry.trail_sense.staging")
        with self.assertRaises(Refused):
            validate_grant(PROTECTED_PACKAGE)
        with self.assertRaises(UsageError):
            validate_grant("not a package")


class ClassifyTest(unittest.TestCase):
    def test_emulator_needs_serial_and_qemu_property(self):
        self.assertTrue(classify("emulator-5554", ["", "1"], False)[0])
        self.assertFalse(classify("emulator-5554", ["", ""], False)[0])
        self.assertFalse(classify("192.168.1.5:5555", ["1"], False)[0])

    def test_forced_physical_overrides(self):
        self.assertFalse(classify("emulator-5554", ["1"], True)[0])


class CommandStructureTest(unittest.TestCase):
    """Every runnable command must declare a Policy, so main() can gate it."""

    def leaves(self, parser, path=()):
        actions = [a for a in parser._actions if isinstance(a, argparse._SubParsersAction)]
        if not actions:
            yield path, parser
            return
        for name, sub in actions[0].choices.items():
            yield from self.leaves(sub, path + (name,))

    def test_every_leaf_declares_a_policy_and_handler(self):
        leaves = list(self.leaves(cli.build_parser()))
        self.assertGreater(len(leaves), 40)
        for path, parser in leaves:
            with self.subTest(command=" ".join(path)):
                self.assertIsInstance(parser.get_default("policy"), Policy)
                self.assertTrue(callable(parser.get_default("fn")))

    def test_data_destructive_commands_are_marked_destructive(self):
        policies = {" ".join(p): parser.get_default("policy") for p, parser in self.leaves(cli.build_parser())}
        for name in ("app install", "app uninstall", "app clear", "app seed", "pref set", "pref delete",
                     "db exec", "app restore", "test android", "adb"):
            with self.subTest(command=name):
                self.assertEqual(policies[name].risk, "destructive")
                self.assertTrue(policies[name].dry_run)
        for name in ("pref get", "pref list", "db query", "app seed", "db exec", "app restore"):
            with self.subTest(command=name):
                self.assertTrue(policies[name].app_data)

    def test_shard_and_dry_run_usage_errors_exit_2(self):
        with self.assertRaises(SystemExit) as e:
            cli.build_parser().parse_args(["test", "android", "--shard", "3"])
        self.assertEqual(e.exception.code, 2)
        self.assertEqual(cli.main(["--dry-run", "app", "launch"]), 2)

    def test_shard_parses(self):
        self.assertEqual(shard("1/4"), (1, 4))
        for bad in ("4/4", "x", "1-4"):
            with self.assertRaises(argparse.ArgumentTypeError):
                shard(bad)


class PrefsTest(unittest.TestCase):
    xml = (b"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>"
           b"<boolean name=\"use_24_hour\" value=\"false\" /><string name=\"distance_units\">meters</string>"
           b"<int name=\"count\" value=\"3\" /></map>")

    def test_set_keeps_existing_type_and_round_trips(self):
        root = parse_prefs(self.xml)
        changes = apply_pref_edits(root, [("use_24_hour", None, "TRUE"), ("count", None, "7"), ("new", None, "x")])
        self.assertEqual([c["after"] for c in changes], ["true", "7", "x"])
        again = parse_prefs(serialize_prefs(root))
        self.assertEqual({e.get("name"): e.tag for e in again},
                         {"distance_units": "string", "use_24_hour": "boolean", "count": "int", "new": "string"})

    def test_delete_and_invalid_values(self):
        root = parse_prefs(self.xml)
        self.assertEqual(apply_pref_edits(root, [("distance_units", None, None)])[0]["before"], "meters")
        self.assertNotIn("distance_units", {e.get("name") for e in root})
        with self.assertRaises(UsageError):
            apply_pref_edits(root, [("use_24_hour", None, "maybe")])
        with self.assertRaises(UsageError):
            apply_pref_edits(root, [("count", "int", "1.5")])

    def test_resource_keys(self):
        strings = {"pref_use_24_hour": "use_24_hour"}
        self.assertEqual(resolve_pref_key("@pref_use_24_hour", strings), "use_24_hour")
        self.assertEqual(resolve_pref_key("raw_key", strings), "raw_key")
        with self.assertRaises(UsageError):
            resolve_pref_key("@missing", strings)

    def test_not_xml_is_none(self):
        self.assertIsNone(parse_prefs(b"run-as: package not debuggable"))


class RetentionTest(unittest.TestCase):
    def test_only_entries_older_than_max_age_expire(self):
        day = 86400
        entries = {"old-run": 0, "fresh-run": 6 * day, "edge": 3 * day}
        self.assertEqual(expired(entries, now=10 * day, max_age_seconds=7 * day), ["old-run"])

    def test_prune_keeps_recent_and_deletes_week_old_backups(self):
        import os
        import tempfile
        import time
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            old_backup = root / "backups" / "20200101-000000-clear"
            new_backup = root / "backups" / "20991231-000000-clear"
            old_run = root / "old-run"
            new_run = root / "new-run"
            for d in (old_backup, new_backup, old_run, new_run):
                d.mkdir(parents=True)
                (d / "f.txt").write_text("x")
            week_ago = time.time() - 8 * 86400
            for d in (old_backup, old_run):
                for p in (d / "f.txt", d):
                    os.utime(p, (week_ago, week_ago))
            original = cli.CAPTURES
            cli.CAPTURES = root
            try:
                self.assertEqual(sorted(p.name for p in cli.prune_captures(dry_run=True)), ["20200101-000000-clear", "old-run"])
                self.assertTrue(old_run.exists())
                cli.prune_captures(dry_run=False)
            finally:
                cli.CAPTURES = original
            self.assertFalse(old_backup.exists())
            self.assertFalse(old_run.exists())
            self.assertTrue(new_backup.exists())
            self.assertTrue(new_run.exists())


class ParserTest(unittest.TestCase):
    def test_tool_ids_skip_quick_actions_and_resolve_names(self):
        ids = parse_tool_ids("const val BEACONS = 7L\nconst val BUBBLE_LEVEL = 12L\nconst val QUICK_ACTION_FOO = 9L")
        self.assertEqual(ids, {"beacons": 7, "bubble-level": 12})
        self.assertEqual(resolve_tool("Bubble Level", ids), 12)
        self.assertEqual(resolve_tool("bubble", ids), 12)
        self.assertEqual(resolve_tool("7", ids), 7)
        with self.assertRaises(UsageError):
            resolve_tool("b", ids)

    def test_real_tools_kt_parses(self):
        ids = cli.tool_ids()
        self.assertEqual(ids["beacons"], 7)
        self.assertEqual(ids["settings"], 34)

    def test_flash_units_take_each_cameras_first_static_entry(self):
        dumpsys = ("== Camera HAL device device@1.1/internal/1 (v1.3) static information: ==\n"
                   "      android.flash.info.available (50000): byte[1]\n        [FALSE ]\n"
                   "      android.flash.info.available (50000): byte[1]\n        [TRUE ]\n"
                   "== Camera HAL device device@1.1/internal/1 (v1.3) dumpState: ==\n"
                   "== Camera HAL device device@1.1/internal/0 (v1.3) static information: ==\n"
                   "      android.flash.info.available (50000): byte[1]\n        [TRUE ]\n")
        self.assertEqual(parse_flash_units(dumpsys), {"0": True, "1": False})

    def test_torch_state(self):
        dumpsys = ("    FlashlightController state:\n      mCameraId=0\n      mFlashlightEnabled=true\n"
                   "      mTorchAvailable=true\n")
        self.assertEqual(parse_torch_state(dumpsys), {"camera": "0", "on": True, "available": True})
        self.assertIsNone(parse_torch_state("no flashlight here"))

    def test_installed_info(self):
        dumpsys = ("Package [com.kylecorry.trail_sense] (abc):\n versionCode=149 minSdk=24\n versionName=8.3.0\n"
                   " pkgFlags=[ DEBUGGABLE HAS_CODE ]\n lastUpdateTime=2026-11-21 06:39:24\n")
        info = parse_installed_info(dumpsys, BASE_PACKAGE)
        self.assertEqual((info["versionName"], info["versionCode"], info["lastUpdateTime"], info["debuggable"]),
                         ("8.3.0", "149", "2026-11-21 06:39:24", True))
        self.assertIsNone(parse_installed_info(dumpsys, BASE_PACKAGE + ".staging"))

    def test_focused_package(self):
        out = "  mCurrentFocus=Window{77 u0 com.kylecorry.trail_sense/com.kylecorry.trail_sense.main.MainActivity}"
        self.assertEqual(parse_focused_package(out), BASE_PACKAGE)

    def test_layout_flattening_skips_system_windows(self):
        data = [{"window-title": "", "type": "SYSTEM", "content": [{"class": "Clock", "text": "6:39"}]},
                {"window-title": "Trail Sense", "type": "APPLICATION",
                 "content": [{"class": "Frame", "children": [{"class": "TextView", "text": "Beacons"}]}]}]
        nodes = flatten_layout(data)
        self.assertEqual([n.get("text") for n in nodes], [None, "Beacons"])
        self.assertTrue(all(n["window"] == "Trail Sense" for n in nodes))

    def test_prune_state_drops_dead_or_detached_emulators(self):
        state = {"emulator-5554": {"pid": 1}, "emulator-5556": {"pid": 2}, "emulator-5558": {"pid": 3}}
        self.assertEqual(prune_state(state, alive_pids={1, 3}, connected={"emulator-5554", "emulator-5556"}),
                         {"emulator-5554": {"pid": 1}})


if __name__ == "__main__":
    unittest.main()
