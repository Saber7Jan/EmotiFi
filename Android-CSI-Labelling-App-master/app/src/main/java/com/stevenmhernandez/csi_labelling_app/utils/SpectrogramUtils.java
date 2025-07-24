package com.stevenmhernandez.csi_labelling_app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SpectrogramUtils {
    // Reads CSI data from CSV and returns a 2D float array of amplitudes
    public static float[][] readCsiAmplitudesFromCsv(File csvFile) {
        List<float[]> amplitudesList = new ArrayList<>();
        int maxSubcarriers = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 28) continue; // skip malformed
                String csiDataStr = cols[27].replace("[", "").replace("]", "").trim();
                if (csiDataStr.isEmpty()) continue;
                float[] amplitude = parseAmplitudeFromCsi(csiDataStr);
                if (amplitude != null) {
                    amplitudesList.add(amplitude);
                    maxSubcarriers = Math.max(maxSubcarriers, amplitude.length);
                }
            }
        } catch (Exception e) {
            Log.e("SpectrogramUtils", "Error reading CSV: " + e.getMessage());
        }
        // Convert to 2D array
        float[][] amplitudes = new float[amplitudesList.size()][maxSubcarriers];
        for (int i = 0; i < amplitudesList.size(); i++) {
            float[] row = amplitudesList.get(i);
            System.arraycopy(row, 0, amplitudes[i], 0, row.length);
        }
        return amplitudes;
    }

    // Parse amplitude from CSI string (real, imag pairs)
    public static float[] parseAmplitudeFromCsi(String csiString) {
        try {
            int start = csiString.indexOf('[');
            int end = csiString.indexOf(']');
            if (start == -1 || end == -1 || end <= start) return null;
            String data = csiString.substring(start + 1, end).trim();
            String[] parts = data.split("\\s+");
            float[] real = new float[parts.length / 2];
            float[] imag = new float[parts.length / 2];
            for (int i = 0; i < real.length && 2 * i + 1 < parts.length; i++) {
                real[i] = Float.parseFloat(parts[2 * i]);
                imag[i] = Float.parseFloat(parts[2 * i + 1]);
            }
            float[] amplitude = new float[real.length];
            for (int i = 0; i < real.length; i++) {
                amplitude[i] = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            }
            return amplitude;
        } catch (Exception e) {
            Log.e("SpectrogramUtils", "parseAmplitudeFromCsi error: " + e.getMessage());
            return null;
        }
    }

    // Overloaded version for fixed subcarrier count
public static float[] parseAmplitudeFromCsi(String csiString, int subcarrierCount) {
    try {
        int start = csiString.indexOf('[');
        int end = csiString.indexOf(']');
        if (start == -1 || end == -1 || end <= start) return null;
        String data = csiString.substring(start + 1, end).trim();
        String[] parts = data.split("\\s+");
        float[] real = new float[subcarrierCount];
        float[] imag = new float[subcarrierCount];
        for (int i = 0; i < subcarrierCount && 2 * i + 1 < parts.length; i++) {
            real[i] = Float.parseFloat(parts[2 * i]);
            imag[i] = Float.parseFloat(parts[2 * i + 1]);
        }
        float[] amplitude = new float[subcarrierCount];
        for (int i = 0; i < subcarrierCount; i++) {
            amplitude[i] = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }
        return amplitude;
    } catch (Exception e) {
        Log.e("SpectrogramUtils", "parseAmplitudeFromCsi error: " + e.getMessage());
        return null;
    }
}

    // Generates a Bitmap spectrogram from amplitude data
    public static Bitmap createSpectrogramBitmap(float[][] amplitudes) {
        if (amplitudes == null || amplitudes.length == 0) return null;
        int width = amplitudes.length;
        int height = amplitudes[0].length;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        for (float[] row : amplitudes) {
            for (float v : row) {
                if (v > max) max = v;
                if (v < min) min = v;
            }
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float norm = (amplitudes[x][y] - min) / (max - min + 1e-6f);
                int color = Color.HSVToColor(new float[]{(1 - norm) * 240, 1f, 1f}); // turbo-like colormap
                bmp.setPixel(x, height - y - 1, color);
            }
        }
        return bmp;
    }
}
