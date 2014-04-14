package au.com.codeka.warworlds;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

public class ImagePickerHelper {
    private static final int CHOOSE_IMAGE_RESULT_ID = 7406;
    private Activity mActivity;
    private ImageHelper mImageHelper;

    public ImagePickerHelper(Activity activity) {
        mActivity = activity;
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHOOSE_IMAGE_RESULT_ID && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try {
                mImageHelper = new ImageHelper(
                        mActivity.getContentResolver().openInputStream(uri));
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
