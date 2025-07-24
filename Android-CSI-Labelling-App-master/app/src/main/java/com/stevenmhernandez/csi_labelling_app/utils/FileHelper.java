package com.stevenmhernandez.csi_labelling_app.Helpers;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper {

    private static final String AUTHORITY = "com.stevenmhernandez.csi_labelling_app.fileprovider";

    /**
     * Appends data to a CSV file.
     * Creates the file if it doesn't exist.
     *
     * @param context  The application context.
     * @param fileName The name of the file.
     * @param data     The data to append.
     */
    public static void appendToFile(Context context, String fileName, String data) {
        try {
            File file = getFile(context, fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true)); // Open in append mode
            writer.append(data);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves or creates the file in the app's external files directory.
     *
     * @param context  The application context.
     * @param fileName The name of the file.
     * @return The file object.
     */
    public static File getFile(Context context, String fileName) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) {
            dir.mkdirs(); // Create the directory if it doesn't exist
        }
        return new File(dir, fileName);
    }

    /**
     * Reads the contents of a file.
     *
     * @param context  The application context.
     * @param fileName The name of the file.
     * @return The content of the file as a String.
     * @throws IOException If an error occurs while reading the file.
     */
    public static String readFile(Context context, String fileName) throws IOException {
        File file = getFile(context, fileName);
        if (!file.exists()) {
            throw new IOException("File does not exist!");
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();

        if (content.length() == 0) {
            return "File is empty!";
        }

        return content.toString();
    }

    /**
     * Deletes a file.
     *
     * @param context  The application context.
     * @param fileName The name of the file to delete.
     * @return True if the file was successfully deleted, false otherwise.
     */
    public static boolean deleteFile(Context context, String fileName) {
        File file = getFile(context, fileName);
        return file.exists() && file.delete();
    }

    /**
     * Provides a URI for the file to be shared securely.
     * Requires `FileProvider` to be configured in the app manifest.
     *
     * @param context  The application context.
     * @param fileName The name of the file.
     * @return The URI of the file.
     */
    public static Uri getFileUri(Context context, String fileName) {
        File file = getFile(context, fileName);
        return FileProvider.getUriForFile(context, AUTHORITY, file);
    }

    /**
     * Clears the contents of the file by overwriting it with an empty string.
     *
     * @param context  The application context.
     * @param fileName The name of the file.
     */
    public static void clearFile(Context context, String fileName) {
        try {
            File file = getFile(context, fileName);
            if (file.exists()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, false)); // Overwrite mode
                writer.write("");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
