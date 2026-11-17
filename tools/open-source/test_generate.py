import tempfile
import unittest
import json
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path
from unittest.mock import patch

from generate import main, metadata


class NoticeInventoryTest(unittest.TestCase):
    def test_same_coordinate_keeps_each_platform_artifacts_separate(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / "web"
            (output / "licenses").mkdir(parents=True)
            (output / "licenses/Apache-2.0.txt").write_text("Apache license")
            (root / "build/open-source").mkdir(parents=True)
            archive = root / "library.jar"
            with zipfile.ZipFile(archive, "w") as entries:
                entries.writestr("META-INF/NOTICE", "Android-specific notice")
            coordinate = {"group": "example", "name": "shared", "version": "1"}
            platforms = {
                "android": [dict(coordinate, artifacts=[str(archive)])],
                "ios": [dict(coordinate, artifacts=[])],
            }
            (root / "build/open-source/artifacts.json").write_text(json.dumps(platforms))
            (output / "assets.json").write_text('{"android": [], "ios": []}')
            pom = ET.fromstring('<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license><name>Apache 2.0</name></license></licenses></project>')
            with patch("generate.ROOT", root), patch("generate.OUTPUT", output), \
                    patch("generate.read_pom", return_value=(pom, "https://example.com/pom")), \
                    patch("sys.argv", ["generate.py", "--refresh"]):
                main()
            inventory = json.loads((output / "dependencies.json").read_text())
            self.assertEqual([], list(output.glob("*.html")))
            android_notices = inventory["android"][0]["notices"]
            self.assertEqual(["Android-specific notice"], [inventory["noticeTexts"][key] for key in android_notices])
            self.assertEqual([], inventory["ios"][0]["notices"])

    def test_missing_resolved_artifact_stops_refresh_before_metadata_lookup(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError):
                metadata({"group": "test", "name": "library", "version": "1", "artifacts": [str(Path(directory) / "missing.jar")]})


if __name__ == "__main__":
    unittest.main()
