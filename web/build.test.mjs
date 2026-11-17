import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import { buildNoticeFiles, buildSite, validatePublishedBuilds } from './build.mjs'
import { buildNoticeData } from './notice-data.mjs'

test('public history is bilingual, ordered and excludes unpublished records', () => {
  const release = (version, date) => ({
    version, date, changes: [{ type: 'fixed', platforms: ['android'], text: { zh: '修复', en: '<Fixed>' } }],
  })
  const releases = [release('1.9.0', '2026-10-01'), release('1.10.0', '2026-11-01'), release('2.0.0', null)]
  const site = buildSite(releases, [])
  const html = site.get('releases/index.html')
  assert.ok(html.indexOf('id="v1.10.0"') < html.indexOf('id="v1.9.0"'))
  assert.match(html, /<title>Release notes/)
  assert.match(html, /更新日志/)
  assert.match(html, /<option value="android">Android<\/option><option value="ios">iOS<\/option>/)
  assert.doesNotMatch(html, /<option value="all"/)
  assert.match(html, /修复/)
  assert.match(html, /&lt;Fixed&gt;/)
  assert.doesNotMatch(html, /2\.0\.0/)
  assert.deepEqual(JSON.parse(site.get('releases/published.json')).map(item => item.version), ['1.10.0', '1.9.0'])
})

test('a manifest must refer to a published release and an approved artifact host', () => {
  const release = {
    version: '1.10.0', date: '2026-11-01',
    changes: [{ type: 'fixed', platforms: ['android'], text: { zh: '修复', en: 'Fixed' } }],
  }
  const build = {
    channel: 'android-prd-full', version: release.version, build: '5',
    artifactUrl: 'https://downloads.zhangls.me/android-prd-full/5/Notes.apk',
  }
  const site = buildSite([release], [build])
  assert.equal(site.has('updates/v1/android-prd-full.json'), false)
  assert.deepEqual(JSON.parse(site.get('updates/v1/android-prd-global.json')), {
    channel: 'android-prd-global', version: build.version, build: build.build,
    updateUrl: 'https://notes.zhangls.me/releases/#v1.10.0',
  })
  assert.deepEqual(JSON.parse(site.get('releases/published.json')), [release])
  const origins = { siteOrigin: 'https://site.example.test/', apkOrigin: 'https://apk.example.test/' }
  const custom = { ...build, artifactUrl: 'https://apk.example.test/android-prd-full/5/Notes.apk' }
  const customSite = buildSite([release], [custom], origins)
  assert.equal(JSON.parse(customSite.get('updates/v1/android-prd-global.json')).updateUrl,
    'https://site.example.test/releases/#v1.10.0')
  assert.ok(customSite.get('downloads/index.html').includes(`href="${custom.artifactUrl}"`))
  assert.throws(() => buildSite([release], [build], origins))
  assert.throws(() => buildSite([release], [custom], { ...origins, siteOrigin: 'http://site.example.test' }))
  assert.throws(() => validatePublishedBuilds([{ ...release, date: null }], [build]))
  assert.throws(() => validatePublishedBuilds([release], [{ ...build, artifactUrl: 'https://example.com/Notes.apk' }]))
})

