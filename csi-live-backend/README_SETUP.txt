# CSI Live Backend Setup Instructions

1. In `d:\rootApp\csi-live-dashboard`:
   - Run:
     npm install
     npm run build
   - This will create a `build` folder with your React dashboard.

2. Copy the entire `build` folder from `d:\rootApp\csi-live-dashboard` to `d:\rootApp\csi-live-backend` (overwrite the placeholder).

3. In `d:\rootApp\csi-live-backend`:
   - Run:
     npm install
     node server.js
   - The backend will serve the dashboard at http://192.168.50.123:5000 and receive CSI packets from your app.

Your Android app is already configured to work with this backend and dashboard.
