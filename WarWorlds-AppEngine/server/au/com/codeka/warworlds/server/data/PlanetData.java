package au.com.codeka.warworlds.server.data;

import java.io.Serializable;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Represents the data we store about a single planet in the game. A planet is associated with
 * a single \c StarData.
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class PlanetData implements Serializable {
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key mKey;

    /** The "index" of this planet, relative to the other planets around the star. */
    @Persistent
    private int mIndex;

    /** The apparent size of this planet. */
    @Persistent
    private int mSize;
}
