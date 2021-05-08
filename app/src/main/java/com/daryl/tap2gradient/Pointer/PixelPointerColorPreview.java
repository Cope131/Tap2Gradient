package com.daryl.tap2gradient.Pointer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.daryl.tap2gradient.R;

public class PixelPointerColorPreview extends View {

    private final float x, y;
    private final int r;
    // Contains the appearance of the drawing
    private final Paint mPaint_Fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaint_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PixelPointerColorPreview(Context context, float x, float y, int r, int color) {
        super(context);
        // FILL
        if (color == 0) {
            mPaint_Fill.setColor(ContextCompat.getColor(context, R.color.white_A60));
        } else {
            mPaint_Fill.setColor(color);
        }
        mPaint_Fill.setStyle(Paint.Style.FILL);
        mPaint_Fill.setStrokeWidth(0);

        // STROKE
        mPaint_STROKE.setColor(ContextCompat.getColor(context, R.color.white));
        mPaint_STROKE.setStyle(Paint.Style.STROKE);
        mPaint_STROKE.setStrokeWidth(7);

        this.x = x;
        this.y = y;
        this.r = r;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(x, y, r + 15, mPaint_STROKE);
        canvas.drawCircle(x, y, r + 12, mPaint_Fill);
    }


}
