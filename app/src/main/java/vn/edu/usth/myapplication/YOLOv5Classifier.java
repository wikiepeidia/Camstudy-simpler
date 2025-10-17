package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.*;

public final class YOLOv5Classifier {
    private static YOLOv5Classifier instance;
    private final Interpreter interpreter;
    private final int inputSize = 640;
    private final List<String> labels = new ArrayList<>();

    private YOLOv5Classifier(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context, "yolov5s-fp16.tflite"));
            loadLabels(context, "labels.txt");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLO model", e);
        }
    }

    public static synchronized YOLOv5Classifier getInstance(Context context) {
        if (instance == null)
            instance = new YOLOv5Classifier(context.getApplicationContext());
        return instance;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(modelName);
        try (FileInputStream is = new FileInputStream(fd.getFileDescriptor())) {
            FileChannel fc = is.getChannel();
            return fc.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        }
    }

    private void loadLabels(Context context, String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) labels.add(line);
        }
    }

    public List<Result> detect(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer input = preprocessBitmap(resized);
        float[][][] output = new float[1][25200][85];
        interpreter.run(input, output);
        return postprocess(output);
    }

    private ByteBuffer preprocessBitmap(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).order(ByteOrder.nativeOrder());
        int[] pixels = new int[inputSize * inputSize];
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);
        for (int val : pixels) {
            buffer.putFloat(((val >> 16) & 0xFF) / 255f);
            buffer.putFloat(((val >> 8) & 0xFF) / 255f);
            buffer.putFloat((val & 0xFF) / 255f);
        }
        return buffer;
    }

    private List<Result> postprocess(float[][][] output) {
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
            if (classId >= 0 && row[4] > bestConf) {
                bestConf = row[4];
                best = new Result(labels.get(classId), bestConf);
            }
        }
        return best == null ? Collections.emptyList() : Collections.singletonList(best);
    }

    public static class Result {
        public final String label;
        public final float conf;
        public Result(String label, float conf) {
            this.label = label;
            this.conf = conf;
        }
    }
}
