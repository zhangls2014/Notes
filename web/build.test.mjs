import assert from 'node:assert/strict'
import { test } from 'node:test'
import { runInNewContext } from 'node:vm'
import { buildSite, validatePublishedBuilds } from './build.mjs'

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
  assert.match(customSite.get('downloads/android/android-prd-full/5/index.html'), /https:\/\/apk\.example\.test\//)
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
  assert.doesNotMatch(html, /href="\/downloads\//)
  const index = site.get('open-source/index.html')
  assert.match(index, /href="\/open-source\/android\.html"/)
  assert.match(index, /href="\/open-source\/ios\.html"/)
  assert.doesNotMatch(index, /1\.2\.0|android-prd|ios-app-store/)
  assert.equal(index, buildSite(releases, []).get('open-source/index.html'))
  assert.ok(site.has('downloads/android/android-prd-full/5/index.html'))
  assert.ok(site.has('updates/v1/ios-app-store.json'))
})

test('only previously published notice URLs forward to the current page', () => {
  const site = buildSite([], [])
  const legacyPaths = [...site.keys()].filter(path => /^open-source\/[^/]+\//.test(path))
  assert.deepEqual(legacyPaths.sort(), [
    'open-source/android-prd-full/1.1.2/5/android-licenses.html',
    'open-source/android-prd-full/1.1.2/5/android.html',
    'open-source/android-prd-mainland/1.1.2/5/android-licenses.html',
    'open-source/android-prd-mainland/1.1.2/5/android.html',
  ])
  for (const path of legacyPaths) {
    const target = '/open-source/' + path.split('/').at(-1)
    const html = site.get(path)
    assert.ok(html.includes(`href="${target}"`))
    const suffix = '?lang=zh&fontScale=2#license-apache-2.0'
    let redirected
    runInNewContext(html.match(/<script>(.*?)<\/script>/s)[1], {
      location: { search: '?lang=zh&fontScale=2', hash: '#license-apache-2.0',
        replace: url => { redirected = url } },
    })
    assert.equal(redirected, target + suffix)
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
