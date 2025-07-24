package com.stevenmhernandez.csi_labelling_app.Experiments;

import com.stevenmhernandez.csi_labelling_app.DisplayCSVActivity;
import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.csi_labelling_app.server.WebDashboardServer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.Experiments.ManualInputMainActivity;
import com.stevenmhernandez.csi_labelling_app.Experiments.PressAndHoldMainActivity;
import com.stevenmhernandez.csi_labelling_app.Experiments.RealTimeTestActivity;
import com.stevenmhernandez.csi_labelling_app.Experiments.TimerMainActivity;
import com.stevenmhernandez.csi_labelling_app.Experiments.ToggleMainActivity;
import com.stevenmhernandez.csi_labelling_app.Helpers.FileHelper;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;

import java.io.File;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import java.util.HashMap;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenuActivity extends AppCompatActivity implements com.stevenmhernandez.esp32csiserial.CSIDataInterface {

    private static final String CSV_FILE_NAME = "csi_data.csv";
    private WebDashboardServer webServer;
    private static final int WEB_PORT = 8080;
    private static final String ACTION_USB_PERMISSION = "com.stevenmhernandez.csi_labelling_app.USB_PERMISSION";

    private boolean csiStreaming = false;
    private ESP32CSISerial csiSerial;
    private boolean usbReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        findViewById(R.id.btnGoLive).setOnClickListener(v -> {
            onGoLiveButtonClick(v);
        });
    }

    public void onGoLiveButtonClick(View view) {
        openWebDashboard(view);
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (!deviceList.isEmpty()) {
            UsbDevice device = deviceList.values().iterator().next();
            if (!usbManager.hasPermission(device)) {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                if (!usbReceiverRegistered) {
                    registerReceiver(usbReceiver, filter);
                    usbReceiverRegistered = true;
                }
                usbManager.requestPermission(device, permissionIntent);
            } else {
                startCsiStreaming();
            }
        } else {
            Toast.makeText(this, "No USB device found. Connect ESP and try again.", Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            startCsiStreaming();
                        }
                    } else {
                        Toast.makeText(context, "USB permission denied.", Toast.LENGTH_SHORT).show();
                    }
                    // Unregister after handling
                    if (usbReceiverRegistered) {
                        unregisterReceiver(this);
                        usbReceiverRegistered = false;
                    }
                }
            }
        }
    };

    @Override
    public void addCsi(String csi) {
        // Use the exact amplitude extraction as LiveSpectrogramActivity
        float[] amplitude = com.stevenmhernandez.csi_labelling_app.utils.SpectrogramUtils.parseAmplitudeFromCsi(csi);
        if (amplitude != null) {
            sendAmplitudeToDashboard(amplitude);
        }
    }

    private void startCsiStreaming() {
        if (!csiStreaming) {
            csiStreaming = true;
            if (csiSerial == null) {
                csiSerial = new ESP32CSISerial();
                csiSerial.setup(this, "main_menu"); // 'this' implements CSIDataInterface
                csiSerial.onCreate(this);
            }
            runOnUiThread(() -> Toast.makeText(this, "CSI streaming started for dashboard.", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendAmplitudeToDashboard(float[] amplitude) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < amplitude.length; i++) {
                sb.append(amplitude[i]);
                if (i < amplitude.length - 1) sb.append(",");
            }
            String amplitudeCsv = sb.toString();
            // Send to backend on your PC's network IP
            URL url = new URL("http://10.113.88.184:5000/api/csi");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String postData = "csi=" + amplitudeCsv;
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes());
            }
            conn.getResponseCode();
            conn.disconnect();
            runOnUiThread(() -> Toast.makeText(this, "CSI data forwarded to http://10.113.88.184:5000/api/csi", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            // Optionally log errors
        }
    }

    // Helper: Get the backend server IP (hardcoded or from settings)
    private String getServerIpAddress() {
        // TODO: Optionally make this configurable via UI or settings
        return "192.168.50.123"; // <-- Replace with your backend/server IP
    }

    @Override
    protected void onDestroy() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        csiStreaming = false;
        csiSerial = null;
        super.onDestroy();
    }

    private String getDeviceIpAddress() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null) {
            int ip = wm.getConnectionInfo().getIpAddress();
            return Formatter.formatIpAddress(ip);
        }
        return "127.0.0.1";
    }

    public void startRealTimeTest(View view) {
        Intent intent = new Intent(this, RealTimeTestActivity.class);
        startActivity(intent);
    }

    public void openTimerMainActivity(View view) {
        Intent intent = new Intent(this, TimerMainActivity.class);
        startActivity(intent);
    }

    public void openToggleMainActivity(View view) {
        Intent intent = new Intent(this, ToggleMainActivity.class);
        startActivity(intent);
    }

    public void openManualInputMainActivity(View view) {
        Intent intent = new Intent(this, ManualInputMainActivity.class);
        startActivity(intent);
    }

    public void openPressAndHoldMainActivity(View view) {
        Intent intent = new Intent(this, PressAndHoldMainActivity.class);
        startActivity(intent);
    }

    public void displayCSV(View view) {
        File csvFile = new File(getFilesDir(), CSV_FILE_NAME);

        if (csvFile.exists()) {
            Intent intent = new Intent(this, DisplayCSVActivity.class);
            intent.putExtra("csv_file_path", csvFile.getAbsolutePath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "No CSV file exists!", Toast.LENGTH_SHORT).show();
        }
    }

    public void clearCSV(View view) {
        if (FileHelper.deleteFile(this, CSV_FILE_NAME)) {
            Toast.makeText(this, "CSV file cleared successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to clear CSV file or file does not exist!", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to open the web dashboard
    public void openWebDashboard(View view) {
        if (webServer == null) {
            webServer = new WebDashboardServer(this, WEB_PORT);
            try {
                webServer.start();
                String ip = getDeviceIpAddress();
                String url = "http://" + ip + ":" + WEB_PORT;
                Toast.makeText(this, "Dashboard available at: " + url, Toast.LENGTH_LONG).show();
            } catch (java.io.IOException e) {
                Toast.makeText(this, "Failed to start dashboard server: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
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
        if (csiSerial != null) {
            csiSerial.onPause(this);
        }
        super.onPause();
    }
}