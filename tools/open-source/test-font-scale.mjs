import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const paletteCases = [
  [null, {}],
  [{ bg: 'faf9f6', card: 'ffffff', fg: '252621', muted: '62665c', line: 'dde1d5', accent: '3e6839' }],
  [{ bg: '151713', card: '1e211b', fg: 'e4e7dd', muted: 'b4baaa', line: '373e31', accent: 'aad395' }],
  [{ bg: '123456', card: '#ffffff', fg: 'red', muted: '12345', line: '1234567', accent: 'red;display:none' }, { bg: '123456' }],
];

for (const page of ['android', 'ios', 'android-licenses', 'ios-licenses']) {
  const html = readFileSync(new URL(`../../web/open-source/${page}.html`, import.meta.url), 'utf8');
  const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
  assert.ok(script, `${page} must include the font scale script`);
  assert.ok(script.includes("get('theme')"), 'Page must read the app theme');
  assert.ok(html.indexOf('<script>') < html.indexOf('<body>'), 'Apply the scale before body rendering');
  const scaleLinks = [...html.matchAll(/<a data-font-scale class="button" href="([^"]+)"|<a data-font-scale href="([^"]+)"/g)];
  assert.ok(scaleLinks.length > 0, 'List and detail navigation must retain the font scale');
  const anchors = [...html.matchAll(/<a\b([^>]+)>/g)];
  for (const [, attributes] of anchors) {
    const href = attributes.match(/href="([^"]+)"/)?.[1];
    if (/^https?:/.test(href)) assert.ok(!attributes.includes('data-font-scale'), 'Do not modify external project links');
    if (/^(android|ios)(-licenses)?\.html/.test(href)) assert.ok(attributes.includes('data-font-scale'), 'Every local page link retains the font scale');
  }
  for (const base of ['https://example.com/notices/', 'file:///android_asset/notices/', 'file:///app/resources/']) {
    for (const theme of [null, 'light', 'dark', 'invalid']) {
      for (const [query, expectedScale] of [
        ['', 1], ['?fontScale=1', 1], ['?fontScale=1.5', 1.5], ['?fontScale=2', 2],
        ['?fontScale=', 1], ['?fontScale=NaN', 1], ['?fontScale=Infinity', 1],
        ['?fontScale=-1', 1], ['?fontScale=0.5', 1], ['?fontScale=3', 1],
      ]) {
        for (const [palette, expected = palette] of paletteCases) {
          const location = new URL(`${base}${page}.html${query}`);
          if (theme !== null) location.searchParams.set('theme', theme);
          for (const [name, value] of Object.entries(palette ?? {})) location.searchParams.set(name, value);
          const root = { style: { setProperty(name, value) { this[name] = value; } } };
          root.dataset = {};
          let onReady;
          const links = scaleLinks.map(match => {
            const href = match[1] ?? match[2];
            return { href, getAttribute: () => href };
          });
          const extra = { href: 'ios-licenses.html?lang=zh#license-MIT', getAttribute() { return this.href; } };
          const external = { href: 'https://external.example/project' };
          const document = {
            documentElement: root,
            addEventListener(event, callback) { assert.equal(event, 'DOMContentLoaded'); onReady = callback; },
            querySelectorAll(selector) {
              assert.equal(selector, 'a[data-font-scale]');
              return [...links, extra];
            },
          };
          vm.runInNewContext(script, { document, location, URL, URLSearchParams });
          assert.equal(root.style.fontSize, `${expectedScale * 100}%`);
          assert.equal(root.style['--app-font-scale'], String(expectedScale));
          const expectedTheme = theme === 'light' || theme === 'dark' ? theme : undefined;
          assert.equal(root.dataset.theme, expectedTheme);
          for (const name of ['bg', 'card', 'fg', 'muted', 'line', 'accent']) {
            assert.equal(root.style[`--${name}`], expected[name] ? `#${expected[name]}` : undefined);
          }
          onReady();
          for (const link of [...links, extra]) {
            const target = new URL(link.href, location);
            assert.equal(target.searchParams.get('fontScale'), String(expectedScale));
            assert.equal(target.searchParams.get('theme'), expectedTheme ?? null);
            assert.equal(target.origin, location.origin);
            for (const name of ['bg', 'card', 'fg', 'muted', 'line', 'accent']) {
              assert.equal(target.searchParams.get(name), expected[name] ?? null);
            }
          }
          assert.equal(new URL(extra.href).searchParams.get('lang'), 'zh');
          assert.equal(new URL(extra.href).hash, '#license-MIT');
          assert.equal(external.href, 'https://external.example/project');
        }
      }
    }
  }
}
console.log('Appearance and navigation: 4 pages × 10 scales × 4 themes × 3 origins × 4 palettes passed');
