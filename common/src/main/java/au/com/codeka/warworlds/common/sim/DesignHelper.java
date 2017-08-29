package au.com.codeka.warworlds.common.sim;

import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Helper class for working with ship and building designs.
 */
public class DesignHelper {
  /** Gets a list of all the {@link Design}s we have. */
  public static List<Design> getDesigns() {
    return DesignDefinitions.designs.designs;
  }

  /** Gets a list of all the {@link Design}s of the given {@link Design.DesignKind} we have. */
  public static Iterable<Design> getDesigns(final Design.DesignKind kind) {
    return Iterables.filter(getDesigns(), new Predicate<Design>() {
      @Override
      public boolean apply(@Nullable Design design) {
        return design != null && design.design_kind.equals(kind);
      }
    });
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

  /** Gets the display name of the given design, correctly pluralized. */
  public static String getDesignName(Design design, boolean plural) {
    return design.display_name + (plural ? "s" : "");
  }

  public static String getDependenciesHtml(Colony colony, Design design) {
    return getDependenciesHtml(colony, design, 0);
  }

  /**
   * Returns the dependencies of the given design a string for display to the user. Dependencies
   * that we don't meet will be coloured red, those that we meet with be green.
   */
  // TODO: localize this string
  public static String getDependenciesHtml(Colony colony, Design design, int level) {
    String required = "Required: ";
    List<Design.Dependency> dependencies = design.dependencies;
    if (level > 1) {
      dependencies = design.upgrades.get(level - 2).dependencies;
    }

    if (dependencies == null || dependencies.size() == 0) {
      required += "none";
    } else {
      int n = 0;
      for (Design.Dependency dep : dependencies) {
        if (n > 0) {
          required += ", ";
        }

        boolean isMet = false;
        for (Building building : colony.buildings) {
          if (building.design_type.equals(dep.type) && building.level >= dep.level) {
            isMet = true;
            break;
          }
        }

        Design dependentDesign = getDesign(dep.type);
        required += "<font color=\""+(isMet ? "green" : "red")+"\">";
        required += dependentDesign.display_name;
        if (dep.level > 1) {
          required += " lvl " + dep.level;
        }
        required += "</font>";
        n++;
      }
    }

    return required;
  }
}
