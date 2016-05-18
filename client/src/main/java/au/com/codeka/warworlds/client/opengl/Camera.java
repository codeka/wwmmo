package au.com.codeka.warworlds.client.opengl;

import android.opengl.Matrix;

/**
 * The {@link Camera} can be used to draw a {@link Scene}, scroll and zoom around.
 */
public class Camera {
  private final float[] projMatrix = new float[16];

  public void onSurfaceChanged(float width, float height) {
    Matrix.orthoM(projMatrix, 0, -width / 2, width / 2, -height / 2, height / 2, 10, -10);
  }

  public float[] getViewProjMatrix() {
    return projMatrix;
  }
}
