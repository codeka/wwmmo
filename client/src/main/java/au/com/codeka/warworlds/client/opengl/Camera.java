package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;

/**
 * The {@link Camera} can be used to draw a {@link Scene}, scroll and zoom around.
 */
public class Camera {
  private final float[] projMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] viewProjMatrix = new float[16];
  private float zoomAmount;

  public Camera() {
    Matrix.setIdentityM(viewMatrix, 0);
  }

  public void onSurfaceChanged(float width, float height) {
    Matrix.orthoM(projMatrix, 0, -width / 2, width / 2, -height / 2, height / 2, 10, -10);
    zoomAmount = 1.0f;
    zoom(2.0f);
  }

  public float[] getViewProjMatrix() {
    Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0);
    return viewProjMatrix;
  }

  public void translate(float x, float y) {
    Matrix.translateM(viewMatrix, 0, x / zoomAmount, y / zoomAmount, 0.0f);
  }

  public void zoom(float factor) {
    zoomAmount *= factor;
    Matrix.scaleM(viewMatrix, 0, factor, factor, 1.0f);
  }
}
