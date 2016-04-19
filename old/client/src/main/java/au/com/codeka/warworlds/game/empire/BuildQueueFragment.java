package au.com.codeka.warworlds.game.empire;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Locale;

import au.com.codeka.NumberFormatter;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.BuildSelectionPanel;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.build.BuildAccelerateDialog;
import au.com.codeka.warworlds.game.build.BuildStopConfirmDialog;
import au.com.codeka.warworlds.game.empire.StarsFragment.StarsListAdapter;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.EmpireStarsFetcher;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;


public class BuildQueueFragment extends BaseFragment {
    private BuildQueueStarsListAdapter adapter;
    private EmpireStarsFetcher fetcher;
    private BuildSelectionPanel buildSelectionPanel;

    public BuildQueueFragment() {
        fetcher = new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Building, null);
        fetcher.getStars(0, 20);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empire_buildqueue_tab, parent, false);
        ExpandableListView starsList = (ExpandableListView) v.findViewById(R.id.stars);
        adapter = new BuildQueueStarsListAdapter(inflater, fetcher);
        starsList.setAdapter(adapter);

        buildSelectionPanel = (BuildSelectionPanel) v.findViewById(R.id.build_selection);
        buildSelectionPanel.setBuildQueueActionListener(
                new BuildSelectionPanel.BuildQueueActionListener() {
            @Override
            public void onAccelerateClick(Star star, BuildRequest buildRequest) {
                BuildAccelerateDialog dialog = new BuildAccelerateDialog();
                dialog.setBuildRequest(star, buildRequest);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }

            @Override
            public void onStopClick(Star star, BuildRequest buildRequest) {
                BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
                dialog.setBuildRequest(star, buildRequest);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }
        });

        starsList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                    int groupPosition, long id) {
                // if it doesn't have any builds, go to the star view
                if (adapter.getChildrenCount(groupPosition) == 0) {
                    Star star = (Star) adapter.getGroup(groupPosition);
                    if (star == null) {
                        // huh?
                        return false;
                    }

                    Intent intent = new Intent(getActivity(), SolarSystemActivity.class);
                    intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                    startActivity(intent);
                }
                return false;
            }
        });
        starsList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                Star star = (Star) adapter.getGroup(groupPosition);
                BuildRequest buildRequest = (BuildRequest) adapter.getChild(star, childPosition);
                buildSelectionPanel.setBuildRequest(star, buildRequest);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        final CheckBox showIdleStars = (CheckBox) v.findViewById(R.id.show_idle_stars);
        showIdleStars.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.updateFetcher(new EmpireStarsFetcher(showIdleStars.isChecked()
                        ? EmpireStarsFetcher.Filter.NotBuilding
                        : EmpireStarsFetcher.Filter.Building, null));
            }
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        StarManager.eventBus.register(eventHandler);
    }

    @Override
    public void onStop() {
        super.onStop();
        StarManager.eventBus.unregister(eventHandler);
    }

    private Object eventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            if (fetcher.onStarUpdated(star)) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    public class BuildQueueStarsListAdapter extends StarsListAdapter {
        private LayoutInflater inflater;
        private MyEmpire empire;

        public BuildQueueStarsListAdapter(LayoutInflater inflater, EmpireStarsFetcher fetcher) {
            super(fetcher);
            this.inflater = inflater;
            empire = EmpireManager.i.getEmpire();
        }

        @Override
        public int getNumChildren(Star star) {
            int numBuildRequests = 0;
            for (int i = 0; i < star.getBuildRequests().size(); i++) {
                Integer empireID = ((BuildRequest) star.getBuildRequests().get(i)).getEmpireID();
                if (empireID != null && empireID == empire.getID()) {
                    numBuildRequests ++;
                }
            }

            return numBuildRequests;
        }

        @Override
        public Object getChild(Star star, int index) {
            int buildRequestIndex = 0;
            for (int i = 0; i < star.getBuildRequests().size(); i++) {
                int empireID = ((BuildRequest) star.getBuildRequests().get(i)).getEmpireID();
                if (empireID == empire.getID()) {
                    if (buildRequestIndex == index) {
                        return star.getBuildRequests().get(i);
                    }
                    buildRequestIndex ++;
                }
            }

            // Shouldn't get here...
            return null;
        }

        @Override
        public View getStarView(Star star, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.empire_buildqueue_list_star_row, parent, false);
            }

            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            TextView starName = (TextView) view.findViewById(R.id.star_name);
            TextView queueEmpty = (TextView) view.findViewById(R.id.queue_empty);
            TextView buildingCount = (TextView) view.findViewById(R.id.building_count);
            TextView shipCount = (TextView) view.findViewById(R.id.ship_count);

            if (star == null) {
                starIcon.setImageBitmap(null);
                starName.setText("");
                queueEmpty.setText("");
                buildingCount.setText("...");
                shipCount.setText("...");
            } else {
                int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
                Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
                starIcon.setImageDrawable(new SpriteDrawable(sprite));
    
                starName.setText(star.getName());
    
                MyEmpire myEmpire = EmpireManager.i.getEmpire();
                EmpirePresence empirePresence = null;
                for (BaseEmpirePresence baseEmpirePresence : star.getEmpirePresences()) {
                    if (baseEmpirePresence.getEmpireKey().equals(myEmpire.getKey())) {
                        empirePresence = (EmpirePresence) baseEmpirePresence;
                        break;
                    }
                }
    
                if (empirePresence == null) {
                    return view;
                }
    
                int numBuildings = 0;
                int numShips = 0;
                DateTime queueEmptyTime = null;
                for (int i = 0; i < star.getBuildRequests().size(); i++) {
                    BuildRequest buildRequest = (BuildRequest) star.getBuildRequests().get(i);
                    int empireID = buildRequest.getEmpireID();
                    if (empireID == empire.getID()) {
                        if (buildRequest.getDesignKind() == DesignKind.BUILDING) {
                            numBuildings += buildRequest.getCount();
                        } else {
                            numShips += buildRequest.getCount();
                        }
                        if (queueEmptyTime == null
                                || queueEmptyTime.isBefore(buildRequest.getEndTime())) {
                            queueEmptyTime = buildRequest.getEndTime();
                        }
                    }
                }
    
                buildingCount.setText(String.format(Locale.ENGLISH, "%s",
                        NumberFormatter.format(numBuildings)));
                shipCount.setText(String.format(Locale.ENGLISH, "%s",
                        NumberFormatter.format(numShips)));
                if (queueEmptyTime == null) {
                    queueEmpty.setText("Queue Empty");
                } else {
                    queueEmpty.setText(TimeFormatter.create().format(
                            DateTime.now(), queueEmptyTime));
                }
            }
            return view;
        }

        @Override
        public View getChildView(Star star, int index, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.empire_buildqueue_list_buildrequest_row,
                        parent, false);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
            LinearLayout row1 = (LinearLayout) view.findViewById(R.id.building_row1);
            TextView progressText = (TextView) view.findViewById(R.id.building_row2);
            TextView row3 = (TextView) view.findViewById(R.id.building_row3);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.building_progress);
            TextView level = (TextView) view.findViewById(R.id.building_level);
            TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);
            TextView notes = (TextView) view.findViewById(R.id.notes);

            BuildRequest buildRequest = (BuildRequest) getChild(star, index);
            Design design = DesignManager.i.getDesign(buildRequest.getDesignKind(),
                                                      buildRequest.getDesignID());

            icon.setImageDrawable(new SpriteDrawable(
                    SpriteManager.i.getSprite(design.getSpriteName())));

            if (buildRequest.getExistingBuildingKey() != null) {
                level.setText(Integer.toString(buildRequest.getExistingBuildingLevel()));
            } else {
                level.setVisibility(View.GONE);
                levelLabel.setVisibility(View.GONE);
            }

            row1.removeAllViews();
            if (buildRequest.getCount() == 1) {
                addTextToRow(inflater.getContext(), row1, design.getDisplayName());
            } else {
                addTextToRow(inflater.getContext(), row1, String.format("%d × %s",
                        buildRequest.getCount(), design.getDisplayName(buildRequest.getCount() > 1)));
            }

            row3.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            if (buildRequest.getNotes() != null) {
                notes.setText(buildRequest.getNotes());
                notes.setVisibility(View.VISIBLE);
            } else {
                notes.setText("");
                notes.setVisibility(View.GONE);
            }

            BuildRequest selectedBuildRequest = buildSelectionPanel.getBuildRequest();
            if (selectedBuildRequest != null
                    && selectedBuildRequest.getKey().equals(buildRequest.getKey())) {
                view.setBackgroundResource(R.color.list_item_selected);
            } else {
                view.setBackgroundResource(android.R.color.transparent);
            }

            refreshEntryProgress(buildRequest, progressBar, progressText);

            return view;
        }

        private void addTextToRow(Context context, LinearLayout row, CharSequence text) {
            TextView tv = new TextView(context);
            tv.setText(text);
            tv.setSingleLine(true);
            tv.setEllipsize(TruncateAt.END);
            row.addView(tv);
        }

        public void refreshEntryProgress(BuildRequest buildRequest, ProgressBar progressBar,
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
    }
}
