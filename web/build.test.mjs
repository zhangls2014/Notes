import assert from 'node:assert/strict'
import { test } from 'node:test'
import { buildSite, validatePublishedBuilds } from './build.mjs'

test('public history is bilingual, ordered and excludes unpublished records', () => {
  const release = (version, date) => ({
    version, date, changes: [{ type: 'fixed', platforms: ['android'], text: { zh: '修复', en: '<Fixed>' } }],
  })
  const releases = [release('1.9.0', '2026-10-01'), release('1.10.0', '2026-11-01'), release('2.0.0', null)]
  const site = buildSite(releases, [])
  const html = site.get('releases/index.html')
  assert.ok(html.indexOf('id="v1.10.0"') < html.indexOf('id="v1.9.0"'))
  assert.match(html, /修复/)
  assert.match(html, /&lt;Fixed&gt;/)
  assert.doesNotMatch(html, /2\.0\.0/)
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
  assert.deepEqual(JSON.parse(site.get('updates/v1/android-prd-full.json')), {
    channel: build.channel, version: build.version, build: build.build,
    updateUrl: 'https://notes.zhangls.me/downloads/android/android-prd-full/5/',
  })
  assert.throws(() => validatePublishedBuilds([{ ...release, date: null }], [build]))
  assert.throws(() => validatePublishedBuilds([release], [{ ...build, artifactUrl: 'https://example.com/Notes.apk' }]))
})

test('release history links to each archived build of the matching version', () => {
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
      artifactUrl: 'https://downloads.zhangls.me/android-prd-mainland/5/Notes.apk',
    },
    {
      channel: 'ios-app-store', version: '1.2.0', build: '6',
      artifactUrl: 'https://apps.apple.com/app/id123',
    },
  ]
  const html = buildSite(releases, builds).get('releases/index.html')
  const article = version => html.split(`<article id="v${version}">`)[1].split('</article>')[0]
  assert.match(article('1.1.0'), /href="\/open-source\/android-prd-full\/1\.1\.0\/4\/android\.html"/)
  assert.doesNotMatch(article('1.1.0'), /open-source\/.*\/1\.2\.0\//)
  assert.match(article('1.2.0'), /href="\/open-source\/android-prd-full\/1\.2\.0\/5\/android\.html"/)
  assert.match(article('1.2.0'), /href="\/open-source\/android-prd-mainland\/1\.2\.0\/5\/android\.html"/)
  assert.match(article('1.2.0'), /href="\/open-source\/ios-app-store\/1\.2\.0\/6\/ios\.html"/)
  assert.doesNotMatch(article('1.3.0'), /href="\/open-source\//)
})
