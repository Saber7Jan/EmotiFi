package com.stevenmhernandez.csi_labelling_app.Experiments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.Helpers.FileHelper;
import com.stevenmhernandez.csi_labelling_app.R;
import com.stevenmhernandez.esp32csiserial.CSIDataInterface;
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial;

import java.util.Timer;
import java.util.TimerTask;

public class TimerMainActivity extends AppCompatActivity implements CSIDataInterface {

    // Parameters
    private final double TIMER_INTERVAL_SECONDS = 5.0; // 5-second hold per action
    private final String[] actions = {"Fear", "Happy", "Neutral", "Sad", "Surprised"}; // New actions
    private static final int PACKET_LIMIT = 500; // CSI packets per action

    // Variables
    private ESP32CSISerial csiSerial = new ESP32CSISerial();
    private TextView textView;
    private TextView frameRateTextView;
    private TextView repetitionsTextView;
    private ConstraintLayout background;
    private Timer timer;

    private int actionIndex = 0;
    private int csiPacketCounter = 0;
    private boolean isCollecting = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stand_walk_run);

        // Initialize UI components
        textView = findViewById(R.id.textView);
        frameRateTextView = findViewById(R.id.frameRateTextView);
        repetitionsTextView = findViewById(R.id.repetitionsTextView);
        background = findViewById(R.id.background);

        // Setup CSI serial
        csiSerial.setup(this, "csi_experiment");
        csiSerial.onCreate(this);

        // Start action cycling
        startActionCycle();
    }

    private void startActionCycle() {
        TimerMainActivity activity = this;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(() -> {
                    // Update UI with the current action
                    updateActionUI(actions[actionIndex]);

                    // Reset CSI packet counter for the new action
                    csiPacketCounter = 0;
                    isCollecting = true;
                });
            }
        }, 0, (long) (TIMER_INTERVAL_SECONDS * 1000)); // Change action every 5 seconds
    }

    private void updateActionUI(String currentAction) {
        textView.setText(currentAction);
        textView.setTextColor(Color.BLACK);
        background.setBackgroundColor(Color.WHITE);
        repetitionsTextView.setText(String.format("Action: %s (Collected: %d/%d)", currentAction, csiPacketCounter, PACKET_LIMIT));
    }

    @Override
    public void addCsi(String csiString) {
        if (!isCollecting) return;

        // Increment packet counter
        csiPacketCounter++;

        // Save CSI data to file
        saveCsiDataToFile(csiString, actions[actionIndex]);

        // Update CSI packet counter in frameRateTextView
        frameRateTextView.setText(String.format("CSI Packets: %d/500", csiPacketCounter));

        // Check if collection is complete
        if (csiPacketCounter >= PACKET_LIMIT) {
            isCollecting = false;
            actionIndex = (actionIndex + 1) % actions.length; // Move to the next action
        }
    }

    private void saveCsiDataToFile(String csiString, String currentAction) {
        // Format data for saving
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String formattedData = String.format("%s,%s,%s,%s\n", deviceId, timestamp, currentAction, csiString);

        // Append data to the CSV file
        FileHelper.appendToFile(this, "csi_data.csv", formattedData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        csiSerial.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        csiSerial.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}