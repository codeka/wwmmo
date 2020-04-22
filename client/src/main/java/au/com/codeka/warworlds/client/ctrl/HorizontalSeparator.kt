package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * A HorizontalSeparator is actually a TextView that also displays a horizontal line, which
 * we can use to separate parts of the UI.
 */
class HorizontalSeparator : AppCompatTextView {
  private var paint: Paint

  constructor(context: Context?) : super(context) {
    paint = Paint()
    paint.setARGB(255, 14, 215, 254)
    paint.style = Paint.Style.STROKE
  }

  /**
   * Constructs a new HorizontalSeparator.
   */
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    paint = Paint()
    paint.setARGB(255, 14, 215, 254)
    paint.style = Paint.Style.STROKE
  }

  public override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val y = height / 2.0f
    var x = 0.0f
    if (this.text != null) {
      val text = this.text.toString()
      x += this.paint.measureText(text) + 8
    }
    canvas.drawLine(x, y, width.toFloat(), y, paint)
  }
}