test('release history and the notice index link to shared notices', () => {
  const releases = ['1.1.0', '1.2.0', '1.3.0'].map(version => ({
    version, date: '2026-11-01',
    changes: [{ type: 'added', platforms: ['android', 'ios'], text: { zh: '新增', en: 'Added' } }],
  }))
  const builds = [
    {
      channel: 'android-prd-full', version: '1.1.0', build: '4',
      artifactUrl: 'https://downloads.zhangls.me/android-prd-full/4/Notes.apk',
    },
    {
      channel: 'android-prd-full', version: '1.2.0', build: '5',
      artifactUrl: 'https://downloads.zhangls.me/android-prd-full/5/Notes.apk',
    },
    {
      channel: 'android-prd-mainland', version: '1.2.0', build: '5',
      artifactUrl: 'https://downloads.zhangls.me/android-prd-mainland/5/Notes-1.2.0-mainland.apk',
    },
    {
      channel: 'ios-app-store', version: '1.2.0', build: '6',
      artifactUrl: 'https://apps.apple.com/app/id123',
    },
  ]
  const site = buildSite(releases, builds)
  const html = site.get('releases/index.html')
  assert.deepEqual([...html.matchAll(/href="(\/open-source\/[^"]*)"/g)].map(match => match[1]), ['/open-source/'])
  assert.match(html, /href="\/downloads\/"/)
  const index = site.get('open-source/index.html')
  assert.match(index, /href="\/open-source\/android\.html"/)
  assert.match(index, /href="\/open-source\/ios\.html"/)
  assert.doesNotMatch(index, /1\.2\.0|android-prd|ios-app-store/)
  assert.equal(index, buildSite(releases, []).get('open-source/index.html'))
  const downloadLinks = [...site.get('downloads/index.html').matchAll(/<li><a href="([^"]+)"/g)]
    .map(match => match[1])
  assert.deepEqual(downloadLinks, builds.slice(1).map(build => build.artifactUrl))
  assert.equal([...site.keys()].some(path => path.startsWith('downloads/android/')), false)
  assert.equal(JSON.parse(site.get('updates/v1/android-prd-mainland.json')).updateUrl,
    'https://notes.zhangls.me/downloads/android/android-prd-mainland/5/')
  assert.ok(site.has('updates/v1/ios-app-store.json'))
})

test('the site does not generate versioned notice redirects', () => {
  const site = buildSite([], [])
  const legacyPaths = [...site.keys()].filter(path => /^open-source\/[^/]+\//.test(path))
  assert.deepEqual(legacyPaths, [])
})

test('the 404 document starts the shared Compose app without static page UI', () => {
  const html = buildSite([], []).get('404.html')
  assert.match(html, /<main id="notes-app"[^>]*data-not-found/)
  assert.match(html, /<script src="\/notes\.js" defer><\/script>/)
  assert.doesNotMatch(html, /<(nav|h1|style)\b/)
})

test('every website document links to the shared SVG icon', async () => {
  const htmlFiles = [...buildSite([], []), ...await buildNoticeFiles()].filter(([path]) => path.endsWith('.html'))
  for (const [path, html] of htmlFiles) {
    assert.match(html, /<link rel="icon" type="image\/svg\+xml" href="\/composeResources\/me\.zhangls\.web\.resources\/drawable\/notes_icon\.svg">/,
      path + ' must use the shared SVG')
  }
})

test('global can announce a release without distributing an APK', () => {
  const release = {
    version: '1.2.0', date: '2026-11-01',
    changes: [{ type: 'fixed', platforms: ['android'], text: { zh: '修复', en: 'Fixed' } }],
  }
  const build = { channel: 'android-prd-global', version: release.version, build: '6' }
  const site = buildSite([release], [build])
  assert.equal(JSON.parse(site.get('updates/v1/android-prd-global.json')).build, '6')
  assert.equal(site.has('downloads/android/android-prd-global/6/index.html'), false)
  assert.throws(() => buildSite([release], [{
    ...build, channel: 'android-prd-mainland',
    artifactUrl: 'https://downloads.zhangls.me/android-prd-mainland/6/wrong.apk',
  }]))
})

test('published notice payloads include every platform component and its declarations', async () => {
  const files = await buildNoticeFiles()
  const inventory = JSON.parse(await readFile(new URL('./open-source/dependencies.json', import.meta.url), 'utf8'))
  assert.equal(files.size, 4)
  for (const platform of ['android', 'ios']) {
    const payload = path => JSON.parse(files.get(path).match(/id="notes-notice-data">(.*?)<\/script>/s)[1])
    const data = {
      ...payload(`open-source/${platform}.html`),
      ...payload(`open-source/${platform}-licenses.html`),
    }
    const projects = data.categories.flatMap(category => category.projects)
    assert.deepEqual(projects.flatMap(project => project.modules.map(module => module.coordinate)).sort(),
      inventory[platform].map(module => module.coordinate).sort())
    for (const project of projects) {
      for (const id of project.declarations) {
        assert.ok(data.declarations[id].length > 0)
      }
    }
    assert.deepEqual(new Set(Object.values(data.declarations).flat()),
      new Set([
        ...await Promise.all([...new Set(inventory[platform].flatMap(module => module.licenses))]
          .map(async id => (await readFile(new URL('./open-source/licenses/' + id + '.txt', import.meta.url), 'utf8')).replace(/\r\n?/g, '\n'))),
        ...inventory[platform].flatMap(module => module.notices.map(key => inventory.noticeTexts[key])),
      ]))
  }
})

test('notice projects group repository aliases and keep unrelated namespaces separate', async () => {
  const module = (coordinate, url, notices = []) => ({ coordinate, name: coordinate, url, notices, licenses: ['MIT'] })
  const aliases = [
    module('androidx.core:core:1', 'https://github.com/Example/Core.git', ['a']),
    module('androidx.core:ktx:1', 'https://github.com/example/core/tree/main', ['a', 'b']),
  ]
  const others = [
    module('androidxother:ui:1', 'https://github.com/Example/Core'),
    module('example:ui:1', 'https://example.test/ui'),
    module('example:util:1', 'https://example.test/util'),
  ]
  const { android, ios } = await buildNoticeData(
    { android: [...aliases, ...others], ios: [], noticeTexts: { a: '© A', b: '© B' } },
    async () => 'MIT text',
  )
  assert.deepEqual(android.categories.map(category => category.projects.length), [1, 3])
  const [series] = android.categories[0].projects
  assert.equal(series.modules.length, 2)
  assert.deepEqual(series.declarations, ['license-MIT', 'notice-3452112adf48fb98'])
  assert.deepEqual(android.declarations[series.declarations[1]], ['© A', '© B'])
  assert.deepEqual(ios, { categories: [], declarations: {} })
})

test('notice routes publish a Compose host and JSON without server-rendered page UI', async () => {
  for (const [path, html] of await buildNoticeFiles()) {
    const payload = html.match(/<script type="application\/json" id="notes-notice-data">(.*?)<\/script>/s)?.[1]
    assert.ok(payload, path + ' must provide Compose notice data')
    assert.match(html, /<main id="notes-app" style="position:fixed;inset:0"><\/main>/)
    assert.doesNotMatch(html, /notice\.css|rel="stylesheet"/)
    assert.match(html, /<script src="\/notes\.js" defer><\/script>/)
    assert.doesNotMatch(html, /<(article|header|template|pre|h1)\b/)
    const data = JSON.parse(payload)
    assert.ok(path.endsWith('-licenses.html') ? Object.keys(data.declarations).length : data.categories.length)
    assert.deepEqual(Object.keys(data), [path.endsWith('-licenses.html') ? 'declarations' : 'categories'])
  }
})
