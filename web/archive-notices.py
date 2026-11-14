"""Copy or verify notice archives from generated HTML or a legacy APK or IPA."""

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
    parser.add_argument("source", type=Path, help="Generated HTML directory or legacy signed APK/IPA")
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
    destination = Path(__file__).resolve().parent / "notice-archives" / args.channel / args.version / args.build
    filenames = (f"{platform}.html", f"{platform}-licenses.html")
    if args.source.is_dir():
        pages = {destination / name: (args.source / name).read_bytes() for name in filenames}
    else:
        expected_suffix = ".apk" if platform == "android" else ".ipa"
        if args.source.suffix.lower() != expected_suffix:
            parser.error(f"Expected an HTML directory or {expected_suffix} for {args.channel}")
        pages = {}
        with ZipFile(args.source) as package:
            for filename in filenames:
                suffix = f"/notes.feature.about.generated.resources/files/open-source/{filename}"
                entries = [name for name in package.namelist() if name.endswith(suffix)]
                if len(entries) != 1:
                    parser.error(f"Expected one packaged resource ending in {suffix}, found {len(entries)}")
                pages[destination / filename] = package.read(entries[0])
    if not all(pages.values()):
        parser.error("Notice pages must be nonempty")

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
