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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
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
    private LinearLayout gradientBox;

    private LinearLayout persBottomSheet;
    private BottomSheetBehavior sheetBehavior;

    private ChipGroup colorsChipGroup;
    private Chip color1Chip;
    private Chip color2Chip;
    private int selectedChipId;

    private Slider color1Slider;
    private Slider color2Slider;

    private LinearLayout colorDisplay;

    View.OnTouchListener onTouchListener;

    private ExecutorService cameraExecutor;

    // Frame Matrix
    private Mat matFrame;

    // Selected Colors
    private int color1;
    private int color2;

    // Check OpenCV
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
        gradientBox = findViewById(R.id.gradientBox);

        persBottomSheet = findViewById(R.id.persBottomSheet);
        sheetBehavior = BottomSheetBehavior.from(persBottomSheet);

        colorsChipGroup = findViewById(R.id.colorsChipGroup);
        color1Chip = findViewById(R.id.color1Chip);
        color2Chip = findViewById(R.id.color2Chip);

        selectedChipId = color1Chip.getId(); // Selected by Default

        color1Slider = findViewById(R.id.color1Slider);
        color2Slider = findViewById(R.id.color2Slider);

        colorDisplay = findViewById(R.id.colorDisplay);

        // Bottom Sheet Behaviour
        sheetBehavior = BottomSheetBehavior.from(persBottomSheet);

        // Request Camera Permission If Not Granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Pixel Selected
        onTouchListener = (view, motionEvent) -> {

            if (motionEvent.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                // Toast.makeText(getApplicationContext(), "onTouch Down - Camera", Toast.LENGTH_SHORT).show();
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

                    int color = Color.rgb(valueR, valueG, valueB);

                    // store to color 1 or 2 according to which is selected
                    if (selectedChipId == color1Chip.getId())
                        color1 = color;
                    else if (selectedChipId == color2Chip.getId())
                        color2 = color;

                    // Toast.makeText(getApplicationContext(), "color1: " + color1 + "\ncolor2: " + color2, Toast.LENGTH_SHORT).show();

                    // display color using text
                    int[] colors = {color1, color2};
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                    gradientBox.setBackground(gradientDrawable);

                    // update slider for lightness of the color
                    float[] hsv = new float[3];
                    Color.RGBToHSV(valueR, valueG, valueB, hsv);
                    float v = hsv[2]*100;

                    if (selectedChipId == color1Chip.getId())
                        color1Slider.setValue(v);
                    else if (selectedChipId == color2Chip.getId())
                        color2Slider.setValue(v);


                } else {
                    Log.d(TAG, "matFrame is Null: ");
                }

            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                Toast.makeText(getApplicationContext(), "onTouch Up - Camera", Toast.LENGTH_SHORT).show();
            }

            return true;
        };
        viewFinder.setOnTouchListener(onTouchListener);

        // Bottom Sheet Selected or Swiped
        persBottomSheet.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                // disable view finder on touch
                viewFinder.setOnTouchListener(null);
                // Toast.makeText(getApplicationContext(), "On Touch Down - Sheet", Toast.LENGTH_SHORT).show();
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // enable view finder on touch
                viewFinder.setOnTouchListener(onTouchListener);
                // Toast.makeText(getApplicationContext(), "On Touch Up - Sheet", Toast.LENGTH_SHORT).show();
            } else if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                // enable view finder on touch
                viewFinder.setOnTouchListener(onTouchListener);
                // Toast.makeText(getApplicationContext(), "On Touch Cancel - Sheet", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Chip Selected
        colorsChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Toast.makeText(getApplicationContext(), "Checked ID" + checkedId, Toast.LENGTH_SHORT).show();
            selectedChipId = checkedId;
        });

        // Slider 1 Selected
        color1Slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                // Toast.makeText(getApplicationContext(), "onValueChange called - Slider", Toast.LENGTH_SHORT).show();

                // get HSV values of RGB color
                int red = Color.red(color1);
                int green = Color.green(color1);
                int blue = Color.blue(color1);
                float[] hsv = new float[3];
                Color.RGBToHSV(red, green, blue, hsv);
                Log.d(TAG, "HSV of Color" + Arrays.toString(hsv));

                // change v (lightness) according to value of slider
                hsv[2] = value/100;
                Log.d(TAG, "Modified HSV of Color" + Arrays.toString(hsv));

                // convert hsv to color
                int color1New = Color.HSVToColor(hsv);
                Log.d(TAG, "RGB Color 1 (int)" + color1);
                Log.d(TAG, "New RGB Color 1 (int)" + color1New);

                color1 = color1New;

                // update gradient
                // colorDisplay.setBackgroundColor(color1New);
                int[] colors = {color1New, color2};
                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                gradientBox.setBackground(gradientDrawable);

            }
        });

        // Slider 2 Selected
        color2Slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                // Toast.makeText(getApplicationContext(), "onValueChange called - Slider", Toast.LENGTH_SHORT).show();

                // get HSV values of RGB color
                int red = Color.red(color2);
                int green = Color.green(color2);
                int blue = Color.blue(color2);
                float[] hsv = new float[3];
                Color.RGBToHSV(red, green, blue, hsv);
                Log.d(TAG, "HSV of Color" + Arrays.toString(hsv));

                // change v (lightness) according to value of slider
                hsv[2] = value/100;
                Log.d(TAG, "Modified HSV of Color" + Arrays.toString(hsv));

                // convert hsv to color
                int color2New = Color.HSVToColor(hsv);
                Log.d(TAG, "RGB Color 2 (int)" + color2);
                Log.d(TAG, "New RGB Color 2 (int)" + color2New);

                color2 = color2New;

                // update gradient
                // colorDisplay.setBackgroundColor(color1New);
                int[] colors = {color1, color2New};
                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                gradientBox.setBackground(gradientDrawable);

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