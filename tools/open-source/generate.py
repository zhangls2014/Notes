"""Generate standalone notices from resolved Gradle coordinates and upstream metadata."""

import argparse
from concurrent.futures import ThreadPoolExecutor
from html import escape
from hashlib import sha256
import io
import json
from pathlib import Path
from urllib.parse import urlparse
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


def render_page(platform, items, license_directory, *, details=False):
    title = {"android": "Android", "ios": "iOS"}[platform]
    groups = {}
    used_licenses = set()
    notices_by_id = {}
    for item in items:
        if not item["licenses"]:
            raise ValueError("Every dependency must declare a reviewed license")
        used_licenses.update(item["licenses"])
        url = item["url"]
        if urlparse(url).scheme not in ("https", "http") or not urlparse(url).netloc:
            raise ValueError("Invalid upstream project URL: " + url)
        links = " ".join(f'<a data-font-scale class="button" href="{platform}-licenses.html#license-{escape(value)}">{escape(value)}</a>' for value in item["licenses"])
        notice_links = []
        for text in item["notices"]:
            notice_id = sha256(text.encode("utf-8")).hexdigest()[:16]
            notices_by_id[notice_id] = text
            notice_links.append(f'<a data-font-scale class="button" href="{platform}-licenses.html#notice-{notice_id}">版权与附加声明 / Notices</a>')
        notices = " ".join(notice_links)
        group = item["coordinate"].split(":", 1)[0]
        groups.setdefault(group, []).append(f'<div class="module"><h4>{escape(item["name"])}</h4><p class="coordinate">{escape(item["coordinate"])}</p><div class="actions">{links}<a class="button" href="{escape(url, quote=True)}">项目来源 / Source</a>{notices}</div></div>')
    sections = [f'<article><h3>{escape(group)}</h3>{"".join(modules)}</article>' for group, modules in groups.items()]
    full_texts = "".join(
        f'<section id="license-{escape(value)}"><h3>{escape(value)}</h3><pre>{escape((license_directory / (value + ".txt")).read_text(encoding="utf-8"))}</pre></section>'
        for value in sorted(used_licenses)
    )
    full_notices = "".join(
        f'<section id="notice-{key}"><h3>版权与附加声明 {index}</h3><pre>{escape(text)}</pre></section>'
        for index, (key, text) in enumerate(notices_by_id.items(), start=1)
    )
    heading = "许可证与版权声明" if details else "开源软件声明"
    if details:
        navigation = f'<a data-font-scale class="button" href="{platform}.html">返回组件列表</a>'
        content = f'<h2 id="licenses">许可证全文 / License texts</h2>{full_texts}<h2>版权与附加声明 / Notices</h2>{full_notices}'
    else:
        navigation = f'<a href="#libraries">开源软件 · {len(groups)}</a><a data-font-scale href="{platform}-licenses.html#licenses">查看许可证全文</a>'
        content = f'<h2 id="libraries">开源组件 / Libraries</h2>{"".join(sections)}'
    return f'''<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark">
<title>摘星 · {title} {heading}</title>
<script>
(() => {{
  const requested = Number(new URLSearchParams(location.search).get('fontScale') ?? '1');
  const fontScale = Number.isFinite(requested) && requested >= 1 && requested <= 2 ? requested : 1;
  const requestedTheme = new URLSearchParams(location.search).get('theme');
  const theme = requestedTheme === 'light' || requestedTheme === 'dark' ? requestedTheme : null;
  if (theme !== null) document.documentElement.dataset.theme = theme;
  document.documentElement.style.fontSize = `${{fontScale * 100}}%`;
  document.documentElement.style.setProperty('--app-font-scale', String(fontScale));
  const colors = new Map();
  const params = new URLSearchParams(location.search);
  for (const name of ['bg', 'card', 'fg', 'muted', 'line', 'accent', 'container', 'on-container']) {{
    const value = params.get(name);
    if (value !== null && /^[0-9a-f]{{6}}$/i.test(value)) {{
      colors.set(name, value);
      document.documentElement.style.setProperty(`--${{name}}`, `#${{value}}`);
    }}
  }}
  document.addEventListener('DOMContentLoaded', () => {{
    for (const button of document.querySelectorAll('a.button')) {{
      const release = () => {{ delete button.dataset.pressed; }};
      button.addEventListener('pointerdown', event => {{
        if (event.isPrimary && event.button === 0) button.dataset.pressed = 'true';
      }}, {{ passive: true }});
      for (const event of ['pointerup', 'pointercancel', 'pointerleave', 'blur', 'keyup']) {{
        button.addEventListener(event, release, {{ passive: true }});
      }}
      button.addEventListener('keydown', event => {{
        if (event.key === 'Enter' && !event.repeat) button.dataset.pressed = 'true';
      }}, {{ passive: true }});
    }}
    for (const link of document.querySelectorAll('a[data-font-scale]')) {{
      const target = new URL(link.getAttribute('href'), location.href);
      target.searchParams.set('fontScale', String(fontScale));
      if (theme !== null) target.searchParams.set('theme', theme);
      for (const [name, value] of colors) target.searchParams.set(name, value);
      link.href = target.href;
    }}
  }});
}})();
</script>
<style>
:root {{ color-scheme:light dark; --bg:#f9faef; --card:#f3f4e9; --fg:#1a1c16; --muted:#44483d; --line:#c5c8ba; --accent:#4c662b; --container:#cdeda3; --on-container:#354e16; }}
@media(prefers-color-scheme:dark) {{ :root {{ --bg:#12140e; --card:#1a1c16; --fg:#e2e3d8; --muted:#c5c8ba; --line:#44483d; --accent:#b1d18a; --container:#354e16; --on-container:#cdeda3; }} }}
:root[data-theme="light"] {{ color-scheme:light; --bg:#f9faef; --card:#f3f4e9; --fg:#1a1c16; --muted:#44483d; --line:#c5c8ba; --accent:#4c662b; --container:#cdeda3; --on-container:#354e16; }}
:root[data-theme="dark"] {{ color-scheme:dark; --bg:#12140e; --card:#1a1c16; --fg:#e2e3d8; --muted:#c5c8ba; --line:#44483d; --accent:#b1d18a; --container:#354e16; --on-container:#cdeda3; }}
:root {{ --corner-large:28px; --motion-duration:180ms; --motion-easing:cubic-bezier(.2,0,0,1); }}
* {{ box-sizing:border-box; }}
body {{ margin:0; background:var(--bg); color:var(--fg); font:1rem/1.5 system-ui,-apple-system,sans-serif; }}
main {{ max-width:880px; margin:auto; padding:32px 24px 64px; }}
header {{ padding:28px; margin-bottom:32px; border-radius:var(--corner-large); background:var(--container); color:var(--on-container); }}
.eyebrow {{ margin-top:0; font-size:.875rem; line-height:1.428571; font-weight:600; letter-spacing:.04em; }}
h1 {{ font-size:2rem; line-height:1.25; font-weight:650; margin:12px 0 16px; overflow-wrap:anywhere; }}
h2 {{ font-size:1.5rem; line-height:1.333333; font-weight:600; margin:32px 0 16px; }}
h3 {{ font-size:1.375rem; line-height:1.272727; font-weight:600; margin:0 0 8px; overflow-wrap:anywhere; }}
h4 {{ font-size:1rem; line-height:1.5; font-weight:600; margin:0 0 8px; overflow-wrap:anywhere; }}
.module {{ padding-top:16px; }}
.module + .module {{ margin-top:20px; border-top:1px solid var(--line); }}
p {{ margin:8px 0; }}
.muted,.coordinate {{ color:var(--muted); }}
.muted,.coordinate,.button {{ font-size:.875rem; line-height:1.428571; }}
header .muted {{ color:inherit; }}
.coordinate {{ font-family:ui-monospace,monospace; overflow-wrap:anywhere; }}
article,section {{ background:var(--card); border-radius:var(--corner-large); padding:24px; margin:16px 0; }}
a {{ color:var(--accent); text-underline-offset:3px; overflow-wrap:anywhere; }}
a:focus-visible {{ outline:3px solid var(--accent); outline-offset:4px; }}
.actions {{ display:flex; align-items:center; flex-wrap:wrap; gap:8px; padding:4px 0; }}
.button {{ position:relative; display:inline-flex; align-items:center; justify-content:center; flex-shrink:0; max-width:100%; min-height:48px; padding:12px 20px; border:0; border-radius:calc(1.428571em + 12px); background:var(--container); color:var(--on-container); font-weight:600; text-decoration:none; text-align:center; -webkit-tap-highlight-color:transparent; }}
.button::before {{ content:""; position:absolute; inset:0; border-radius:inherit; background:currentColor; opacity:0; pointer-events:none; transition:opacity var(--motion-duration) var(--motion-easing); }}
.actions .button {{ white-space:normal; overflow-wrap:anywhere; }}
@media(hover:hover) {{ .button:hover::before {{ opacity:.08; }} }}
.button:focus-visible::before {{ opacity:.1; }}
.button[data-pressed]::before {{ opacity:.1; }}
header a {{ color:inherit; }}
header .button {{ background:var(--card); color:var(--fg); }}
header a:focus-visible {{ outline-color:currentColor; }}
pre {{ white-space:pre-wrap; overflow-wrap:anywhere; font:1rem/1.5 ui-monospace,monospace; margin:16px 0 0; }}
nav {{ display:flex; align-items:center; flex-wrap:wrap; gap:8px 20px; margin-top:20px; }}
nav a:not(.button) {{ display:inline-flex; align-items:center; min-height:48px; font-weight:600; }}
footer {{ margin-top:32px; color:var(--muted); font-size:.875rem; line-height:1.428571; }}
@media(max-width:400px) {{ main {{ padding:16px 16px 48px; }} header {{ padding:20px; }} article,section {{ padding:16px; }} }}
@media(prefers-reduced-motion:reduce) {{ .button::before {{ transition:none; }} }}
@media print {{ header,article,section {{ break-inside:avoid; }} }}
</style>
</head>
<body><main>
<header><p class="eyebrow">摘星 NOTES · {title}</p>
<h1>{heading}</h1><p class="muted" lang="en">Open-source software notices</p>
<p>摘星的 {title} 版本使用了以下开源软件。感谢这些项目的作者与贡献者。</p>
<p class="muted">本页汇总 {title} 版本相关的开源组件与许可信息，不同版本或发行渠道包含的组件可能有所不同。许可证与附加声明保留上游原文。</p>
<nav aria-label="页面目录">{navigation}</nav></header>
{content}
<footer>本声明仅适用于摘星 {title} 版本。各开源项目的权利与使用条件以对应许可证及版权声明为准。</footer>
</main></body></html>
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", help="Read Gradle export and retrieve missing upstream POMs")
    args = parser.parse_args()
    inventory_file = OUTPUT / "dependencies.json"
    if args.refresh:
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
    inventories = json.loads(inventory_file.read_text(encoding="utf-8"))
    notices = inventories.pop("noticeTexts")
    for items in inventories.values():
        for item in items:
            item["notices"] = [notices[key] for key in item["notices"]]
    for platform, items in inventories.items():
        for details in (False, True):
            filename = f"{platform}{'-licenses' if details else ''}.html"
            page = render_page(platform, items, OUTPUT / "licenses", details=details)
            (OUTPUT / filename).write_text(page, encoding="utf-8")
            print(filename)


if __name__ == "__main__":
    main()
