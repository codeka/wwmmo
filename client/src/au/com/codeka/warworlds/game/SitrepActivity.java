package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.Notifications;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.model.SituationReport;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

public class SitrepActivity extends BaseActivity {
    private static Logger log = LoggerFactory.getLogger(SitrepActivity.class);
    private Context mContext = this;
    private SituationReportAdapter mSituationReportAdapter;
    private String mStarKey;
    private Map<String, StarSummary> mStarSummaries;
    private Handler mHandler;
    private String mCursor;
    private Messages.SituationReportFilter mFilter;
    private boolean mShowOldItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        mStarSummaries = new TreeMap<String, StarSummary>();
        mSituationReportAdapter = new SituationReportAdapter();

        mStarKey = getIntent().getStringExtra("au.com.codeka.warworlds.StarKey");
        mHandler = new Handler();

        int realmID = getIntent().getIntExtra("au.com.codeka.warworlds.RealmID", 0);
        if (realmID != 0) {
            RealmManager.i.selectRealm(realmID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setContentView(R.layout.sitrep);

        final Spinner filterSpinner = (Spinner) findViewById(R.id.filter);
        filterSpinner.setAdapter(new FilterAdapter());
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFilter = (Messages.SituationReportFilter) filterSpinner.getSelectedItem();
                refreshReportItems();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final Button markReadBtn = (Button) findViewById(R.id.mark_read);
        if (mStarKey != null) {
            markReadBtn.setVisibility(View.GONE);
        } else {
            markReadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    markAsRead();
                }
            });
        }

        final CheckBox showOldItems = (CheckBox) findViewById(R.id.show_read);
        showOldItems.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mShowOldItems = showOldItems.isChecked();
                refreshReportItems();
            }
        });

        final ListView reportItems = (ListView) findViewById(R.id.report_items);
        reportItems.setAdapter(mSituationReportAdapter);
        reportItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                SituationReport sitrep = mSituationReportAdapter.getSituationReport(position);

                Intent intent = new Intent(mContext, SolarSystemActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", sitrep.getStarKey());

                if (sitrep.getPlanetIndex() >= 0) {
                    intent.putExtra("au.com.codeka.warworlds.PlanetIndex", sitrep.getPlanetIndex());
                }

                SituationReport.MoveCompleteRecord mcr = sitrep.getMoveCompleteRecord();
                if (mcr != null) {
                    if (mcr.getScoutReportKey() != null &&
                        mcr.getScoutReportKey().length() > 0) {
                        // if there's a scout report, we'll want to show that
                        intent.putExtra("au.com.codeka.warworlds.ShowScoutReport", true);
                    }
                }

                String combatReportKey = null;
                SituationReport.FleetUnderAttackRecord fuar = sitrep.getFleetUnderAttackRecord();
                if (fuar != null) {
                    combatReportKey = fuar.getCombatReportKey();
                }
                SituationReport.FleetDestroyedRecord fdr = sitrep.getFleetDestroyedRecord();
                if (fdr != null) {
                    combatReportKey = fdr.getCombatReportKey();
                }
                SituationReport.FleetVictoriousRecord fvr = sitrep.getFleetVictoriousRecord();
                if (fvr != null) {
                    combatReportKey = fvr.getCombatReportKey();
                }

                if (combatReportKey != null) {
                    intent.putExtra("au.com.codeka.warworlds.CombatReportKey", combatReportKey);
                }

                startActivity(intent);
            }
        });

        refresh();
    }

    private void refreshTitle() {
        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);

        if (mStarKey == null) {
            // clear all our notifications
            Notifications.clearNotifications();

            MyEmpire empire = EmpireManager.i.getEmpire();

            if (empire != null) {
                // TODO: add an "empire updated" listener here!
                empireName.setText(empire.getDisplayName());
                empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(this, empire));
            }
        } else {
            StarManager.getInstance().requestStarSummary(mStarKey, new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary s) {
                    empireName.setText(s.getName());
                    Sprite starSprite = StarImageManager.getInstance().getSprite(s, empireIcon.getWidth(), true);
                    empireIcon.setImageDrawable(new SpriteDrawable(starSprite));
                }
            });
        }

    }

    private void refreshReportItems() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        final ListView reportItems = (ListView) findViewById(R.id.report_items);

        progressBar.setVisibility(View.VISIBLE);
        reportItems.setVisibility(View.GONE);

        mCursor = null;
        fetchReportItems(null, new FetchItemsCompleteHandler() {
            @Override
            public void onItemsFetched(List<SituationReport> items) {
                progressBar.setVisibility(View.GONE);
                reportItems.setVisibility(View.VISIBLE);

                mSituationReportAdapter.setItems(items, mStarSummaries);
            }
        });
    }

    private void fetchNextReportItems() {
        String cursor = mCursor;
        mCursor = null;

        fetchReportItems(cursor, new FetchItemsCompleteHandler() {
            @Override
            public void onItemsFetched(List<SituationReport> items) {
                if (items == null || items.size() == 0) {
                    // if there's no more, we set the cursor to null so the adapter knows
                    // there's no more
                    mCursor = null;
                }

                mSituationReportAdapter.appendItems(items, mStarSummaries);
            }
        });
    }

    private void fetchReportItems(final String cursor, final FetchItemsCompleteHandler handler) {
        new BackgroundRunner<List<SituationReport>>() {
            @Override
            protected List<SituationReport> doInBackground() {
                String url = "sit-reports";
                if (mStarKey != null) {
                    url = String.format("stars/%s/sit-reports", mStarKey);
                }
                boolean hasQuery = false;
                if (cursor != null) {
                    url += "?cursor="+cursor;
                    hasQuery = true;
                }
                if (mFilter != null && mFilter != Messages.SituationReportFilter.ShowAll) {
                    if (hasQuery) {
                        url += "&";
                    } else {
                        url += "?";
                    }
                    url += "filter="+mFilter;
                    hasQuery = true;
                }
                if (mShowOldItems) {
                    if (hasQuery) {
                        url += "&";
                    } else {
                        url += "?";
                    }
                    url += "show-old-items=1";
                    hasQuery = true;
                }

                try {
                    Messages.SituationReports pb = ApiClient.getProtoBuf(
                            url, Messages.SituationReports.class);

                    Set<String> missingStarSummaries = new TreeSet<String>();
                    ArrayList<SituationReport> items = new ArrayList<SituationReport>();
                    for(Messages.SituationReport srpb : pb.getSituationReportsList()) {
                        SituationReport sitrep = SituationReport.fromProtocolBuffer(srpb);

                        String starKey = sitrep.getStarKey();
                        if (!mStarSummaries.containsKey(starKey)) {
                            if (!missingStarSummaries.contains(starKey)) {
                                missingStarSummaries.add(starKey);
                            }
                        }

                        items.add(sitrep);
                    }

                    // grab the cursor we'll need to fetch the next batch
                    mCursor = pb.getCursor();

                    // here we want to fetch all of the star summaries for any
                    // stars that are new and we don't currently have summaries
                    // for.
                    for (String starKey : missingStarSummaries) {
                        StarSummary starSummary = StarManager.getInstance()
                                .requestStarSummarySync(starKey,
                                        Float.MAX_VALUE // always prefer a cached version, no matter how old
                                    );
                        mStarSummaries.put(starKey, starSummary);
                    }

                    return items;
                } catch (ApiException e) {
                    log.error("Error occured fetching situation reports.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(List<SituationReport> items) {
                handler.onItemsFetched(items);
            }
        }.execute();
    }

    private void markAsRead() {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "sit-reports/read";

                try {
                    ApiClient.postProtoBuf(url, null, null);
                } catch (ApiException e) {
                    log.error("Error occured fetching situation reports.", e);
                }

                return true;
            }

            @Override
            protected void onComplete(Boolean success) {
                refreshReportItems();
            }
        }.execute();
    }

    private interface FetchItemsCompleteHandler {
        public void onItemsFetched(List<SituationReport> items);
    }

    private void refresh() {
        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(SitrepActivity.this, WarWorldsActivity.class));
                } else {
                    refreshReportItems();
                    refreshTitle();
                }
            }
        });
    }

    private class SituationReportAdapter extends BaseAdapter {
        private List<SituationReport> mItems;
        private Map<String, StarSummary> mStarSummaries;

        public SituationReportAdapter() {
            mItems = new ArrayList<SituationReport>();
        }

        public SituationReport getSituationReport(int position) {
            return mItems.get(position);
        }

        public void setItems(List<SituationReport> items,
                             Map<String, StarSummary> starSummaries) {
            if (items == null) {
                items = new ArrayList<SituationReport>();
            }

            mStarSummaries = starSummaries;
            mItems = items;
            notifyDataSetChanged();
        }

        public void appendItems(List<SituationReport> items,
                                Map<String, StarSummary> starSummaries) {
            if (items == null) {
                items = new ArrayList<SituationReport>();
            }

            mStarSummaries = starSummaries;
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public int getViewTypeCount() {
            // The other type is the "please wait..." at the bottom
            return 2;
        }

        @Override
        public int getCount() {
            if (mCursor == null) {
                return mItems.size();
            }

            return mItems.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position == mItems.size())
                return null;
            return mItems.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) == null) {
                return 1;
            }
            return 0;
        }

        @Override
        public boolean isEnabled(int position) {
            if (getItem(position) == null) {
                return false;
            }

            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (position < mItems.size()) {
                    view = inflater.inflate(R.layout.sitrep_row, null);
                } else {
                    view = inflater.inflate(R.layout.sitrep_row_loading, null);
                }
            }

            if (position >= mItems.size()) {
                // note: once this view comes into... view, we'll want to load the next
                // lot of reports
                if (mCursor != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            fetchNextReportItems();
                        }
                    });
                }

                return view;
            }

            SituationReport sitrep = mItems.get(position);
            StarSummary starSummary = mStarSummaries.get(sitrep.getStarKey());
            String msg = sitrep.getDescription(starSummary);

            TextView reportTitle = (TextView) view.findViewById(R.id.report_title);
            TextView reportContent = (TextView) view.findViewById(R.id.report_content);
            TextView reportTime = (TextView) view.findViewById(R.id.report_time);
            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            ImageView overlayIcon = (ImageView) view.findViewById(R.id.overlay_icon);

            int imageSize = (int)(starSummary.getSize() * starSummary.getStarType().getImageScale() * 2);
            Sprite starSprite = StarImageManager.getInstance().getSprite(starSummary, imageSize, true);
            starIcon.setImageDrawable(new SpriteDrawable(starSprite));

            reportTime.setText(TimeInHours.format(sitrep.getReportTime()));
            reportContent.setText(msg);
            reportTitle.setText(sitrep.getTitle());

            Sprite designSprite = sitrep.getDesignSprite();
            if (designSprite != null) {
                overlayIcon.setImageDrawable(new SpriteDrawable(designSprite));
            }

            return view;
        }
    }

    public class FilterAdapter extends BaseAdapter implements SpinnerAdapter {
        Messages.SituationReportFilter[] mFilters;
        String[] mFilterDescriptions;

        public FilterAdapter() {
            mFilters = new Messages.SituationReportFilter[] {
                    Messages.SituationReportFilter.ShowAll,
                    Messages.SituationReportFilter.MoveComplete,
                    Messages.SituationReportFilter.BuildCompleteAny,
                    Messages.SituationReportFilter.BuildCompleteShips,
                    Messages.SituationReportFilter.BuildCompleteBuilding,
                    Messages.SituationReportFilter.FleetAttacked,
                    Messages.SituationReportFilter.FleetDestroyed,
                    Messages.SituationReportFilter.FleetVictorious,
                    Messages.SituationReportFilter.ColonyAttacked,
                    Messages.SituationReportFilter.ColonyDestroyed,
            };
            mFilterDescriptions = new String[] {
                    "Show All",
                    "Move Complete",
                    "Build Complete",
                    "Build Complete (Ships)",
                    "Build Complete (Building)",
                    "Fleet Attacked",
                    "Fleet Destroyed",
                    "Fleet Victorious",
                    "Colony Attacked",
                    "Colony Destroyed",
            };
        }

        @Override
        public int getCount() {
            return mFilters.length;
        }

        @Override
        public Object getItem(int position) {
            return mFilters[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = getCommonView(position, convertView, parent);

            view.setTextColor(Color.WHITE);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = getCommonView(position, convertView, parent);

            ViewGroup.LayoutParams lp = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                                                                     LayoutParams.MATCH_PARENT);
            lp.height = 80;
            view.setLayoutParams(lp);
            view.setTextColor(Color.WHITE);
            view.setText("  "+view.getText().toString());
            return view;
        }

        private TextView getCommonView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = new TextView(mContext);
                view.setGravity(Gravity.CENTER_VERTICAL);
            }

            String displayValue = mFilterDescriptions[position];
            view.setText(displayValue);
            return view;
        }
    }

}
