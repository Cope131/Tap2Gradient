package com.daryl.tap2gradient.BottomSheetDialogs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;

import com.daryl.tap2gradient.Data.Color;
import com.daryl.tap2gradient.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class CopyColorValuesBottomSheetDialog extends BottomSheetDialog implements View.OnClickListener {

    private static final String TAG = CopyColorValuesBottomSheetDialog.class.getSimpleName();

    private final Context context;

    // Views
    private ImageButton closeButton;
    private View color1View, color2View;
    private TextView color1Hex, color1Rgb, color1Hsv, color2Hex, color2Rgb, color2Hsv;
    private ImageButton color1HexButton, color1RgbButton, color1HsvButton,
            color2HexButton, color2RgbButton, color2HsvButton;
    private AppCompatButton copyAllButton;
    private Color color1, color2;

    public CopyColorValuesBottomSheetDialog(@NonNull Context context, int bottomSheetLayout) {
        super(context);
        this.context = context;
        setContentView(bottomSheetLayout);

        // Color 1 & 2 Objects
        color1 = new Color();
        color2 = new Color();
        initViews();
        updateViews();

        // Show Full Layout
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        getBehavior().setFitToContents(true);
    }

    private void initViews() {
        closeButton = findViewById(R.id.close_copy_color_values_app_compat_image_button);
        closeButton.setOnClickListener(this::onClick);

        // Color Display
        color1View = findViewById(R.id.gradient_color1_view);
        color2View = findViewById(R.id.gradient_color2_view);

        // Color Values
        color1Hex = findViewById(R.id.gradient_color1_hexadecimal_value);
        color1Rgb = findViewById(R.id.gradient_color1_rgb_value);
        color1Hsv = findViewById(R.id.gradient_color1_hsv_value);
        color2Hex = findViewById(R.id.gradient_color2_hexadecimal_value);
        color2Rgb = findViewById(R.id.gradient_color2_rgb_value);
        color2Hsv = findViewById(R.id.gradient_color2_hsv_value);

        // Copy Color Value Buttons
        color1HexButton = findViewById(R.id.copy_hex_color1_value_image_button);
        color1RgbButton = findViewById(R.id.copy_rgb_color1_value_image_button);
        color1HsvButton = findViewById(R.id.copy_hsv_color1_value_image_button);
        color2HexButton = findViewById(R.id.copy_hex_color2_value_image_button);
        color2RgbButton = findViewById(R.id.copy_rgb_color2_value_image_button);
        color2HsvButton = findViewById(R.id.copy_hsv_color2_value_image_button);
        copyAllButton = findViewById(R.id.copy_all_appcompat_button);

        color1HexButton.setOnClickListener(this::onClick);
        color1RgbButton.setOnClickListener(this::onClick);
        color1HsvButton.setOnClickListener(this::onClick);
        color2HexButton.setOnClickListener(this::onClick);
        color2RgbButton.setOnClickListener(this::onClick);
        color2HsvButton.setOnClickListener(this::onClick);
        copyAllButton.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_copy_color_values_app_compat_image_button:
                dismiss();
                break;
            case R.id.copy_hex_color1_value_image_button:
                saveToClipboard(color1.getHEX());
                break;
            case R.id.copy_rgb_color1_value_image_button:
                saveToClipboard(color1.getFormattedRGBString());
                break;
            case R.id.copy_hsv_color1_value_image_button:
                saveToClipboard(color1.getFormattedHSVString());
                break;
            case R.id.copy_hex_color2_value_image_button:
                saveToClipboard(color2.getHEX());
                break;
            case R.id.copy_rgb_color2_value_image_button:
                saveToClipboard(color2.getFormattedRGBString());
                break;
            case R.id.copy_hsv_color2_value_image_button:
                saveToClipboard(color2.getFormattedHSVString());
                break;
            case R.id.copy_all_appcompat_button:
                saveToClipboard(String.format("Color 1:\n%s\n\nColor 2:\n%s", color1.getAll(), color2.getAll()));
                break;
        }
    }

    public void updateViews() {
        // Color 1
        color1Hex.setText(color1.getHEX());
        color1Rgb.setText(color1.getFormattedRGBString());
        color1Hsv.setText(color1.getFormattedHSVString());

        int[] color1RGBInt = color1.getRGBInt();
        int intColor1 = android.graphics.Color.rgb(color1RGBInt[0], color1RGBInt[1], color1RGBInt[2]);
        color1View.setBackgroundTintList(ColorStateList.valueOf(intColor1));

        // Color 2
        color2Hex.setText(color2.getHEX());
        color2Rgb.setText(color2.getFormattedRGBString());
        color2Hsv.setText(color2.getFormattedHSVString());

        int[] color2RGBInt = color2.getRGBInt();
        int intColor2 = android.graphics.Color.rgb(color2RGBInt[0], color2RGBInt[1], color2RGBInt[2]);
        color2View.setBackgroundTintList(ColorStateList.valueOf(intColor2));
    }


    private void saveToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Gradient Color Values", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
    }



    public Color getColor1() {
        return color1;
    }

    public void setColor1(Color color1) {
        this.color1 = color1;
    }

    public Color getColor2() {
        return color2;
    }

    public void setColor2(Color color2) {
        this.color2 = color2;
    }
}

