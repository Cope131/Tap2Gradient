package com.daryl.tap2gradient.Activities;

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

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daryl.tap2gradient.BottomSheetDialogs.CopyColorValuesBottomSheetDialog;
import com.daryl.tap2gradient.BottomSheetDialogs.CopyGradientCodeSheetDialog;
import com.daryl.tap2gradient.BottomSheetDialogs.CustomizeAppBottomSheetDialog;
import com.daryl.tap2gradient.BottomSheetDialogs.SaveToGallerySheetDialog;
import com.daryl.tap2gradient.Pointer.PixelPointer;
import com.daryl.tap2gradient.Pointer.PixelPointerColorPreview;
import com.daryl.tap2gradient.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

public class HomeActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnTouchListener, Slider.OnChangeListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    // Permissions
    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // CameraX
    private PreviewView viewFinder;
    private ExecutorService cameraExecutor;

    // Views
    // -> Top
    private ImageButton customizeAppButton;

    // -> Persistent Bottom Sheet Content
    // --> Top
    private MaterialButton draggableButton;
    // --> Color Selectors
    private ImageButton color1SelectorImageButton, color2SelectorImageButton;
    private View color1SelectedView, color2SelectedView;
    private ColorNum selectedColor = ColorNum.COLOR_1;
    // --> Gradient Preview
    private View gradientView;
    // --> Adjust Lightness
    private Slider color1Slider, color2Slider;
    private View color1SliderView, color2SliderView;

    // -> Bottom Bar
    private ImageButton copyColorValuesButton, saveToGalleryButton, copyGradientCodeButton;


    // Bottom Sheet Dialog
    // -> Copy Color Values
    private CopyColorValuesBottomSheetDialog copyColorValuesBottomSheetDialog;
    // -> Save to Galley
    private SaveToGallerySheetDialog saveToGallerySheetDialog;
    // -> Copy Gradient Code for Developers
    private CopyGradientCodeSheetDialog copyGradientCodeSheetDialog;
    // -> Customize App
    private CustomizeAppBottomSheetDialog customizeAppBottomSheetDialog;


    // Persistent Bottom Sheet
    private LinearLayout persBottomSheet;
    private BottomSheetBehavior persBottomSheetBehavior;
    private final MyBottomBehavior myBottomSheetBehavior = new MyBottomBehavior();

    // Pointer
    private FrameLayout pixelPointerMainView;

    View.OnTouchListener onTouchListener;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Views
        initViews();

        // Initialize Bottom Sheet Behaviour (Init Bottom Sheet View First)
        persBottomSheetBehavior = BottomSheetBehavior.from(persBottomSheet);

        // Initialize Bottom Sheet Dialogs
        copyColorValuesBottomSheetDialog =
                new CopyColorValuesBottomSheetDialog(this, R.layout.bottom_sheet_dialog_copy_color_values);
        saveToGallerySheetDialog =
                new SaveToGallerySheetDialog(this, R.layout.bottom_sheet_dialog_save_to_gallery);
        copyGradientCodeSheetDialog =
                new CopyGradientCodeSheetDialog(this, R.layout.bottom_sheet_dialog_copy_gradient_code);
        customizeAppBottomSheetDialog = new CustomizeAppBottomSheetDialog(this, R.layout.bottom_sheet_dialog_customize_app);


        // Initialize Camera
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Request Camera Permission If Not Granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Handle Selection of Pixel
        initOnTouchListener();
        viewFinder.setOnTouchListener(onTouchListener);

        // Handle onTouch of the Camera View Finder when Touching Sheet
        persBottomSheet.setOnTouchListener(this::onTouch);

        // Handle Bottom Sheet Dragging
        persBottomSheetBehavior.addBottomSheetCallback(myBottomSheetBehavior);


    } // <--- end of onCreate method --->


    // ============================================================================================
    private void initViews() {
        // Pointer
        pixelPointerMainView = findViewById(R.id.pixelPointerFrameLayout);

        // Top
        customizeAppButton = findViewById(R.id.customize_app_image_button);
        customizeAppButton.setOnClickListener(this::onClick);

        // CameraX
        viewFinder = findViewById(R.id.viewFinder);

        // Persistent Bottom Sheet
        persBottomSheet = findViewById(R.id.persBottomSheet);

        // -> Content
        draggableButton = findViewById(R.id.draggableButton);

        color1SelectorImageButton = findViewById(R.id.color1_selector_image_button);
        color2SelectorImageButton = findViewById(R.id.color2_selector_image_button);
        color1SelectorImageButton.setOnClickListener(this::onClick);
        color2SelectorImageButton.setOnClickListener(this::onClick);

        color1SelectedView = findViewById(R.id.color1_selected_ring);
        color2SelectedView = findViewById(R.id.color2_selected_ring);
        color2SelectedView.setVisibility(View.GONE);


        gradientView = findViewById(R.id.gradient_view);

        color1Slider = findViewById(R.id.color1Slider);
        color2Slider = findViewById(R.id.color2Slider);
        color1Slider.addOnChangeListener(this::onValueChange);
        color2Slider.addOnChangeListener(this::onValueChange);

        // Disable Slider (No color selected)
        color1Slider.setEnabled(false);
        color2Slider.setEnabled(false);

        color1SliderView = findViewById(R.id.color1_slider_view);
        color2SliderView = findViewById(R.id.color2_slider_view);

        // Initial color of slider view
        color1SliderView.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(255, 255, 255)));
        color2SliderView.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(255, 255, 255)));

        // Bottom Bar
        copyColorValuesButton = findViewById(R.id.copy_color_values_app_image_button);
        saveToGalleryButton = findViewById(R.id.save_gradient_color_to_gallery_app_image_button);
        copyGradientCodeButton = findViewById(R.id.copy_gradient_code_image_button);
        copyColorValuesButton.setOnClickListener(this::onClick);
        saveToGalleryButton.setOnClickListener(this::onClick);
        copyGradientCodeButton.setOnClickListener(this::onClick);
    }

    // ============================================================================================

    // View is Clicked
    @Override
    public void onClick(View v) {
        com.daryl.tap2gradient.Data.Color[] colors = getColors();
        switch (v.getId()) {
            case R.id.customize_app_image_button:
                customizeAppBottomSheetDialog.show();
                break;
            case R.id.copy_color_values_app_image_button:

                if (colors != null) {
                    copyColorValuesBottomSheetDialog.setColor1(colors[0]);
                    copyColorValuesBottomSheetDialog.setColor2(colors[1]);
                    copyColorValuesBottomSheetDialog.updateViews();
                    copyColorValuesBottomSheetDialog.getBehavior().setFitToContents(true);
                    copyColorValuesBottomSheetDialog.show();
                }
                else {
                    Toast.makeText(this, "Please Select Colors", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.save_gradient_color_to_gallery_app_image_button:
                if (colors != null) {
                    saveToGallerySheetDialog.setColor1(colors[0]);
                    saveToGallerySheetDialog.setColor2(colors[1]);
                    saveToGallerySheetDialog.updateViews();
                    saveToGallerySheetDialog.getBehavior().setFitToContents(true);
                    saveToGallerySheetDialog.show();
                } else {
                    Toast.makeText(this, "Please Select Colors", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.copy_gradient_code_image_button:
                if (colors != null) {
                    copyGradientCodeSheetDialog.setColor1(colors[0]);
                    copyGradientCodeSheetDialog.setColor2(colors[1]);
                    copyGradientCodeSheetDialog.updateViews();
                    copyGradientCodeSheetDialog.getBehavior().setFitToContents(true);
                    copyGradientCodeSheetDialog.show();
                } else {
                    Toast.makeText(this, "Please Select Colors", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.color1_selector_image_button:
                selectedColor = ColorNum.COLOR_1;
                color1SelectedView.setVisibility(View.VISIBLE);
                color2SelectedView.setVisibility(View.GONE);
                break;
            case R.id.color2_selector_image_button:
                selectedColor = ColorNum.COLOR_2;
                color1SelectedView.setVisibility(View.GONE);
                color2SelectedView.setVisibility(View.VISIBLE);
                break;
        }
    }

    // Value of Slider is Changed
    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        switch (slider.getId()) {
            case R.id.color1Slider:
                // Log.e(TAG, "onValueChange called - Color 1 Slider");
                // enable Slider
                color1Slider.setEnabled(true);

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
                gradientView.setBackground(gradientDrawable);

                // update color 1 view
                color1SliderView.setBackgroundTintList(ColorStateList.valueOf(color1));
                color1SelectorImageButton.setBackgroundTintList(ColorStateList.valueOf(color1));

                break;
            case R.id.color2Slider:
                // Log.e(TAG, "onValueChange called - Color 2 Slider");
                // enable Slider
                color2Slider.setEnabled(true);

                // get HSV values of RGB color
                int red2 = Color.red(color2);
                int green2 = Color.green(color2);
                int blue2 = Color.blue(color2);
                float[] hsv2 = new float[3];
                Color.RGBToHSV(red2, green2, blue2, hsv2);
                Log.d(TAG, "HSV of Color" + Arrays.toString(hsv2));

                // change v (lightness) according to value of slider
                hsv2[2] = value / 100;
                Log.d(TAG, "Modified HSV of Color" + Arrays.toString(hsv2));

                // convert hsv to color
                int color2New = Color.HSVToColor(hsv2);
                Log.d(TAG, "RGB Color 2 (int)" + color2);
                Log.d(TAG, "New RGB Color 2 (int)" + color2New);

                color2 = color2New;

                // update gradient
                int[] colors2 = {color1, color2New};
                GradientDrawable gradientDrawable2 = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors2);
                gradientView.setBackground(gradientDrawable2);

                // update color 2 view
                color2SliderView.setBackgroundTintList(ColorStateList.valueOf(color2));
                color2SelectorImageButton.setBackgroundTintList(ColorStateList.valueOf(color2));

                break;
        }
    }

    // View is Touched
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        // Bottom Sheet Selected or Swiped
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            // Disable view finder on touch
            viewFinder.setOnTouchListener(null);
            //Log.e(TAG, "On Touch Down - Sheet");
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            // Enable view finder on touch
            viewFinder.setOnTouchListener(onTouchListener);
            //Log.e(TAG, "On Touch Up - Sheet");
        } else if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            // Enable view finder on touch
            viewFinder.setOnTouchListener(onTouchListener);
            //Log.e(TAG, "On Touch Cancel - Sheet");
        }
        return true;
    }

    private void initOnTouchListener() {
        onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int eventAction = motionEvent.getAction();
                // selected x and y coordinates
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();

                if (eventAction == MotionEvent.ACTION_DOWN) {
                    // Log.e(TAG, "onTouch - Action Down");

                } else if (eventAction == MotionEvent.ACTION_UP) {
                    // Log.e(TAG, "onTouch - Action UP");

                    pixelPointerMainView.removeAllViews();

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
                        if (selectedColor == ColorNum.COLOR_1) {
                            color1 = color;
                            color1SelectorImageButton.setBackgroundTintList(ColorStateList.valueOf(color1));
                        } else {
                            color2 = color;
                            color2SelectorImageButton.setBackgroundTintList(ColorStateList.valueOf(color2));
                        }

                        // display color using text
                        int[] colors = {color1, color2};
                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
                        // top-left, top-right, bottom-right, bottom-left.
                        gradientDrawable.setCornerRadii(new float[]{20, 0, 0, 0, 0, 0, 0, 20});
                        gradientView.setBackground(gradientDrawable);
                        gradientView.setBackgroundTintList(null);

                        // update slider for lightness of the color
                        float[] hsv = new float[3];
                        Color.RGBToHSV(valueR, valueG, valueB, hsv);
                        float v = hsv[2] * 100;

                        if (selectedColor == ColorNum.COLOR_1) {
                            color1Slider.setValue(v);
                        } else {
                            color2Slider.setValue(v);
                        }

                    } else {
                        Log.d(TAG, "matFrame is Null: ");
                    }

                } else if (eventAction == MotionEvent.ACTION_DOWN || eventAction == MotionEvent.ACTION_MOVE) {
                    // Log.e(TAG, "onTouch - Action UP or Action DOWN");

                    int color = 0;
                    if (matFrame != null && y > 0 && x > 0) {
                        double[] rgbValues = matFrame.get(y, x);
                        int valueR = (int) rgbValues[0];
                        int valueG = (int) rgbValues[1];
                        int valueB = (int) rgbValues[2];
                        color = Color.rgb(valueR, valueG, valueB);
                    }

                    pixelPointerMainView.removeAllViews();

                    Log.e(TAG, "x: " + x + " y: " + y);

                    // Pixel Pointer
                    if (x > 0 && y > 0) {
                        PixelPointer pointer = new PixelPointer(HomeActivity.this, x, y, 15);
                        pixelPointerMainView.addView(pointer);
                    }

                    // Preview Color
                    int y2 = y - 150;
                    if (x > 0 && y2 > 0) {
                        PixelPointerColorPreview preview = new PixelPointerColorPreview(
                                HomeActivity.this, x, y2, 50, color);
                        pixelPointerMainView.addView(preview);
                    }

                }

                return true;
            }
        };
    }

    // ============================================================================================
    // CameraX
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
                // Log.d(TAG, "Image Info: " + image.getImageInfo());

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

    // ============================================================================================
    // Permissions
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

    // ============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
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

    // ============================================================================================
    private com.daryl.tap2gradient.Data.Color[] getColors() {

        if (color1 != 0 && color2 != 0) {
            // --- Color 1
            com.daryl.tap2gradient.Data.Color c1 = new com.daryl.tap2gradient.Data.Color();

            // -> Set Color 1 Hex
            c1.setHEX("#" + Integer.toHexString(color1).substring(2).toUpperCase());

            // -> Set Color 1 RGB
            int red = Color.red(color1);
            int green = Color.green(color1);
            int blue = Color.blue(color1);
            c1.setRGBInt(new int[]{red, green, blue});

            // -> Set Color 1 HSV
            float[] hsv = new float[3];
            Color.RGBToHSV(red, green, blue, hsv);
            c1.setHSVInt(new float[]{hsv[0], hsv[1] * 100, hsv[2] * 100});

            // --- Color 2
            com.daryl.tap2gradient.Data.Color c2 = new com.daryl.tap2gradient.Data.Color();

            // -> Set Color 2 Hex
            c2.setHEX("#" + Integer.toHexString(color2).substring(2).toUpperCase());

            // -> Set Color 2 RGB
            red = Color.red(color2);
            green = Color.green(color2);
            blue = Color.blue(color2);
            c2.setRGBInt(new int[]{red, green, blue});

            // -> Set Color 2 HSV
            hsv = new float[3];
            Color.RGBToHSV(red, green, blue, hsv);
            c2.setHSVInt(new float[]{hsv[0], hsv[1] * 100, hsv[2] * 100});

            return new com.daryl.tap2gradient.Data.Color[]{c1, c2};
        }
        return null;
    }

    private void showSaveOptionsDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(HomeActivity.this, R.style.Transparent_AlertDialog);
        View view = HomeActivity.this.getLayoutInflater().inflate(R.layout.alert_dialog, null);
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


    private Bitmap gradientColorValuesBitmap() {

        // Base Image for Gradient & Text
        Bitmap baseBitmap = Bitmap.createBitmap(700, 400, Bitmap.Config.ARGB_8888);
        Canvas baseCanvas = new Canvas(baseBitmap);
        baseCanvas.drawColor(Color.WHITE);

        // Gradient Box Image
        Bitmap gradientBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        Canvas gradientCanvas = new Canvas(gradientBitmap);

        Drawable backgroundDrawable = gradientView.getBackground();
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

    // ============================================================================================
    // Persistent Bottom Sheet Behavior
    private class MyBottomBehavior extends BottomSheetBehavior.BottomSheetCallback {
        private float lastSlideOffSet = 0;

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            // Change Save Button Color Dynamically
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                Log.d(TAG, "onStateChanged - Sheet Expanded");
//                saveFAB.setIconTintResource(R.color.save_icon_color_on_sheet);
//                saveFAB.setTextColor(getResources().getColorStateList(R.color.black, getTheme()));
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                Log.d(TAG, "onStateChanged - Sheet Collapsed");
//                saveFAB.setIconTintResource(R.color.save_icon_color);
//                saveFAB.setTextColor(getResources().getColorStateList(R.color.white, getTheme()));
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            Log.d(TAG, "SlideOffSet: " + slideOffset);
            if (slideOffset > 0.8) {
//                saveFAB.setIconTintResource(R.color.save_icon_color_on_sheet);
//                saveFAB.setTextColor(getResources().getColorStateList(R.color.black, getTheme()));
            } else if (slideOffset < 0.4) {
//                saveFAB.setIconTintResource(R.color.save_icon_color);
//                saveFAB.setTextColor(getResources().getColorStateList(R.color.white, getTheme()));
            }

            // Swiping Up
            if (slideOffset > lastSlideOffSet) {
                if (slideOffset > 0.99f) {
                    getWindow().setStatusBarColor(getColor(R.color.white));
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    persBottomSheet.setBackground(getDrawable(R.drawable.pers_bottom_sheet_bg_expanded));
                    draggableButton.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.white));
                }
                if (slideOffset > 0.5) {
                    // getSupportActionBar().show();
                }
            }
            // Swiping Down
            else if (slideOffset < lastSlideOffSet) {
                if (slideOffset < 0.99f) {
                    getWindow().setStatusBarColor(getColor(R.color.transparent));
                    getWindow().getDecorView().setSystemUiVisibility(0);
                    persBottomSheet.setBackground(getDrawable(R.drawable.pers_bottom_sheet_bg));
                    draggableButton.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.gray_2));
                }
                if (slideOffset < 0.5) {
                    // getSupportActionBar().hide();
                }
            }
            lastSlideOffSet = slideOffset;
        }

    }

    private enum ColorNum {
        COLOR_1, COLOR_2
    }

}