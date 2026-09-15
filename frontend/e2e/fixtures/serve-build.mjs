import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { resolve, extname, sep } from "node:path";

const root = resolve("dist");
const types = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".json": "application/json", ".png": "image/png", ".svg": "image/svg+xml", ".webp": "image/webp", ".woff2": "font/woff2" };
const server = createServer(async (req, res) => {
  try {
    const pathname = decodeURIComponent(new URL(req.url, "http://localhost").pathname);
    if (pathname === "/__test_shutdown" && req.method === "POST") {
      res.writeHead(200).end();
      server.close();
      server.closeIdleConnections();
      return;
    }
    const target = resolve(root, `.${pathname}`);
    if (target !== root && !target.startsWith(root + sep)) { res.writeHead(403).end(); return; }
    let file = target;
    let content;
    try { content = await readFile(file); }
    catch {
      if (extname(pathname)) { res.writeHead(404).end(); return; }
      file = resolve(root, "index.html"); content = await readFile(file);
    }
    res.writeHead(200, { "Content-Type": types[extname(file)] || "application/octet-stream" });
    res.end(content);
  } catch { res.writeHead(500).end(); }
}).listen(15173, "127.0.0.1");
