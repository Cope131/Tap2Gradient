package com.daryl.tap2gradient.Pointer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.daryl.tap2gradient.R;

// Reference: http://www.kellbot.com/android-hello-circle/
public class PixelPointer extends View {

    private final float x, y;
    private final int r;
    // Contains the appearance of the drawing
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PixelPointer(Context context, float x, float y, int r) {
        super(context);
        mPaint.setColor(ContextCompat.getColor(context, R.color.white_A60));
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        this.x = x;
        this.y = y;
        this.r = r;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(x, y, r, mPaint);
    }


}
