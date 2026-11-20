import assert from 'node:assert/strict'
import { createHash, randomUUID } from 'node:crypto'
import { spawn } from 'node:child_process'
import { createReadStream } from 'node:fs'
import { copyFile, mkdir, readFile, realpath, stat, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseArgs } from 'node:util'
import { buildSite, validatePublishedBuilds } from '../../web/build.mjs'

const repository = fileURLToPath(new URL('../../', import.meta.url))
const hash = bytes => createHash('sha256').update(bytes).digest('hex')
const quote = value => "'" + value.replaceAll("'", "'\\''") + "'"
const normalizeFingerprint = value => value.replaceAll(':', '').toLowerCase()

async function fileHash(path) {
  const digest = createHash('sha256')
  for await (const block of createReadStream(path)) digest.update(block)
  return digest.digest('hex')
}

function run(command, args, options = {}) {
  return new Promise((accept, reject) => {
    const process = spawn(command, args, { stdio: ['ignore', 'pipe', 'pipe'], ...options })
    let output = ''
    let error = ''
    process.stdout.on('data', bytes => { output += bytes })
    process.stderr.on('data', bytes => { error += bytes })
    process.on('error', reject)
    process.on('close', code => code === 0 ? accept(output) :
      reject(new Error(`${command} failed (${code}): ${error.trim()}`)))
  })
}

function manifestIdentity(xml) {
  const manifest = xml.match(/<manifest\b[^>]*>/)?.[0] ?? ''
  const attribute = (tag, name) => tag.match(new RegExp(`(?:^|\\s)${name}="([^"]*)"`))?.[1]
  const metadata = [...xml.matchAll(/<meta-data\b[^>]*>/g)].map(match => match[0])
    .find(tag => attribute(tag, 'android:name') === 'me.zhangls.notes.UPDATE_CHANNEL') ?? ''
  return { packageName: attribute(manifest, 'package'), version: attribute(manifest, 'android:versionName'),
    build: attribute(manifest, 'android:versionCode'), channel: attribute(metadata, 'android:value') }
}

