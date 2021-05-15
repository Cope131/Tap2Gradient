package com.daryl.tap2gradient.BottomSheetDialogs;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.daryl.tap2gradient.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public class CustomizeAppBottomSheetDialog extends BottomSheetDialog implements View.OnClickListener {

    private static final String TAG = CustomizeAppBottomSheetDialog.class.getSimpleName();

    private final Context context;

    // Views
    // -> Accent
    private ImageButton blueAccentImageButton, yellowGreenImageButton, limeAccentImageButton,
            orchidAccentImageButton, floralAccentImageButton, lavenderAccentImageButton;
    // -> Theme & App Icon
    private ImageButton darkThemeImageButton, lightThemeImageButton,
            classicAppIconImageButton, dreamAppIconImageButton;
    private LinearLayout checkedDarkThemeLinearLayout, checkedLightThemeLinearLayout,
            checkedClassicAppIconThemeLinearLayout, checkedDreamAppIconThemeLinearLayout;
    // -> Bottom
    MaterialButton cancelButton, applyButton;

    // Track Checked Items
    // -> Accent
    private CheckedAccent accentCheckedItem = CheckedAccent.BLUE;
    // -> Theme
    private CheckedItem themeCheckedItem = CheckedItem.LIGHT_THEME;
    // -> App Icon
    private CheckedItem iconCheckedItem = CheckedItem.CLASSIC_ICON;

    public CustomizeAppBottomSheetDialog(@NonNull Context context, int bottomSheetLayout) {
        super(context);
        this.context = context;
        setContentView(bottomSheetLayout);

        initViews();
        updateViews();

        updateCheckedAccent();
        updateCheckedItem();

        // Show Full Layout
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        getBehavior().setFitToContents(true);
    }

    private void initViews() {
        // -> Accent
        blueAccentImageButton = findViewById(R.id.blue_accent_image_button);
        yellowGreenImageButton = findViewById(R.id.yellow_green_accent_image_button);
        limeAccentImageButton = findViewById(R.id.lime_accent_image_button);
        orchidAccentImageButton = findViewById(R.id.orchid_accent_image_button);
        floralAccentImageButton = findViewById(R.id.floral_accent_image_button);
        lavenderAccentImageButton = findViewById(R.id.lavender_accent_image_button);

        blueAccentImageButton.setOnClickListener(this::onClick);
        yellowGreenImageButton.setOnClickListener(this::onClick);
        limeAccentImageButton.setOnClickListener(this::onClick);
        orchidAccentImageButton.setOnClickListener(this::onClick);
        floralAccentImageButton.setOnClickListener(this::onClick);
        lavenderAccentImageButton.setOnClickListener(this::onClick);

        // -> Theme & App Icon
        darkThemeImageButton = findViewById(R.id.dark_theme_image_button);
        lightThemeImageButton = findViewById(R.id.light_theme_image_button);
        classicAppIconImageButton = findViewById(R.id.classic_app_icon_image_button);
        dreamAppIconImageButton = findViewById(R.id.dream_app_icon_image_button);

        darkThemeImageButton.setOnClickListener(this::onClick);
        lightThemeImageButton.setOnClickListener(this::onClick);
        classicAppIconImageButton.setOnClickListener(this::onClick);
        dreamAppIconImageButton.setOnClickListener(this::onClick);

        checkedDarkThemeLinearLayout = findViewById(R.id.checked_dark_theme_linear_layout);
        checkedLightThemeLinearLayout = findViewById(R.id.checked_light_theme_linear_layout);
        checkedClassicAppIconThemeLinearLayout = findViewById(R.id.checked_classic_app_icon_linear_layout);
        checkedDreamAppIconThemeLinearLayout = findViewById(R.id.checked_dream_app_icon_linear_layout);

        // Bottom
        cancelButton = findViewById(R.id.cancel_button_customize_app);
        applyButton = findViewById(R.id.apply_button_customize_app);

        cancelButton.setOnClickListener(this::onClick);
        applyButton.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == cancelButton.getId()) {
            dismiss();
        } else if (id == applyButton.getId()) {
            applyChanges();
            dismiss();
        }

        // Accent
        else if (id == blueAccentImageButton.getId()) {
            accentCheckedItem = CheckedAccent.BLUE;
            updateCheckedAccent();
        } else if (id == yellowGreenImageButton.getId()) {
            accentCheckedItem = CheckedAccent.YELLOW_GREEN;
            updateCheckedAccent();
        } else if (id == limeAccentImageButton.getId()) {
            accentCheckedItem = CheckedAccent.LIME;
            updateCheckedAccent();
        } else if (id == orchidAccentImageButton.getId()) {
            accentCheckedItem = CheckedAccent.ORCHID;
            updateCheckedAccent();
        } else if (id == floralAccentImageButton.getId()) {
            accentCheckedItem = CheckedAccent.FLORAL;
            updateCheckedAccent();
        } else if (id == lavenderAccentImageButton.getId()) {
            accentCheckedItem = CheckedAccent.LAVENDER;
            updateCheckedAccent();
        }

        // Theme
        else if (id == darkThemeImageButton.getId()) {
            themeCheckedItem = CheckedItem.DARK_THEME;
            updateCheckedItem();
        } else if (id == lightThemeImageButton.getId()) {
            themeCheckedItem = CheckedItem.LIGHT_THEME;
            updateCheckedItem();
        }

        // App Icon
        else if (id == classicAppIconImageButton.getId()) {
            iconCheckedItem = CheckedItem.CLASSIC_ICON;
            updateCheckedItem();
        } else if (id == dreamAppIconImageButton.getId()) {
            iconCheckedItem = CheckedItem.DREAM_ICON;
            updateCheckedItem();
        }
    }


    public void updateViews() {
    }


    private void applyChanges() {
        // Accent
        if (accentCheckedItem == CheckedAccent.BLUE) {
            // TODO: APPLY ACCENT
        } else if (accentCheckedItem == CheckedAccent.YELLOW_GREEN) {
        } else if (accentCheckedItem == CheckedAccent.LIME) {
        } else if (accentCheckedItem == CheckedAccent.ORCHID) {
        } else if (accentCheckedItem == CheckedAccent.FLORAL) {
        } else {
        }

        // Theme
        if (themeCheckedItem == CheckedItem.LIGHT_THEME) {
            // TODO: APPLY THEME
        } else {

        }

        // App Icon
        if (iconCheckedItem == CheckedItem.CLASSIC_ICON) {
            // TODO: APPLY APP ICON STYLE
        } else {

        }
    }

    private void updateCheckedItem() {
        // Theme
        if (themeCheckedItem == CheckedItem.LIGHT_THEME) {
            checkedLightThemeLinearLayout.setVisibility(View.VISIBLE);
            checkedDarkThemeLinearLayout.setVisibility(View.GONE);
            lightThemeImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_light_filled));
            darkThemeImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_dark_outlined));
        } else {
            checkedDarkThemeLinearLayout.setVisibility(View.VISIBLE);
            checkedLightThemeLinearLayout.setVisibility(View.GONE);
            darkThemeImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_dark_filled));
            lightThemeImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_light_outlined));
        }

        // App Icon
        if (iconCheckedItem == CheckedItem.CLASSIC_ICON) {
            checkedClassicAppIconThemeLinearLayout.setVisibility(View.VISIBLE);
            checkedDreamAppIconThemeLinearLayout.setVisibility(View.GONE);
        } else {
            checkedDreamAppIconThemeLinearLayout.setVisibility(View.VISIBLE);
            checkedClassicAppIconThemeLinearLayout.setVisibility(View.GONE);
        }
    }

    private void uncheckAllAccent() {
        blueAccentImageButton.setImageDrawable(null);
        yellowGreenImageButton.setImageDrawable(null);
        limeAccentImageButton.setImageDrawable(null);
        orchidAccentImageButton.setImageDrawable(null);
        floralAccentImageButton.setImageDrawable(null);
        lavenderAccentImageButton.setImageDrawable(null);
    }

    private void updateCheckedAccent() {
        uncheckAllAccent();
        if (accentCheckedItem == CheckedAccent.BLUE) {
            blueAccentImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_button));
        } else if (accentCheckedItem == CheckedAccent.YELLOW_GREEN) {
            yellowGreenImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_button));
        } else if (accentCheckedItem == CheckedAccent.LIME) {
            limeAccentImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_button));
        } else if (accentCheckedItem == CheckedAccent.ORCHID) {
            orchidAccentImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_button));
        } else if (accentCheckedItem == CheckedAccent.FLORAL) {
            floralAccentImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_button));
        } else {
            lavenderAccentImageButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_button));
        }
    }


    private enum CheckedItem {
        LIGHT_THEME, DARK_THEME, CLASSIC_ICON, DREAM_ICON
    }

    private enum CheckedAccent {
        BLUE, YELLOW_GREEN, LIME, ORCHID, FLORAL, LAVENDER;
    }


}

