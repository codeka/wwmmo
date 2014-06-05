package au.com.codeka.warworlds;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageHelper {
    private InputStream mInputStream;
    private Bitmap mImage;
    private int mWidth;
    private int mHeight;

    public ImageHelper(InputStream ins) {
        mInputStream = ins;
    }
    public ImageHelper(InputStream ins, int width, int height) {
        mInputStream = ins;
        mWidth = width;
        mHeight = height;
    }
    public ImageHelper(byte[] bytes) {
        mInputStream = new ByteArrayInputStream(bytes);
    }

    public Bitmap getImage() {
        if (mImage == null && mInputStream != null) {
            loadImage();
        }

        return mImage;
    }

    private void loadImage() {
        if (mWidth > 0 && mHeight > 0) {
            // If we known the width/height in advance (some content providers let you query for
            // that directly) then it saves an extra decode of the image to calculate it.
            loadImageWithKnownSize();
        } else if (mInputStream.markSupported()) {
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
        while (mWidth / scale / 2 >= 100 && mHeight / scale / 2 >= 100) {
            scale *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inSampleSize = scale;
        mImage = BitmapFactory.decodeStream(mInputStream, null, opts);
    }

    private void loadImageWithMark() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        mInputStream.mark(10 * 1024 * 1024);
        BitmapFactory.decodeStream(mInputStream, null, opts);

        int scale = 1;
        while (opts.outWidth / scale / 2 >= 100 && opts.outHeight / scale / 2 >= 100) {
            scale *= 2;
        }

        opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inSampleSize = scale;
        mImage = BitmapFactory.decodeStream(mInputStream, null, opts);
    }

    private void loadImageNoMark() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        mImage = BitmapFactory.decodeStream(mInputStream, null, opts);
    }
}
