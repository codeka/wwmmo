package au.com.codeka.common;

import org.joda.time.DateTimeZone;
import org.joda.time.tz.Provider;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/** This is a replacement {@link Provider} which doesn't load timezone data from the Joda jar file
 * (which is quite slow on Android).
 *
 * TODO: we could probably strip out timezone info from the joda jar file for ~150KB saving.
 *
 * See: http://stackoverflow.com/a/6298241
 */
public class FastDateTimeZoneProvider implements Provider {
  public static final Set<String> AVAILABLE_IDS = new HashSet<>();

  static {
    AVAILABLE_IDS.addAll(Arrays.asList(TimeZone.getAvailableIDs()));
  }

  public DateTimeZone getZone(String id) {
    if (id == null) {
      return DateTimeZone.UTC;
    }

    TimeZone tz = TimeZone.getTimeZone(id);
    if (tz == null) {
      return DateTimeZone.UTC;
    }

    int rawOffset = tz.getRawOffset();
    if (tz.inDaylightTime(new Date())) {
      rawOffset += tz.getDSTSavings();
    }

    return DateTimeZone.forOffsetMillis(rawOffset);
  }

  public Set getAvailableIDs() {
    return AVAILABLE_IDS;
  }
}
