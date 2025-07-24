package com.stevenmhernandez.csi_labelling_app.Experiments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.esp32csiserial.CSIDataInterface;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;
import com.stevenmhernandez.csi_labelling_app.utils.SpectrogramUtils;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LiveSpectrogramActivity extends AppCompatActivity implements CSIDataInterface {
    private static final int MAX_PACKETS = 500;
    private static final int SUBCARRIER_COUNT = 64;
    private final float[][] csiBuffer = new float[MAX_PACKETS][SUBCARRIER_COUNT];
    private int bufferIndex = 0;
    private ImageView spectrogramView;
    private ESP32CSISerial csiSerial;
    private Handler handler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateSpectrogram();
            handler.postDelayed(this, 50);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_spectrogram);
        spectrogramView = findViewById(R.id.spectrogramImageView);
        csiSerial = new ESP32CSISerial();
        csiSerial.setup(this, "live_spectrogram");
        csiSerial.onCreate(this);
        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void addCsi(String csiString) {
        android.util.Log.d("LiveSpectrogramActivity", "Received CSI: " + csiString);
        float[] amplitude = SpectrogramUtils.parseAmplitudeFromCsi(csiString, SUBCARRIER_COUNT);
        if (amplitude != null) {
            csiBuffer[bufferIndex % MAX_PACKETS] = amplitude;
            bufferIndex++;
            // Send CSI to embedded web server for dashboard sync
            postCsiToWebDashboard(csiString);
        }
    }

    private void postCsiToWebDashboard(String csiString) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/csi");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String postData = "csi=" + java.net.URLEncoder.encode(csiString, "UTF-8");
                conn.setFixedLengthStreamingMode(postData.getBytes().length);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();
                int responseCode = conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                android.util.Log.e("LiveSpectrogramActivity", "Failed to POST CSI to dashboard: " + e.getMessage());
            }
        }).start();
    }

    private void updateSpectrogram() {
        int count = Math.min(bufferIndex, MAX_PACKETS);
        float[][] displayBuffer = new float[count][SUBCARRIER_COUNT];
        for (int i = 0; i < count; i++) {
            int idx = (bufferIndex - count + i + MAX_PACKETS) % MAX_PACKETS;
            displayBuffer[i] = csiBuffer[idx];
        }
        Bitmap bmp = SpectrogramUtils.createSpectrogramBitmap(displayBuffer);
        if (bmp != null) {
            runOnUiThread(() -> spectrogramView.setImageBitmap(bmp));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (csiSerial != null) {
            csiSerial.onResume(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (csiSerial != null) {
            csiSerial.onPause(this);
        }
    }
}
