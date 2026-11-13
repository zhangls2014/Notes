import { mkdir, readFile, realpath, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const webRoot = fileURLToPath(new URL('.', import.meta.url))
const defaultOrigins = {
  siteOrigin: 'https://notes.zhangls.me',
  apkOrigin: 'https://downloads.zhangls.me',
}

function httpsOrigin(value) {
  const url = new URL(value)
  requireValue(url.protocol === 'https:' && !url.username && !url.password &&
    url.pathname === '/' && !url.search && !url.hash, 'Expected an HTTPS service origin')
  return url.origin
}
const channels = {
  'android-prd-full': 'android',
  'android-prd-mainland': 'android',
  'ios-app-store': 'ios',
}
const labels = {
  added: { zh: '新增', en: 'Added' },
  improved: { zh: '优化', en: 'Improved' },
  fixed: { zh: '修复', en: 'Fixed' },
}
const platforms = { android: 'Android', ios: 'iOS' }
const versionPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/
const escape = value => value.replace(/[&<>"']/g, char => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
})[char])
const isText = value => typeof value === 'string' && value.trim().length > 0

function requireValue(condition, message) {
  if (!condition) throw new Error(message)
}

function compareNumbers(left, right) {
  const a = left.split('.').map(BigInt)
  const b = right.split('.').map(BigInt)
  for (let index = 0; index < Math.max(a.length, b.length); index++) {
    const delta = (a[index] ?? 0n) - (b[index] ?? 0n)
    if (delta !== 0n) return delta > 0n ? 1 : -1
  }
  return 0
}

function validateReleases(releases) {
  requireValue(Array.isArray(releases), 'Expected an array of release notes')
  const versions = new Set()
  for (const release of releases) {
    const { version, date, changes } = release
    requireValue(typeof version === 'string' && versionPattern.test(version) &&
      version.split('.').every(part => BigInt(part) <= 2147483647n) && !versions.has(version),
      `Invalid or duplicate release version: ${version}`)
    versions.add(version)
    requireValue(date === null || (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date) &&
      Number.isFinite(Date.parse(date)) && new Date(date).toISOString().startsWith(date)),
    `Invalid release date for ${version}`)
    requireValue(Array.isArray(changes) && changes.length > 0, `Missing changes for ${version}`)
    for (const change of changes) {
      requireValue(Object.hasOwn(labels, change.type) &&
        Array.isArray(change.platforms) && change.platforms.length > 0 &&
        new Set(change.platforms).size === change.platforms.length &&
        change.platforms.every(platform => Object.hasOwn(platforms, platform)) &&
        isText(change.text?.zh) && isText(change.text?.en), `Invalid change for ${version}`)
    }
  }
  const unpublished = releases.filter(release => release.date === null)
  requireValue(unpublished.length <= 1 && unpublished.every(draft => releases.every(release =>
    release === draft || compareNumbers(draft.version, release.version) > 0)), 'Invalid unpublished release')
}

export function validatePublishedBuilds(releases, builds, downloadOrigin = defaultOrigins.apkOrigin) {
  const apkOrigin = httpsOrigin(downloadOrigin)
  validateReleases(releases)
  requireValue(Array.isArray(builds), 'Expected an array of published builds')
  const seen = new Set()
  const previous = new Map()
  for (const entry of builds) {
    const { channel, version, build, artifactUrl } = entry
    requireValue(Object.hasOwn(channels, channel), `Unknown release channel: ${channel}`)
    requireValue(releases.some(release => release.version === version && release.date !== null),
      `Build refers to an unpublished or missing release: ${channel}/${version}`)
    const android = channels[channel] === 'android'
    const buildPattern = android ? /^[1-9]\d*$/ : /^[1-9]\d*(\.\d+){0,2}$/
    requireValue(typeof build === 'string' && buildPattern.test(build) &&
      build.split('.').every(part => BigInt(part) <= 2147483647n), `Invalid build number: ${build}`)
    const key = `${channel}/${build}`
    requireValue(!seen.has(key), `Duplicate build: ${key}`)
    seen.add(key)
    const last = previous.get(channel)
    requireValue(!last || (compareNumbers(version, last.version) >= 0 && compareNumbers(build, last.build) > 0),
      `Builds must be appended in release order: ${key}`)
    previous.set(channel, entry)

    const url = new URL(artifactUrl)
    const validArtifact = android
      ? url.origin === apkOrigin &&
        url.pathname.startsWith(`/${channel}/${build}/`) &&
        /^[\w.-]+\.apk$/.test(url.pathname.split('/').at(-1)) &&
        url.pathname.split('/').length === 4
      : url.origin === 'https://apps.apple.com' && /^\/(?:[a-z]{2}\/)?app\/(?:[\w-]+\/)?id\d+$/.test(url.pathname)
    requireValue(validArtifact && !url.username && !url.password && !url.search && !url.hash,
      `Invalid artifact URL for ${key}`)
  }
}

