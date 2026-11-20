import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdir, mkdtemp, readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const wait = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds))
const paragraph = position => position?.split(':').slice(0, 2).join(':')

test('release page keeps platform and paragraph through window and hinge changes', { timeout: 60_000 }, async () => {
  assert.ok(process.env.CHROME_BIN, 'Set CHROME_BIN to a Chrome executable')
  const preview = process.env.NOTES_PREVIEW_URL ?? 'http://127.0.0.1:8842'
  const output = fileURLToPath(new URL('./build/release-candidates/', import.meta.url))
  const profile = await mkdtemp(join(tmpdir(), 'notes-release-browser-'))
  await mkdir(output, { recursive: true })
  const chrome = spawn(process.env.CHROME_BIN, [
    '--headless=new', '--no-first-run', '--no-default-browser-check',
    '--remote-debugging-port=0', '--user-data-dir=' + profile,
  ], { stdio: ['ignore', 'ignore', 'pipe'] })
  let socket
  try {
    const endpoint = await new Promise((resolve, reject) => {
      let text = ''
      chrome.stderr.on('data', bytes => {
        text += bytes
        const match = text.match(/DevTools listening on (ws:\/\/[^\s]+)/)
        if (match) resolve(match[1])
      })
      chrome.on('exit', code => reject(new Error('Chrome exited ' + code)))
    })
    socket = new WebSocket(endpoint)
    await new Promise(resolve => socket.addEventListener('open', resolve, { once: true }))
    let serial = 0
    const pending = new Map(), errors = []
    socket.addEventListener('message', event => {
      const message = JSON.parse(event.data)
      if (message.id) {
        const request = pending.get(message.id)
        pending.delete(message.id)
        if (message.error) request?.reject(new Error(JSON.stringify(message.error)))
        else request?.resolve(message.result)
      } else if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails)
    })
    const send = (method, params = {}, sessionId) => new Promise((resolve, reject) => {
      const id = ++serial
      pending.set(id, { resolve, reject })
      socket.send(JSON.stringify({ id, method, params, sessionId }))
    })
    const { targetId } = await send('Target.createTarget', { url: 'about:blank' })
    const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true })
    const page = (method, params) => send(method, params, sessionId)
    await page('Runtime.enable')
    await page('Page.enable')
    const evaluate = async expression => {
      const result = await page('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
      if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
      return result.result.value
    }
    const position = () => evaluate('history.state?.notesReleasePosition')
    const resize = (width, height) => page('Emulation.setDeviceMetricsOverride', {
      width, height, deviceScaleFactor: 1, mobile: true,
    })
    async function ready() {
      for (let attempt = 0; attempt < 100; attempt++) {
        if (await evaluate("document.getElementById('notes-app')?.dataset.pageFrame === 'ready'")) {
          await wait(500)
          return
        }
        await wait(200)
      }
      throw new Error('Page did not draw')
    }
    async function capture(name) {
      await wait(700)
      assert.ok(await evaluate('document.body.scrollWidth <= innerWidth'), 'No horizontal document overflow')
      const { data } = await page('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
      await writeFile(join(output, name + '.png'), Buffer.from(data, 'base64'))
    }
    await page('Emulation.setUserAgentOverride', {
      userAgent: 'Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/145.0.0.0 Mobile Safari/537.36',
      platform: 'Linux armv8l',
    })
    await resize(390, 844)
    await page('Page.navigate', { url: new URL('/releases/?embed=1&lang=zh&platform=ios', preview).href })
    await ready()
    await capture('phone-390')
    await resize(960, 900)
    await capture('tablet-top')
    await resize(390, 844)
    await wait(700)
    await page('Input.dispatchMouseEvent', { type: 'mouseWheel', x: 190, y: 600, deltaY: 1700, deltaX: 0 })
    await wait(1000)
    await capture('phone-reading')
    const before = await position()
    assert.ok(Number(before?.split(':')[1]) >= 0, 'User scroll records a paragraph')
    assert.equal(before?.split(':')[3], 'android', 'A conflicting URL cannot override the mobile platform')
    await resize(960, 900)
    await capture('tablet-960')
    assert.equal(paragraph(await position()), paragraph(before), 'Width changes retain the paragraph anchor')

    const history = await (await fetch(new URL('/releases/published.json', preview))).json()
    const second = history.filter(release => release.changes.some(change => change.platforms.includes('android')))[1]
    await page('Input.dispatchMouseEvent', { type: 'mousePressed', x: 100, y: 250, button: 'left', clickCount: 1 })
    await page('Input.dispatchMouseEvent', { type: 'mouseReleased', x: 100, y: 250, button: 'left', clickCount: 1 })
    await wait(700)
    assert.equal(await evaluate('location.hash'), '#v' + second.version)
    await evaluate('history.back()')
    await wait(700)
    assert.equal(paragraph(await position()), paragraph(before), 'Browser back restores the prior reading anchor')

    async function publish(sequence, features) {
      const payload = JSON.stringify({
        protocol: 'device-viewport', version: 1, sequence, type: 'snapshot',
        coordinateSpace: 'webview-normalized', availability: 'available', features,
      })
      return evaluate(`deviceViewport.receive({...${payload},documentId:deviceViewport.documentId})`)
    }
    const fold = {
      kind: 'fold', bounds: { left: .49, top: 0, right: .51, bottom: 1 },
      orientation: 'vertical', state: 'half-opened', separating: true, occlusion: 'none',
    }
    assert.equal(await publish(1, [fold]), true)
    await capture('fold-half-open')
    assert.equal(await publish(2, [{ ...fold, state: 'flat' }]), true)
    await capture('fold-flat')
    assert.deepEqual(
      await readFile(join(output, 'fold-half-open.png')),
      await readFile(join(output, 'fold-flat.png')),
      'The same hinge geometry keeps the pane layout unchanged between half-open and flat',
    )
    assert.equal(paragraph(await position()), paragraph(before))
    assert.equal(await publish(3, []), true)
    await capture('hinge-cleared')
    assert.equal(await evaluate('deviceViewport.getSnapshot().features.length'), 0)
    await resize(320, 740)
    await capture('phone-320')
    assert.equal(paragraph(await position()), paragraph(before), 'Returning to a narrow window retains the paragraph')

    await page('Emulation.setUserAgentOverride', {
      userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1',
      platform: 'MacIntel',
    })
    await page('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 5 })
    await page('Page.navigate', {
      url: new URL('/releases/?embed=1&lang=en&platform=android&fontScale=1.5', preview).href,
    })
    await ready()
    await capture('ipad-large-text')
    assert.equal((await position())?.split(':')[3], 'ios', 'iPadOS desktop user agent still resolves to iOS')
    await writeFile(join(output, 'runtime-errors.json'), JSON.stringify(errors, null, 2))
    assert.equal(errors.length, 0)
    await send('Target.closeTarget', { targetId })
  } finally {
    socket?.close()
    chrome.kill()
  }
})
