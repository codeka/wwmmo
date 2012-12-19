package au.com.codeka.warworlds.model;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * The "native" empire is a special empire that represents the NPC colonies and fleets.
 */
public class NativeEmpire extends Empire {
    @Override
    public String getDisplayName() {
        return "Native";
    }

    @Override
    public Bitmap getShield(Context context) {
        return null; // TODO, something?
    }
}
