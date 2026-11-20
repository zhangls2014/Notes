import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import { runInNewContext } from 'node:vm'
import { buildNoticeFiles, buildSite, validatePublishedBuilds } from './build.mjs'
import { buildNoticeData } from './notice-data.mjs'

test('the app cache manifest identifies only Skiko and the content-versioned MiSans', async () => {
  const { buildWebAssetManifest } = await import('./build.mjs')
  const skiko = Buffer.from('skiko')
  const font = Buffer.from('misans')
  const digest = bytes => createHash('sha256').update(bytes).digest('hex')
  const manifest = buildWebAssetManifest('0123456789abcdef0123.wasm', skiko, font)
  assert.deepEqual(manifest, {
    skiko: { path: '/0123456789abcdef0123.wasm', size: skiko.length, sha256: digest(skiko) },
    miSans: { path: `/web-assets/misans-${digest(font).slice(0, 20)}.otf`, size: font.length, sha256: digest(font) },
  })
  assert.throws(() => buildWebAssetManifest('../skiko.wasm', skiko, font))
})

test('content-versioned resources reuse unchanged URLs across site builds', async () => {
  const { versionWebsiteAssets } = await import('./build.mjs')
  const iconPath = 'composeResources/me.zhangls.web.resources/drawable/notes_icon.svg'
  const fontPath = 'composeResources/me.zhangls.web.resources/font/misans_regular.otf'
  const build = (script, icon) => {
    const files = buildSite([], [])
    const results = versionWebsiteAssets(
      files,
      Buffer.from(script),
      Buffer.from('source map'),
      new Map([
        [iconPath, Buffer.from(icon)],
        [fontPath, Buffer.from('font')],
      ]),
      '/web-assets/misans-test.otf',
    )
    return { files, ...results }
  }
  const first = build('one', 'icon')
  const second = build('two', 'icon')
  const third = build('two', 'changed icon')
  assert.notEqual(first.script, second.script)
  assert.equal(second.script, third.script)
  assert.equal(first.paths[iconPath], second.paths[iconPath])
  assert.notEqual(second.paths[iconPath], third.paths[iconPath])
  assert.equal(third.paths[fontPath], '/web-assets/misans-test.otf')
  assert.ok(third.files.get('index.html').includes(`src="${third.script}"`))
  assert.ok(third.files.get('index.html').includes(`href="${third.paths[iconPath]}"`))
  assert.ok(third.files.has(third.script.slice(1)))
  assert.ok(third.files.has(third.paths[iconPath].slice(1)))
  assert.match(third.files.get('_headers'), /\/\n  Cache-Control: no-cache/)
  assert.match(third.files.get('_headers'), /\/assets\/\*\n  Cache-Control: public, max-age=31536000, immutable/)
  assert.match(third.files.get('_headers'), /\/notes-\*\.js\n  Cache-Control: public, max-age=31536000, immutable/)
})

test('public history is bilingual, ordered and excludes unpublished records', () => {
  const release = (version, date) => ({
    version, date, changes: [{ type: 'fixed', platforms: ['android'], text: { zh: '修复', en: '<Fixed>' } }],
  })
  const older = release('1.9.0', '2026-10-01')
  const latest = release('1.10.0', '2026-11-01')
  const site = buildSite([older, latest, release('2.0.0', null)], [])
  assert.deepEqual(JSON.parse(site.get('releases/published.json')), [latest, older])
})

test('public routes start Compose with site data and no static page UI', () => {
  const files = buildSite([], [])
  for (const path of ['index.html', 'downloads/index.html', 'releases/index.html', 'open-source/index.html']) {
    const html = files.get(path)
    assert.match(html, /<body><main id="notes-app" style="position:fixed;inset:0" data-page-frame="pending"><\/main>/, path)
    assert.match(html, /<script src="\/notes\.js" defer><\/script>/, path)
    assert.match(html, /<script type="application\/json" id="notes-data">\{"builds":\[\]\}<\/script><\/body>/, path)
  }
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
  assert.throws(() => buildSite([release], [build], origins))
  assert.throws(() => buildSite([release], [custom], { ...origins, siteOrigin: 'http://site.example.test' }))
  assert.throws(() => validatePublishedBuilds([{ ...release, date: null }], [build]))
  assert.throws(() => validatePublishedBuilds([release], [{ ...build, artifactUrl: 'https://example.com/Notes.apk' }]))
})

