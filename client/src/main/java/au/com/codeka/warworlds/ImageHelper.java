package au.com.codeka.warworlds;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import au.com.codeka.common.Log;

public class ImageHelper {
  private static final Log log = new Log("ImageHelper");
  private InputStream inputStream;
  private Bitmap image;
  private int width;
  private int height;

  public ImageHelper(InputStream ins) {
    inputStream = ins;
  }

  public ImageHelper(InputStream ins, int width, int height) {
    inputStream = ins;
    this.width = width;
    this.height = height;
  }

  public ImageHelper(byte[] bytes) {
    inputStream = new ByteArrayInputStream(bytes);
  }

  public Bitmap getImage() {
    if (image == null && inputStream != null) {
      loadImage();
    }

    return image;
  }

  private void loadImage() {
    if (width > 0 && height > 0) {
      // If we known the width/height in advance (some content providers let you query for
      // that directly) then it saves an extra decode of the image to calculate it.
      loadImageWithKnownSize();
    } else if (inputStream.markSupported()) {
      // if the stream supports mark (e.g. a local file vs. a "cloud" file) we can
      // load the image using less memory, and therefore will prefer that. If it doesn't
      // support mark then we don't have much choice...
      loadImageWithMark();
    } else {
      loadImageNoMark();
    }
  }

  private void loadImageWithKnownSize() {
    int scale = 1;
    while (width / scale / 2 >= 100 && height / scale / 2 >= 100) {
      scale *= 2;
    }
    log.info("Loading image width known size: %dx%d, scale=%d", width, height, scale);

    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inPurgeable = true;
    opts.inInputShareable = true;
    opts.inSampleSize = scale;
    image = BitmapFactory.decodeStream(inputStream, null, opts);
  }

  private void loadImageWithMark() {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inJustDecodeBounds = true;
    inputStream.mark(10 * 1024 * 1024);
    BitmapFactory.decodeStream(inputStream, null, opts);

    int scale = 1;
    while (opts.outWidth / scale / 2 >= 100 && opts.outHeight / scale / 2 >= 100) {
      scale *= 2;
    }
    log.info("Loading image from markable stream, scale=%d", scale);

    opts = new BitmapFactory.Options();
    opts.inPurgeable = true;
    opts.inInputShareable = true;
    opts.inSampleSize = scale;
    image = BitmapFactory.decodeStream(inputStream, null, opts);
  }

  private void loadImageNoMark() {
    log.info("Loading image from unmarkable");
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inPurgeable = true;
    opts.inInputShareable = true;
    image = BitmapFactory.decodeStream(inputStream, null, opts);
  }
}
