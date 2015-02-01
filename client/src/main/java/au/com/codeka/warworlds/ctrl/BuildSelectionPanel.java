package au.com.codeka.warworlds.ctrl;

import java.util.Locale;

import org.joda.time.Duration;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.Design;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class BuildSelectionPanel extends RelativeLayout {
    private BuildQueueActionListener actionListener;
    private Star star;
    private BuildRequest buildRequest;
    private Handler handler;
    private ProgressUpdater progressUpdater;

    public BuildSelectionPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.build_selection_panel_ctrl, this);

        Button stopBtn = (Button) findViewById(R.id.stop_btn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null && buildRequest != null) {
                    actionListener.onStopClick(star, buildRequest);
                }
            }
        });

        Button accelerateBtn = (Button) findViewById(R.id.accelerate_btn);
        accelerateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null && buildRequest != null) {
                    actionListener.onAccelerateClick(star, buildRequest);
                }
            }
        });

        refresh();

        handler = new Handler();
    }

    public void setBuildQueueActionListener(BuildQueueActionListener listener) {
        actionListener = listener;
    }

    public void setBuildRequest(Star star, BuildRequest buildRequest) {
        this.star = star;
        this.buildRequest = buildRequest;
        refresh();
    }

    public Star getStar() {
        return star;
    }

    public BuildRequest getBuildRequest() {
        return buildRequest;
    }

    public static void refreshEntryProgress(BuildRequest buildRequest, ProgressBar progressBar,
            TextView progressText) {
        String prefix = String.format(Locale.ENGLISH, "<font color=\"#0c6476\">%s:</font> ",
                buildRequest.getExistingBuildingKey() == null ? "Building" : "Upgrading");

        Duration remainingDuration = buildRequest.getRemainingTime();
        String msg;
        if (remainingDuration.equals(Duration.ZERO)) {
            if (buildRequest.getPercentComplete() > 99.0) {
                msg = String.format(Locale.ENGLISH, "%s %d %%, almost done",
                        prefix, (int) buildRequest.getPercentComplete());
            } else {
                msg = String.format(Locale.ENGLISH, "%s %d %%, not enough resources to complete.",
                        prefix, (int) buildRequest.getPercentComplete());
            }
        } else if (remainingDuration.getStandardMinutes() > 0) {
            msg = String.format(Locale.ENGLISH, "%s %d %%, %s left",
                                prefix, (int) buildRequest.getPercentComplete(),
                                TimeFormatter.create().format(remainingDuration));
        } else {
            msg = String.format(Locale.ENGLISH, "%s %d %%, almost done",
                                prefix, (int) buildRequest.getPercentComplete());
        }
        progressText.setText(Html.fromHtml(msg));

        progressBar.setProgress((int) buildRequest.getPercentComplete());
    }

    private void refresh() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.building_progress);
        TextView progressText = (TextView) findViewById(R.id.progress_text);
        ImageView icon = (ImageView) findViewById(R.id.building_icon);
        TextView buildingName = (TextView) findViewById(R.id.building_name);

        if (buildRequest == null) {
            findViewById(R.id.stop_btn).setEnabled(false);
            findViewById(R.id.accelerate_btn).setEnabled(false);
            buildingName.setVisibility(View.GONE);
            icon.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.stop_btn).setEnabled(true);
        findViewById(R.id.accelerate_btn).setEnabled(true);
        buildingName.setVisibility(View.VISIBLE);
        icon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        Design design = DesignManager.i.getDesign(buildRequest.getDesignKind(),
                buildRequest.getDesignID());

        icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        if (buildRequest.getCount() == 1) {
            buildingName.setText(design.getDisplayName());
        } else {
            buildingName.setText(String.format(Locale.ENGLISH, "%d × %s",
                    buildRequest.getCount(), design.getDisplayName(buildRequest.getCount() > 1)));
        }

        refreshEntryProgress(buildRequest, progressBar, progressText);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        StarManager.eventBus.register(eventHandler);

        progressUpdater = new ProgressUpdater();
        handler.postDelayed(progressUpdater, 5000);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        StarManager.eventBus.unregister(eventHandler);

        handler.removeCallbacks(progressUpdater);
        progressUpdater = null;
    }

    private void onStarUpdated(Star star) {
        this.star = star;
        for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
            if (baseBuildRequest.getKey().equals(buildRequest.getKey())) {
                this.buildRequest = (BuildRequest) baseBuildRequest;
            }
        }
        refresh();
    }

    private Object eventHandler = new Object() {
        @EventHandler
        public void onStarFetcher(Star star) {
            if (BuildSelectionPanel.this.star != null
                    && BuildSelectionPanel.this.star.getID() == star.getID()) {
                onStarUpdated(star);
            }
        }
    };

    /**
     * This is called every five seconds, we'll refresh the current progress values.
     */
    private class ProgressUpdater implements Runnable {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 5000);
        }
    }

    public interface BuildQueueActionListener {
        void onAccelerateClick(Star star, BuildRequest buildRequest);
        void onStopClick(Star star, BuildRequest buildRequest);
    }
}
