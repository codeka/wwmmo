package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;
import android.view.animation.DecelerateInterpolator;

/**
 * The {@link Camera} can be used to draw a {@link Scene}, scroll and zoom around.
 */
public class Camera {
  private final float[] projMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] viewProjMatrix = new float[16];
  private float zoomAmount;
  private boolean flinging;
  private float flingX;
  private float flingY;
  private float flingFactor;
  private DecelerateInterpolator decelerateInterpolator;

  public Camera() {
    decelerateInterpolator = new DecelerateInterpolator(1.0f);
    Matrix.setIdentityM(viewMatrix, 0);
  }

  public void onSurfaceChanged(float width, float height) {
    Matrix.orthoM(projMatrix, 0, -width / 2, width / 2, -height / 2, height / 2, 10, -10);
    zoomAmount = 1.0f;
    flinging = false;
    zoom(2.0f);
  }

  public void onDraw() {
    if (flinging) {
      float dt = 0.016f;
      flingFactor += dt * 2.0f;
      if (flingFactor > 1.0f) {
        flinging = false;
      } else {
        float factor = 1.0f - decelerateInterpolator.getInterpolation(flingFactor);
        translate(flingX * factor * dt, flingY * factor * dt);
      }
    }
  }

  public float[] getViewProjMatrix() {
    Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0);
    return viewProjMatrix;
  }

  public void translate(float x, float y) {
    Matrix.translateM(viewMatrix, 0, x / zoomAmount, y / zoomAmount, 0.0f);
  }

  public void fling(float x, float y) {
    flinging = true;
    flingX = x;
    flingY = y;
    flingFactor = 0.0f;
  }

  public void zoom(float factor) {
    zoomAmount *= factor;
    Matrix.scaleM(viewMatrix, 0, factor, factor, 1.0f);
  }
}
