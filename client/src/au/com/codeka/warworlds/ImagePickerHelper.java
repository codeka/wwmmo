package au.com.codeka.warworlds;

import java.io.FileNotFoundException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

public class ImagePickerHelper {
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

            // If we can query for with/height, that can save us loading the image twice. Not
            // all content providers support it, though.
            int width = 0;
            int height = 0;
            try {
                Cursor cursor = mActivity.getContentResolver().query(uri, new String[] {
                        MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT
                    }, null, null, null);
                if (cursor.moveToFirst()) {
                    width = cursor.getInt(0);
                    height = cursor.getInt(1);
                }
            } catch (Exception e) {
                // There can be various exceptions, all of which result in us ignoring width/height.
                width = height = 0;
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
