package au.com.codeka.warworlds.game.starfield;

import android.graphics.Color;

import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

/** Indicator entity to show the radius of the radar in a star. */
public class RadarIndicatorEntity extends BaseIndicatorEntity {
  private static final IndicatorEntityShaderProgram SHADER_PROGRAM
      = new IndicatorEntityShaderProgram(Color.GREEN);

  public static ShaderProgram getShaderProgram() {
        return SHADER_PROGRAM;
    }

  public RadarIndicatorEntity(VertexBufferObjectManager vertexBufferObjectManager) {
    super(vertexBufferObjectManager, SHADER_PROGRAM);
  }
}