const bilingual = (zh, en) => `<span lang="zh-Hans" data-lang="zh">${zh}</span><span lang="en" data-lang="en">${en}</span>`
const history = bilingual('历史更新', 'Release history')
const notices = bilingual('开源声明', 'Open-source notices')
const downloads = bilingual('下载', 'Downloads')
const styles = `
:root { color-scheme: light dark; font-family: system-ui, sans-serif; line-height: 1.55 }
body { max-width: 54rem; margin: auto; padding: 1.25rem }
nav { display: flex; align-items: center; flex-wrap: wrap; gap: .75rem 1rem }
nav a:first-child { font-weight: 700; margin-inline-end: auto }
a { color: LinkText; text-underline-offset: .2em }
article { border: 1px solid GrayText; border-radius: 1rem; margin-block: 1rem; padding: 1rem 1.25rem }
h1 { margin-block: 2rem 1rem }
h2, h3 { margin-block: .5rem }
li + li { margin-top: .5rem }
small { color: GrayText }
select { font: inherit; padding: .4rem }
html[lang=en] [data-lang=zh], html[lang=zh-Hans] [data-lang=en] { display: none }
`
const controls = `<script>
const params = new URLSearchParams(location.search)
const language = document.querySelector('#language')
language.value = ['zh', 'en'].includes(params.get('lang'))
  ? params.get('lang') : (navigator.language.toLowerCase().startsWith('zh') ? 'zh' : 'en')
function setLanguage() {
  document.documentElement.lang = language.value === 'zh' ? 'zh-Hans' : 'en'
  for (const link of document.querySelectorAll('nav a, [data-notice-link]')) {
    const url = new URL(link.href)
    url.searchParams.set('lang', language.value)
    link.href = url
  }
}
setLanguage()
language.addEventListener('change', () => {
  setLanguage()
  const url = new URL(location.href)
  url.searchParams.set('lang', language.value)
  window.history.replaceState(null, '', url)
})
const platform = document.querySelector('#platform')
platform?.addEventListener('change', () => {
  for (const change of document.querySelectorAll('[data-platforms]')) {
    change.hidden = platform.value !== 'all' && !change.dataset.platforms.split(' ').includes(platform.value)
  }
  for (const section of document.querySelectorAll('article section, article')) {
    section.hidden = !section.querySelector('[data-platforms]:not([hidden])')
  }
})
</script>`

function page(title, body) {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title} · Notes</title><style>${styles}</style></head><body>
<nav aria-label="Notes"><a href="/">Notes</a><a href="/downloads/">${downloads}</a>
<a href="/releases/">${history}</a><a href="/open-source/">${notices}</a>
<select id="language" aria-label="Language / 语言"><option value="zh">简体中文</option><option value="en">English</option></select></nav>
<main>${body}</main>${controls}</body></html>
`
}

function releaseCard({ version, date, changes }, builds) {
  const groups = Object.entries(labels).map(([type, label]) => {
    const entries = changes.filter(change => change.type === type)
    if (!entries.length) return ''
    return `<section><h3>${bilingual(label.zh, label.en)}</h3><ul>${entries.map(change =>
      `<li data-platforms="${change.platforms.join(' ')}">${bilingual(escape(change.text.zh), escape(change.text.en))}
      <small>${change.platforms.map(platform => platforms[platform]).join(' / ')}</small></li>`).join('')}</ul></section>`
  }).join('')
  const archivedBuilds = builds.filter(build => build.version === version)
  const noticeSection = archivedBuilds.length
    ? `<section><h3>${notices}</h3><ul>${archivedBuilds.map(build =>
      `<li data-platforms="${channels[build.channel]}">${noticeLink(build, `${build.channel} (${build.build})`)}</li>`
    ).join('')}</ul></section>`
    : ''
  return `<article id="v${version}"><h2><a href="#v${version}">${version}</a></h2>
<time datetime="${date}">${date}</time>${groups}${noticeSection}</article>`
}

const archive = entry => `open-source/${entry.channel}/${entry.version}/${entry.build}`
const noticeLink = (entry, label) =>
  `<a data-notice-link href="/${archive(entry)}/${channels[entry.channel]}.html">${label}</a>`
const download = entry => `downloads/android/${entry.channel}/${entry.build}/`

export function buildSite(releases, builds, origins = defaultOrigins) {
  const origin = httpsOrigin(origins.siteOrigin)
  validatePublishedBuilds(releases, builds, origins.apkOrigin)
  const files = new Map()
  const current = new Map()
  for (const entry of builds) current.set(entry.channel, entry)
  files.set('index.html', page('Notes', `<h1>Notes</h1><p>${bilingual('适用于 Android 和 iOS。', 'Available for Android and iOS.')}</p>`))
  const published = releases.filter(release => release.date !== null)
    .sort((a, b) => compareNumbers(b.version, a.version))
  files.set('releases/index.html', page('Release history', `<h1>${history}</h1>
