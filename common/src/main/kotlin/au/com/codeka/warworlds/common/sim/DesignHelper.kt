package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Design
import com.google.common.collect.Iterables

/**
 * Helper class for working with ship and building designs.
 */
object DesignHelper {
  /** Gets a list of all the [Design]s we have.  */
  val designs: List<Design>
    get() = DesignDefinitions.designs.designs

  /** Gets a list of all the [Design]s of the given [Design.DesignKind] we have.  */
  fun getDesigns(kind: Design.DesignKind): Iterable<Design> {
    return Iterables.filter(designs) {
      design -> design != null && design.design_kind == kind
    }
  }

  /** Gets the [Design] with the given identifier.  */
  fun getDesign(type: Design.DesignType): Design {
    for (design in designs) {
      if (design.type == type) {
        return design
      }
    }
    throw IllegalStateException("No design with id=$type found.")
  }

  /** Gets the display name of the given design, correctly pluralized.  */
  fun getDesignName(design: Design, plural: Boolean): String {
    return design.display_name.toString() + if (plural) "s" else ""
  }

  fun getDependenciesHtml(colony: Colony, design: Design): String {
    return getDependenciesHtml(colony, design, 0)
  }

  /**
   * Returns the dependencies of the given design a string for display to the user. Dependencies
   * that we don't meet will be coloured red, those that we meet with be green.
   */
  // TODO: localize this string
  fun getDependenciesHtml(colony: Colony, design: Design, level: Int): String {
    val required = StringBuilder()
    required.append("Required: ")
    var dependencies: List<Design.Dependency> = design.dependencies
    if (level > 1) {
      dependencies = design.upgrades[level - 2].dependencies
    }
    if (dependencies.isEmpty()) {
      required.append("none")
    } else {
      for ((n, dep) in dependencies.withIndex()) {
        if (n > 0) {
          required.append(", ")
        }
        var isMet = false
        for (building in colony.buildings) {
          if (building.design_type == dep.type && building.level!! >= dep.level!!) {
            isMet = true
            break
          }
        }
        val dependentDesign: Design = getDesign(dep.type!!)
        required.append("<font color=\"")
        required.append(if (isMet) "green" else "red")
        required.append("\">")
        required.append(dependentDesign.display_name)
        if (dep.level!! > 1) {
          required.append(" lvl ")
          required.append(dep.level)
        }
        required.append("</font>")
      }
    }
    return required.toString()
  }
}