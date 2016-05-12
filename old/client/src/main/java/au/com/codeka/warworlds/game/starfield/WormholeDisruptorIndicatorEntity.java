package au.com.codeka.warworlds.game.starfield;

import android.graphics.Color;

import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

/** Indicator entity for a wormhole disruptor. */
public class WormholeDisruptorIndicatorEntity extends BaseIndicatorEntity {
  private static final IndicatorEntityShaderProgram SHADER_PROGRAM
      = new IndicatorEntityShaderProgram(Color.RED);

  public static ShaderProgram getShaderProgram() {
    return SHADER_PROGRAM;
  }

  public WormholeDisruptorIndicatorEntity(VertexBufferObjectManager vertexBufferObjectManager) {
    super(vertexBufferObjectManager, SHADER_PROGRAM);
  }
}
