package au.com.codeka.warworlds;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

public class ImagePickerHelper {
    private static final int CHOOSE_IMAGE_RESULT_ID = 7406;
    private Activity mActivity;
    private InputStream mImageStream;
    private Bitmap mImage;

    public ImagePickerHelper(Activity activity) {
        mActivity = activity;
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHOOSE_IMAGE_RESULT_ID && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try {
                mImageStream = mActivity.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
            }

            return true;
        }
        return false;
    }

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        mActivity.startActivityForResult(Intent.createChooser(intent, "Choose Image"),
                CHOOSE_IMAGE_RESULT_ID);
    }

    public Bitmap getImage() {
        if (mImage == null && mImageStream != null) {
            loadImage();
        }

        return mImage;
    }

    private void loadImage() {
        // if the stream supports mark (e.g. a local file vs. a "cloud" file) we can
        // load the image using less memory, and therefore will prefer that. If it doesn't
        // support mark then we don't have much choice...
        if (mImageStream.markSupported()) {
            loadImageWithMark();
        } else {
            loadImageNoMark();
        }
    }

    private void loadImageWithMark() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        mImageStream.mark(10 * 1024 * 1024);
        BitmapFactory.decodeStream(mImageStream, null, opts);

        int scale = 1;
        while (opts.outWidth / scale / 2 >= 100 && opts.outHeight / scale / 2 >= 100) {
            scale *= 2;
        }

        opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inSampleSize = scale;
        mImage = BitmapFactory.decodeStream(mImageStream, null, opts);
    }

    private void loadImageNoMark() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        mImage = BitmapFactory.decodeStream(mImageStream, null, opts);
    }
}
