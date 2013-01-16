package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.SituationReport;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class SitrepActivity extends BaseActivity {
    private static Logger log = LoggerFactory.getLogger(SitrepActivity.class);
    private Context mContext = this;
    private SituationReportAdapter mSituationReportAdapter;

    private Map<String, StarSummary> mStarSummaries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        mStarSummaries = new TreeMap<String, StarSummary>();
        mSituationReportAdapter = new SituationReportAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();

        // clear all our notifications
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();

        setContentView(R.layout.sitrep);

        MyEmpire empire = EmpireManager.getInstance().getEmpire();

        TextView empireName = (TextView) findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);

        if (empire != null) {
            // TODO: add an "empire updated" listener here!
            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(empire.getShield(this));
        }

        final ListView reportItems = (ListView) findViewById(R.id.report_items);
        reportItems.setAdapter(mSituationReportAdapter);
        reportItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                SituationReport sitrep = mSituationReportAdapter.getSituationReport(position);

                Intent intent = new Intent(mContext, SolarSystemActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", sitrep.getStarKey());

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

    private void refresh() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        final ListView reportItems = (ListView) findViewById(R.id.report_items);

        progressBar.setVisibility(View.VISIBLE);
        reportItems.setVisibility(View.GONE);

        new AsyncTask<Void, Void, List<SituationReport>>() {

            @Override
            protected List<SituationReport> doInBackground(Void... params) {
                String url = "/api/v1/sit-reports";

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

                    // here we want to fetch all of the star summaries for any
                    // stars that are new and we don't currently have summaries
                    // for.
                    for (String starKey : missingStarSummaries) {
                        StarSummary starSummary = StarManager.getInstance()
                                .requestStarSummarySync(mContext, starKey);
                        mStarSummaries.put(starKey, starSummary);
                    }

                    return items;
                } catch (ApiException e) {
                    log.error("Error occured fetching situation reports.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<SituationReport> items) {
                progressBar.setVisibility(View.GONE);
                reportItems.setVisibility(View.VISIBLE);

                mSituationReportAdapter.setItems(items, mStarSummaries);
            }
        }.execute();
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

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SituationReport sitrep = mItems.get(position);
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.sitrep_row, null);
            }

            StarSummary starSummary = mStarSummaries.get(sitrep.getStarKey());
            String msg = sitrep.getDescription(starSummary);

            TextView reportTitle = (TextView) view.findViewById(R.id.report_title);
            TextView reportContent = (TextView) view.findViewById(R.id.report_content);
            TextView reportTime = (TextView) view.findViewById(R.id.report_time);
            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            ImageView overlayIcon = (ImageView) view.findViewById(R.id.overlay_icon);

            int imageSize = (int)(starSummary.getSize() * starSummary.getStarType().getImageScale() * 2);
            Sprite starSprite = StarImageManager.getInstance().getSprite(
                    SitrepActivity.this, starSummary, imageSize);
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
}
