package au.com.codeka.warworlds.ctrl;

import org.joda.time.DateTime;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.Star;

public class BuildEstimateView extends FrameLayout {
    private View mView;
    private boolean mRefreshRunning = false;
    private boolean mNeedRefresh = false;
    private BuildEstimateRefreshRequiredHandler mBuildEstimateRefreshRequiredHandler;

    public BuildEstimateView(Context context) {
        this(context, null);
    }

    public BuildEstimateView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mView = inflate(context, R.layout.build_estimate_ctrl, null);
        addView(mView);
    }

    public void setOnBuildEstimateRefreshRequired(BuildEstimateRefreshRequiredHandler handler) {
        mBuildEstimateRefreshRequiredHandler = handler;
    }

    /** Runs a simulation on the star with the new build request and gets an estimate of the time taken. */
    public void refresh(final Star star, final BuildRequest br) {
        if (isInEditMode()) {
            return;
        }

        if (mRefreshRunning) {
            mNeedRefresh = true;
            return;
        }
        mRefreshRunning = true;

        final TextView timeToBuildText = (TextView) mView.findViewById(R.id.building_timetobuild);
        final TextView mineralsToBuildText = (TextView) mView.findViewById(R.id.building_mineralstobuild);

        timeToBuildText.setText("-");
        mineralsToBuildText.setText("-");

        if (star == null || br == null) {
            mRefreshRunning = false;
            return;
        }

        new BackgroundRunner<Boolean>() {
            private BuildRequest buildRequest;
            private EmpirePresence empire;
            private Star starCopy;

            @Override
            protected Boolean doInBackground() {
                starCopy = (Star) star.clone();
                buildRequest = br;

                try {
                    Simulation sim = new Simulation();
                    sim.simulate(starCopy);
                    starCopy.getBuildRequests().add(buildRequest);
                    sim.simulate(starCopy);
                } catch (Exception e) {
                    // if we can't simulate, it's some kind of error...
                    return false;
                }

                empire = (EmpirePresence) starCopy.getEmpire(br.getEmpireKey());
                return true;
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success != null && success) {
                    DateTime endTime = buildRequest.getEndTime();
    
                    EmpirePresence empirePresenceBefore = (EmpirePresence) star.getEmpire(br.getEmpireKey());
                    float deltaMineralsPerHourBefore = 0;
                    if (empirePresenceBefore != null) {
                        deltaMineralsPerHourBefore = empirePresenceBefore.getDeltaMineralsPerHour();
                    }
                    float deltaMineralsPerHourAfter = empire.getDeltaMineralsPerHour();

                    timeToBuildText.setText(TimeInHours.format(br.getStartTime(), endTime));
                    mineralsToBuildText.setText(Html.fromHtml(
                                                String.format("<font color=\"red\">%d</font>/hr",
                                                        (int) (deltaMineralsPerHourAfter - deltaMineralsPerHourBefore))));
                } else {
                    timeToBuildText.setText("??");
                    mineralsToBuildText.setText("??");
                }

                mRefreshRunning = false;
                if (mNeedRefresh) {
                    mNeedRefresh = false;
                    if (mBuildEstimateRefreshRequiredHandler != null) {
                        mBuildEstimateRefreshRequiredHandler.onBuildEstimateRefreshRequired();
                    }
                }
            }
        }.execute();
    }

    public interface BuildEstimateRefreshRequiredHandler {
        void onBuildEstimateRefreshRequired();
    }
}
