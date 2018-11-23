package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * The {@link Camera} can be used to draw a {@link Scene}, scroll and zoom around.
 */
public class Camera {
  public interface CameraUpdateListener {
    /**
     * Called when the camera is translated.
     *
     * @param x The total distance the camera is translated away from the origin in the X direction.
     * @param y The total distance the camera is translated away from the origin in the Y direction.
     * @param dx The delta the camera has moved in the X direction.
     * @param dy The delta the camera has moved in the Y direction.
     */
    void onCameraTranslate(float x, float y, float dx, float dy);
  }

  private final float[] projMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] viewProjMatrix = new float[16];
  private final float[] translateHelper = new float[16];
  private float zoomAmount;
  private boolean flinging;
  private float flingX;
  private float flingY;
  private float flingFactor;
  private DecelerateInterpolator decelerateInterpolator;
  private float screenWidth;
  private float screenHeight;
  private float translateX;
  private float translateY;
  @Nullable private CameraUpdateListener listener;

  public Camera() {
    decelerateInterpolator = new DecelerateInterpolator(1.0f);
    Matrix.setIdentityM(viewMatrix, 0);
  }

  public void setCameraUpdateListener(CameraUpdateListener listener) {
    this.listener = listener;
  }

  public void onSurfaceChanged(float width, float height) {
    Matrix.orthoM(projMatrix, 0, -width, width, -height, height, 10, -10);
    zoomAmount = 1.0f;
    flinging = false;
    screenWidth = width;
    screenHeight = height;
    zoom(2.0f);
  }

  public float getScreenWidth() {
    return screenWidth;
  }

  public float getScreenHeight() {
    return screenHeight;
  }

  public float getZoomAmount() {
    return zoomAmount;
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
    Matrix.setIdentityM(viewMatrix, 0);
    Matrix.scaleM(viewMatrix, 0, zoomAmount, zoomAmount, 1.0f);
    Matrix.translateM(viewMatrix, 0, translateX, translateY, 0.0f);
    Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0);
    return viewProjMatrix;
  }

  public void translate(float x, float y) {
    translate(x, y, false);
  }

  /**
   * Translate by the given amount.
   *
   * @param x Amount to translate in X direction.
   * @param y Amount to translate in Y direction.
   * @param silent If true, we will not call the listener.
   */
  public void translate(float x, float y, boolean silent) {
    x /= (zoomAmount * 0.5f);
    y /= (zoomAmount * 0.5f);
    translateX += x;
    translateY += y;
    if (listener != null && !silent) {
      listener.onCameraTranslate(-translateX, translateY, x, y);
    }
  }

  /**
   * Simliar to {@link #translate(float, float, boolean)} except that we translate to an absolute
   * X,Y coordinate.
   *
   * @param x The X-coordinate you want to warp to.
   * @param y The Y-coordinate you want to warp to.
   */
  public void warpTo(float x, float y) {
    translateX = x;
    translateY = y;
  }

  public void fling(float x, float y) {
    flinging = true;
    flingX = x;
    flingY = y;
    flingFactor = 0.0f;
  }

  public void zoom(float factor) {
    zoomAmount *= factor;
    if (zoomAmount > 5.0f) {
      zoomAmount = 5.0f;
    }
    if (zoomAmount < 0.25f) {
      zoomAmount = 0.25f;
    }
  }
}
