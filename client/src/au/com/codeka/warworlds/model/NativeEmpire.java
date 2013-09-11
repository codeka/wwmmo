package au.com.codeka.warworlds.model;

import au.com.codeka.warworlds.Util;

/**
 * The "native" empire is a special empire that represents the NPC colonies and fleets.
 */
public class NativeEmpire extends Empire {
    @Override
    public String getDisplayName() {
        if (Util.isDebug()) {
            // helps with testing wrapping and stuff... people like long names!
            return "Native with a really long name";
        } else {
            return "Native";
        }
    }
}
