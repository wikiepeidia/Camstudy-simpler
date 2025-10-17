/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: YOLOv5Classifier.java
 * Last Modified: 17/10/2025 0:56
 */

// app/src/main/java/vn/edu/usth/myapplication/YOLOv5Classifier.java
package vn.edu.usth.myapplication;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.*;

public class YOLOv5Classifier {

    private static final String TAG = "YOLOv5Classifier";
    private final Interpreter interpreter;
    private final int inputSize = 640;
    private final List<String> labels = new ArrayList<>();

    public YOLOv5Classifier(AssetManager assetManager, String modelName) throws IOException {
        interpreter = new Interpreter(loadModelFile(assetManager, modelName));
        loadLabels(assetManager, "labels.txt");
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelName) throws IOException {
        AssetFileDescriptor fd = assetManager.openFd(modelName);
        try (FileInputStream is = new FileInputStream(fd.getFileDescriptor())) {
            FileChannel fc = is.getChannel();
            return fc.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        }
    }

    private void loadLabels(AssetManager assetManager, String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) labels.add(line);
        } catch (IOException e) {
            Log.e(TAG, "Cannot read labels.txt", e);
        }
    }

    public List<Result> detect(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer inputBuffer = preprocessBitmap(resized);
        float[][][] output = new float[1][25200][85];
        interpreter.run(inputBuffer, output);
        return postprocess(output, bitmap.getWidth(), bitmap.getHeight());
    }

    private ByteBuffer preprocessBitmap(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);
        for (int val : intValues) {
            buffer.putFloat(((val >> 16) & 0xFF) / 255f);
            buffer.putFloat(((val >> 8) & 0xFF) / 255f);
            buffer.putFloat((val & 0xFF) / 255f);
        }
        return buffer;
    }

    private List<Result> postprocess(float[][][] output, int origW, int origH) {
        Result best = null;
        float bestConf = 0;
        for (float[] row : output[0]) {
            if (row[4] < 0.4f) continue;
            int classId = -1;
            float maxProb = 0;
            for (int c = 5; c < 85; c++) {
                if (row[c] > maxProb) {
                    maxProb = row[c];
                    classId = c - 5;
                }
            }
            if (classId >= 0 && classId < labels.size()) {
                float left = (row[0] - row[2] / 2) / inputSize * origW;
                float top = (row[1] - row[3] / 2) / inputSize * origH;
                float right = (row[0] + row[2] / 2) / inputSize * origW;
                float bottom = (row[1] + row[3] / 2) / inputSize * origH;
                if (row[4] > bestConf) {
                    bestConf = row[4];
                    best = new Result(labels.get(classId), row[4], left, top, right, bottom);
                }
            }
        }
        if (best != null) {
            Log.d(TAG, "Detected: " + best.label + " " + (best.conf * 100) + "%");
            return Collections.singletonList(best);
        }
        Log.d(TAG, "No objects detected above threshold");
        return Collections.emptyList();
    }

    public Bitmap drawDetections(Bitmap bitmap, List<Result> results) {
        Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(40);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        for (Result r : results) {
            canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
            canvas.drawText(r.label + " " + String.format("%.1f%%", r.conf * 100), r.left, r.top - 10, textPaint);
        }
        return mutable;
    }

    public void close() {
        interpreter.close();
    }

    public static class Result {
        public final String label;
        public final float conf, left, top, right, bottom;
        public Result(String label, float conf, float left, float top, float right, float bottom) {
            this.label = label;
            this.conf = conf;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
