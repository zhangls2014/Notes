#!/usr/bin/env python3
"""Generate Compose vectors from pinned Google Material Symbols Rounded SVGs."""
import argparse
import hashlib
import json
import re
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[2]
ASSETS = Path(__file__).resolve().parent
OUTPUT = ROOT / 'core/theme/src/commonMain/kotlin/me/zhangls/theme/icon'


METHODS = {
    'M': ('moveTo', 2), 'L': ('lineTo', 2), 'H': ('horizontalLineTo', 1),
    'V': ('verticalLineTo', 1), 'Q': ('quadTo', 4), 'T': ('reflectiveQuadTo', 2),
    'C': ('curveTo', 6), 'S': ('reflectiveCurveTo', 4),
}
TOKEN = re.compile(r'[A-Za-z]|[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?')


def path_calls(data):
    if re.sub(TOKEN, '', data).strip(' ,\t\r\n'):
        raise ValueError('Invalid SVG path data')
    tokens = TOKEN.findall(data)
    index = 0
    command = None
    first = True
    calls = []
    while index < len(tokens):
        if tokens[index].isalpha():
            command = tokens[index]
            index += 1
            if command.upper() == 'Z':
                calls.append('close()')
                command = None
                continue
        if command is None or command.upper() not in METHODS:
            raise ValueError(f'Unsupported SVG command: {command}')
        method, count = METHODS[command.upper()]
        if index + count > len(tokens) or any(t.isalpha() for t in tokens[index:index + count]):
            raise ValueError('Incomplete SVG command')
        values = [float(t) for t in tokens[index:index + count]]
        index += count
        relative = command.islower()
        # Translate the negative SVG viewport directly into the original positive viewport.
        if first:
            if command.upper() != 'M':
                raise ValueError('SVG path must start with a move')
            relative = False
        if not relative:
            if command.upper() == 'V':
                values[0] += 960
            elif command.upper() != 'H':
                for i in range(1, count, 2):
                    values[i] += 960
        args = ', '.join(f'{v:g}f' for v in values)
        calls.append(f"{method}{'Relative' if relative else ''}({args})")
        first = False
        if command == 'M':
            command = 'L'
        elif command == 'm':
            command = 'l'
    return calls


def render(name, svg):
    root = ET.fromstring(svg)
    if root.attrib['viewBox'] != '0 -960 960 960':
        raise ValueError(f'Unexpected viewport for {name}')
    paths = root.findall('{http://www.w3.org/2000/svg}path')
    if len(list(root)) != len(paths) or not paths:
        raise ValueError(f'Unsupported SVG structure for {name}')
    calls = []
    for path in paths:
        if set(path.attrib) != {'d'}:
            raise ValueError(f'Unsupported path attributes for {name}')
        body = '\n'.join('        ' + c for c in path_calls(path.attrib['d']))
        calls.append('      path(fill = SolidColor(Color.Black)) {\n' + body + '\n      }')
    return f"""package me.zhangls.theme.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


val Icons.Rounded.{name}: ImageVector
  get() {{
    if (_{name} != null) {{
      return _{name}!!
    }}
    _{name} = ImageVector.Builder(
      name = "Rounded.{name}",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {{
{chr(10).join(calls)}
    }}.build()

    return _{name}!!
  }}

@Suppress("ObjectPropertyName")
private var _{name}: ImageVector? = null

@Preview(name = "{name}", showBackground = true)
@Composable
private fun {name}Preview() {{
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {{
    Icon(imageVector = Icons.Rounded.{name}, contentDescription = null)
  }}
}}
"""


def source_key(name, symbol):
    fill = manifest.get('fill_overrides', {}).get(name, manifest['fill'])
    if fill not in (0, 1):
        raise ValueError(f'Unsupported fill: {fill}')
    return symbol + ('_fill1' if fill else '')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--download', action='store_true', help='Fetch SVGs from the pinned official revision')
    parser.add_argument('--check', action='store_true', help='Verify hashes and generated output without writing')
    args = parser.parse_args()
    if args.download and args.check:
        parser.error('--download and --check are mutually exclusive')
    manifest = json.loads((ASSETS / 'manifest.json').read_text())
    if (manifest['style'], manifest['fill'], manifest['weight'], manifest['grade'], manifest['optical_size']) != ('materialsymbolsrounded', 0, 400, 0, 24):
        raise ValueError('Icon style parameters violate the project constraint')
    if args.download:
        (ASSETS / 'svg').mkdir(exist_ok=True)
        def download(item):
            name, symbol = item
            key = source_key(name, symbol)
            url = (f"https://raw.githubusercontent.com/google/material-design-icons/{manifest['revision']}/"
                   f"symbols/web/{symbol}/{manifest['style']}/{key}_24px.svg")
            req = urllib.request.Request(url, headers={'User-Agent': 'Notes-icon-generator'})
            data = urllib.request.urlopen(req, timeout=60).read()
            render(name, data)
            return key, data
        for symbol, data in ThreadPoolExecutor(max_workers=6).map(download, manifest['icons'].items()):
            (ASSETS / 'svg' / (symbol + '.svg')).write_bytes(data)
            manifest.setdefault('sha256', {})[symbol] = hashlib.sha256(data).hexdigest()
        (ASSETS / 'manifest.json').write_text(json.dumps(manifest, indent=2, sort_keys=True) + '\n')
    for name, symbol in manifest['icons'].items():
        key = source_key(name, symbol)
        data = (ASSETS / 'svg' / (key + '.svg')).read_bytes()
        if hashlib.sha256(data).hexdigest() != manifest['sha256'][key]:
            raise ValueError(f'Source hash mismatch: {symbol}')
        content = render(name, data)
        target = OUTPUT / (name + '.kt')
        if args.check:
            if target.read_text() != content:
                raise ValueError(f'Generated output mismatch: {target}')
        else:
            target.write_text(content)
    expected = {name + '.kt' for name in manifest['icons']} | {'Icons.kt'}
    actual = {p.name for p in OUTPUT.glob('*.kt')}
    if actual != expected:
        raise ValueError(f'Unexpected icon files: {actual ^ expected}')
    print(f"Verified {len(manifest['icons'])} Rounded icons (default FILL=0; explicit overrides supported) at {manifest['revision']}")
