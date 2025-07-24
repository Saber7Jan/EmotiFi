# csi-live-backend

This is the backend server for EmotiFi, responsible for receiving CSI data from the Android app and forwarding it to the React dashboard for real-time visualization and inference.

## Features
- Receives CSI data via HTTP POST from the mobile app
- Serves CSI data to the dashboard via API endpoints
- Can be extended for data storage, analytics, or remote monitoring

## Setup & Usage
1. Install dependencies:
   ```bash
   npm install
   ```
2. Start the server:
   ```bash
   node server.js
   ```
3. The server will listen for CSI data on `/api/csi` and provide endpoints for the dashboard.

## Configuration
- Update the server IP and port in the Android app to match your backend server.
- Default port: 5000 (can be changed in `server.js`)

## API Endpoints
- `POST /api/csi` — Receive CSI data
- `GET /api/csi` — Get latest CSI data (for dashboard)

## License
MIT License
