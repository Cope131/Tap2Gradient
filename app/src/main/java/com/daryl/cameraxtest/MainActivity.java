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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    // Views
    private PreviewView viewFinder;
    private TextView textView;
    private LinearLayout gradientBox;

    private LinearLayout persBottomSheet;
    private BottomSheetBehavior sheetBehavior;

    View.OnTouchListener onTouchListener;


    private ExecutorService cameraExecutor;

    // Frame Matrix
    Mat matFrame;

    // check openCV
    static {
        if (OpenCVLoader.initDebug())
            Log.d(TAG, "OpenCV installed successfully");
        else
            Log.d(TAG, "OpenCV not installed");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Views
        viewFinder = findViewById(R.id.viewFinder);
        textView = findViewById(R.id.colorTextView);
        gradientBox = findViewById(R.id.gradientBox);

        persBottomSheet = findViewById(R.id.persBottomSheet);
        sheetBehavior = BottomSheetBehavior.from(persBottomSheet);

        // request camera permission if not granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Pixel Selected
        onTouchListener = (view, motionEvent) -> {
            // selected x and y coordinates
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            // get RGB values of that (x, y) pixel
            if (matFrame != null) {
                double[] rgbValues = matFrame.get(y, x);
                Log.d(TAG, "RGB of Selected Row and Col: " + Arrays.toString(rgbValues));

                int valueR = (int) rgbValues[0];
                int valueG = (int) rgbValues[1];
                int valueB = (int) rgbValues[2];
                Log.d(TAG, "R G B: " + valueR + " " + valueG + " " + valueB);

                // display color using text
                textView.setTextColor(Color.rgb(valueR, valueG, valueB));
                gradientBox.setBackgroundColor(Color.rgb(valueR, valueG, valueB));

            } else {
                Log.d(TAG, "matFrame is Null: ");
            }
            return false;
        };

        viewFinder.setOnTouchListener(onTouchListener);

        // Bottom Sheet Interacted
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    // disable camera on touch
                    viewFinder.setOnTouchListener(null);
                    //Toast.makeText(getApplicationContext(), "onStateChanged Called - Dragging", Toast.LENGTH_SHORT).show();
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // enable camera on touch
                    viewFinder.setOnTouchListener(onTouchListener);
                    //Toast.makeText(getApplicationContext(), "onStateChanged Called - Collapsed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });


    } // - end -


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

                // Image Analyzer
                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // non-blocking mode
                        .build();

                imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
                    Log.d(TAG, "Image Info: " + image.getImageInfo());

                    // convert from bitmap to Matrix Frame to get RGB values
                    final Bitmap bitmapFrame = viewFinder.getBitmap();
                    matFrame = new Mat();

                    if (bitmapFrame == null)
                        return;
                    Utils.bitmapToMat(bitmapFrame, matFrame);

                    Log.d(TAG, "Mat Frame: " + matFrame);

                    image.close();
                });

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
                } catch (Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                }
        }, ContextCompat.getMainExecutor(this));

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