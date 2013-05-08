package au.com.codeka.warworlds.server.handlers.pages;

import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.SectorController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles the /admin/actions/move-star page.
 */
public class ActionsMoveStarPageHandler extends HtmlPageHandler {
    @Override
    protected void post() throws RequestException {
        Star star1 = new StarController().getStar(Integer.parseInt(getRequest().getParameter("star1")));
        Star star2 = new StarController().getStar(Integer.parseInt(getRequest().getParameter("star2")));

        try (Transaction t = DB.beginTransaction()) {
            new SectorController(t).swapStars(star1, star2);
            t.commit();
        } catch (Exception e) {
            throw new RequestException(e);
        }

        TreeMap<String, Object> data = new TreeMap<String, Object>();
        data.put("star1", star1.getID());
        data.put("star2", star2.getID());

        render("admin/actions/move-star.html", data);
    }
}
