import { createHash } from 'node:crypto'
import { readFile } from 'node:fs/promises'

const root = new URL('./open-source/', import.meta.url)
const categoryIds = ['google-jetbrains-components', 'third-party-components']
const namespaces = ['androidx', 'com.google', 'org.jetbrains', 'io.ktor']
const normalizeLineEndings = text => text.replace(/\r\n?/g, '\n')

export async function loadNoticeData() {
  const inventory = JSON.parse(await readFile(new URL('dependencies.json', root), 'utf8'))
  return buildNoticeData(inventory, license => readFile(new URL('licenses/' + license + '.txt', root), 'utf8'))
}

export async function buildNoticeData(inventory, readLicense) {
  const licenses = new Map()
  const result = {}
  for (const platform of ['android', 'ios']) {
    const projects = new Map()
    for (const item of inventory[platform]) {
      const url = new URL(item.url)
      if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password) {
        throw new Error('Invalid upstream project URL: ' + item.url)
      }
      if (!item.licenses.length) throw new Error('Missing license: ' + item.coordinate)
      const group = item.coordinate.split(':', 1)[0]
      const official = ['google', 'jetbrains'].includes(group) ||
        namespaces.some(namespace => group === namespace || group.startsWith(namespace + '.'))
      const category = categoryIds[official ? 0 : 1]
      const segments = url.pathname.replace(/^\/+|\/+$/g, '').split('/')
      const repository = ['github.com', 'www.github.com'].includes(url.hostname) && segments.length >= 2
      const name = repository ? segments.slice(0, 2).join('/').replace(/\.git$/, '') : group
      const source = repository ? 'https://github.com/' + name : item.url.split('#', 1)[0].replace(/\/+$/, '')
      const key = JSON.stringify([category, repository ? source.toLowerCase() : group, repository ? null : source])
      if (!projects.has(key)) projects.set(key, {
        category, name, source, modules: [], licenseIds: new Set(), notices: new Set(),
      })
      const project = projects.get(key)
      project.modules.push({ name: item.name, coordinate: item.coordinate })
      for (const license of item.licenses) project.licenseIds.add(license)
      for (const ref of item.notices) {
        if (!Object.hasOwn(inventory.noticeTexts, ref)) throw new Error('Missing upstream notice: ' + ref)
        project.notices.add(inventory.noticeTexts[ref])
      }
    }

    const categories = Object.fromEntries(categoryIds.map(id => [id, []]))
    const declarations = {}
    for (const { category, name, source, modules, licenseIds, notices } of projects.values()) {
      const ids = []
      for (const license of [...licenseIds].sort()) {
        if (!/^[a-z0-9_-]+(?:\.[a-z0-9_-]+)*$/i.test(license)) throw new Error('Invalid license ID: ' + license)
        if (!licenses.has(license)) licenses.set(license, normalizeLineEndings(await readLicense(license)))
        const id = 'license-' + license
        ids.push(id)
        declarations[id] = [licenses.get(license)]
      }
      const texts = [...notices].sort((a, b) => Buffer.compare(Buffer.from(a), Buffer.from(b)))
      if (texts.length) {
        // Match Python's json.dumps(..., ensure_ascii=False) separators for published fragment IDs.
        const identity = texts.length === 1 ? texts[0] : '[' + texts.map(text => JSON.stringify(text)).join(', ') + ']'
        const id = 'notice-' + createHash('sha256').update(identity).digest('hex').slice(0, 16)
        ids.push(id)
        declarations[id] = texts
      }
      categories[category].push({ name, source, modules, declarations: ids })
    }
    result[platform] = {
      categories: categoryIds.filter(id => categories[id].length).map(id => ({ id, projects: categories[id] })),
      declarations,
    }
  }
  return result
}
