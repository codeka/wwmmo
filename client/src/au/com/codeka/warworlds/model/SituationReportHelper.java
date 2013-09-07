package au.com.codeka.warworlds.model;

import java.util.Locale;

import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.design.Design;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.design.ShipDesign;
import au.com.codeka.common.model.SituationReport;
import au.com.codeka.common.model.Star;

public class SituationReportHelper {

    public static String getTitle(SituationReport sitrep) {
        if (sitrep.build_complete_record != null) {
            return "Build Complete";
        } else if (sitrep.move_complete_record != null) {
            return "Move Complete";
        } else if (sitrep.fleet_destroyed_record != null) {
            return "Fleet Destroyed";
        } else if (sitrep.fleet_victorious_record != null) {
            return "Fleet Victorious";
        } else if (sitrep.fleet_under_attack_record != null) {
            return "Fleet Under Attack";
        } else if (sitrep.colony_destroyed_record != null) {
            return "Colony Destroyed";
        } else if (sitrep.colony_attacked_record != null) {
            return "Colony Attacked";
        } else if (sitrep.star_ran_out_of_goods_record != null) {
            return "Goods Exhausted";
        }

        return "War Worlds";
    }

    /**
     * Gets an HTML summary of this notification, useful for notification messages.
     */
    public static String getSummaryLine(Star starSummary, SituationReport sitrep) {
        String msg;
        if (sitrep.planet_index != null && sitrep.planet_index >= 0) {
            msg = String.format(Locale.ENGLISH, "<b>%s %s:</b>",
                                starSummary.name, RomanNumeralFormatter.format(sitrep.planet_index));
        } else {
            msg = String.format(Locale.ENGLISH, "<b>%s:</b> ", starSummary.name);
        }

        if (sitrep.move_complete_record != null) {
            msg += getFleetLine(sitrep.move_complete_record.fleet_design_id, sitrep.move_complete_record.num_ships);
            msg += " arrived";
        }

        if (sitrep.build_complete_record != null) {
            DesignKind designKind = DesignKind.fromBuildKind(sitrep.build_complete_record.build_kind);
            if (designKind.equals(DesignKind.SHIP)) {
                msg += getFleetLine(sitrep.build_complete_record.design_id, 1);
            } else {
                Design design = DesignManager.i.getDesign(DesignKind.BUILDING, sitrep.build_complete_record.design_id);
                msg += design.getDisplayName();
            }
            msg += " built";
        }

        if (sitrep.fleet_under_attack_record != null) {
            if (sitrep.move_complete_record != null || sitrep.build_complete_record != null) {
                msg += ", and attacked!";
            } else {
                msg += getFleetLine(sitrep.fleet_under_attack_record.fleet_design_id, sitrep.fleet_under_attack_record.num_ships);
                msg += " attacked!";
            }
        }

        if (sitrep.fleet_destroyed_record != null) {
            if (sitrep.fleet_under_attack_record != null) {
                msg += " and <i>destroyed</i>";
            } else {
                msg += String.format(Locale.ENGLISH, "%s <i>destroyed</i>",
                        getFleetLine(sitrep.fleet_destroyed_record.fleet_design_id, 1));
            }
        }

        if (sitrep.fleet_victorious_record != null) {
            msg += String.format(Locale.ENGLISH, "%s <i>victorious</i>",
                    getFleetLine(sitrep.fleet_victorious_record.fleet_design_id, sitrep.fleet_victorious_record.num_ships));
        }

        if (sitrep.colony_destroyed_record != null) {
            msg += "Colony <em>destroyed!</em>";
        }

        if (sitrep.colony_attacked_record != null) {
            msg += "Colony <em>attacked!</em>";
        }

        if (sitrep.star_ran_out_of_goods_record != null) {
            msg += "Goods <em>exhausted!</em>";
        }

        if (msg.length() == 0) {
            msg = "Unknown situation";
        }

        return msg;
    }

