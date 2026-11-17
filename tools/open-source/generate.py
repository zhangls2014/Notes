"""Refresh the notice inventory from resolved Gradle coordinates and upstream metadata."""

import argparse
from concurrent.futures import ThreadPoolExecutor
from hashlib import sha256
import io
import json
from pathlib import Path
from urllib.request import urlopen
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "web/open-source"
CACHE = Path.home() / ".gradle/caches/modules-2/files-2.1"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def download(url):
    with urlopen(url, timeout=60) as response:
        return response.read().decode("utf-8")


def license_id(name):
    normalized = name.lower()
    if "apache" in normalized and ("2.0" in normalized or "version 2" in normalized):
        return "Apache-2.0"
    if normalized in ("mit", "mit license", "the mit license"):
        return "MIT"
    if normalized in ("bsd-3-clause", "bsd 3-clause", "bsd 3-clause license"):
        return "BSD-3-Clause"
    raise ValueError("Unreviewed license: " + name)


def archive_notices(archive):
    result = set()
    with zipfile.ZipFile(archive) as entries:
        for name in entries.namelist():
            basename = name.rsplit("/", 1)[-1].lower()
            if basename.startswith(("license", "notice", "copying", "copyright")) and not name.endswith("/"):
                content = entries.read(name)
                try:
                    result.add(content.decode("utf-8"))
                except UnicodeDecodeError:
                    # SLF4J's published LICENSE uses Latin-1 for the copyright symbol.
                    result.add(content.decode("latin-1"))
            elif name == "classes.jar":
                result.update(archive_notices(io.BytesIO(entries.read(name))))
    return result


def read_pom(group, name, version):
    directory = CACHE / group / name / version
    poms = sorted(directory.glob("*/*.pom"))
    relative = f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}.pom"
    repository = "https://dl.google.com/dl/android/maven2/" if group.startswith("androidx.") else "https://repo.maven.apache.org/maven2/"
    pom_url = repository + relative
    pom_cache = ROOT / "build/open-source/poms" / group / name / f"{version}.pom"
    if poms:
        text = poms[0].read_text(encoding="utf-8")
    elif pom_cache.exists():
        text = pom_cache.read_text(encoding="utf-8")
    else:
        text = download(pom_url)
        pom_cache.parent.mkdir(parents=True, exist_ok=True)
        pom_cache.write_text(text, encoding="utf-8")
    return ET.fromstring(text), pom_url


def metadata(coordinate):
    group, name, version = (coordinate[key] for key in ("group", "name", "version"))
    artifacts = [Path(path) for path in coordinate["artifacts"]]
    for artifact in artifacts:
        if not artifact.is_file():
            raise FileNotFoundError("Resolved artifact is missing; rerun Gradle export: " + str(artifact))
    pom, pom_url = read_pom(group, name, version)
    license_pom = pom
    declared = pom.findall("m:licenses/m:license", NS)
    while not declared:
        parent = license_pom.find("m:parent", NS)
        if parent is None:
            raise ValueError("Missing license: " + pom_url)
        license_pom, _ = read_pom(*(parent.findtext("m:" + key, namespaces=NS) for key in ("groupId", "artifactId", "version")))
        declared = license_pom.findall("m:licenses/m:license", NS)
    licenses = sorted({license_id(item.findtext("m:name", default="", namespaces=NS)) for item in declared})
    if not licenses:
        raise ValueError("Missing license: " + pom_url)
    notices = set()
    for artifact in artifacts:
        if artifact.suffix in (".jar", ".aar", ".klib"):
            notices.update(archive_notices(artifact))
    return {
        "coordinate": f"{group}:{name}:{version}",
        "name": pom.findtext("m:name", namespaces=NS) or name,
        "url": pom.findtext("m:url", namespaces=NS) or pom_url,
        "pom": pom_url,
        "licenses": licenses,
        "notices": sorted(notices),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", required=True, help="Read Gradle export and retrieve missing upstream POMs")
    parser.parse_args()
    inventory_file = OUTPUT / "dependencies.json"
    platforms = json.loads((ROOT / "build/open-source/artifacts.json").read_text())
    with ThreadPoolExecutor(max_workers=12) as executor:
        inventories = {
            platform: list(executor.map(metadata, items))
            for platform, items in platforms.items()
        }
    assets = json.loads((OUTPUT / "assets.json").read_text())
    for platform in inventories:
        for asset in assets[platform]:
            asset = dict(asset)
            asset["notices"] = [(OUTPUT / path).read_text(encoding="utf-8") for path in asset.pop("noticeFiles")]
            inventories[platform].append(asset)
    notices = {}
    for items in inventories.values():
        for item in items:
            references = []
            for text in item["notices"]:
                key = sha256(text.encode("utf-8")).hexdigest()
                notices[key] = text
                references.append(key)
            item["notices"] = references
    inventories["noticeTexts"] = notices
    inventory_file.write_text(json.dumps(inventories, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
