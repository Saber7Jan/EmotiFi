# csi-live-dashboard

This is the React web dashboard for EmotiFi. It visualizes live CSI spectrograms and runs browser-based emotion inference using ONNX.js.

## Features
- Live spectrogram visualization from CSI data
- Real-time emotion prediction in the browser
- Confidence metrics and per-class probability display
- CSV export and advanced analytics (planned)

## Setup & Usage
1. Install dependencies:
   ```bash
   npm install
   ```
2. Start the dashboard:
   ```bash
   npm start
   ```
3. The dashboard will connect to the backend API (see `package.json` for proxy settings).

## Model
- Place your ONNX emotion model in `public/models/emotion_model.onnx`.

## Customization
- Update API endpoints or model as needed for your deployment.

## License
MIT License
