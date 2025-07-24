# EmotiFi

EmotiFi is a real-time emotion recognition system that leverages WiFi Channel State Information (CSI) and deep learning to sense human emotions through wireless signals. The system consists of an Android mobile app, a Node.js backend, and a React dashboard for live visualization and analysis.

## Project Structure
- **Android-CSI-Labelling-App-master/**: Android app for collecting CSI data, running on-device emotion inference, and streaming data to the backend.
- **csi-live-backend/**: Node.js backend server for receiving CSI data and forwarding it to the dashboard.
- **csi-live-dashboard/**: React web dashboard for live spectrogram visualization and browser-based emotion inference.

## Key Features
- Real-time CSI data collection from ESP32 via Android app
- On-device deep learning inference (PyTorch/ONNX)
- Live spectrogram and emotion prediction display
- Backend API for data forwarding and remote monitoring
- Web dashboard for visualization and analytics

## Quick Start
1. **Android App**: Build and install the app from `Android-CSI-Labelling-App-master` on your device. Connect an ESP32 for CSI data.
2. **Backend**: Start the Node.js server in `csi-live-backend` to receive CSI data.
3. **Dashboard**: Run the React app in `csi-live-dashboard` to view live spectrograms and emotion predictions.

See each subfolder's README for detailed setup instructions.

---

## License
MIT License
