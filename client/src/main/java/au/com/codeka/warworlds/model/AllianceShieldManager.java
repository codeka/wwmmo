package au.com.codeka.warworlds.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.App;

public class AllianceShieldManager extends ShieldManager {
  public static AllianceShieldManager i = new AllianceShieldManager();

  private Bitmap defaultShield;

  private AllianceShieldManager() {
  }

  /**
   * Gets (or creates, if there isn't one) the {@link Bitmap} that represents this Alliance's
   * shield (i.e. their icon).
   */
  public Bitmap getShield(Context context, Alliance alliance) {
    return getShield(context, getShieldInfo(alliance));
  }

  @Override
  protected Bitmap getDefaultShield(ShieldInfo shieldInfo) {
    ensureDefaultImage(App.i);
    return defaultShield;
  }

  @Override
  protected String getFetchUrl(ShieldInfo shieldInfo) {
    return "alliances/" + shieldInfo.id + "/shield";
  }

  private ShieldInfo getShieldInfo(Alliance alliance) {
    int id = alliance.getID();
    Long lastUpdate = alliance.getDateImageUpdated() == null
        ? null : alliance.getDateImageUpdated().getMillis();
    return new ShieldInfo(ShieldManager.AllianceShield, id, lastUpdate);
  }

  private void ensureDefaultImage(Context context) {
    if (defaultShield == null) {
      AssetManager assetManager = context.getAssets();
      InputStream ins;
      try {
        ins = assetManager.open("img/alliance.png");
      } catch (IOException e) {
        // should never happen!
        return;
      }

      try {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        defaultShield = BitmapFactory.decodeStream(ins, null, opts);
      } finally {
        try {
          ins.close();
        } catch (IOException e) {
          // Ignore.
        }
      }
    }
  }
}
