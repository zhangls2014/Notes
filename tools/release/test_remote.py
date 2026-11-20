import hashlib
import fcntl
import importlib.util
import json
import pathlib
import tempfile
import unittest


class PublicationTest(unittest.TestCase):
    def test_stage_commit_preserves_channels_and_rejects_repeat_or_tampering(self):
        spec = importlib.util.spec_from_file_location('remote', pathlib.Path(__file__).with_name('remote.py'))
        remote = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(remote)
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            config = dict(siteRoot=str(root / 'site'), downloadsRoot=str(root / 'downloads'),
                          stagingRoot=str(root / 'staging'), siteOrigin='https://notes.example.com',
                          apkOrigin='https://downloads.example.com')
            site = root / 'site'
            (site / 'releases').mkdir(parents=True)
            other = dict(channel='android-prd-global', version='1.0.0', build='1')
            (site / 'releases/site.json').write_text(json.dumps(dict(builds=[other])))
            (site / 'releases/published.json').write_text('[]')
            upload = remote.init(config, 'a' * 32)
            apk = b'test APK bytes'
            entry = dict(channel='android-prd-mainland', version='1.2.0', build='9',
                         artifactUrl='https://downloads.example.com/android-prd-mainland/9/Notes-1.2.0-mainland.apk')
            release = dict(version='1.2.0', date='2026-11-20', changes=[
                dict(type='fixed', platforms=['android'], text=dict(zh='修复', en='Fix'))])
            bundle = dict(schema=1, entry=entry, releases=[release], siteOrigin=config['siteOrigin'],
                          apkOrigin=config['apkOrigin'], apk=dict(size=len(apk), sha256=hashlib.sha256(apk).hexdigest()))
            raw = json.dumps(bundle).encode()
            (upload / 'release.json').write_bytes(raw)
            (upload / 'artifact.apk').write_bytes(apk)
            digest = hashlib.sha256(raw).hexdigest()
            (upload / 'artifact.apk').write_bytes(b'bad')
            with self.assertRaises(ValueError):
                remote.stage(config, 'a' * 32, digest)
            self.assertFalse((root / 'downloads/android-prd-mainland/9/Notes-1.2.0-mainland.apk').exists())
            (upload / 'artifact.apk').write_bytes(apk)
            remote.stage(config, 'a' * 32, digest)
            self.assertEqual(json.loads((site / 'releases/site.json').read_text())['builds'], [other])
            with (upload.parent / 'publication.lock').open('a') as lock:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                with self.assertRaises(BlockingIOError):
                    remote.commit(config, 'a' * 32, digest)
            with self.assertRaises(ValueError):
                remote.commit(config, 'a' * 32, '0' * 64)
            original_write = remote.write_json

            def fail_feed(target, value):
                if target.name == 'android-prd-mainland.json':
                    raise OSError('injected feed write failure')
                original_write(target, value)

            remote.write_json = fail_feed
            with self.assertRaises(OSError):
                remote.commit(config, 'a' * 32, digest)
            remote.write_json = original_write
            self.assertEqual(json.loads((site / 'releases/site.json').read_text())['builds'], [other, entry])
            self.assertFalse((site / 'updates/v1/android-prd-mainland.json').exists())
            # The exact interrupted bundle must finish before another publication can proceed.
            alternate = dict(bundle, entry=dict(channel='android-prd-global', version='1.2.0', build='9'))
            alternate.pop('apk')
            (upload / 'release.json').write_text(json.dumps(alternate))
            with self.assertRaisesRegex(ValueError, 'interrupted publication'):
                remote.commit(config, 'a' * 32, remote.sha256(upload / 'release.json'))
            (upload / 'release.json').write_bytes(raw)
            remote.commit(config, 'a' * 32, digest)
            self.assertEqual(json.loads((site / 'releases/site.json').read_text())['builds'], [other, entry])
            feed = json.loads((site / 'updates/v1/android-prd-mainland.json').read_text())
            self.assertEqual(feed['build'], '9')
            with self.assertRaises(ValueError):
                remote.commit(config, 'a' * 32, digest)
            # A new bundle with an older build cannot replace the channel's current version.
            bundle['entry']['build'] = '8'
            bundle.pop('apk')
            bundle['entry']['channel'] = 'android-prd-global'
            bundle['entry'].pop('artifactUrl')
            (upload / 'release.json').write_text(json.dumps(bundle))
            global_digest = remote.sha256(upload / 'release.json')
            remote.commit(config, 'a' * 32, global_digest)
            bundle['entry']['build'] = '7'
            (upload / 'release.json').write_text(json.dumps(bundle))
            with self.assertRaises(ValueError):
                remote.commit(config, 'a' * 32, remote.sha256(upload / 'release.json'))
            (upload / 'release.json').write_bytes(raw)
            (upload / 'artifact.apk').write_bytes(b'bad')
            with self.assertRaises(ValueError):
                remote.stage(config, 'a' * 32, digest)

    def test_staging_cannot_be_served_or_escape_with_an_identifier(self):
        spec = importlib.util.spec_from_file_location('remote', pathlib.Path(__file__).with_name('remote.py'))
        remote = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(remote)
        with tempfile.TemporaryDirectory() as directory:
            config = dict(siteRoot=directory, downloadsRoot=directory + '/downloads', stagingRoot=directory + '/uploads')
            with self.assertRaises(ValueError):
                remote.init(config, 'a' * 32)
            config['stagingRoot'] = directory + '-private'
            with self.assertRaises(ValueError):
                remote.init(config, '../escape')
            self.assertEqual(remote.numbers('1'), remote.numbers('1.0.0'))


if __name__ == '__main__':
    unittest.main()
