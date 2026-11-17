import { cp, mkdir, readFile, realpath, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadNoticeData } from './notice-data.mjs'

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
  'android-prd-full': 'android', // Historical APK links only.
  'android-prd-global': 'android',
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
    const releaseChannel = channel === 'android-prd-full' ? 'android-prd-global' : channel
    const last = previous.get(releaseChannel)
    requireValue(!last || (compareNumbers(version, last.version) >= 0 && compareNumbers(build, last.build) > 0),
      `Builds must be appended in release order: ${key}`)
    previous.set(releaseChannel, entry)

    if (channel === 'android-prd-global') {
      requireValue(artifactUrl === undefined, 'Global builds do not distribute APKs')
      continue
    }
    const url = new URL(artifactUrl)
    const validArtifact = android
      ? url.origin === apkOrigin &&
        url.pathname.startsWith(`/${channel}/${build}/`) &&
        /^[\w.-]+\.apk$/.test(url.pathname.split('/').at(-1)) &&
        url.pathname.split('/').length === 4 &&
        (channel !== 'android-prd-mainland' || url.pathname === `/${channel}/${build}/Notes-${version}-mainland.apk`)
      : url.origin === 'https://apps.apple.com' && /^\/(?:[a-z]{2}\/)?app\/(?:[\w-]+\/)?id\d+$/.test(url.pathname)
    requireValue(validArtifact && !url.username && !url.password && !url.search && !url.hash,
      `Invalid artifact URL for ${key}`)
  }
}

const bilingual = (zh, en) => `<span lang="zh-Hans" data-lang="zh">${zh}</span><span lang="en" data-lang="en">${en}</span>`
const history = bilingual('更新日志', 'Release notes')
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
language.value = /^zh(?:-|$)/i.test(params.get('lang') || navigator.language) ? 'zh' : 'en'
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
</script>`

const favicon = '<link rel="icon" type="image/svg+xml" href="/composeResources/me.zhangls.web.resources/drawable/notes_icon.svg">'

function page(title, body, pageStyles = '', pageScript = '') {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title} · Notes</title>${favicon}<style>${styles}${pageStyles}</style></head><body>
<nav aria-label="Notes"><a href="/">Notes</a><a href="/downloads/">${downloads}</a>
<a href="/releases/">${history}</a><a href="/open-source/">${notices}</a>
<select id="language" aria-label="Language / 语言"><option value="zh">简体中文</option><option value="en">English</option></select></nav>
<main>${body}</main>${controls}${pageScript}</body></html>
`
}

const releaseStyles = `
body { background: var(--bg, Canvas); color: var(--fg, CanvasText); padding: clamp(1rem, 4vw, 2rem); }
html[data-theme=light] { color-scheme: light }
html[data-theme=dark] { color-scheme: dark }
html[data-embed] nav, html[data-embed] h1 { display: none }
[hidden] { display: none !important }
main { overflow-wrap: anywhere }
.release-platform { display: flex; flex-wrap: wrap; align-items: center; gap: .5rem 1rem; margin-block: 1rem }
.release-platform select { min-height: 44px; max-width: 100%; background: var(--container, Field);
  color: var(--on-container, FieldText); border: 1px solid var(--line, GrayText); border-radius: .5rem }
article { background: var(--card, Canvas); border-color: var(--line, GrayText); padding: clamp(.75rem, 4vw, 1.5rem) }
article ul { padding-inline-start: 1.35em }
article a, nav a { color: var(--accent, LinkText) }
article time { color: var(--muted, GrayText) }
`

const releaseControls = `<script>
{
  const root = document.documentElement
  const url = new URL(location.href)
  const query = url.searchParams
  const theme = query.get('theme')
  const scale = Number(query.get('fontScale'))
  if (theme === 'light' || theme === 'dark') root.dataset.theme = theme
  if (Number.isFinite(scale) && scale > 0) root.style.fontSize = (scale * 100) + '%'
  if (query.get('embed') === '1') root.dataset.embed = ''
  const colors = ['bg', 'card', 'fg', 'muted', 'line', 'accent', 'container', 'on-container']
  for (const key of colors) {
    const value = query.get(key)
    if (/^[a-f0-9]{6}$/i.test(value || '')) root.style.setProperty('--' + key, '#' + value)
  }
  const selector = document.querySelector('#platform')
  const requestedPlatform = query.get('platform')
  const appleMobile = /iPhone|iPad|iPod/i.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  selector.value = requestedPlatform === 'ios' || requestedPlatform === 'android'
    ? requestedPlatform : appleMobile ? 'ios' : 'android'
  const entries = document.querySelectorAll('[data-platforms]')
  const sections = document.querySelectorAll('article section')
  const articles = document.querySelectorAll('article')
  function filter() {
    for (const entry of entries) entry.hidden = !entry.dataset.platforms.split(' ').includes(selector.value)
    for (const section of sections) section.hidden = !section.querySelector('[data-platforms]:not([hidden])')
    for (const article of articles) article.hidden = !article.querySelector('[data-release-change]:not([hidden])')
    document.querySelector('#release-empty').hidden = [...articles].some(article => !article.hidden)
  }
  filter()
  selector.addEventListener('change', () => {
    const current = new URL(location.href)
    current.searchParams.set('platform', selector.value)
    window.history.replaceState(null, '', current)
    filter()
  })
}
</script>`

