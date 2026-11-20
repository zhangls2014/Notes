import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import { runInNewContext } from 'node:vm'

async function browser(segments) {
  const listeners = new Map(), frames = []
  const window = { innerWidth: 1000, innerHeight: 800, crypto: { randomUUID: () => 'test-document' },
    addEventListener: (name, callback) => listeners.set(name, callback),
    requestAnimationFrame: callback => frames.push(callback),
    screen: { orientation: { addEventListener: (name, callback) => listeners.set('orientation:' + name, callback) } },
    viewport: segments ? { segments, addEventListener() {} } : undefined }
  window.window = window
  runInNewContext(await readFile(new URL('./device-viewport.js', import.meta.url), 'utf8'), { window })
  return { api: window.deviceViewport, window, emit: name => listeners.get(name)?.(), flush: () => { while (frames.length) frames.shift()() } }
}
const feature = { kind: 'fold', bounds: { left: .5, top: 0, right: .5, bottom: 1 }, orientation: 'vertical', state: 'flat', separating: false, occlusion: 'none' }
const packet = (sequence = 1, features = [feature]) => ({ protocol: 'device-viewport', version: 1, documentId: 'test-document', sequence, type: 'snapshot', coordinateSpace: 'webview-normalized', availability: 'available', features })

test('fallback, immediate subscription, host updates and frame coalescing', async () => {
 const b = await browser(), seen = []
 assert.equal(b.api.getSnapshot().source, 'fallback')
 const stop = b.api.subscribe(s => seen.push(s))
 assert.equal(seen.length, 1)
 assert.equal(b.api.receive(packet()), true)
 assert.equal(b.api.receive(packet(2)), true)
 assert.equal(seen.length, 1)
 b.flush()
 assert.equal(seen.length, 2)
 assert.equal(seen[1].features[0].bounds.left, 500)
 assert.equal(Object.isFrozen(seen[1].features[0].bounds), true)
 assert.equal(b.api.receive(packet(1)), false)
 assert.equal(b.api.receive({ ...packet(3), documentId: 'old' }), false)
 stop(); b.api.receive(packet(3, [])); b.flush(); assert.equal(seen.length, 2)
})
test('invalid geometry rejected, unknown feature ignored and unavailable clears old geometry', async () => {
 const { api } = await browser()
 assert.equal(api.receive(packet(1, [{ ...feature, bounds: { left: NaN, right: .5, top: 0, bottom: 1 } }])), false)
 assert.equal(api.receive({ ...packet(), version: 2 }), false)
 assert.equal(api.receive(packet(1, [{ ...feature, state: 'future' }])), true)
 assert.equal(api.getSnapshot().features.length, 0)
 assert.equal(api.receive(packet(2)), true)
 assert.equal(api.receive({ ...packet(3, []), availability: 'unavailable' }), true)
 assert.equal(api.getSnapshot().availability, 'unavailable')
 assert.equal(api.getSnapshot().features.length, 0)
})
test('resize clears old geometry; detach releases host but preserves ordering', async () => {
 const b = await browser()
 b.api.receive(packet())
 b.window.innerWidth = 600; b.emit('resize'); b.flush()
 assert.equal(b.api.getSnapshot().features.length, 0)
 assert.equal(b.api.detach('old'), false)
 assert.equal(b.api.detach('test-document'), true)
 assert.equal(b.api.getSnapshot().source, 'fallback')
 assert.equal(b.api.receive(packet()), false)
 assert.equal(b.api.receive(packet(2)), true)
})
test('standard segments preserve zero-width fold boundaries and host wins', async () => {
 const b = await browser([{ x: 0, y: 0, width: 500, height: 800 }, { x: 500, y: 0, width: 500, height: 800 }])
 const s = b.api.getSnapshot()
 assert.equal(s.source, 'browser'); assert.equal(s.features.length, 1)
 assert.equal(s.features[0].bounds.left, s.features[0].bounds.right)
 b.api.receive(packet(1, [])); assert.equal(b.api.getSnapshot().source, 'host')
 b.api.detach('test-document'); assert.equal(b.api.getSnapshot().source, 'browser')
})

test('complete snapshots clear features and caller mutations cannot change stored data', async () => {
 const { api } = await browser()
 const p = packet()
 assert.equal(api.receive(p), true)
 p.features = []
 assert.equal(api.getSnapshot().features.length, 1)
 assert.equal(api.receive(packet(2, [])), true)
 assert.equal(api.getSnapshot().features.length, 0)
 assert.throws(() => { api.getSnapshot().viewport.width = 1 }, TypeError)
})
test('multiple and horizontal hinges map each axis without DPR scaling', async () => {
 const b = await browser()
 b.window.devicePixelRatio = 3
 const horizontal = { ...feature, orientation: 'horizontal', bounds: { left: 0, top: .4, right: 1, bottom: .45 }, occlusion: 'full' }
 assert.equal(b.api.receive(packet(1, [feature, horizontal])), true)
 const fs = b.api.getSnapshot().features
 assert.equal(fs.length, 2); assert.equal(fs[1].bounds.top, 320); assert.equal(fs[1].bounds.right, 1000)
})
test('invalid point and reversed or out-of-range bounds do not replace a valid snapshot', async () => {
 const { api } = await browser(); api.receive(packet())
 for (const bounds of [{ left: .5, right: .5, top: .5, bottom: .5 }, { left: .7, right: .2, top: 0, bottom: 1 }, { left: -.1, right: .2, top: 0, bottom: 1 }]) {
   assert.equal(api.receive(packet(2, [{ ...feature, bounds }])), false)
   assert.equal(api.getSnapshot().sequence, 1)
 }
})
test('three standard regions produce only adjacent folds', async () => {
 const b = await browser([0, 300, 650].map((x, i) => ({ x, y: 0, width: [300,350,350][i], height: 800 })))
 assert.equal(b.api.getSnapshot().features.length, 2)
})

test('resize invalidates host geometry until a fresh complete snapshot arrives', async () => {
 const b = await browser()
 b.api.receive(packet())
 b.window.innerWidth = 800; b.window.innerHeight = 1000
 b.emit('resize'); b.flush()
 assert.equal(b.api.getSnapshot().availability, 'unavailable')
 assert.equal(b.api.getSnapshot().features.length, 0)
 b.api.receive(packet(2))
 assert.equal(b.api.getSnapshot().features[0].bounds.left, 400)
})
test('wire sequence starts at one and unavailable packets have no geometry', async () => {
 const { api } = await browser()
 assert.equal(api.receive(packet(0)), false)
 assert.equal(api.receive({ ...packet(), availability: 'unavailable' }), false)
})

test('screen orientation changes refresh standard segments without a window resize', async () => {
 const b = await browser([{ x: 0, y: 0, width: 500, height: 800 }, { x: 500, y: 0, width: 500, height: 800 }])
 const seen = []
 b.api.subscribe(value => seen.push(value))
 b.window.viewport.segments = [{ x: 0, y: 0, width: 1000, height: 400 }, { x: 0, y: 400, width: 1000, height: 400 }]
 b.emit('orientation:change'); b.flush()
 assert.equal(seen.length, 2)
 assert.equal(seen[1].features[0].orientation, 'horizontal')
})