    public static String getDescription(SituationReport sitrep, Star starSummary) {
        String msg = "";

        if (sitrep.move_complete_record != null) {
            msg += getFleetLine(sitrep.move_complete_record.fleet_design_id, sitrep.move_complete_record.num_ships);
            msg += String.format(Locale.ENGLISH, " arrived at %s", starSummary.name);
        }

        if (sitrep.build_complete_record != null) {
            msg = "Construction of ";
            DesignKind designKind = DesignKind.fromBuildKind(sitrep.build_complete_record.build_kind);
            if (designKind.equals(DesignKind.SHIP)) {
                msg += getFleetLine(sitrep.build_complete_record.design_id, 1);
            } else {
                Design design = DesignManager.i.getDesign(DesignKind.BUILDING, sitrep.build_complete_record.design_id);
                msg += design.getDisplayName();
            }
            msg += String.format(Locale.ENGLISH, " complete on %s %s",
                    starSummary.name, RomanNumeralFormatter.format(sitrep.planet_index));
        }

        if (sitrep.fleet_under_attack_record != null) {
            if (sitrep.move_complete_record != null) {
                msg += ", and is under attack";
            } else if (sitrep.build_complete_record != null) {
                msg += ", which is under attack";
            } else {
                msg += getFleetLine(sitrep.fleet_under_attack_record.fleet_design_id, sitrep.fleet_under_attack_record.num_ships);
                msg += String.format(Locale.ENGLISH, " is under attack at %s", starSummary.name);
            }
        }

        if (sitrep.fleet_destroyed_record != null) {
            if (sitrep.fleet_under_attack_record != null) {
                msg += " - it was DESTROYED!";
            } else {
                msg += String.format(Locale.ENGLISH, "%s fleet destroyed on %s",
                        getFleetLine(sitrep.fleet_destroyed_record.fleet_design_id, 1),
                        starSummary.name);
            }
        }

        if (sitrep.fleet_victorious_record != null) {
            msg += String.format(Locale.ENGLISH, "%s fleet prevailed in battle on %s",
                    getFleetLine(sitrep.fleet_victorious_record.fleet_design_id, sitrep.fleet_victorious_record.num_ships),
                    starSummary.name);
        }

        if (sitrep.colony_destroyed_record != null) {
            msg += String.format(Locale.ENGLISH, "Colony on %s %s destroyed",
                    starSummary.name, RomanNumeralFormatter.format(sitrep.planet_index));
        }

        if (sitrep.colony_attacked_record != null) {
            msg += String.format(Locale.ENGLISH, "Colony on %s %s attacked by %d ships",
                    starSummary.name, RomanNumeralFormatter.format(sitrep.planet_index),
                    (int) Math.ceil(sitrep.colony_attacked_record.num_ships));
        }

        if (sitrep.star_ran_out_of_goods_record != null) {
            msg += String.format(Locale.ENGLISH, "%s has run out of goods, beware population decline!",
                                 starSummary.name);
        }

        if (msg.length() == 0) {
            msg = "We got a situation over here!";
        }

        return msg;
    }

    public static Sprite getDesignSprite(SituationReport sitrep) {
        String designID = null;
        DesignKind designKind = DesignKind.SHIP;
        if (sitrep.build_complete_record != null) {
            SituationReport.BuildCompleteRecord bcr = sitrep.build_complete_record;
            designKind = DesignKind.fromBuildKind(bcr.build_kind);
            designID = bcr.design_id;
        } else if (sitrep.move_complete_record != null) {
            designID = sitrep.move_complete_record.fleet_design_id;
        } else if (sitrep.fleet_destroyed_record != null) {
            designID = sitrep.fleet_destroyed_record.fleet_design_id;
        } else if (sitrep.fleet_victorious_record != null) {
            designID = sitrep.fleet_victorious_record.fleet_design_id;
        } else if (sitrep.fleet_under_attack_record != null) {
            designID = sitrep.fleet_under_attack_record.fleet_design_id;
        }

        if (designID != null) {
            Design design = DesignManager.i.getDesign(designKind, designID);
            return SpriteManager.i.getSprite(design.getSpriteName());
        } else {
            return null;
        }
    }

    private static String getFleetLine(String designID, float numShips) {
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, designID);
        int n = (int)(Math.ceil(numShips));
        return String.format(Locale.ENGLISH, "%d × %s",
                n, design.getDisplayName(n > 1));
    }
}
