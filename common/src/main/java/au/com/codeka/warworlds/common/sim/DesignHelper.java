package au.com.codeka.warworlds.common.sim;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Design;

/**
 * Helper class for working with ship and building designs.
 */
public class DesignHelper {

  public static String getDesignName(Design design, boolean plural) {
    return design.display_name + (plural ? "s" : "");
  }

  /** Gets the {@link Design} with the given identifier. */
  public static Design getDesign(Design.DesignType type) {
    for (Design design : getDesigns()) {
      if (design.type.equals(type)) {
        return design;
      }
    }

    throw new IllegalStateException("No design with id=" + type + " found.");
  }

  public static List<Design> getDesigns() {
    return DesignDefinitions.designs.designs;
  }

  public static Iterable<Design> getDesigns(final Design.DesignKind kind) {
    return Iterables.filter(getDesigns(), new Predicate<Design>() {
      @Override
      public boolean apply(@Nullable Design design) {
        return design != null && design.design_kind.equals(kind);
      }
    });
  }
}
