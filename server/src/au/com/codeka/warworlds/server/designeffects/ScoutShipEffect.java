package au.com.codeka.warworlds.server.designeffects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.ShipEffect;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.ScoutReportController;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.ScoutReport;
import au.com.codeka.warworlds.server.model.Star;

/**
 * The scout \see ShipEffect is attached to ships that will scout enemy stars.
 */
public class ScoutShipEffect extends ShipEffect {
    private final Logger log = LoggerFactory.getLogger(ScoutShipEffect.class);

    /**
     * This is called when we arrive on a star. If there's anybody to attack, we'll switch to
     * an attacking state.
     */
    @Override
    public void onArrived(BaseStar star, BaseFleet fleet) {
        log.info(String.format("Generating scout report.... star=%s (# planets=%d)", star.getName(), star.getPlanets().length));

        ScoutReport scoutReport = new ScoutReport(((Star) star).getID(),
                ((Fleet) fleet).getEmpireID(), (Star) star);
        try {
            new ScoutReportController().saveScoutReport(scoutReport);
        } catch (RequestException e) {
            log.error("Error saving scout report.", e);
        }

        ((Star) star).getScoutReports().add(scoutReport);
    }

}
