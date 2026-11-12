"""Copy or verify notice archives against a signed Android APK or iOS IPA."""

import argparse
import re
from pathlib import Path
from zipfile import ZipFile


CHANNELS = {
    "android-prd-full": "android",
    "android-prd-mainland": "android",
    "ios-app-store": "ios",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path)
    parser.add_argument("channel", choices=CHANNELS)
    parser.add_argument("version")
    parser.add_argument("build")
    parser.add_argument("--verify", action="store_true", help="Compare an existing archive without writing files")
    args = parser.parse_args()

    platform = CHANNELS[args.channel]
    version_pattern = r"(?:0|[1-9]\d*)(?:\.(?:0|[1-9]\d*)){2}"
    build_pattern = r"[1-9]\d*" if platform == "android" else r"[1-9]\d*(?:\.\d+){0,2}"
    if not re.fullmatch(version_pattern, args.version) or not re.fullmatch(build_pattern, args.build):
        parser.error("Invalid release version or build number")
    expected_suffix = ".apk" if platform == "android" else ".ipa"
    if args.package.suffix.lower() != expected_suffix:
        parser.error(f"Expected a {expected_suffix} for {args.channel}")

    destination = Path(__file__).resolve().parent / "notice-archives" / args.channel / args.version / args.build
    pages = {}
    with ZipFile(args.package) as package:
        for filename in (f"{platform}.html", f"{platform}-licenses.html"):
            suffix = f"/notes.feature.about.generated.resources/files/open-source/{filename}"
            entries = [name for name in package.namelist() if name.endswith(suffix)]
            if len(entries) != 1:
                parser.error(f"Expected one packaged resource ending in {suffix}, found {len(entries)}")
            pages[destination / filename] = package.read(entries[0])

    for path, content in pages.items():
        if args.verify or path.exists():
            if not path.is_file() or path.read_bytes() != content:
                parser.error(f"Archive differs from the package: {path}")
    if not args.verify:
        destination.mkdir(parents=True, exist_ok=True)
        for path, content in pages.items():
            if not path.exists():
                with path.open("xb") as output:
                    output.write(content)
    print(f"Verified {len(pages)} notice pages for {args.channel}/{args.version}/{args.build}")


if __name__ == "__main__":
    main()
