const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));

let latestCSI = null;

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

wss.on('connection', ws => {
  if (latestCSI) ws.send(JSON.stringify({ type: 'csi', data: latestCSI }));
});

app.post('/api/csi', (req, res) => {
  const csi = req.body.csi;
  latestCSI = csi;
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify({ type: 'csi', data: csi }));
    }
  });
  res.sendStatus(200);
});

app.get('/api/csi', (req, res) => {
  res.send(latestCSI || '');
});

// Serve React build
app.use(express.static(path.join(__dirname, 'build')));
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'build', 'index.html'));
});

const PORT = 5000;
server.listen(PORT, '0.0.0.0', () => console.log(`Server running on http://192.168.50.123:${PORT}`));
