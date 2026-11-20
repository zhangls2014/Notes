"""Invoked over SSH by release.mjs; no daemon or installed package required."""
import base64
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import uuid

# Published directories must remain traversable by Nginx, even with a private SSH umask.
os.umask(0o022)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def upload_path(config, identifier):
    require(re.fullmatch(r'[a-f0-9]{32}', identifier), 'Invalid upload identifier')
    root = Path(config['stagingRoot']).resolve()
    for name in ('siteRoot', 'downloadsRoot'):
        public = Path(config[name]).resolve()
        require(root != public and public not in root.parents, 'Staging must be outside public roots')
    return root / identifier


def init(config, identifier):
    path = upload_path(config, identifier)
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    require(path.parent.stat().st_mode & 0o077 == 0, 'Staging root must have mode 0700')
    path.mkdir(mode=0o700)
    return path


def load(config, identifier, digest):
    path = upload_path(config, identifier)
    require(sha256(path / 'release.json') == digest, 'Bundle digest mismatch')
    bundle = json.loads((path / 'release.json').read_text())
    require(bundle['schema'] == 1, 'Unsupported bundle schema')
    for origin in ('siteOrigin', 'apkOrigin'):
        require(bundle[origin] == config[origin], 'Target origin mismatch')
    entry = bundle['entry']
    require(entry['channel'] in ('android-prd-mainland', 'android-prd-global', 'ios-app-store'), 'Invalid channel')
    require(re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)', entry['version']), 'Invalid version')
    require(re.fullmatch(r'[1-9]\d*(\.\d+){0,2}', entry['build']), 'Invalid build')
    if bundle.get('apk'):
        apk = path / 'artifact.apk'
        require(apk.stat().st_size == bundle['apk']['size'] and sha256(apk) == bundle['apk']['sha256'], 'APK digest mismatch')
    require(bool(bundle.get('apk')) == (entry['channel'] == 'android-prd-mainland'), 'Missing or unexpected APK')
    return path, bundle


def artifact(config, bundle):
    entry = bundle['entry']
    relative = f"{entry['channel']}/{entry['build']}/Notes-{entry['version']}-mainland.apk"
    require(entry['artifactUrl'] == config['apkOrigin'] + '/' + relative, 'Invalid artifact URL')
    return Path(config['downloadsRoot']) / relative


def stage(config, identifier, digest):
    path, bundle = load(config, identifier, digest)
    if bundle.get('apk'):
        target = artifact(config, bundle)
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            require(sha256(target) == bundle['apk']['sha256'], 'Published APK cannot be overwritten')
        else:
            temporary = target.parent / ('.upload-' + identifier)
            # Exclusive creation makes interrupted/repeated staging fail without overwriting bytes.
            with temporary.open('xb') as output, (path / 'artifact.apk').open('rb') as source:
                shutil.copyfileobj(source, output)
                output.flush()
                os.fsync(output.fileno())
            os.chmod(temporary, 0o644)
            os.link(temporary, target)
        require(sha256(target) == bundle['apk']['sha256'], 'Installed APK digest mismatch')


def numbers(value):
    parts = [int(part) for part in value.split('.')]
    return tuple(parts + [0] * (3 - len(parts)))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.parent / ('.' + path.name + '-' + uuid.uuid4().hex)
    with temporary.open('x') as stream:
        json.dump(value, stream, ensure_ascii=False)
        stream.write('\n')
        stream.flush()
        os.fsync(stream.fileno())
    os.chmod(temporary, 0o644)
    os.replace(temporary, path)


def commit(config, identifier, digest):
    path, bundle = load(config, identifier, digest)
    # One lock across channels protects the shared website data and history.
    with (path.parent / 'publication.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        site = Path(config['siteRoot'])
        builds = json.loads((site / 'releases/site.json').read_text())['builds']
        history = json.loads((site / 'releases/published.json').read_text())
        entry = bundle['entry']
        progress_path = path.parent / 'publication-state.json'
        progress = json.loads(progress_path.read_text()) if progress_path.exists() else {}
        recovering = progress.get('status') == 'pending'
        if recovering:
            require(progress.get('digest') == digest and progress.get('entry') == entry and
                    progress.get('siteRoot') == config['siteRoot'],
                    'An interrupted publication must finish with its original bundle before another release')
        for old in builds:
            channel = 'android-prd-global' if old['channel'] == 'android-prd-full' else old['channel']
            if channel == entry['channel']:
                if recovering and old == entry:
                    continue
                require(numbers(entry['build']) > numbers(old['build']) and
                        numbers(entry['version']) >= numbers(old['version']), 'Build must increase; repeat/downgrade rejected')
        if bundle.get('apk'):
            require(sha256(artifact(config, bundle)) == bundle['apk']['sha256'], 'APK is not staged')
        versions = {release['version']: release for release in history}
        for release in bundle['releases']:
            require(release['date'] is not None, 'Unpublished release')
            require(release['version'] not in versions or versions[release['version']] == release,
                    'Published history differs; reconcile before publishing')
            versions[release['version']] = release
        require(entry['version'] in versions, 'Missing release notes')
        builds = [old for old in builds if
                  ('android-prd-global' if old['channel'] == 'android-prd-full' else old['channel']) != entry['channel']] + [entry]
        history = sorted(versions.values(), key=lambda release: numbers(release['version']), reverse=True)
        url = (config['siteOrigin'] + '/downloads/android/' + entry['channel'] + '/' + entry['build'] + '/'
               if entry['channel'] == 'android-prd-mainland' else
               config['siteOrigin'] + '/releases/#v' + entry['version'] if entry['channel'] == 'android-prd-global'
               else entry['artifactUrl'])
        progress = dict(status='pending', digest=digest, entry=entry, siteRoot=config['siteRoot'])
        write_json(progress_path, progress)
        write_json(site / 'releases/published.json', history)
        write_json(site / 'releases/site.json', dict(builds=builds))
        write_json(site / ('updates/v1/' + entry['channel'] + '.json'),
                   dict(channel=entry['channel'], version=entry['version'], build=entry['build'], updateUrl=url))
        write_json(progress_path, dict(progress, status='complete'))


if __name__ == '__main__':
    operation, encoded, identifier = sys.argv[1:4]
    configuration = json.loads(base64.b64decode(encoded))
    if operation == 'init':
        init(configuration, identifier)
    elif operation in ('stage', 'commit'):
        globals()[operation](configuration, identifier, sys.argv[4])
    else:
        raise ValueError('Unknown operation')
