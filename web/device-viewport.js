/* Device Viewport Protocol v1. Standalone; no native methods or business dependencies. */
(() => {
  'use strict'
  const w = window
  if (w.deviceViewport) return
  const documentId = w.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const subscribers = new Set()
  let host = null, lastSequence = -1, pending = false
  const enums = { kind: ['fold', 'hinge', 'unknown'], orientation: ['vertical', 'horizontal'], state: ['flat', 'half-opened', 'unknown'], occlusion: ['none', 'full', 'unknown'] }
  const freeze = value => {
    if (value && typeof value === 'object') { Object.values(value).forEach(freeze); Object.freeze(value) }
    return value
  }
  function validBounds(b, normalized = false) {
    return b && ['left', 'top', 'right', 'bottom'].every(k => Number.isFinite(b[k]) && (!normalized || b[k] >= 0 && b[k] <= 1)) &&
      b.left <= b.right && b.top <= b.bottom && (b.left !== b.right || b.top !== b.bottom)
  }
  function standardFeatures() {
    const segments = w.viewport?.segments
    if (!segments) return null
    const rects = Array.from(segments, r => ({ left: r.x, top: r.y, right: r.x + r.width, bottom: r.y + r.height }))
    if (!rects.length || rects.some(r => !validBounds(r))) return null
    const features = []
    // Only full-span neighbouring regions establish a fold; never invent a fold from diagonals.
    for (let i = 0; i < rects.length; i++) for (let j = i + 1; j < rects.length; j++) {
      let a = rects[i], b = rects[j], bounds, orientation
      if (a.top === b.top && a.bottom === b.bottom) {
        if (a.left > b.left) [a, b] = [b, a]
        if (a.right <= b.left) { bounds = { left: a.right, right: b.left, top: a.top, bottom: a.bottom }; orientation = 'vertical' }
      } else if (a.left === b.left && a.right === b.right) {
        if (a.top > b.top) [a, b] = [b, a]
        if (a.bottom <= b.top) { bounds = { left: a.left, right: a.right, top: a.bottom, bottom: b.top }; orientation = 'horizontal' }
      }
      if (bounds && !rects.some(r => r !== a && r !== b && (orientation === 'vertical' ? r.left >= a.right && r.right <= b.left : r.top >= a.bottom && r.bottom <= b.top))) {
        features.push({ kind: 'unknown', bounds, orientation, state: w.navigator?.devicePosture?.type === 'folded' ? 'half-opened' : 'unknown', separating: true, occlusion: 'unknown' })
      }
    }
    return features
  }
  function snapshot() {
    const viewport = { width: w.innerWidth, height: w.innerHeight }
    const standard = host ? null : standardFeatures()
    const hostCurrent = host && host.width === viewport.width && host.height === viewport.height
    const features = host ? (hostCurrent ? host.features : []).map(f => ({ ...f, bounds: { left: f.bounds.left * viewport.width, right: f.bounds.right * viewport.width, top: f.bounds.top * viewport.height, bottom: f.bounds.bottom * viewport.height } })) : standard ?? []
    return freeze({ protocol: 'device-viewport', version: 1, documentId, sequence: host?.sequence ?? 0, coordinateSpace: 'layout-viewport-css-px', source: host ? 'host' : standard ? 'browser' : 'fallback', availability: host ? (hostCurrent ? host.availability : 'unavailable') : (standard ? 'available' : 'unavailable'), viewport, features })
  }
  function notify() {
    if (pending) return
    pending = true
    w.requestAnimationFrame(() => {
      pending = false
      const next = snapshot()
      for (const callback of subscribers) { try { callback(next) } catch (error) { w.console?.error('Device viewport subscriber failed', error) } }
    })
  }
  const api = {
    documentId,
    getSnapshot: snapshot,
    subscribe(callback) {
      if (typeof callback !== 'function') throw new TypeError('Expected a viewport listener')
      subscribers.add(callback)
      callback(snapshot())
      return () => subscribers.delete(callback)
    },
    receive(payload) {
      if (!payload || payload.protocol !== 'device-viewport' || payload.version !== 1 || payload.documentId !== documentId ||
          payload.type !== 'snapshot' || payload.coordinateSpace !== 'webview-normalized' ||
          !Number.isSafeInteger(payload.sequence) || payload.sequence < 1 || payload.sequence <= lastSequence ||
          !['available', 'unavailable'].includes(payload.availability) || !Array.isArray(payload.features) || payload.features.length > 64 ||
          (payload.availability === 'unavailable' && payload.features.length !== 0)) return false
      const features = []
      for (const f of payload.features) {
        if (!f || typeof f !== 'object' || !validBounds(f.bounds, true) || typeof f.separating !== 'boolean') return false
        if (Object.entries(enums).some(([key, values]) => !values.includes(f[key]))) continue
        features.push({ kind: f.kind, bounds: { left: f.bounds.left, top: f.bounds.top, right: f.bounds.right, bottom: f.bounds.bottom }, orientation: f.orientation, state: f.state, separating: f.separating, occlusion: f.occlusion })
      }
      host = { width: w.innerWidth, height: w.innerHeight, sequence: payload.sequence, availability: payload.availability, features: payload.availability === 'available' ? features : [] }
      lastSequence = payload.sequence
      notify()
      return true
    },
    detach(id) {
      if (id !== documentId) return false
      host = null
      notify()
      return true
    },
  }
  Object.defineProperty(w, 'deviceViewport', { value: Object.freeze(api), configurable: false, writable: false })
  for (const event of ['resize', 'scroll', 'pageshow']) w.addEventListener(event, notify)
  w.addEventListener('pagehide', event => { host = null; if (!event.persisted) subscribers.clear() })
  w.screen?.orientation?.addEventListener('change', notify)
  w.navigator?.devicePosture?.addEventListener?.('change', notify)
  w.visualViewport?.addEventListener('resize', notify)
  w.visualViewport?.addEventListener('scroll', notify)
})()
