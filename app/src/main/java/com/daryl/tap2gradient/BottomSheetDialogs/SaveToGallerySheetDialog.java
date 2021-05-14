package com.daryl.tap2gradient.BottomSheetDialogs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.daryl.tap2gradient.Data.Color;
import com.daryl.tap2gradient.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class SaveToGallerySheetDialog extends BottomSheetDialog
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = SaveToGallerySheetDialog.class.getSimpleName();

    private Context context;

    // Views
    // -> Top
    private ImageButton closeButton;
    // -> Preview Picture
    private View gradientView, leftDivider;
    private LinearLayout previewPictureLinearLayout, color1LinearLayout, color2LinearLayout;
    private TextView color1LabelTextView, color2LabelTextView;
    // -> Picture Options
    private ChipGroup pictureOptionsChipGroup;
    private Chip gradientCheckChip, hexValueCheckChip;
    // -> Bottom
    private AppCompatButton saveImageButton;

    private Color color1, color2;

    public SaveToGallerySheetDialog(@NonNull Context context, int bottomSheetLayout) {
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

        // Hide Hex Value, Gradient in Preview Picture, Left Divider
        color1LabelTextView.setVisibility(View.GONE);
        color2LabelTextView.setVisibility(View.GONE);
        gradientView.setVisibility(View.GONE);
        previewPictureLinearLayout.setWeightSum(2);
        leftDivider.setVisibility(View.GONE);
    }

    private void initViews() {
        // -> Top
        closeButton = findViewById(R.id.close_save_to_gallery_app_compat_image_button);
        closeButton.setOnClickListener(this::onClick);

        // -> Preview Picture
        gradientView = findViewById(R.id.gradient_color_preview_view);
        leftDivider = findViewById(R.id.left_divider);
        previewPictureLinearLayout = findViewById(R.id.preview_picture_linear_layout);
        color1LinearLayout = findViewById(R.id.gradient_color1_preview_linear_layout);
        color2LinearLayout = findViewById(R.id.gradient_color2_preview_linear_layout);
        color1LabelTextView = findViewById(R.id.gradient_color1_preview_label);
        color2LabelTextView = findViewById(R.id.gradient_color2_preview_label);

        // -> Picture Options
        pictureOptionsChipGroup = findViewById(R.id.picture_options_chip_group);
        gradientCheckChip = findViewById(R.id.gradient_check_chip);
        hexValueCheckChip = findViewById(R.id.hex_value_check_chip);
        gradientCheckChip.setOnCheckedChangeListener(this::onCheckedChanged);
        hexValueCheckChip.setOnCheckedChangeListener(this::onCheckedChanged);


        // -> Bottom
        saveImageButton = findViewById(R.id.save_appcompat_button);
        saveImageButton.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_save_to_gallery_app_compat_image_button:
                dismiss();
                break;
            case R.id.save_appcompat_button:
                saveToGallery(String.format("%s\n%s", color1.getAll(), color2.getAll()));
                break;
        }
    }

    public void updateViews() {
        int[] color1RGBInt = color1.getRGBInt();
        int intColor1 = android.graphics.Color.rgb(color1RGBInt[0], color1RGBInt[1], color1RGBInt[2]);

        int[] color2RGBInt = color2.getRGBInt();
        int intColor2 = android.graphics.Color.rgb(color2RGBInt[0], color2RGBInt[1], color2RGBInt[2]);

        // Gradient Preview
        int[] colors = new int[]{
                android.graphics.Color.rgb(color1RGBInt[0], color1RGBInt[1], color1RGBInt[2]),
                android.graphics.Color.rgb(color2RGBInt[0], color2RGBInt[1], color2RGBInt[2])
        };
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        // top-left, top-right, bottom-right, bottom-left.
        gradientDrawable.setCornerRadii(new float[]{20, 0, 0, 0, 0, 0, 0, 20});
        gradientView.setBackground(gradientDrawable);
        gradientView.setBackgroundTintList(null);

        // Color 1 Preview & Label
        color1LinearLayout.setBackgroundTintList(ColorStateList.valueOf(intColor1));
        if (isDark(intColor1)) {
            color1LabelTextView.setTextColor(context.getColor(R.color.white));
        }
        color1LabelTextView.setText(color1.getHEX());

        // Color 2 Preview & Label
        color2LinearLayout.setBackgroundTintList(ColorStateList.valueOf(intColor2));
        if (isDark(intColor2)) {
            color2LabelTextView.setTextColor(context.getColor(R.color.white));
        }
        color2LabelTextView.setText(color2.getHEX());

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.gradient_check_chip) {
            if (isChecked) {
                Log.e(TAG, "gradient check chip is checked");
                gradientView.setVisibility(View.VISIBLE);
                // Change Weight Sum of Parent to 3
                previewPictureLinearLayout.setWeightSum(3);
                leftDivider.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "gradient check chip is not checked");
                gradientView.setVisibility(View.GONE);
                // Change Weight Sum of Parent to 2
                previewPictureLinearLayout.setWeightSum(2);
                leftDivider.setVisibility(View.GONE);
            }
        }

        if (id == R.id.hex_value_check_chip) {
            if (isChecked) {
                Log.e(TAG, "hex value check chip is checked");
                color1LabelTextView.setVisibility(View.VISIBLE);
                color2LabelTextView.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "hex value check chip is not checked");
                color1LabelTextView.setVisibility(View.GONE);
                color2LabelTextView.setVisibility(View.GONE);
            }
        }
    }

    private void saveToGallery(String text) {
        // Log.e(TAG, "in saveToGallery");
        // Save to Gallery
        Bitmap gImage = gradientColorValuesBitmap();
        // gradientImage.setImageBitmap(gImage);
        MediaStore.Images.Media.insertImage(
                context.getContentResolver(),
                gImage,
                "Gradient Color Values\n" + text,
                "Created with Tap 2 Gradient"
        );
        Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show();
    }

    private Bitmap gradientColorValuesBitmap() {
        List<Integer> checkedIds = pictureOptionsChipGroup.getCheckedChipIds();

        int BITMAP_WIDTH_PER_VIEW = 235;
        int BITMAP_WIDTH_FULL = 709;
        int BITMAP_HEIGHT = 400;
        float COLOR1_X_TEXT = BITMAP_WIDTH_PER_VIEW + 2;
        float COLOR2_X_TEXT = (BITMAP_WIDTH_PER_VIEW * 2) + 2;
        float COLOR_Y_TEXT = 365;

        if (!checkedIds.contains(R.id.gradient_check_chip)) {
            BITMAP_WIDTH_FULL--;
            BITMAP_WIDTH_PER_VIEW = 353;
            COLOR1_X_TEXT = 0;
            COLOR2_X_TEXT = BITMAP_WIDTH_PER_VIEW + 2;
        }

        // Base Image for Gradient & Text
        Bitmap baseBitmap = Bitmap.createBitmap(BITMAP_WIDTH_FULL, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas baseCanvas = new Canvas(baseBitmap);
        baseCanvas.drawColor(android.graphics.Color.WHITE);

        // -> Gradient View
        Bitmap gradientBitmap = Bitmap.createBitmap(BITMAP_WIDTH_PER_VIEW, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas gradientCanvas = new Canvas(gradientBitmap);

        Drawable backgroundDrawable = gradientView.getBackground();
        if (backgroundDrawable != null)
            backgroundDrawable.draw(gradientCanvas);
        else
            gradientCanvas.drawColor(android.graphics.Color.WHITE);

        // -> Color 1 View
        Bitmap color1Bitmap = Bitmap.createBitmap(BITMAP_WIDTH_PER_VIEW, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas color1Canvas = new Canvas(color1Bitmap);

        Drawable color1BgDrawable = color1LinearLayout.getBackground();
        if (color1BgDrawable != null)
            color1BgDrawable.draw(color1Canvas);
        else
            color1Canvas.drawColor(android.graphics.Color.WHITE);

        // -> Color 2 View
        Bitmap color2Bitmap = Bitmap.createBitmap(BITMAP_WIDTH_PER_VIEW, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas color2Canvas = new Canvas(color2Bitmap);

        Drawable color2BgDrawable = color2LinearLayout.getBackground();
        if (color2BgDrawable != null)
            color2BgDrawable.draw(color2Canvas);
        else
            color2Canvas.drawColor(android.graphics.Color.WHITE);

        // Text Style
        Paint paint = new Paint();
        paint.setTextSize(toPx(10));
        paint.setTextScaleX(1.f);
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand));
        paint.setColor(android.graphics.Color.BLACK);
        paint.setAntiAlias(true);

        // -> Text Color 1
        int[] RGBIntValues1 = color1.getRGBInt();
        if (isDark(android.graphics.Color.rgb(RGBIntValues1[0], RGBIntValues1[1], RGBIntValues1[2]))) {
            paint.setColor(android.graphics.Color.WHITE);
        }

        // -> Text Color 2
        int[] RGBIntValues2 = color2.getRGBInt();
        if (isDark(android.graphics.Color.rgb(RGBIntValues2[0], RGBIntValues2[1], RGBIntValues2[2]))) {
            paint.setColor(android.graphics.Color.WHITE);
        }

        // --- Draw Colors View with Gradient View, Color Values Text to Canvas

        if (checkedIds.contains(R.id.gradient_check_chip)) {
            // Draw Color Gradient View
            baseCanvas.drawBitmap(gradientBitmap, 0, 0, null);
            // Draw Color 1 & 2 View
            baseCanvas.drawBitmap(color1Bitmap, BITMAP_WIDTH_PER_VIEW + 2, 0, null);
            baseCanvas.drawBitmap(color2Bitmap, (BITMAP_WIDTH_PER_VIEW * 2) + 4, 0, null);
        } else {
            // Draw Color 1 & 2 View
            baseCanvas.drawBitmap(color1Bitmap, 0, 0, null);
            baseCanvas.drawBitmap(color2Bitmap, BITMAP_WIDTH_PER_VIEW + 2, 0, null);
        }

        if (checkedIds.contains(R.id.hex_value_check_chip)) {
            if (checkedIds.contains(R.id.gradient_check_chip)) {
                baseCanvas.drawText(color1.getHEX(), COLOR1_X_TEXT + 50, COLOR_Y_TEXT, paint);
                baseCanvas.drawText(color2.getHEX(), COLOR2_X_TEXT + 50, COLOR_Y_TEXT, paint);
            } else {
                baseCanvas.drawText(color1.getHEX(), COLOR1_X_TEXT + 100, COLOR_Y_TEXT, paint);
                baseCanvas.drawText(color2.getHEX(), COLOR2_X_TEXT + 100, COLOR_Y_TEXT, paint);
            }
        }



        return baseBitmap;

    }

    private int toPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        int px = (int) (dp * density);
        return px;
    }

    boolean isDark(int color) {
        return ColorUtils.calculateLuminance(color) < 0.5;
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