function releasePage(releases) {
  return page('Release notes', `<h1>${history}</h1>
<div class="release-platform"><label for="platform">${bilingual('平台', 'Platform')}</label>
<select id="platform"><option value="android">Android</option><option value="ios">iOS</option></select></div>
${releases.map(releaseCard).join('')}
<p id="release-empty"${releases.length ? ' hidden' : ''}>${bilingual('暂无已发布记录。', 'No published release notes yet.')}</p>`,
  releaseStyles, releaseControls)
}

function releaseCard({ version, date, changes }) {
  const groups = Object.entries(labels).map(([type, label]) => {
    const entries = changes.filter(change => change.type === type)
    if (!entries.length) return ''
    return `<section><h3>${bilingual(label.zh, label.en)}</h3><ul>${entries.map(change =>
      `<li data-release-change data-platforms="${change.platforms.join(' ')}">${bilingual(escape(change.text.zh), escape(change.text.en))}</li>`).join('')}</ul></section>`
  }).join('')
  return `<article id="v${version}"><h2><a href="#v${version}">${version}</a></h2>
<time datetime="${date}">${date}</time>${groups}</article>`
}

function composeDocument(title, content = '', marker = '') {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark"><title>${title}</title>${favicon}
<script src="/notes.js" defer></script></head>
<body><main id="notes-app" style="position:fixed;inset:0"${marker}></main>${content}</body></html>
`
}

export function buildSite(releases, builds, origins = defaultOrigins) {
  const origin = httpsOrigin(origins.siteOrigin)
  validatePublishedBuilds(releases, builds, origins.apkOrigin)
  const files = new Map()
  const current = new Map()
  for (const entry of builds) {
    const channel = entry.channel === 'android-prd-full' ? 'android-prd-global' : entry.channel
    current.set(channel, entry)
  }
  const downloadable = [...current.values()].filter(entry => entry.artifactUrl)
  files.set('index.html', page('Notes', `<h1>Notes</h1><p>${bilingual('适用于 Android 和 iOS。', 'Available for Android and iOS.')}</p>`))
  const published = releases.filter(release => release.date !== null)
    .sort((a, b) => compareNumbers(b.version, a.version))
  files.set('releases/index.html', releasePage(published))
  files.set('releases/published.json', JSON.stringify(published) + '\n')
  const empty = `<p>${bilingual('暂无已发布构建。', 'No published builds yet.')}</p>`
  files.set('downloads/index.html', page('Downloads', `<h1>${downloads}</h1>${downloadable.length ?
    `<ul>${downloadable.map(entry => `<li><a href="${escape(entry.artifactUrl)}">${entry.channel} · ${entry.version}</a></li>`).join('')}</ul>` : empty}`))
  files.set('open-source/index.html', page('Open-source notices', `<h1>${notices}</h1>
<ul>${Object.entries(platforms).map(([platform, label]) =>
    `<li><a data-notice-link href="/open-source/${platform}.html">${label}</a></li>`).join('')}</ul>`))
  for (const [channel, { version, build, artifactUrl }] of current) {
    // Installed mainland clients validate the v1 path but download from the configured APK origin.
    const updateUrl = channel === 'android-prd-global' ? `${origin}/releases/#v${version}`
      : channel === 'android-prd-mainland' ? `${origin}/downloads/android/${channel}/${build}/`
      : artifactUrl
    files.set(`updates/v1/${channel}.json`, JSON.stringify({ channel, version, build, updateUrl }) + '\n')
  }
  files.set('404.html', composeDocument('404 · Notes', '', ' data-not-found'))
  files.set('_headers', '/releases/published.json\n  Cache-Control: no-cache\n/updates/v1/*\n  Cache-Control: no-cache\n/.well-known/apple-app-site-association\n  Content-Type: application/json\n')
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

export async function buildNoticeFiles() {
  const files = new Map()
  for (const [platform, { categories, declarations }] of Object.entries(await loadNoticeData())) {
    for (const [filename, data] of [[platform, { categories }], [platform + '-licenses', { declarations }]]) {
      const payload = JSON.stringify(data).replaceAll('<', '\\u003c')
      files.set(`open-source/${filename}.html`, composeDocument('Open-source notices · Notes',
        `<script type="application/json" id="notes-notice-data">${payload}</script>`))
    }
  }
  return files
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
  for (const [path, content] of await buildNoticeFiles()) files.set(path, content)
  const outputDir = process.env.NOTES_WEB_OUTPUT_DIR ?? join(webRoot, 'dist')
  const distribution = process.argv[3]
  requireValue(process.argv[2] === '--wasm' && distribution, 'Expected the Wasm distribution directory')
  await readFile(join(distribution, 'notes.js'))
  await cp(distribution, outputDir, { recursive: true })
  const data = JSON.stringify({ builds }).replaceAll('<', '\\u003c')
  for (const path of ['index.html', 'downloads/index.html', 'releases/index.html', 'open-source/index.html']) {
    files.set(path, files.get(path)
      .replace('<body>', '<body><div id="notes-app" style="position:fixed;inset:0;visibility:hidden"></div><div id="notes-fallback">')
      .replace('</body>', `</div><script type="application/json" id="notes-data">${data}</script>
<script src="/notes.js" defer></script></body>`))
  }
  for (const [path, content] of files) {
    const output = join(outputDir, path)
    await mkdir(dirname(output), { recursive: true })
    await writeFile(output, content)
  }
}

if (process.argv[1] && await realpath(process.argv[1]) === fileURLToPath(import.meta.url)) await main()
