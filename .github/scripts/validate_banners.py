import json
import re
import sys
from pathlib import Path

BANNERS_DIR = Path(__file__).resolve().parents[2] / "banners"
INDEX = BANNERS_DIR / "index.json"
FILENAME_RE = re.compile(r"^[A-Za-z0-9._-]+\.png$")
COLOR_RE = re.compile(r"^#[0-9A-Fa-f]{6}$")

errors = []


def err(msg):
    errors.append(msg)


def main():
    try:
        raw = INDEX.read_text(encoding="utf-8")
    except OSError as e:
        print(f"ERROR: cannot read {INDEX}: {e}")
        return 1

    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        print(f"ERROR: banners/index.json is not valid JSON: {e}")
        print("Hint: check for trailing commas, missing quotes, or unbalanced brackets.")
        return 1

    if not isinstance(data, list):
        print("ERROR: top level of index.json must be a list of contributors")
        return 1

    seen_files = {}
    for i, contributor in enumerate(data):
        where = f"contributor #{i + 1}"
        if not isinstance(contributor, dict):
            err(f"{where}: must be an object")
            continue
        name = contributor.get("name")
        if not isinstance(name, str) or not name.strip():
            err(f"{where}: missing or empty \"name\"")
        else:
            where = f"contributor \"{name}\""
        color = contributor.get("color")
        if color is not None and (not isinstance(color, str) or not COLOR_RE.match(color)):
            err(f"{where}: \"color\" must be a hex string like \"#34a4eb\", got {color!r}")
        unknown = set(contributor) - {"name", "color", "banners"}
        if unknown:
            err(f"{where}: unknown keys {sorted(unknown)}")
        banners = contributor.get("banners")
        if not isinstance(banners, list) or not banners:
            err(f"{where}: \"banners\" must be a non-empty list")
            continue
        for j, banner in enumerate(banners):
            bwhere = f"{where}, banner #{j + 1}"
            if not isinstance(banner, dict):
                err(f"{bwhere}: must be an object")
                continue
            fname = banner.get("file")
            if not isinstance(fname, str) or not FILENAME_RE.match(fname):
                err(f"{bwhere}: \"file\" must be a plain .png filename (letters, digits, . _ -), got {fname!r}")
                continue
            if fname in seen_files:
                err(f"{bwhere}: \"{fname}\" already listed under {seen_files[fname]}")
            seen_files[fname] = where
            if not (BANNERS_DIR / fname).is_file():
                err(f"{bwhere}: \"{fname}\" does not exist in banners/")
            v = banner.get("v")
            if not isinstance(v, int):
                err(f"{bwhere}: missing or non-integer \"v\" (bump it when you update the image)")
            unknown = set(banner) - {"file", "v"}
            if unknown:
                err(f"{bwhere}: unknown keys {sorted(unknown)}")

    if errors:
        print(f"banners/index.json validation FAILED with {len(errors)} problem(s):")
        for e in errors:
            print(f"  - {e}")
        return 1

    print(f"banners/index.json is valid: {len(data)} contributor(s), {len(seen_files)} banner(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
