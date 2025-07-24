import torch
import numpy as np
from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import io
import matplotlib.pyplot as plt

app = Flask(__name__)
CORS(app)

# Load the trained PyTorch model (update path as needed)
MODEL_PATH = '../Android-CSI-Labelling-App-master/app/src/main/assets/emotion_model.pt'
model = torch.jit.load(MODEL_PATH, map_location='cpu')
model.eval()

# Store CSI data for live spectrogram
csi_history = []

@app.route('/api/predict', methods=['POST'])
def predict():
    data = request.json
    csi = data.get('csi')
    # Convert CSI string to numpy array (assume comma-separated)
    try:
        arr = np.array([float(x) for x in csi.split(',')], dtype=np.float32)
        # Preprocess as needed for your model
        input_tensor = torch.tensor(arr).unsqueeze(0)
        with torch.no_grad():
            output = model(input_tensor)
            pred = int(torch.argmax(output, dim=1).item())
        return jsonify({'emotion': pred})
    except Exception as e:
        return jsonify({'error': str(e)}), 400

@app.route('/api/spectrogram', methods=['POST'])
def spectrogram():
    data = request.json
    csi = data.get('csi')
    try:
        arr = np.array([float(x) for x in csi.split(',')], dtype=np.float32)
        csi_history.append(arr)
        if len(csi_history) > 100:
            csi_history.pop(0)
        # Generate spectrogram image
        fig, ax = plt.subplots(figsize=(6, 2))
        ax.imshow(np.array(csi_history).T, aspect='auto', cmap='viridis', origin='lower')
        ax.axis('off')
        buf = io.BytesIO()
        plt.savefig(buf, format='png', bbox_inches='tight', pad_inches=0)
        plt.close(fig)
        buf.seek(0)
        return send_file(buf, mimetype='image/png')
    except Exception as e:
        return jsonify({'error': str(e)}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=6000)
