package au.com.codeka.warworlds;

import java.io.FileNotFoundException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import au.com.codeka.common.Log;

public class ImagePickerHelper {
    private static final Log log = new Log("ImagePickerHelper");
    private static final int CHOOSE_IMAGE_RESULT_ID = 7406;
    private Activity mActivity;
    private ImageHelper mImageHelper;

    public ImagePickerHelper(Activity activity) {
        mActivity = activity;
    }

    @SuppressLint("InlinedApi")
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHOOSE_IMAGE_RESULT_ID && data != null && data.getData() != null) {
            Uri uri = data.getData();
            log.info("Image picked: %s", uri);

            // If we can query for with/height, that can save us loading the image twice. Not
            // all content providers support it, though.
            int width = 0;
            int height = 0;
            Cursor cursor = null;
            try {
                cursor = mActivity.getContentResolver().query(uri, new String[] {
                        MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT
                    }, null, null, null);
                if (cursor.moveToFirst()) {
                    width = cursor.getInt(0);
                    height = cursor.getInt(1);
                    log.info("Got image size: %dx%d", width, height);
                }
            } catch (Exception e) {
                // There can be various exceptions, all of which result in us ignoring width/height.
                log.info("Could not get image size: %s", e.getMessage());
                width = height = 0;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            try {
                mImageHelper = new ImageHelper(mActivity.getContentResolver().openInputStream(uri),
                        width, height);
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
        if (mImageHelper != null) {
            return mImageHelper.getImage();
        }
        return null;
    }
}
