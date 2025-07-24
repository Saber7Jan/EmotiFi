package com.stevenmhernandez.csi_labelling_app.Services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;

public class FileDataCollectorService extends BaseDataCollectorService {
    private String LOG_TAG = "FileDataCollectorService";
    private File outputFile = null;
    private BufferedWriter writer = null;
    private boolean headerWritten = false;

    // CSV headings as per user requirement
    private static final String[] HEADINGS = {
        "timestamp", "label", "type", "role", "mac", "rssi", "rate", "sig_mode", "mcs", "bandwidth",
        "smoothing", "not_sounding", "aggregation", "stbc", "fec_coding", "sgi", "noise_floor", "ampdu_cnt",
        "channel", "secondary_channel", "local_timestamp", "ant", "sig_len", "rx_state", "real_time_set",
        "real_timestamp", "len", "CSI_DATA"
    };

    // Clears the CSV file and writes the header
    public void clearCsvFile(Context context) {
        try {
            outputFile = new File(context.getExternalFilesDir(null), "csi_data.csv");
            writer = new BufferedWriter(new FileWriter(outputFile, false)); // Overwrite mode
            writer.write(String.join(",", HEADINGS));
            writer.newLine();
            writer.flush();
            headerWritten = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setup(Context context) {
        clearCsvFile(context);
    }

    @Override
    public void handle(String csi) {
        try {
            if (writer != null && headerWritten) {
                // Parse CSI string and format as CSV row
                String[] parsed = parseCsiPacket(csi);
                if (parsed != null) {
                    String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                    String[] row = new String[HEADINGS.length];
                    row[0] = timestamp;
                    row[1] = "1"; // Force label to 1 for all data
                    for (int i = 2; i < HEADINGS.length && i - 2 < parsed.length; i++) {
                        row[i] = parsed[i - 2];
                    }
                    writer.write(String.join(",", row));
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Parses a CSI packet string into an array matching the CSV columns (except timestamp/label)
    private String[] parseCsiPacket(String packet) {
        // Expecting comma-separated values, last field may contain brackets
        try {
            String[] data = packet.split(",");
            if (data.length < HEADINGS.length - 2) {
                // Pad with empty strings if not enough fields
                String[] padded = new String[HEADINGS.length - 2];
                System.arraycopy(data, 0, padded, 0, data.length);
                for (int i = data.length; i < padded.length; i++) {
                    padded[i] = "";
                }
                return padded;
            }
            return data;
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error parsing CSI packet: " + e.toString());
            return null;
        }
    }

    @Override
    public Uri getFileUri() throws IOException {
        return Uri.fromFile(this.outputFile);
    }

    // Optionally, close writer on destroy
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}