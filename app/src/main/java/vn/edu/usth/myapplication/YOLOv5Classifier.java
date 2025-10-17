package vn.edu.usth.myapplication;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
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

    /** Detect one best object only */
    public List<Result> detect(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer inputBuffer = preprocessBitmap(resized);
        float[][][] output = new float[1][25200][85];
        interpreter.run(inputBuffer, output);
        return postprocess(output);
    }

    private ByteBuffer preprocessBitmap(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
                .order(ByteOrder.nativeOrder());
        int[] pixels = new int[inputSize * inputSize];
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);
        for (int val : pixels) {
            buffer.putFloat(((val >> 16) & 0xFF) / 255f);
            buffer.putFloat(((val >> 8) & 0xFF) / 255f);
            buffer.putFloat((val & 0xFF) / 255f);
        }
        return buffer;
    }

    /** Return only the highest-confidence object */
    private List<Result> postprocess(float[][][] output) {
        Result best = null;
        float bestConf = 0f;

        for (float[] row : output[0]) {
            if (row[4] < 0.4f) continue;
            int classId = -1;
            float maxProb = 0f;
            for (int c = 5; c < 85; c++) {
                if (row[c] > maxProb) {
                    maxProb = row[c];
                    classId = c - 5;
                }
            }
            if (classId >= 0 && classId < labels.size() && row[4] > bestConf) {
                bestConf = row[4];
                best = new Result(labels.get(classId), bestConf);
            }
        }

        if (best != null) {
            Log.d(TAG, "Detected: " + best.label + " (" + best.conf * 100 + "%)");
            return Collections.singletonList(best);
        }
        Log.d(TAG, "No object detected");
        return Collections.emptyList();
    }

    public void close() {
        interpreter.close();
    }

    /** Simple class label + confidence */
    public static class Result {
        public final String label;
        public final float conf;
        public Result(String label, float conf) {
            this.label = label;
            this.conf = conf;
        }
    }
}
