package au.com.codeka.warworlds;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageHelper {
    private InputStream mInputStream;
    private Bitmap mImage;

    public ImageHelper(InputStream ins) {
        mInputStream = ins;
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
        // if the stream supports mark (e.g. a local file vs. a "cloud" file) we can
        // load the image using less memory, and therefore will prefer that. If it doesn't
        // support mark then we don't have much choice...
        if (mInputStream.markSupported()) {
            loadImageWithMark();
        } else {
            loadImageNoMark();
        }
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
