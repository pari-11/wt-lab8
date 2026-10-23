// Lightweight stand-in for the Spring Boot backend: serves the dashboard and the same /api endpoints
// (REST + Server-Sent Events). Zero dependencies:  node server.js  ->  http://localhost:8080
const http = require('http');
const fs = require('fs');
const path = require('path');
const engine = require('./mock-backend');

const root = __dirname;
const port = process.env.PORT || 8080;
const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript' };

const json = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};
const problem = (res, status, detail) =>
  json(res, status, { type: 'about:blank', title: { 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict' }[status], status, detail });

function readBody(req) {
  return new Promise(resolve => {
    let data = '';
    req.on('data', c => { data += c; });
    req.on('end', () => { try { resolve(data ? JSON.parse(data) : {}); } catch (_) { resolve(null); } });
  });
}

// Live stream: every score change is pushed to all connected dashboards as a "match" event.
const clients = new Set();
engine.subscribe(view => {
  const msg = `event:match\ndata:${JSON.stringify(view)}\n\n`;
  clients.forEach(res => res.write(msg));
}, () => {});
setInterval(() => clients.forEach(res => res.write(':ping\n\n')), 15000);

async function handleApi(req, res, url) {
  const parts = url.pathname.split('/').filter(Boolean).slice(1); // drop "api"
  const send = p => p.then(v => json(res, 200, v)).catch(e => problem(res, /not found/i.test(e.message) ? 404 : 409, e.message));

  if (req.method === 'GET' && parts[0] === 'stream') {
    res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', Connection: 'keep-alive' });
    res.write(':connected\n\n');
    clients.add(res);
    req.on('close', () => clients.delete(res));
    return;
  }
  if (parts[0] === 'matches') {
    const [, id, action] = parts;
    if (req.method === 'GET' && !id) {
      const status = url.searchParams.get('status');
      return send(engine.listMatches().then(l => status ? l.filter(m => m.status === status) : l));
    }
    if (req.method === 'GET' && !action) return send(engine.getMatch(id));
    if (req.method === 'GET' && action === 'summary') return send(engine.getSummary(id));
    if (req.method === 'GET' && action === 'commentary') {
      const limit = +url.searchParams.get('limit') || 50;
      return send(engine.getMatch(id).then(m => m.commentary.slice(0, limit)));
    }
    if (req.method === 'POST' && action === 'start') return send(engine.startMatch(id));
    if (req.method === 'POST' && action === 'balls') {
      const body = await readBody(req);
      if (!body || !Number.isInteger(body.runs) || body.runs < 0 || body.runs > 6) {
        return problem(res, 400, 'runs: must be an integer between 0 and 6');
      }
      return send(engine.recordBall(id, body));
    }
  }
  if (req.method === 'GET' && parts[0] === 'players' && parts[1] === 'leaderboard') {
    return send(engine.getLeaderboard(url.searchParams.get('category') || 'runs', +url.searchParams.get('limit') || 5));
  }
  if (parts[0] === 'simulator') {
    if (req.method === 'GET') return send(engine.getSimulator());
    if (req.method === 'POST' && (parts[1] === 'pause' || parts[1] === 'resume')) return send(engine.setSimulator(parts[1] === 'resume'));
  }
  problem(res, 404, `No endpoint ${req.method} ${url.pathname}`);
}

http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  if (url.pathname.startsWith('/api/')) return handleApi(req, res, url);

  const file = path.join(root, url.pathname === '/' ? 'index.html' : decodeURIComponent(url.pathname));
  const isPublic = file === path.join(root, 'index.html') || file.startsWith(path.join(root, 'css')) || file.startsWith(path.join(root, 'js'));
  if (!isPublic || !types[path.extname(file)]) { res.writeHead(404); return res.end('Not found'); }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); return res.end('Not found'); }
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] });
    res.end(data);
  });
}).listen(port, () => console.log(`CricLive running at http://localhost:${port}`));
