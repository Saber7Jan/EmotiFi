package com.stevenmhernandez.csi_labelling_app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.stevenmhernandez.csi_labelling_app.Helpers.FileHelper;

import java.io.IOException;

public class DisplayCSVActivity extends AppCompatActivity {

    private static final String CSV_FILE_NAME = "csi_data.csv";
    private TextView csvTextView;
    private ReadCSVTask readCSVTask; // Declare the AsyncTask as a class member

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_csv);

        // Initialize the TextView where the CSV content will be displayed
        csvTextView = findViewById(R.id.csvTextView);

        // Execute the AsyncTask to read the CSV file in the background
        readCSVTask = new ReadCSVTask();
        readCSVTask.execute();
    }

    @Override
    public void onBackPressed() {
        // Cancel the AsyncTask when the back button is pressed to avoid potential issues
        if (readCSVTask != null && readCSVTask.getStatus() == AsyncTask.Status.RUNNING) {
            readCSVTask.cancel(true); // Cancel the AsyncTask if it is still running
        }
        super.onBackPressed(); // Call the super method to handle the back press
    }

    // AsyncTask to read the CSV file in the background
    private class ReadCSVTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            // Check if the task was cancelled before proceeding
            if (isCancelled()) {
                return null;
            }
            // Read the file in the background
            try {
                return FileHelper.readFile(DisplayCSVActivity.this, CSV_FILE_NAME);
            } catch (IOException e) {
                return "Error reading CSV file!";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Check if the task was cancelled before updating the UI
            if (isCancelled()) {
                return;
            }
            // Update the UI with the CSV content once the background task is done
            csvTextView.setText(result);
        }

        @Override
        protected void onCancelled() {
            // Handle the cancellation of the AsyncTask
            csvTextView.setText("CSV load was cancelled.");
        }
    }
}
