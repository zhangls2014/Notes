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

    def localized(chinese, english):
        return f'<span data-zh="{escape(chinese, quote=True)}" data-en="{escape(english, quote=True)}">{escape(english)}</span>'

    source_label = localized("项目来源", "Project source")
    notices_label = localized("版权与附加声明", "Copyright and notices")
    licenses_label = localized("许可证全文", "License texts")
    series = {}
    used_licenses = set()
    notices_by_id = {}
    categories = {"google-jetbrains-components": [], "third-party-components": []}
    for item in items:
        if not item["licenses"]:
            raise ValueError("Every dependency must declare a reviewed license")
        url = urlparse(item["url"])
        if url.scheme not in ("https", "http") or not url.netloc:
            raise ValueError("Invalid upstream project URL: " + item["url"])
        group = item["coordinate"].split(":", 1)[0]
        platform_component = group in ("google", "jetbrains") or any(
            group == namespace or group.startswith(namespace + ".")
            for namespace in ("androidx", "com.google", "org.jetbrains", "io.ktor")
        )
        category = "google-jetbrains-components" if platform_component else "third-party-components"
        source = url._replace(fragment="").geturl().rstrip("/")
        name = group
        key = (category, group, source)
        path = url.path.strip("/").split("/")
        if url.hostname in ("github.com", "www.github.com") and len(path) >= 2:
            name = "/".join(path[:2]).removesuffix(".git")
            source = "https://github.com/" + name
            key = (category, source.lower())
        project = series.setdefault(key, {"category": category, "name": name,
                                          "source": source, "items": []})
        project["items"].append(item)

    for project in series.values():
        modules = []
        licenses = set()
        notices = set()
        for item in project["items"]:
            licenses.update(item["licenses"])
            for text in item["notices"]:
                notices.add(text)
            modules.append(f'<div class="module"><h4>{escape(item["name"])}</h4><p class="coordinate">{escape(item["coordinate"])}</p></div>')
        used_licenses.update(licenses)
        links = " ".join(f'<a data-font-scale class="button" href="{platform}-licenses.html#license-{escape(value)}">{escape(value)}</a>' for value in sorted(licenses))
        notice_link = ""
        if notices:
            texts = sorted(notices)
            identity = texts[0] if len(texts) == 1 else json.dumps(texts, ensure_ascii=False)
            notice_id = sha256(identity.encode("utf-8")).hexdigest()[:16]
            notices_by_id[notice_id] = texts
            notice_link = f'<a data-font-scale class="button" href="{platform}-licenses.html#notice-{notice_id}">{notices_label}</a>'
        categories[project["category"]].append(
            f'<article><h3>{escape(project["name"])}</h3>{"".join(modules)}'
            f'<div class="actions">{links}<a class="button" href="{escape(project["source"], quote=True)}">{source_label}</a>{notice_link}</div></article>'
        )
    labels = {
        "google-jetbrains-components": localized("Google / JetBrains 组件", "Google / JetBrains components"),
        "third-party-components": localized("其他第三方组件", "Other third-party components"),
    }
    sections = [f'<h2 id="{category}">{labels[category]}</h2>{"".join(cards)}'
                for category, cards in categories.items() if cards]
    shortcuts = "".join(f'<a class="button" href="#{category}">{labels[category]}</a>'
                        for category, cards in categories.items() if cards)
    navigation = f'<nav class="actions">{shortcuts}</nav>' if shortcuts else ""
    templates = []
    for value in sorted(used_licenses):
        text = (license_directory / (value + ".txt")).read_text(encoding="utf-8")
        templates.append(f'<template data-detail="license-{escape(value)}"><section><h1>{escape(value)}</h1><pre>{escape(text)}</pre></section></template>')
    for key, texts in notices_by_id.items():
        blocks = "".join(f'<section><pre>{escape(text)}</pre></section>' for text in texts)
        templates.append(f'<template data-detail="notice-{key}">{blocks}</template>')
    heading = "许可证与版权声明" if details else "开源软件声明"
    content = ("".join(templates) if details else
               f'{navigation}{"".join(sections)}')
    heading_en = "Licenses and copyright notices" if details else "Open-source software notices"
    heading_label = localized(heading, heading_en)
    intro = localized(f"摘星的 {title} 版本使用了以下开源软件。感谢这些项目的作者与贡献者。", f"Notes for {title} uses the following open-source software. Thank you to the authors and contributors.")
    summary = localized(f"本页汇总 {title} 版本相关的开源组件与许可信息，不同版本或发行渠道包含的组件可能有所不同。许可证与附加声明保留上游原文。", f"This page lists open-source components and license information for {title}. Components may vary by version or distribution. License texts and notices are preserved in their original form.")
    introduction = "" if details else f'<header><p class="eyebrow">NOTES · {title}</p><h1>{heading_label}</h1><p>{intro}</p><p class="muted">{summary}</p></header>'
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark">
<title data-zh="摘星 · {title} {heading}" data-en="Notes · {title} {heading_en}">Notes · {title} {heading_en}</title>
<script>
(() => {{
  const languageHint = new URLSearchParams(location.search).get('lang') || navigator.language || 'en';
  const language = /^zh(?:-|$)/i.test(languageHint) ? 'zh-CN' : 'en';
  document.documentElement.lang = language;
  const requested = Number(new URLSearchParams(location.search).get('fontScale') ?? '1');
  const fontScale = Number.isFinite(requested) && requested > 0 ? requested : 1;
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
    const templates = new Map([...document.querySelectorAll('template[data-detail]')].map(node => [node.getAttribute('data-detail'), node]));
    if (templates.size > 0) {{
      const renderDetail = () => {{
        const selected = templates.get(location.hash.slice(1));
        const main = document.querySelector('main');
        if (selected) {{
          main.replaceChildren(selected.content.cloneNode(true));
        }} else {{
          main.textContent = language === 'zh-CN' ? '未找到该声明。' : 'Notice not found.';
        }}
      }};
      renderDetail();
      globalThis.addEventListener?.('hashchange', renderDetail);
    }}
    for (const node of document.querySelectorAll('[data-en]')) {{
      node.textContent = node.getAttribute(language === 'zh-CN' ? 'data-zh' : 'data-en');
    }}
    for (const node of document.querySelectorAll('nav[aria-label]')) {{
      node.setAttribute('aria-label', language === 'zh-CN' ? '页面目录' : 'Page navigation');
    }}
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
      target.searchParams.set('lang', language);
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
main {{ max-width:880px; margin:auto; padding:24px; }}
main > :first-child {{ margin-top:0; }}
main > :last-child {{ margin-bottom:0; }}
header {{ padding:28px; margin-bottom:32px; border-radius:var(--corner-large); background:var(--container); color:var(--on-container); }}
.eyebrow {{ margin-top:0; font-size:.875rem; line-height:1.428571; font-weight:600; letter-spacing:.04em; }}
h1 {{ font-size:2rem; line-height:1.25; font-weight:650; margin:12px 0 16px; overflow-wrap:anywhere; }}
h2 {{ overflow-wrap:anywhere; font-size:1.5rem; line-height:1.333333; font-weight:600; margin:32px 0 16px; }}
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
nav a:not(.button) {{ max-width:100%; display:inline-flex; align-items:center; min-height:48px; font-weight:600; }}
@media(max-width:400px) {{ main {{ padding:16px; }} header {{ padding:20px; }} article,section {{ padding:16px; }} }}
@media(prefers-reduced-motion:reduce) {{ .button::before {{ transition:none; }} }}
@media print {{ header,article,section {{ break-inside:avoid; }} }}
</style>
</head>
<body><main>
{introduction}
{content}
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

    # Remove only the superseded detail files owned by this generator.
    for platform in inventories:
        for pattern in (f"{platform}-license-*.html", f"{platform}-notice-*.html"):
            for obsolete in OUTPUT.glob(pattern):
                obsolete.unlink()



if __name__ == "__main__":
    main()
