package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A HorizontalSeparator is actually a TextView that also displays a horizontal line, which
 * we can use to separate parts of the UI.
 */
public class HorizontalSeparator extends TextView {
  private Paint mPaint;

  public HorizontalSeparator(Context context) {
    super(context);

    mPaint = new Paint();
    mPaint.setARGB(255, 14, 215, 254);
    mPaint.setStyle(Paint.Style.STROKE);
  }

  /**
   * Constructs a new HorizontalSeparator.
   */
  public HorizontalSeparator(Context context, AttributeSet attrs) {
    super(context, attrs);

    mPaint = new Paint();
    mPaint.setARGB(255, 14, 215, 254);
    mPaint.setStyle(Paint.Style.STROKE);
  }

  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    float y = canvas.getHeight() / 2.0f;
    float x = 0.0f;

    if (this.getText() != null) {
      String text = this.getText().toString();
      x += this.getPaint().measureText(text) + 8;
    }
    canvas.drawLine(x, y, canvas.getWidth(), y, mPaint);
  }
}