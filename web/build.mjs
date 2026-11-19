import { createHash } from 'node:crypto'
import { cp, mkdir, readFile, readdir, realpath, writeFile } from 'node:fs/promises'
import { basename, dirname, join, relative, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadNoticeData } from './notice-data.mjs'

// Resolve the pre-Wasm background from the same palette as ComposeAppTheme.
const themePalette = await readFile(new URL('../core/theme/src/commonMain/kotlin/me/zhangls/theme/Color.kt', import.meta.url), 'utf8')
const themeSurface = mode => {
  const value = themePalette.match(new RegExp(`val surface${mode} = Color\\(0xFF([0-9A-Fa-f]{6})\\)`))?.[1]
  if (!value) throw new Error(`Missing shared surface${mode} color`)
  return '#' + value.toLowerCase()
}
const initialSurfaces = { light: themeSurface('Light'), dark: themeSurface('Dark') }

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
const changeTypes = ['added', 'improved', 'fixed']
const platforms = ['android', 'ios']
const versionPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/
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
      requireValue(changeTypes.includes(change.type) &&
        Array.isArray(change.platforms) && change.platforms.length > 0 &&
        new Set(change.platforms).size === change.platforms.length &&
        change.platforms.every(platform => platforms.includes(platform)) &&
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

const faviconPath = 'composeResources/me.zhangls.web.resources/drawable/notes_icon.svg'
const favicon = `<link rel="icon" type="image/svg+xml" href="/${faviconPath}">`
const miSansPath = 'composeResources/me.zhangls.web.resources/font/misans_regular.otf'
const wasmName = /^[a-f0-9]{20}\.wasm$/
const fontFingerprintLength = 20
const sha256 = bytes => createHash('sha256').update(bytes).digest('hex')

export function buildWebAssetManifest(skikoName, skiko, font) {
  requireValue(wasmName.test(skikoName), 'Expected the Webpack filename for Skiko Wasm')
  const fontHash = sha256(font)
  const fingerprint = fontHash.slice(0, fontFingerprintLength)
  return {
    skiko: { path: `/${skikoName}`, size: skiko.length, sha256: sha256(skiko) },
    miSans: { path: `/web-assets/misans-${fingerprint}.otf`, size: font.length, sha256: fontHash },
  }
}

async function addWebAssets(files, distribution, skikoPath) {
  const skiko = await readFile(skikoPath)
  const font = await readFile(join(distribution, miSansPath))
  const names = (await readdir(distribution)).filter(name => wasmName.test(name))
  const matches = await Promise.all(names.map(async name =>
    (await readFile(join(distribution, name))).equals(skiko)))
  requireValue(matches.filter(Boolean).length === 1, 'Expected exactly one published Skiko Wasm')
  const skikoName = names[matches.indexOf(true)]
  const manifest = buildWebAssetManifest(skikoName, skiko, font)
  files.set('web-assets/manifest-v2.json', JSON.stringify(manifest) + '\n')
  files.set(manifest.miSans.path.slice(1), font)

  const resourceDir = join(distribution, 'composeResources')
  const entries = await readdir(resourceDir, { recursive: true, withFileTypes: true })
  const resources = new Map(await Promise.all(entries.filter(entry => entry.isFile()).map(async entry => {
    const path = join(entry.parentPath, entry.name)
    return [relative(distribution, path).split(sep).join('/'), await readFile(path)]
  })))
  versionWebsiteAssets(
    files,
    await readFile(join(distribution, 'notes.js')),
    await readFile(join(distribution, 'notes.js.map')),
    resources,
    manifest.miSans.path,
  )
}

export function versionWebsiteAssets(files, scriptBytes, sourceMap, resources, fontPath) {
  const mapName = `notes-${sha256(sourceMap)}.js.map`
  const scriptText = scriptBytes.toString('utf8')
    .replace(/^\/\/# sourceMappingURL=notes\.js\.map$/m, `//# sourceMappingURL=${mapName}`)
  const script = `/notes-${sha256(scriptText)}.js`
  files.set(mapName, sourceMap)
  files.set(script.slice(1), scriptText)

  const paths = {}
  for (const [name, bytes] of resources) {
    if (name === miSansPath) {
      paths[name] = fontPath
    } else {
      const path = `assets/${sha256(bytes)}/${basename(name)}`
      files.set(path, bytes)
      paths[name] = `/${path}`
    }
  }
  const mapping = JSON.stringify(paths).replaceAll('<', '\\u003c')
  const metadata = `<script type="application/json" id="notes-resource-paths">${mapping}</script>`
  for (const [name, html] of files) {
    if (name.endsWith('.html')) {
      const withIcon = paths[faviconPath] ? html.replace(`href="/${faviconPath}"`, `href="${paths[faviconPath]}"`) : html
      files.set(name, withIcon.replace('<script src="/notes.js"', `${metadata}\n<script src="${script}"`))
    }
  }
  return { script, paths }
}

function siteHeaders() {
  const revalidate = [
    '/', '/index.html', '/404.html',
    '/downloads', '/downloads/*', '/releases', '/releases/*', '/open-source', '/open-source/*',
    '/notes.js', '/notes.js.map', '/composeResources/*', '/licenses/*',
    '/web-assets/manifest-v2.json', '/updates/v1/*',
  ]
  const immutable = ['/assets/*', '/notes-*.js', '/notes-*.js.map', '/*.wasm', '/web-assets/misans-*.otf']
  return [
    ...revalidate.map(path => `${path}\n  Cache-Control: no-cache`),
    ...immutable.map(path => `${path}\n  Cache-Control: public, max-age=31536000, immutable`),
    '/.well-known/apple-app-site-association\n  Content-Type: application/json',
    '',
  ].join('\n')
}

function composeDocument(title, content = '', marker = '') {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark"><title>${title}</title>${favicon}
<script id="notes-initial-theme">
(() => {
  const params = new URLSearchParams(location.search);
  let saved;
  try { saved = localStorage.getItem('notes.theme'); } catch {}
  const theme = params.get('theme');
  const preference = theme === 'dark' ? 'DARK' : theme === 'light' ? 'LIGHT' : saved;
  const dark = preference === 'DARK' || preference !== 'LIGHT' && matchMedia('(prefers-color-scheme: dark)').matches;
  const bg = params.get('bg');
  const style = document.documentElement.style;
  style.colorScheme = dark ? 'dark' : 'light';
  style.backgroundColor = /^[0-9a-f]{6}$/i.test(bg ?? '') ? '#' + bg : dark ? ${JSON.stringify(initialSurfaces.dark)} : ${JSON.stringify(initialSurfaces.light)};
})();
</script>
<script src="/notes.js" defer></script></head>
<body><main id="notes-app" style="position:fixed;inset:0" data-page-frame="pending"${marker}></main>${content}</body></html>
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
  const siteData = JSON.stringify({ builds }).replaceAll('<', '\\u003c')
  const content = `<script type="application/json" id="notes-data">${siteData}</script>`
  files.set('index.html', composeDocument('Notes · Notes', content))
  files.set('downloads/index.html', composeDocument('Downloads · Notes', content))
  files.set('releases/index.html', composeDocument('Release notes · Notes', content))
  files.set('open-source/index.html', composeDocument('Open-source notices · Notes', content))
  const published = releases.filter(release => release.date !== null)
    .sort((a, b) => compareNumbers(b.version, a.version))
  files.set('releases/published.json', JSON.stringify(published) + '\n')
  for (const [channel, { version, build, artifactUrl }] of current) {
    // Installed mainland clients validate the v1 path but download from the configured APK origin.
    const updateUrl = channel === 'android-prd-global' ? `${origin}/releases/#v${version}`
      : channel === 'android-prd-mainland' ? `${origin}/downloads/android/${channel}/${build}/`
      : artifactUrl
    files.set(`updates/v1/${channel}.json`, JSON.stringify({ channel, version, build, updateUrl }) + '\n')
  }
  files.set('404.html', composeDocument('404 · Notes', '', ' data-not-found'))
  files.set('_headers', siteHeaders())
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
  const skikoPath = process.argv[4]
  requireValue(process.argv[2] === '--wasm' && distribution && skikoPath, 'Expected the Wasm distribution and Skiko')
  await readFile(join(distribution, 'notes.js'))
  await addWebAssets(files, distribution, skikoPath)
  await cp(distribution, outputDir, { recursive: true })
  for (const [path, content] of files) {
    const output = join(outputDir, path)
    await mkdir(dirname(output), { recursive: true })
    await writeFile(output, content)
  }
}

if (process.argv[1] && await realpath(process.argv[1]) === fileURLToPath(import.meta.url)) await main()
