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

import com.daryl.tap2gradient.Data.Color;
import com.daryl.tap2gradient.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class CopyGradientCodeSheetDialog extends BottomSheetDialog implements View.OnClickListener {

    private static final String TAG = CopyGradientCodeSheetDialog.class.getSimpleName();

    private final Context context;

    // Views
    private ImageButton closeButton;
    private TextView cssCodeTextView, swiftUICodeTextView;
    private ImageButton copyCSSButton, copySwiftUIButton;
    private Color color1, color2;

    public CopyGradientCodeSheetDialog(@NonNull Context context, int bottomSheetLayout) {
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
        closeButton = findViewById(R.id.close_copy_gradient_code_app_compat_image_button);
        closeButton.setOnClickListener(this::onClick);

        cssCodeTextView = findViewById(R.id.css_code_gradient_color_text_view);
        swiftUICodeTextView = findViewById(R.id.swift_code_gradient_color_text_view);

        copyCSSButton = findViewById(R.id.copy_css_code_image_button);
        copySwiftUIButton = findViewById(R.id.copy_swift_code_image_button);
        copyCSSButton.setOnClickListener(this::onClick);
        copySwiftUIButton.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_copy_gradient_code_app_compat_image_button:
                dismiss();
                break;
            case R.id.copy_css_code_image_button:
                saveToClipboard(generateCSSCode());
                break;
            case R.id.copy_swift_code_image_button:
                saveToClipboard(generateSwiftUICode());
                break;
        }
    }

    public void updateViews() {
        cssCodeTextView.setText(generateCSSCode());
        swiftUICodeTextView.setText(generateSwiftUICode());
    }

    private String generateCSSCode() {
        return String.format("linear-gradient(%ddeg, %s %s)", 0, color1.getHEX(), color2.getHEX());
    }

    private String generateSwiftUICode() {
        String c1 = String.format("Color(\"%s\")", color1.getHEX());
        String c2 = String.format("Color(\"%s\")", color2.getHEX());
        return String.format("LinearGradient(gradient: Gradient(colors: [%s, %s], startPoint: .leading, .endpoint, .trailing))", c1, c2);
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

