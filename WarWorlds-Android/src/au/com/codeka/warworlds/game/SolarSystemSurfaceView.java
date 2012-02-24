package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import au.com.codeka.warworlds.model.Star;

/**
 * \c SurfaceView that displays a solar system. Star in the top-left, planets arrayed around,
 * and representations of the fleets, etc.
 */
public class SolarSystemSurfaceView extends UniverseElementSurfaceView {
    private static Logger log = LoggerFactory.getLogger(SolarSystemSurfaceView.class);
    private Star mStar;

    public SolarSystemSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.isInEditMode()) {
            return;
        }

        // TODO: initialize
    }

    public void setStar(Star star) {
        mStar = star;
    }

    /**
     * Draws the actual starfield to the given \c Canvas. This will be called in
     * a background thread, so we can't do anything UI-specific, except drawing
     * to the canvas.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            // TODO: do something?
            return;
        }
        log.info("onDraw() called...");

        canvas.drawColor(0x0); // clear it to black

        if (mStar != null) {
            drawSun(canvas);
            drawPlanets(canvas);
        }
    }

    private Paint p;
    private void drawSun(Canvas canvas) {
        int[] colours = { Color.YELLOW, Color.YELLOW, 0x00000000 };
        float[] positions = { 0.0f, 0.4f, 1.0f };

        RadialGradient gradient = new RadialGradient(0, 0, 200, 
                colours, positions, android.graphics.Shader.TileMode.CLAMP);
        if (p == null) {
            p = new Paint();
            p.setDither(true);
        }
        p.setShader(gradient);

        canvas.drawCircle(0, 0, 200, p);
    }

    private void drawPlanets(Canvas canvas) {
        Paint p2 = new Paint();
        p2.setARGB(255, 255, 255, 255);
        p2.setStyle(Style.STROKE);

        for (int i = 0; i < mStar.getNumPlanets(); i++) {
            canvas.drawCircle(0, 0, (50*i) + 250, p2);
        }
    }
}
