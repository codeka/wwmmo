package au.com.codeka.warworlds.client.opengl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.util.HashMap;
import java.util.Map;

import au.com.codeka.warworlds.common.Log;

/**
 * This is a {@link Texture} which is used to back an image for drawing arbitrary strings.
 */
public class TextTexture extends Texture {
  private static final Log log = new Log("TextTexture");

  // TODO: make the bitmap automatically resize if it needs to.
  private static final int BITMAP_WIDTH = 512;
  private static final int BITMAP_HEIGHT = 512;

  /** The height of the text. */
  private static final int TEXT_HEIGHT = 28;
  private static final int ROW_HEIGHT = 32;

  private final Bitmap bitmap;
  private final Canvas canvas;
  private final Paint paint;
  private final Map<Character, Rect> characters = new HashMap<>();

  private int id;
  private boolean dirty;

  /** The offset from the left of the texture that we'll draw the next character. */
  private int currRowOffsetX = 0;

  /** The offset from the top of the texture that we'll draw the next character. */
  private int currRowOffsetY = 0;

  public TextTexture() {
    bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_4444);
    canvas = new Canvas(bitmap);
    paint = new Paint();
    paint.setTextSize(TEXT_HEIGHT);
    paint.setAntiAlias(true);
    paint.setARGB(255, 255, 255, 255);
    id = -1;
    ensureText("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
        + "!@#$%^&*()`~-=_+[]\\{}|;':\",./<>? ");
  }

  @Override
  public void bind() {
    if (dirty) {
      if (id == -1) {
        final int[] textureHandleBuffer = new int[1];
        GLES20.glGenTextures(1, textureHandleBuffer, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandleBuffer[0]);
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        id = textureHandleBuffer[0];
        setTextureId(id);
      }
      super.bind();
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
      dirty = false;
      return;
    }

    super.bind();
  }

  public int getWidth() {
    return BITMAP_WIDTH;
  }

  public int getHeight() {
    return BITMAP_HEIGHT;
  }

  public float getTextHeight() {
    return TEXT_HEIGHT;
  }

  /** Ensures that we have cached all the characters needed to draw every character in the string.*/
  public void ensureText(String str) {
    for (int i = 0; i < str.length(); i++) {
      ensureChar(str.charAt(i));
    }
  }

  /** Gets the bounds of the given char. */
  public Rect getCharBounds(char ch) {
    Rect bounds = characters.get(ch);
    if (bounds == null) {
      ensureChar(ch);
      bounds = characters.get(ch);
    }

    return bounds;
  }

  private void ensureChar(char ch) {
    if (characters.containsKey(ch)) {
      return;
    }

    String str = new String(new char[] { ch });
    int charWidth = (int) Math.ceil(paint.measureText(str));
    if (currRowOffsetX + charWidth > BITMAP_WIDTH) {
      currRowOffsetX = 0;
      currRowOffsetY += ROW_HEIGHT;
    }

    canvas.drawText(str,
        currRowOffsetX,
        currRowOffsetY + TEXT_HEIGHT - paint.descent() + (ROW_HEIGHT - TEXT_HEIGHT),
        paint);

    Rect bounds = new Rect(currRowOffsetX, currRowOffsetY,
        currRowOffsetX + charWidth, currRowOffsetY + ROW_HEIGHT);
    characters.put(ch, bounds);

    currRowOffsetX += charWidth;
    dirty = true;
  }
}
