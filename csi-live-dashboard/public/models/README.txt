Place your ONNX model file here as 'emotion_model.onnx'.

To convert your PyTorch model to ONNX, use the following script (save as convert_to_onnx.py):

import torch

# Load the TorchScript model
model = torch.jit.load('D:/React_App/Android-CSI-Labelling-App-master/app/src/main/assets/emotion_model.pt', map_location='cpu')
model.eval()

# Dummy input for export (batch size 1, 3 channels, 224x224)
dummy_input = torch.randn(1, 3, 224, 224)

# Export to ONNX in the assets folder
onnx_path = 'D:/React_App/Android-CSI-Labelling-App-master/app/src/main/assets/emotion_model.onnx'
torch.onnx.export(
    model,
    dummy_input,
    onnx_path,
    input_names=['input'],
    output_names=['output'],
    dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}}
)
print(f"Exported to {onnx_path}")

After conversion, copy 'emotion_model.onnx' to this folder if you want to use it in the dashboard.
