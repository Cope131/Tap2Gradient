package com.daryl.cameraxtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    static {
        if (OpenCVLoader.initDebug())
            Log.d(TAG, "OpenCV installed successfully");
        else
            Log.d(TAG, "OpenCV not installed");
    }

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    PreviewView viewFinder;
    FloatingActionButton btnCapture;
    TextView textView;
//    ImageView ivFrame;

    private ExecutorService cameraExecutor;
    ImageCapture imageCapture;

    private final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    // RGB Image Matrix
//    Mat matFrameRgba;
    Mat matFrame;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        textView = findViewById(R.id.colorTextView);
//        ivFrame = findViewById(R.id.frameImageView);

        // request camera permission if not granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btnCapture.setOnClickListener(view -> takePhoto());

        cameraExecutor = Executors.newSingleThreadExecutor();

        // select pixel
        viewFinder.setOnTouchListener((view, motionEvent) -> {

            // selected x and y coordinates
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            // get RGB values of that (x, y) pixel
//            double[] rgbValues = matFrameRgba.get(y, x);
            double[] rgbValues = matFrame.get(y, x);
            Log.d(TAG, "RGB of Selected Row and Col: " + Arrays.toString(rgbValues));

            int valueR = (int) rgbValues[0];
            int valueG = (int) rgbValues[1];
            int valueB = (int) rgbValues[2];
            Log.d(TAG, "R G B: " + valueR + " " + valueG + " " + valueB);

            // display color using text
            textView.setTextColor(Color.rgb(valueR, valueG, valueB));

            // display image
//            Bitmap bitmap = Bitmap.createBitmap(matFrame.cols(), matFrame.rows(), Bitmap.Config.ARGB_8888);
                // convert Matrix to Bitmap to display
//            Utils.matToBitmap(matFrame, bitmap);
//            ivFrame.setImageBitmap(bitmap);

            return false;
        });

    } // end of onCreate() method


    private void startCamera() {

        // (1) Request Camera Provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
                // (2) Check Camera Provider Availability
                ProcessCameraProvider cameraProvider = null;
                try {
                    cameraProvider = cameraProviderFuture.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // (3) Select Camera Bind Lifecycle and Uses Cases
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                preview.setSurfaceProvider(viewFinder.createSurfaceProvider());

                // initialize image capture
                imageCapture = new ImageCapture.Builder().build();

                // Image Analyzer
                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // non-blocking mode
                        .build();

                imageAnalyzer.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        Log.d(TAG, "Image Info: " + image.getImageInfo());

                        // convert from bitmap to Matrix Frame to get RGB values
                        final Bitmap bitmapFrame = viewFinder.getBitmap();
                        matFrame = new Mat();

                        if (bitmapFrame == null)
                            return;
                        Utils.bitmapToMat(bitmapFrame, matFrame);

                           // convert frame to rgb
//                        matFrameRgba = new Mat();
//                        Imgproc.cvtColor(matFrame, matFrameRgba, Imgproc.COLOR_);

                        Log.d(TAG, "Mat Frame: " + matFrame);
//                        Log.d(TAG, "Mat Frame RBA: " + matFrame);

                        image.close();
                    }
                });

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
                } catch (Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                }
        }, ContextCompat.getMainExecutor(this));

    }

//    private ImageAnalysis setImageAnalysis() {
//    }
//
//    private ImageCapture setImageCapture() {
//    }
//
//    private Preview setPreview() {
//    }

    private void takePhoto() {

        // exit function when image
        if (imageCapture == null)
            return;

        // image file
        File photoFile = new File(getExternalFilesDir(getResources().getString(R.string.app_name)),
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();


        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                    // captured photo successfully - save the photo
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String message = "Photo capture succeeded: " + savedUri;
                        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message);
                    }

                    // failed to capture photo - log error
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exception);
                    }
                }
        );
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
                // start camera when granted
            if (allPermissionsGranted()) {
                startCamera();
                // exit app when not granted
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}