package au.com.codeka.warworlds.model;

/**
 * The "native" empire is a special empire that represents the NPC colonies and fleets.
 */
public class NativeEmpire extends Empire {
    @Override
    public String getDisplayName() {
        return "Native";
    }
}
