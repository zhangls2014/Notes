import tempfile
import unittest
import json
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path
from hashlib import sha256
from unittest.mock import patch
from html.parser import HTMLParser

from generate import main, metadata, render_page


class NoticePageTest(unittest.TestCase):
    def test_source_categories_keep_every_component_once_on_both_platforms(self):
        platform_groups = ["androidx.activity", "com.google.guava", "org.jetbrains.kotlin",
                           "io.ktor", "io.ktor.client", "google", "jetbrains", "org.jetbrains"]
        other_groups = ["io.insert-koin", "sqlite", "androidxother", "com.googlefake",
                        "org.jetbrainsfake", "io.ktorfake"]
        with tempfile.TemporaryDirectory() as directory:
            licenses = Path(directory)
            (licenses / "MIT.txt").write_text("Original MIT text")
            items = [{"coordinate": group + ":core:1", "name": group,
                      "url": "https://example.com", "licenses": ["MIT"], "notices": []}
                     for group in other_groups + platform_groups]
            for platform in ("android", "ios"):
                page = render_page(platform, items, licenses)
                self.assertIn('data-zh="Google / JetBrains 组件"', page)
                self.assertIn('data-zh="其他第三方组件"', page)
                official, other = page.split('<h2 id="third-party-components">')
                self.assertIn('id="google-jetbrains-components"', official)
                for group in platform_groups:
                    self.assertIn(group + ":core:1", official)
                    self.assertNotIn(group + ":core:1", other)
                for group in other_groups:
                    self.assertIn(group + ":core:1", other)
                    self.assertNotIn(group + ":core:1", official)
                for item in items:
                    self.assertEqual(1, page.count('>' + item["coordinate"] + '</p>'))
                empty = render_page(platform, [], licenses)
                self.assertNotIn('id="google-jetbrains-components"', empty)
                self.assertNotIn('id="third-party-components"', empty)

    def test_category_shortcuts_target_visible_headings_only(self):
        with tempfile.TemporaryDirectory() as directory:
            licenses = Path(directory)
            (licenses / "MIT.txt").write_text("Original MIT text")
            items = [{"coordinate": group + ":core:1", "name": group,
                      "url": "https://example.com", "licenses": ["MIT"], "notices": []}
                     for group in ("androidx.activity", "io.insert-koin")]
            for platform in ("android", "ios"):
                page = render_page(platform, items, licenses)
                navigation = page.split('<nav class="actions">')[1].split('</nav>')[0]
                self.assertLess(page.index('</header>'), page.index('<nav'))
                self.assertLess(page.index('</nav>'), page.index('<h2'))
                for category in ("google-jetbrains-components", "third-party-components"):
                    self.assertIn(f'class="button" href="#{category}"', navigation)
                    self.assertIn(f'<h2 id="{category}">', page)
                self.assertIn('data-zh="Google / JetBrains 组件"', navigation)
                self.assertIn('data-en="Other third-party components"', navigation)
                single = render_page(platform, items[:1], licenses)
                self.assertNotIn('href="#third-party-components"', single)
                self.assertNotIn('<nav', render_page(platform, [], licenses))
                self.assertNotIn('<nav', render_page(platform, items, licenses, details=True))

    def test_buttons_navigate_to_fixed_platform_detail_page(self):
        with tempfile.TemporaryDirectory() as directory:
            licenses = Path(directory)
            (licenses / "MIT.txt").write_text("Complete license text")
            item = {"coordinate": "example:core:1", "name": "Core", "url": "https://example.com",
                    "licenses": ["MIT"], "notices": ["Original copyright notice"]}
            for platform in ("android", "ios"):
                page = render_page(platform, [item], licenses)
                self.assertIn(f'class="button" href="{platform}-licenses.html#license-MIT"', page)
                self.assertIn('class="button" href="https://example.com"', page)
                actions = page.split('<div class="actions">', 1)[1].split('</div>', 1)[0]
                self.assertEqual(3, actions.count('class="button"'))
                self.assertNotIn('<p>', actions)
                self.assertNotIn("<pre>", page)
                self.assertNotIn("Complete license text", page)
                self.assertNotIn("Original copyright notice", page)
                details = render_page(platform, [item], licenses, details=True)
                self.assertIn('data-detail="license-MIT"', details)
                self.assertIn("Complete license text", details)
                self.assertIn("Original copyright notice", details)
                self.assertNotIn("<header>", details)
                self.assertIn("<nav", page)
                self.assertNotIn("<article>", details)
                self.assertEqual(2, details.count("<template data-detail="))

    def test_same_package_modules_share_one_card_without_losing_declarations(self):
        class Cards(HTMLParser):
            def __init__(self):
                super().__init__()
                self.count = 0

            def handle_starttag(self, tag, attrs):
                if tag == "article":
                    self.count += 1

        with tempfile.TemporaryDirectory() as directory:
            licenses = Path(directory)
            for license_name in ("Apache-2.0", "MIT"):
                (licenses / (license_name + ".txt")).write_text(license_name + " full text")
            items = [
                {"coordinate": "example.lib:core:1", "name": "Core", "url": "https://example.com/core",
                 "licenses": ["Apache-2.0"], "notices": ["Core copyright"]},
                {"coordinate": "example.lib:ui:2", "name": "UI", "url": "https://example.com/ui",
                 "licenses": ["MIT"], "notices": ["UI copyright"]},
                {"coordinate": "example.lib.extra:extension:3", "name": "Extension", "url": "https://example.com/extension",
                 "licenses": ["MIT"], "notices": []},
            ]
            page = render_page("android", items, licenses)
            details = render_page("android", items, licenses, details=True)
            cards = Cards()
            cards.feed(page)
            self.assertEqual(2, cards.count)
            self.assertIn("Other third-party components</span>", page)
            for item in items:
                self.assertIn(item["coordinate"], page)
                self.assertIn(item["url"], page)
                for notice in item["notices"]:
                    self.assertIn(notice, details)
            self.assertIn("Apache-2.0 full text", details)
            self.assertIn("MIT full text", details)

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
            self.assertEqual(
                {"android.html", "android-licenses.html", "ios.html", "ios-licenses.html"},
                {file.name for file in output.glob("*.html")},
            )
            self.assertFalse((root / "feature/about/src").exists())
            self.assertTrue(inventory["android"][0]["notices"])
            self.assertEqual([], inventory["ios"][0]["notices"])
            self.assertIn("Android-specific notice", (output / "android-licenses.html").read_text())
            self.assertNotIn("Android-specific notice", (output / "ios-licenses.html").read_text())
            stale = output / "android-notice-0000000000000000.html"
            stale.write_text("obsolete")
            unrelated = output / "custom.html"
            unrelated.write_text("keep")
            inventory["noticeTexts"] = {key: text + " updated" for key, text in inventory["noticeTexts"].items()}
            (output / "dependencies.json").write_text(json.dumps(inventory))
            with patch("generate.OUTPUT", output), patch("sys.argv", ["generate.py"]):
                main()
            self.assertFalse(stale.exists())
            self.assertTrue(unrelated.exists())
            self.assertIn("Android-specific notice updated", (output / "android-licenses.html").read_text())
            self.assertEqual(5, len(list(output.glob("*.html"))))


    def test_missing_resolved_artifact_stops_refresh_before_metadata_lookup(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError):
                metadata({"group": "test", "name": "library", "version": "1", "artifacts": [str(Path(directory) / "missing.jar")]})

    def test_platform_inventory_and_upstream_text_are_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            licenses = Path(directory)
            (licenses / "Apache-2.0.txt").write_text("License <text> & terms", encoding="utf-8")
            item = {
                "coordinate": "example:android-only:1.0",
                "name": "Library <name>",
                "url": "https://example.com/?a=1&b=2",
                "licenses": ["Apache-2.0"],
                "notices": ["Copyright <author>"],
            }
            page = render_page("android", [item], licenses)
            details = render_page("android", [item], licenses, details=True)
            self.assertIn("Android", page)
            self.assertIn("example:android-only:1.0", page)
            self.assertIn("Library &lt;name&gt;", page)
            self.assertIn("License &lt;text&gt; &amp; terms", details)
            self.assertIn("Copyright &lt;author&gt;", details)
            self.assertEqual(1, page.count("<script>"))
            self.assertNotIn("<script src=", page)
            self.assertNotIn("android-only", render_page("ios", [], licenses))

    def test_missing_license_cannot_silently_generate_a_page(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                render_page("ios", [{"licenses": []}], Path(directory))

    def test_non_web_project_links_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            licenses = Path(directory)
            (licenses / "MIT.txt").write_text("MIT", encoding="utf-8")
            with self.assertRaises(ValueError):
                render_page("ios", [{"licenses": ["MIT"], "url": "javascript:alert(1)"}], licenses)


if __name__ == "__main__":
    unittest.main()
