package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A HorizontalSeparator is actually a TextView that also displays a horizontal line, which
 * we can use to separate parts of the UI.
 */
public class HorizontalSeparator extends TextView {
    private Paint mPaint;

    /**
     * Constructs a new \c HorizontalSeparator.
     */
    public HorizontalSeparator(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setARGB(255, 14, 215, 254);
        mPaint.setStyle(Style.STROKE);
    }

    @Override
    public void onDraw(Canvas canvas) {
        float y = canvas.getHeight() / 2.0f;
        float x = 0.0f;

        if (this.getText() != null) {
            String text = this.getText().toString();
            x += this.getPaint().measureText(text) + 8;
        }
        canvas.drawLine(x, y, canvas.getWidth(), y, mPaint);

        super.onDraw(canvas);
    }
}
