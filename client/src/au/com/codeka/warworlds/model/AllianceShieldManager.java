package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;

import org.andengine.opengl.texture.region.ITextureRegion;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseGlActivity;

public class AllianceShieldManager extends ShieldManager {
    public static AllianceShieldManager i = new AllianceShieldManager();

    private Bitmap sDefaultShield;

    private AllianceShieldManager() {
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public Bitmap getShield(Context context, Alliance alliance) {
        return getShield(context, getShieldInfo(alliance));
    }

    /** Gets (or creates) the empire's shield as an andengine texture. */
    public ITextureRegion getShieldTexture(BaseGlActivity glActivity, Alliance alliance) {
        return getShieldTexture(glActivity, getShieldInfo(alliance));
    }

    @Override
    protected Bitmap processShieldImage(Bitmap bmp) {
        return bmp;
    }

    @Override
    protected Bitmap getDefaultShield(ShieldInfo shieldInfo) {
        ensureDefaultImage(App.i);
        return sDefaultShield;
    }

    @Override
    protected String getFetchUrl(ShieldInfo shieldInfo) {
        return "alliances/" + shieldInfo.id + "/shield";
    }

    private ShieldInfo getShieldInfo(Alliance alliance) {
        int id = alliance.getID();
        Long lastUpdate = alliance.getDateImageUpdated() == null ? null : alliance.getDateImageUpdated().getMillis();
        return new ShieldInfo(ShieldManager.AllianceShield, id, lastUpdate);
    }

    private void ensureDefaultImage(Context context) {
        if (sDefaultShield == null) {
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
                sDefaultShield = BitmapFactory.decodeStream(ins, null, opts);
            } finally {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
