package com.daryl.tap2gradient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    // Views
    private PreviewView viewFinder;
    private LinearLayout gradientBox;

    private ExtendedFloatingActionButton saveFAB;
    private LinearLayout persBottomSheet;
    private BottomSheetBehavior sheetBehavior;

    private ChipGroup colorsChipGroup;
    private Chip color1Chip;
    private Chip color2Chip;
    private int selectedChipId;

    private Slider color1Slider;
    private Slider color2Slider;

    // private ImageView gradientImage;

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

        saveFAB = findViewById(R.id.saveFAB);
        persBottomSheet = findViewById(R.id.persBottomSheet);
        sheetBehavior = BottomSheetBehavior.from(persBottomSheet);

        colorsChipGroup = findViewById(R.id.colorsChipGroup);
        color1Chip = findViewById(R.id.color1Chip);
        color2Chip = findViewById(R.id.color2Chip);

        selectedChipId = color1Chip.getId(); // Selected by Default

        color1Slider = findViewById(R.id.color1Slider);
        color2Slider = findViewById(R.id.color2Slider);



        // gradientImage = findViewById(R.id.gradientImageView);


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
                    float v = hsv[2] * 100;

                    if (selectedChipId == color1Chip.getId())
                        color1Slider.setValue(v);
                    else if (selectedChipId == color2Chip.getId())
                        color2Slider.setValue(v);


                } else {
                    Log.d(TAG, "matFrame is Null: ");
                }

            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                //Toast.makeText(getApplicationContext(), "onTouch Up - Camera", Toast.LENGTH_SHORT).show();

                String colorChipSelected = selectedChipId == color1Chip.getId() ?
                        "Color 1 Selected" : "Color 2 Selected";
                Toast.makeText(getApplicationContext(), colorChipSelected, Toast.LENGTH_SHORT).show();
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
            Log.d(TAG, "Checked ID" + checkedId);
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
                hsv[2] = value / 100;
                Log.d(TAG, "Modified HSV of Color" + Arrays.toString(hsv));

                // convert hsv to color
                int color1New = Color.HSVToColor(hsv);
                Log.d(TAG, "RGB Color 1 (int)" + color1);
                Log.d(TAG, "New RGB Color 1 (int)" + color1New);

                color1 = color1New;

                // update gradient
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
                hsv[2] = value / 100;
                Log.d(TAG, "Modified HSV of Color" + Arrays.toString(hsv));

                // convert hsv to color
                int color2New = Color.HSVToColor(hsv);
                Log.d(TAG, "RGB Color 2 (int)" + color2);
                Log.d(TAG, "New RGB Color 2 (int)" + color2New);

                color2 = color2New;

                // update gradient
                int[] colors = {color1, color2New};
                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                gradientBox.setBackground(gradientDrawable);

            }
        });

        // Change Save Button Color Dynamically
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    Log.d(TAG, "onStateChanged - Sheet Expanded");
                    saveFAB.setIconTintResource(R.color.save_icon_color_on_sheet);
                    saveFAB.setTextColor(getResources().getColorStateList(R.color.black, getTheme()));
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    Log.d(TAG, "onStateChanged - Sheet Collapsed");
                    saveFAB.setIconTintResource(R.color.save_icon_color);
                    saveFAB.setTextColor(getResources().getColorStateList(R.color.white, getTheme()));
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.d(TAG, "SlideOffSet: " + slideOffset);
                if (slideOffset > 0.8) {
                    saveFAB.setIconTintResource(R.color.save_icon_color_on_sheet);
                    saveFAB.setTextColor(getResources().getColorStateList(R.color.black, getTheme()));
                } else if (slideOffset < 0.4) {
                    saveFAB.setIconTintResource(R.color.save_icon_color);
                    saveFAB.setTextColor(getResources().getColorStateList(R.color.white, getTheme()));
                }
            }
        });

        // Save Button Clicked
        saveFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (color1 != 0 && color2 != 0)
                    showSaveOptionsDialog();
                else
                    Toast.makeText(getApplicationContext(), "Please Select Colors", Toast.LENGTH_SHORT).show();
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

    private void showSaveOptionsDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this, R.style.Transparent_AlertDialog);
        View view = MainActivity.this.getLayoutInflater().inflate(R.layout.alert_dialog, null);
        adb.setView(view);

        // init views in dialog
        Button okButton = view.findViewById(R.id.dialogOkButton);
        Button cancelButton = view.findViewById(R.id.dialogCancelButton);
        TextView color1ValuesTv = view.findViewById(R.id.color1ValuesDialogTextView);
        TextView color2ValuesTv = view.findViewById(R.id.color2ValuesDialogTextView);
        ChipGroup saveOptionsChipGrp = view.findViewById(R.id.saveOptionsChipGroup);
        LinearLayout gradientBoxDialog = view.findViewById(R.id.gradientBoxDialog);

        // display color values
        String color1ValuesStr = getColorValues(color1);
        String color2ValuesStr = getColorValues(color2);
        color1ValuesTv.setText(color1ValuesStr);
        color2ValuesTv.setText(color2ValuesStr);

        // display gradient
        int[] colors = {color1, color2};
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        gradientBoxDialog.setBackground(gradientDrawable);

        AlertDialog ad = adb.create();
        ad.show();

        // action
        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.dialogOkButton:
                        doSave(saveOptionsChipGrp, color1ValuesStr + "\n" + color2ValuesStr);
                        ad.dismiss();
                        break;
                    case R.id.dialogCancelButton:
                        ad.dismiss();
                        break;
                }
            }
        };

        okButton.setOnClickListener(onClick);
        cancelButton.setOnClickListener(onClick);
    }

    private String getColorValues(int colorValue) {
        String colorValues = "";

        // hexadecimal
        String hexValue = "#" + Integer.toHexString(colorValue).substring(2).toUpperCase();
        // rgb
        int red = Color.red(colorValue);
        int green = Color.green(colorValue);
        int blue = Color.blue(colorValue);
        String rgbValue = String.format("%d, %d, %d", red, green, blue);
        // hsv
        float[] hsv = new float[3];
        Color.RGBToHSV(red, green, blue, hsv);
        String hsvValue = String.format("%.0f, %.0f, %.0f", hsv[0], hsv[1] * 100, hsv[2] * 100);
        // store
        colorValues = String.format("Rgb: %s\nHex: %s\nHsv: %s", rgbValue, hexValue, hsvValue);

        return colorValues;
    }

    private void doSave(ChipGroup chipGroup, String text) {
        // Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
        List<Integer> checkedIds = chipGroup.getCheckedChipIds();

        if (checkedIds.contains(R.id.galleryCheckChip)) {
            // Save to Gallery
            Bitmap gImage = gradientColorValuesBitmap();
            // gradientImage.setImageBitmap(gImage);
            MediaStore.Images.Media.insertImage(getContentResolver(), gImage, "Gradient Color Values", "Created with Tap 2 Gradient");
            Toast.makeText(getApplicationContext(), "Saved to Gallery", Toast.LENGTH_SHORT).show();
        }
        if (checkedIds.contains(R.id.clipboardCheckChip)) {
            // Copy to Clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Gradient Color Values", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), "Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // image analysis is called again
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    private Bitmap gradientColorValuesBitmap() {

        // Base Image for Gradient & Text
        Bitmap baseBitmap = Bitmap.createBitmap(700, 400, Bitmap.Config.ARGB_8888);
        Canvas baseCanvas = new Canvas(baseBitmap);
        baseCanvas.drawColor(Color.WHITE);

        // Gradient Box Image
        Bitmap gradientBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        Canvas gradientCanvas = new Canvas(gradientBitmap);

        Drawable backgroundDrawable = gradientBox.getBackground();
        if (backgroundDrawable != null)
            backgroundDrawable.draw(gradientCanvas);
        else
            gradientCanvas.drawColor(Color.WHITE);

        // Text
         float scale = getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setTextSize(30);
        paint.setTextScaleX(1.f);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);

        // Text + Gradient Box Image on Base Image

            // Text Color 1
        float x = 40;
        float y = 100;

        baseCanvas.drawText("Color 1", x, y - 40, paint);
        paint.setTextSize(20);

        String[] lines = getColorValues(color1).split("\\R",0);
        for (String line : lines) {
            baseCanvas.drawText(line, x, y, paint);
            y += 30;
        }

            // Text Color 2
        paint.setTextSize(30);
        baseCanvas.drawText("Color 2", x, y + 50, paint);

        y += 50 + 40;
        paint.setTextSize(20);

        lines = getColorValues(color2).split("\\R",0);
        for (String line : lines) {
            baseCanvas.drawText(line, x, y, paint);
            y += 30;
        }

            // Gradient
        baseCanvas.drawBitmap(gradientBitmap, 300, 0, null);

        return baseBitmap;

    }

}