export function validateApkIdentity(xml, certificates, entry, config) {
  assert(!/<application\b[^>]*\bandroid:debuggable="true"/.test(xml), 'Debug APKs cannot be published')
  const actual = manifestIdentity(xml)
  assert.equal(actual.packageName, config.packageName, 'APK package mismatch')
  assert.equal(actual.version, entry.version, 'APK version mismatch')
  assert.equal(actual.build, entry.build, 'APK build mismatch')
  assert.equal(actual.channel, entry.channel.replace('android-prd-', ''), 'APK channel mismatch')
  const signers = new Set([...certificates.matchAll(/^(?:Signer #\d+|V\d+ Signer):? certificate SHA-256 digest: ([a-fA-F0-9:]+)\s*$/gm)]
    .map(match => normalizeFingerprint(match[1])))
  assert.equal(signers.size, 1, 'Expected one APK signing identity')
  assert(signers.has(normalizeFingerprint(config.signerSha256)), 'APK signer mismatch')
}

async function readConfig(path) {
  const absolute = resolve(path)
  for (const file of [dirname(absolute), absolute]) {
    assert.equal((await stat(file)).mode & 0o077, 0, 'Private config directory/file must use 0700/0600')
  }
  const config = JSON.parse(await readFile(absolute, 'utf8'))
  for (const key of ['siteOrigin', 'apkOrigin']) {
    const url = new URL(config[key])
    assert(url.protocol === 'https:' && url.origin === config[key], `Invalid ${key}`)
  }
  return config
}

async function inspectApk(apk, config) {
  assert(config.apkanalyzer && config.apksigner && config.packageName &&
    /^[a-fA-F0-9]{64}$/.test(normalizeFingerprint(config.signerSha256 ?? '')), 'Missing APK tools or trusted identity')
  const env = config.javaHome ? { ...process.env, JAVA_HOME: config.javaHome } : process.env
  const xml = await run(config.apkanalyzer, ['manifest', 'print', apk], { env })
  const certificates = await run(config.apksigner, ['verify', '--print-certs', apk], { env })
  return { xml, certificates, ...manifestIdentity(xml) }
}

export async function prepare(options, config) {
  assert(['android-prd-mainland', 'android-prd-global', 'ios-app-store'].includes(options.channel), 'Unsupported channel')
  assert(Boolean(options.apk) === (options.channel === 'android-prd-mainland'), 'Only mainland requires an APK')
  const inspected = options.apk ? await inspectApk(resolve(options.apk), config) : null
  const entry = { channel: options.channel, version: options.version ?? inspected?.version,
    build: options.build ?? inspected?.build }
  if (entry.channel === 'android-prd-mainland') {
    entry.artifactUrl = `${config.apkOrigin}/${entry.channel}/${entry.build}/Notes-${entry.version}-mainland.apk`
    validateApkIdentity(inspected.xml, inspected.certificates, entry, config)
  } else if (entry.channel === 'ios-app-store') entry.artifactUrl = options['store-url']
  const releases = JSON.parse(await readFile(join(repository,
    'feature/about/src/commonMain/composeResources/files/releases.json'), 'utf8'))
  validatePublishedBuilds(releases, [entry], config.apkOrigin)
  const bundle = { schema: 1, entry, releases: releases.filter(release => release.date !== null),
    siteOrigin: config.siteOrigin, apkOrigin: config.apkOrigin }
  const destination = resolve(options.output ?? join(repository, '.local/releases', randomUUID()))
  await mkdir(dirname(destination), { recursive: true, mode: 0o700 })
  await mkdir(destination, { mode: 0o700 })
  if (options.apk) {
    const apk = join(destination, 'artifact.apk')
    await copyFile(resolve(options.apk), apk)
    // Inspect the actual bundle copy as well, protecting against a source changing during preparation.
    const copy = await inspectApk(apk, config)
    validateApkIdentity(copy.xml, copy.certificates, entry, config)
    bundle.apk = { size: (await stat(apk)).size, sha256: await fileHash(apk) }
  }
  const raw = JSON.stringify(bundle, null, 2) + '\n'
  await writeFile(join(destination, 'release.json'), raw, { mode: 0o600, flag: 'wx' })
  console.log(JSON.stringify({ bundle: destination, entry, digest: hash(raw) }, null, 2))
}

async function validateBundle(path, digest, config) {
  const raw = await readFile(join(path, 'release.json'))
  assert.match(digest ?? '', /^[a-f0-9]{64}$/, 'publish requires --confirm <bundle SHA-256>')
  assert.equal(hash(raw), digest, 'Bundle digest mismatch')
  const bundle = JSON.parse(raw)
  assert.equal(bundle.schema, 1, 'Unsupported bundle schema')
  for (const key of ['siteOrigin', 'apkOrigin']) assert.equal(bundle[key], config[key], 'Target origin mismatch')
  validatePublishedBuilds(bundle.releases, [bundle.entry], config.apkOrigin)
  assert(bundle.releases.every(release => release.date !== null), 'Bundle contains unpublished history')
  assert.equal(Boolean(bundle.apk), bundle.entry.channel === 'android-prd-mainland', 'Missing or unexpected APK')
  if (bundle.apk) {
    const apk = join(path, 'artifact.apk')
    assert.equal((await stat(apk)).size, bundle.apk.size, 'APK size mismatch')
    assert.equal(await fileHash(apk), bundle.apk.sha256, 'APK digest mismatch')
    const inspected = await inspectApk(apk, config)
    validateApkIdentity(inspected.xml, inspected.certificates, bundle.entry, config)
  }
  return bundle
}

function validateSsh(config) {
  assert.match(config.sshHost ?? '', /^(?:[a-zA-Z0-9_][a-zA-Z0-9_.-]*@)?[a-zA-Z0-9][a-zA-Z0-9_.-]*$/, 'Use a trusted SSH host or alias')
  for (const key of ['siteRoot', 'downloadsRoot', 'stagingRoot']) {
    assert.match(config[key] ?? '', /^\/[a-zA-Z0-9_./-]+$/, `Invalid ${key}`)
    assert(!config[key].split('/').includes('..') && config[key] !== '/', `Invalid ${key}`)
  }
}

export function sshOptions(config) {
  const options = ['-oBatchMode=yes', '-oStrictHostKeyChecking=yes', '-oConnectTimeout=15']
  for (const key of ['identityFile', 'knownHostsFile']) {
    if (config[key] !== undefined) assert.match(config[key], /^\/[^\r\n"]+$/, `Invalid ${key}`)
  }
  if (config.identityFile) options.push('-oIdentitiesOnly=yes', '-i', config.identityFile)
  if (config.knownHostsFile) options.push(`-oUserKnownHostsFile="${config.knownHostsFile}"`, '-oGlobalKnownHostsFile=/dev/null')
  return options
}

async function verifyPublicApk(bundle) {
  const response = await fetch(bundle.entry.artifactUrl, {
    cache: 'no-cache', redirect: 'error', signal: AbortSignal.timeout(15 * 60_000),
    headers: { 'Accept-Encoding': 'identity' },
  })
  assert.equal(response.status, 200, 'APK is not publicly available')
  const digest = createHash('sha256')
  let size = 0
  for await (const block of response.body) {
    size += block.length
    assert(size <= bundle.apk.size, 'Public APK is too large')
    digest.update(block)
  }
  assert.equal(size, bundle.apk.size, 'Public APK size mismatch')
  assert.equal(digest.digest('hex'), bundle.apk.sha256, 'Public APK digest mismatch')
}

async function publicJson(origin, path) {
  const response = await fetch(origin + path, { cache: 'no-cache', redirect: 'error', signal: AbortSignal.timeout(30_000) })
  assert.equal(response.status, 200, `Public JSON unavailable: ${path}`)
  return response.json()
}

export async function publish(options, config) {
  assert(options.bundle, 'publish requires --bundle')
  validateSsh(config)
  const path = resolve(options.bundle)
  const bundle = await validateBundle(path, options.confirm, config)
  // Preflight requires the one-time website deployment to have exposed runtime data.
  await publicJson(config.siteOrigin, '/releases/site.json')
  const id = randomUUID().replaceAll('-', '')
  const script = await readFile(new URL('./remote.py', import.meta.url), 'utf8')
  const encoded = Buffer.from(JSON.stringify({ siteRoot: config.siteRoot, downloadsRoot: config.downloadsRoot,
    stagingRoot: config.stagingRoot, siteOrigin: config.siteOrigin, apkOrigin: config.apkOrigin })).toString('base64')
  const connectionOptions = sshOptions(config)
  const remote = operation => run('ssh', [...connectionOptions, config.sshHost,
    ['python3', '-c', script, operation, encoded, id, options.confirm].map(quote).join(' ')])
  console.log(JSON.stringify({ target: config.sshHost, entry: bundle.entry, digest: options.confirm }, null, 2))
  await remote('init')
  const files = [join(path, 'release.json'), ...(bundle.apk ? [join(path, 'artifact.apk')] : [])]
  await run('scp', ['-B', ...connectionOptions, ...files, `${config.sshHost}:${config.stagingRoot}/${id}/`])
  await remote('stage')
  if (bundle.apk) await verifyPublicApk(bundle)
  else if (bundle.entry.channel === 'ios-app-store') {
    const response = await fetch(bundle.entry.artifactUrl, { signal: AbortSignal.timeout(30_000) })
    assert(response.ok, 'App Store page unavailable; verify regional availability before publication')
  }
  console.log('Staged artifacts verified. Switching public release data.')
  try {
    await remote('commit')
  } catch (error) {
    throw new Error('Publication command failed; the live state may have changed. Inspect feeds before retrying.', { cause: error })
  }
  // Errors after commit do not imply rollback: leave the actual state visible to the operator.
  try {
    const feeds = buildSite(bundle.releases, [bundle.entry], { siteOrigin: config.siteOrigin, apkOrigin: config.apkOrigin })
    const feedPath = `updates/v1/${bundle.entry.channel}.json`
    assert.deepEqual(await publicJson(config.siteOrigin, '/' + feedPath), JSON.parse(feeds.get(feedPath)))
    const data = await publicJson(config.siteOrigin, '/releases/site.json')
    assert.deepEqual(data.builds.find(entry => entry.channel === bundle.entry.channel), bundle.entry)
    const history = await publicJson(config.siteOrigin, '/releases/published.json')
    assert.deepEqual(history.find(release => release.version === bundle.entry.version),
      bundle.releases.find(release => release.version === bundle.entry.version))
    // Keep subsequent full website builds from restoring the repository's previous channel pointers.
    await writeFile(join(repository, 'web/published-builds.json'), JSON.stringify(data.builds, null, 2) + '\n')
  } catch (error) {
    throw new Error('Publication switched, but public verification failed. Inspect live state before any further action.', { cause: error })
  }
  console.log('Published and verified; web/published-builds.json synchronized. Review and commit the release records. Device installation remains a separate acceptance check.')
}

async function main() {
  const { values, positionals } = parseArgs({ allowPositionals: true, options: Object.fromEntries(
    ['config', 'channel', 'apk', 'version', 'build', 'store-url', 'output', 'bundle', 'confirm'].map(key => [key, { type: 'string' }])) })
  assert.equal(positionals.length, 1, 'Expected prepare or publish')
  assert(['prepare', 'publish'].includes(positionals[0]), 'Expected prepare or publish')
  const config = await readConfig(values.config ?? join(repository, '.local/release.json'))
  await (positionals[0] === 'prepare' ? prepare : publish)(values, config)
}

if (process.argv[1] && await realpath(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => { console.error(error.message); if (error.cause) console.error(error.cause.message); process.exitCode = 1 })
}