<label for="platform">${bilingual('平台', 'Platform')}</label>
<select id="platform"><option value="all">All / 全部</option><option value="android">Android</option><option value="ios">iOS</option></select>
${published.map(release => releaseCard(release, builds)).join('') || `<p>${bilingual('暂无已发布记录。', 'No published release notes yet.')}</p>`}`))
  const empty = `<p>${bilingual('暂无已发布构建。', 'No published builds yet.')}</p>`
  files.set('downloads/index.html', page('Downloads', `<h1>${downloads}</h1>${current.size ?
    `<ul>${[...current.values()].map(entry => `<li><a href="${channels[entry.channel] === 'android' ?
      `/${download(entry)}` : escape(entry.artifactUrl)}">${entry.channel} · ${entry.version}</a></li>`).join('')}</ul>` : empty}`))
  files.set('open-source/index.html', page('Open-source notices', `<h1>${notices}</h1>${builds.length ?
    `<ul>${[...builds].reverse().map(entry =>
      `<li>${noticeLink(entry, `${entry.channel} · ${entry.version} (${entry.build})`)}</li>`).join('')}</ul>` : empty}`))
  for (const entry of builds) {
    if (channels[entry.channel] !== 'android') continue
    files.set(`${download(entry)}index.html`, page('Android download', `<h1>Notes ${entry.version}</h1>
<p>${entry.channel} · ${entry.build}</p><a href="${escape(entry.artifactUrl)}">${bilingual('下载 APK', 'Download APK')}</a>
<p><a href="/releases/#v${entry.version}">${history}</a></p>`))
  }
  for (const { channel, version, build, artifactUrl } of current.values()) {
    const updateUrl = channels[channel] === 'android' ? `${origin}/${download({ channel, build })}` : artifactUrl
    files.set(`updates/v1/${channel}.json`, JSON.stringify({ channel, version, build, updateUrl }) + '\n')
  }
  files.set('404.html', page('Page not found', `<h1>${bilingual('找不到此页面', 'Page not found')}</h1>`))
  files.set('_headers', '/updates/v1/*\n  Cache-Control: no-cache\n/.well-known/apple-app-site-association\n  Content-Type: application/json\n')
  return files
}

function addAssociations(files) {
  const { NOTES_ANDROID_SIGNING_SHA256: fingerprint, NOTES_IOS_APP_ID: iosId, CF_PAGES_BRANCH } = process.env
  if (CF_PAGES_BRANCH === 'pages-production' || fingerprint || iosId) {
    requireValue(typeof fingerprint === 'string' && /^(?:[\dA-F]{2}:){31}[\dA-F]{2}$/i.test(fingerprint),
      'Set NOTES_ANDROID_SIGNING_SHA256 to the release signing certificate fingerprint')
    requireValue(typeof iosId === 'string' && /^[A-Z0-9]{10}\.[\w.-]+$/.test(iosId),
      'Set NOTES_IOS_APP_ID to the full signed iOS application identifier')
    files.set('.well-known/assetlinks.json', JSON.stringify([{
      relation: ['delegate_permission/common.handle_all_urls'],
      target: { namespace: 'android_app', package_name: 'me.zhangls.notes', sha256_cert_fingerprints: [fingerprint] },
    }]))
    files.set('.well-known/apple-app-site-association', JSON.stringify({
      applinks: { details: [{ appIDs: [iosId], components: [{ '/': '/home' }, { '/': '/email' }] }] },
    }))
  }
}

async function main() {
  const releases = JSON.parse(await readFile(join(webRoot,
    '../feature/about/src/commonMain/composeResources/files/releases.json'), 'utf8'))
  const builds = JSON.parse(await readFile(process.env.NOTES_PUBLISHED_BUILDS_FILE ?? join(webRoot, 'published-builds.json'), 'utf8'))
  const files = buildSite(releases, builds, {
    siteOrigin: process.env.NOTES_SITE_BASE_URL ?? defaultOrigins.siteOrigin,
    apkOrigin: process.env.NOTES_APK_BASE_URL ?? defaultOrigins.apkOrigin,
  })
  addAssociations(files)
  for (const entry of builds) {
    const platform = channels[entry.channel]
    for (const file of [`${platform}.html`, `${platform}-licenses.html`]) {
      const input = join(webRoot, 'notice-archives', entry.channel, entry.version, entry.build, file)
      const html = await readFile(input)
      requireValue(html.length > 0 && html.length <= 25 * 1024 * 1024, `Invalid Pages asset: ${input}`)
      files.set(`${archive(entry)}/${file}`, html)
    }
  }
  for (const [path, content] of files) {
    const output = join(webRoot, 'dist', path)
    await mkdir(dirname(output), { recursive: true })
    await writeFile(output, content)
  }
}

if (process.argv[1] && await realpath(process.argv[1]) === fileURLToPath(import.meta.url)) await main()