test('download data carries builds across public routes and keeps legacy update URLs', () => {
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
  for (const path of ['index.html', 'downloads/index.html', 'releases/index.html', 'open-source/index.html']) {
    const data = site.get(path).match(/id="notes-data">(.*?)<\/script>/s)?.[1]
    assert.ok(data, path)
    assert.deepEqual(JSON.parse(data), { builds })
  }
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
    assert.match(html, /<main id="notes-app" style="position:fixed;inset:0" data-page-frame="pending"><\/main>/)
    assert.doesNotMatch(html, /data-notice-frame/)
    assert.doesNotMatch(html, /notice\.css|rel="stylesheet"/)
    assert.match(html, /<script src="\/notes\.js" defer><\/script>/)
    assert.doesNotMatch(html, /<(article|header|template|pre|h1)\b/)
    const data = JSON.parse(payload)
    assert.ok(path.endsWith('-licenses.html') ? Object.keys(data.declarations).length : data.categories.length)
    assert.deepEqual(Object.keys(data), [path.endsWith('-licenses.html') ? 'declarations' : 'categories'])
  }
})

test('HTML initializes the app theme before the Wasm bootstrap', () => {
  const files = buildSite([], [])
  for (const [path, html] of files) {
    if (!path.endsWith('.html')) continue
    const script = html.match(/<script id="notes-initial-theme">([\s\S]*?)<\/script>/)?.[1]
    assert.ok(script, path)
    assert.ok(html.indexOf('id="notes-initial-theme"') < html.indexOf('src="/notes.js"'), path)
    const resolve = (search, saved, systemDark, storageFails = false) => {
      const style = {}
      runInNewContext(script, {
        URLSearchParams, location: { search }, document: { documentElement: { style } },
        localStorage: { getItem: () => { if (storageFails) throw Error('disabled'); return saved } },
        matchMedia: () => ({ matches: systemDark }),
      })
      return style
    }
    assert.deepEqual(resolve('?theme=dark&bg=182030', 'LIGHT', false), {
      colorScheme: 'dark', backgroundColor: '#182030',
    })
    assert.equal(resolve('?theme=light', 'DARK', true).colorScheme, 'light')
    assert.equal(resolve('', 'DARK', false).backgroundColor, '#12140e')
    assert.equal(resolve('', 'FOLLOW_SYSTEM', true).colorScheme, 'dark')
    assert.equal(resolve('', null, false, true).backgroundColor, '#f9faef')
    assert.equal(resolve('?theme=dark&bg=invalid', null, false).backgroundColor, '#12140e')
  }
})


test('all website launch documents start with a pending first frame', async () => {
  const files = new Map([...buildSite([], []), ...await buildNoticeFiles()])
  for (const [path, html] of files) {
    if (path.endsWith('.html')) assert.match(html, /data-page-frame="pending"/, path)
  }
})

test('the page reports ready only after drawing and subsequent animation frames', async () => {
  const source = await readFile(new URL('./src/wasmJsMain/kotlin/me/zhangls/web/Browser.kt', import.meta.url), 'utf8')
  const script = source.match(/internal fun reportPageDrawn\(\): Unit = js\("""([\s\S]*?)"""\)/)?.[1]
  assert.ok(script)
  let frame = 'pending'
  const callbacks = []
  const container = { getAttribute: () => frame, setAttribute: (_, value) => { frame = value } }
  const context = {
    document: { getElementById: () => container },
    requestAnimationFrame: callback => callbacks.push(callback),
  }
  runInNewContext(script, context)
  assert.equal(frame, 'drawn')
  runInNewContext(script, context)
  assert.equal(callbacks.length, 1)
  callbacks.shift()()
  assert.equal(frame, 'drawn')
  callbacks.shift()()
  assert.equal(frame, 'ready')
})

test('all own launch documents load the standalone viewport bridge before Wasm', async () => {
  const files = buildSite([], [])
  const notices = await buildNoticeFiles()
  const bridge = [...files.keys()].find(name => /^device-viewport-[a-f0-9]+\.js$/.test(name))
  assert.ok(bridge, 'content-versioned viewport bridge must be distributed')
  for (const [name, html] of [...files, ...notices]) {
    if (!name.endsWith('.html')) continue
    assert.ok(html.includes(`src="/${bridge}"`), name)
    assert.ok(html.indexOf(`src="/${bridge}"`) < html.indexOf('src="/notes.js"'), name)
  }
})
