package au.com.codeka.warworlds.server;

import java.sql.Timestamp;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * This is the base class for events, which the \see EventProcessor uses to schedule
 * "things" to run in the future. An example is when you move a fleet, a \see FleetMoveCompleteEvent
 * is schedule to be run when the fleet arrives at it's destination.
 */
public abstract class Event {
    /**
     * Gets the \see DateTime the next event of this type is supposed to run.
     */
    public DateTime getNextEventTime() {
        try (SqlStmt stmt = DB.prepare(getNextEventTimeSql())) {
            Timestamp ts = stmt.selectFirstValue(Timestamp.class);
            if (ts == null) {
                return null;
            }

            return new DateTime(ts.getTime());
        } catch(Exception e) {
            // todo: log errors
            return null;
        }
    }

    /**
     * This is called when it's time to process (at least) one event. We'll need to fetch details
     * from the database of the event and perform whatever actions are required.
     */
    public abstract void process();

    /**
     * Gets the SQL that will return (as the first column in the first row) the date/time the next
     * event of this kind is scheduled to run.
     */
    protected abstract String getNextEventTimeSql();
}
