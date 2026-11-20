import assert from 'node:assert/strict'
import { test } from 'node:test'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { mkdtemp, chmod, readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

test('SSH and SCP can share a private deployment identity and verified known-hosts file', async () => {
  const { sshOptions } = await import('./release.mjs')
  const options = sshOptions({ identityFile: '/private/deploy-key', knownHostsFile: '/private/known-hosts' })
  assert(options.includes('-oBatchMode=yes'))
  assert(options.includes('-oStrictHostKeyChecking=yes'))
  assert(options.includes('-oIdentitiesOnly=yes'))
  assert(options.includes('-oUserKnownHostsFile="/private/known-hosts"'))
  assert(options.includes('-oGlobalKnownHostsFile=/dev/null'))
  assert.equal(options[options.indexOf('-i') + 1], '/private/deploy-key')
  assert.throws(() => sshOptions({ knownHostsFile: '/path\nHost *' }))
})

test('APK identity rejects mismatched package, channel, build and signer', async () => {
  const { validateApkIdentity } = await import('./release.mjs')
  const manifest = '<manifest package="me.example.notes" android:versionCode="9" android:versionName="1.2.0"><application><meta-data android:name="me.zhangls.notes.UPDATE_CHANNEL" android:value="mainland" /></application></manifest>'
  const signer = 'a'.repeat(64)
  const expected = { packageName: 'me.example.notes', signerSha256: signer }
  const entry = { channel: 'android-prd-mainland', version: '1.2.0', build: '9' }
  assert.doesNotThrow(() => validateApkIdentity(manifest, `Signer #1 certificate SHA-256 digest: ${signer}`, entry, expected))
  assert.doesNotThrow(() => validateApkIdentity(manifest, `V2 Signer: certificate SHA-256 digest: ${signer}`, entry, expected))
  for (const change of [{ version: '1.3.0' }, { build: '10' }, { channel: 'android-prd-global' }]) {
    assert.throws(() => validateApkIdentity(manifest, `Signer #1 certificate SHA-256 digest: ${signer}`, { ...entry, ...change }, expected))
  }
  assert.throws(() => validateApkIdentity(manifest, `Signer #1 certificate SHA-256 digest: ${'b'.repeat(64)}`, entry, expected))
  assert.throws(() => validateApkIdentity(manifest, `Signer #1 certificate SHA-256 digest: ${signer}`, entry, { ...expected, packageName: 'wrong' }))
  assert.throws(() => validateApkIdentity(manifest.replace('<application>', '<application android:debuggable="true">'),
    `Signer #1 certificate SHA-256 digest: ${signer}`, entry, expected))
})

test('offline CLI prepares a global bundle and rejects a draft or unauthorized publication', async () => {
  const exec = promisify(execFile)
  const root = await mkdtemp(join(tmpdir(), 'notes-release-cli-'))
  await chmod(root, 0o700)
  const config = join(root, 'config.json')
  await writeFile(config, JSON.stringify({ siteOrigin: 'https://notes.example.com', apkOrigin: 'https://downloads.example.com',
    sshHost: 'example-host', siteRoot: '/var/www/site', downloadsRoot: '/var/www/downloads', stagingRoot: '/var/lib/releases' }), { mode: 0o600 })
  const command = ['tools/release/release.mjs', 'prepare', '--config', config, '--channel', 'android-prd-global', '--build', '99']
  const releases = JSON.parse(await readFile(new URL('../../feature/about/src/commonMain/composeResources/files/releases.json', import.meta.url), 'utf8'))
  const version = releases.find(release => release.date !== null).version
  const { stdout } = await exec(process.execPath, [...command, '--version', version, '--output', join(root, 'bundle')])
  const summary = JSON.parse(stdout)
  const bundle = JSON.parse(await readFile(join(root, 'bundle/release.json'), 'utf8'))
  assert.equal(bundle.entry.version, version)
  assert.equal(bundle.apk, undefined)
  assert(bundle.releases.every(release => release.date !== null))
  assert.match(summary.digest, /^[a-f0-9]{64}$/)
  await assert.rejects(exec(process.execPath, [...command, '--version', '2147483647.0.0', '--output', join(root, 'draft')]),
    /unpublished or missing release/)
  await assert.rejects(exec(process.execPath, ['tools/release/release.mjs', 'publish', '--config', config,
    '--bundle', join(root, 'bundle')]), /requires --confirm/)
  await writeFile(join(root, 'bundle/release.json'), '{}')
  await assert.rejects(exec(process.execPath, ['tools/release/release.mjs', 'publish', '--config', config,
    '--bundle', join(root, 'bundle'), '--confirm', summary.digest]), /Bundle digest mismatch/)
})
