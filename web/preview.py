from argparse import ArgumentParser
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class PreviewHandler(SimpleHTTPRequestHandler):
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
        server.serve_forever()
