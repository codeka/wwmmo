package au.com.codeka.warworlds.server;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.events.*;

/**
 * The \c EventProcessor looks at all events scheduled for the future (e.g. fleet arrives at
 * star, build completes, etc) and schedules itself to pick up the work of the event when it's
 * scheduled to occur.
 */
public class EventProcessor {
    private final Logger log = LoggerFactory.getLogger(EventProcessor.class);
    public static EventProcessor i = new EventProcessor();

    private static ArrayList<Class<?>> sEventClasses;
    static {
        sEventClasses = new ArrayList<Class<?>>();
        sEventClasses.add(FleetMoveCompleteEvent.class);
        sEventClasses.add(BuildCompleteEvent.class);
        sEventClasses.add(FleetDestroyedEvent.class);
        sEventClasses.add(EmpireStarGoodsReachedZeroEvent.class);
    }

    private Thread mThread;
    private Runnable mThreadRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                threadProc();
            }
        }
    };

    /**
     * Call this every now and then (at the very least, every time a new event is scheduled)
     * to make sure we're going to wake up at the correct time to handle the next event.
     */
    public void ping() {
        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(mThreadRunnable);
            mThread.setDaemon(true);
            mThread.setName("EventProcessor");
            mThread.start();
        } else {
            // calling interrupt will cause the thread to wake up and loop around again
            mThread.interrupt();
        }
    }

    /**
     * This method is called in a background thread to actually process events. Basically, we
     * just loop forever checking for new events and waiting for those events to happen.
     */
    private void threadProc() {
        log.info("EventProcessor thread starting.");

        while (true) {
            // work out when the next event is scheduled
            DateTime nextEventDateTime = null;
            ArrayList<Event> events = new ArrayList<Event>();
            for (Class<?> eventClass : sEventClasses) {
                Event event;
                try {
                    event = (Event) eventClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    continue;
                }

                DateTime next = event.getNextEventTime();
                if (next != null) {
                    log.info(String.format("Event %s says next event is at %s", eventClass.getName(), next));
                }
                if (next != null && next.isBefore(DateTime.now())) {
                    events.add(event);
                }
                if (next != null && (nextEventDateTime == null || next.isBefore(nextEventDateTime))) {
                    nextEventDateTime = next;
                }
            }

            if (nextEventDateTime == null) {
                // if there's nothing scheduled, let's just sleep for ten minutes and try again
                nextEventDateTime = DateTime.now().plusMinutes(10);
            }

            log.info(String.format("Next event is scheduled at %s", nextEventDateTime));

            // make sure we don't try to sleep for a negative amount of time...
            if (nextEventDateTime.isBefore(DateTime.now())) {
                nextEventDateTime = DateTime.now();
            } else {
                try {
                    int seconds = Seconds.secondsBetween(DateTime.now(), nextEventDateTime).getSeconds();
                    Thread.sleep(seconds * 1000);
                } catch(InterruptedException e) {
                    // if we get interrupted it's because somebody pinged us and we need to
                    // check the next time again
                    log.info("EventProcessor pinged, checking for new events.");
                    continue;
                }
            }

            for (Event event : events) {
                event.process();
            }
        }
    }
}
