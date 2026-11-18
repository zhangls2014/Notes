from argparse import ArgumentParser
from fnmatch import fnmatchcase
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit


def cache_control(directory, request_path):
    rules = Path(directory) / "_headers"
    if rules.is_file():
        path = urlsplit(request_path).path
        pattern = ""
        for line in rules.read_text().splitlines():
            if line and not line[0].isspace():
                pattern = line
            elif line.lstrip().startswith("Cache-Control:") and fnmatchcase(path, pattern):
                return line.split(":", 1)[1].strip()
    return "no-cache"


class PreviewHandler(SimpleHTTPRequestHandler):
    def send_response(self, code, message=None):
        super().send_response(code, message)
        value = cache_control(self.directory, self.path) if code in (200, 206, 304) else "no-cache"
        self.send_header("Cache-Control", value)

    def send_error(self, code, message=None, explain=None):
        if code != 404 or self.command not in ("GET", "HEAD"):
            return super().send_error(code, message, explain)
        try:
            document = (Path(self.directory) / "404.html").read_bytes()
        except OSError:
            return super().send_error(code, message, explain)
        self.send_response(404)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(document)))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(document)


if __name__ == "__main__":
    parser = ArgumentParser(description="Preview the Notes website and its custom 404 page.")
    parser.add_argument("--port", type=int, default=8842)
    parser.add_argument("--directory", type=Path, default=Path(__file__).resolve().parent / "dist")
    args = parser.parse_args()
    handler = partial(PreviewHandler, directory=str(args.directory))
    with ThreadingHTTPServer(("127.0.0.1", args.port), handler) as server:
        print(f"Serving notes preview at http://127.0.0.1:{server.server_port}", flush=True)
        server.serve_forever()
