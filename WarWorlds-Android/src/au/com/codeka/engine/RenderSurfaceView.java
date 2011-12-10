package au.com.codeka.engine;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class RenderSurfaceView extends GLSurfaceView {

	public RenderSurfaceView(final Context context) {
		super(context);
		
		// use OpenGL ES 2
		this.setEGLContextClientVersion(2);
	}

	/**
	 * Creates the \c Renderer and associates it with this \c RenderSurfaceView.
	 */
	public void createRenderer() {
		this.setRenderer(new Renderer());
	}

	protected class Renderer implements GLSurfaceView.Renderer {

		@Override
		public void onDrawFrame(GL10 gl) {
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			gl.glViewport(0, 0, width, height);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			gl.glClearColor(0.0f, 0.0f, 1.0f, 0.5f);

			gl.glShadeModel(GL10.GL_SMOOTH);

			gl.glEnable(GL10.GL_DEPTH_TEST);
			gl.glClearDepthf(1.0f);
			gl.glDepthFunc(GL10.GL_LEQUAL);

		}
		
	}
}
