package au.com.codeka.warworlds;

import java.io.FileNotFoundException;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import au.com.codeka.common.Log;

public class ImagePickerHelper {
  private static final Log log = new Log("ImagePickerHelper");

  private final Activity activity;
  private final HashMap<Integer, PendingImagePick> pendingImagePicks = new HashMap<>();

  /** This is implemented by a fragment that wants to receive a callback when an image is picked. */
  public interface ImagePickedHandler {
    void onImagePicked(Bitmap bmp);
  }

  public ImagePickerHelper(Activity activity) {
    this.activity = activity;
  }

  /** Register a callback that you want called when an image is picked with the given ID. */
  public void registerImagePickedHandler(int id, ImagePickedHandler handler) {
    PendingImagePick pick = pendingImagePicks.get(id);
    if (pick == null) {
      pick = new PendingImagePick();
      pendingImagePicks.put(id, pick);
    }

    if (pick.bitmap != null) {
      handler.onImagePicked(pick.bitmap);
      pick.bitmap = null;
    } else {
      pick.handler = handler;
    }
  }

  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    PendingImagePick pendingPick = pendingImagePicks.get(requestCode);
    if (pendingPick != null && data != null && data.getData() != null) {
      Uri uri = data.getData();
      log.info("Image picked: %s", uri);

      // If we can query for with/height, that can save us loading the image twice. Not
      // all content providers support it, though.
      int width = 0;
      int height = 0;
      try (Cursor cursor = activity.getContentResolver().query(uri, new String[]{
          MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT
      }, null, null, null)) {
        if (cursor.moveToFirst()) {
          width = cursor.getInt(0);
          height = cursor.getInt(1);
          log.info("Got image size: %dx%d", width, height);
        }
      } catch (Exception e) {
        // There can be various exceptions, all of which result in us ignoring width/height.
        log.info("Could not get image size: %s", e.getMessage());
        width = height = 0;
      }

      try {
        pendingPick.imageHelper =
            new ImageHelper(activity.getContentResolver().openInputStream(uri), width, height);
      } catch (FileNotFoundException e) {
        log.error("Got unexpected error.", e);
      }

      if (pendingPick.handler != null) {
        pendingPick.handler.onImagePicked(pendingPick.imageHelper.getImage());
      } else {
        pendingPick.bitmap = pendingPick.imageHelper.getImage();
      }
      return true;
    }
    return false;
  }

  /**
   * Ask the user to pick an image. The ID is used to find the handler you previously registered
   * with {@link #registerImagePickedHandler(int, ImagePickedHandler)}.
   */
  public void chooseImage(int id) {
    PendingImagePick pendingPick = pendingImagePicks.get(id);
    if (pendingPick != null) {
      pendingPick.bitmap = null;
    } else {
      pendingPick = new PendingImagePick();
      pendingImagePicks.put(id, pendingPick);
    }

    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    activity.startActivityForResult(Intent.createChooser(intent, "Choose Image"), id);
  }

  public Bitmap getImage() {
    // TODO: remove
    return null;
  }

  private final static class PendingImagePick {
    public Bitmap bitmap;
    public ImageHelper imageHelper;
    public ImagePickedHandler handler;
  }
}
