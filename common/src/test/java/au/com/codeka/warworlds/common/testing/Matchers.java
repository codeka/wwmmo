package au.com.codeka.warworlds.common.testing;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;

/**
 * Some custom matchers.
 */
public class Matchers {
  public static Matcher<Float> closeTo(float expected, int decimalPlaces) {
    return new BaseMatcher<Float>() {
      @Override
      public boolean matches(Object item) {
        if (item instanceof Float) {
          // I guess this isn't that fast. but it doesn't suffer from precision issues with
          // large decimalPlaces values (like multiplying by 10^decimalPlaces would).
          DecimalFormat df =
              new DecimalFormat("#." + String.join("", Collections.nCopies(decimalPlaces, "#")));
          df.setRoundingMode(RoundingMode.HALF_EVEN);

          float value = (float) item;
          return df.format(value).equals(df.format(expected));
        }

        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendValue(expected).appendText(" (to " + decimalPlaces + " decimal places)");
      }
    };
  }
}
