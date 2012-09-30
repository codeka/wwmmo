package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ScoutReport;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

public class ScoutReportDialog extends Dialog implements DialogManager.DialogConfigurable {
    private static Logger log = LoggerFactory.getLogger(ScoutReportDialog.class);
    private Context mContext;
    private Star mStar;
    private List<ScoutReport> mReports;

    public static final int ID = 98475;

    public ScoutReportDialog(Activity activity) {
        super(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.scout_report_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        final View progressBar = findViewById(R.id.progress_bar);
        final View reportList = findViewById(R.id.report_items);
        final View reportDate = findViewById(R.id.report_date);
        final View newerButton = findViewById(R.id.newer_btn);
        final View olderButton = findViewById(R.id.older_btn);

        progressBar.setVisibility(View.VISIBLE);
        reportList.setVisibility(View.GONE);
        reportDate.setEnabled(false);
        newerButton.setEnabled(false);
        olderButton.setEnabled(false);
    }

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        final View progressBar = findViewById(R.id.progress_bar);
        final View reportList = findViewById(R.id.report_items);

        progressBar.setVisibility(View.VISIBLE);
        reportList.setVisibility(View.GONE);

        String starKey = bundle.getString("au.com.codeka.warworlds.StarKey");
        mStar = SectorManager.getInstance().findStar(starKey);
        if (mStar == null) {
            // TODO: should never happen...
            log.error("SectorManager.findStar() returned null!");
            return;
        }

        EmpireManager.getInstance().getEmpire().fetchScoutReports(mStar, new MyEmpire.FetchScoutReportCompleteHandler() {
            @Override
            public void onComplete(List<ScoutReport> reports) {
                progressBar.setVisibility(View.GONE);
                reportList.setVisibility(View.VISIBLE);

                refreshReports(reports);
            }
        });
    }

    private void refreshReports(List<ScoutReport> reports) {
        mReports = reports;

        Spinner reportDates = (Spinner) findViewById(R.id.report_date);
        reportDates.setAdapter(new ReportDatesAdapter(reports));

        final View reportDate = findViewById(R.id.report_date);
        final View newerButton = findViewById(R.id.newer_btn);
        final View olderButton = findViewById(R.id.older_btn);
        reportDate.setEnabled(true);
        newerButton.setEnabled(true);
        olderButton.setEnabled(true);
    }

    private class ReportDatesAdapter extends BaseAdapter implements SpinnerAdapter {
        List<ScoutReport> mReports;

        public ReportDatesAdapter(List<ScoutReport> reports) {
            mReports = reports;
        }

        @Override
        public int getCount() {
            return mReports.size();
        }

        @Override
        public Object getItem(int position) {
            return mReports.get(position);
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
            lp.height = 60;
            view.setLayoutParams(lp);

            view.setTextColor(Color.BLACK);
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

            ScoutReport report = mReports.get(position);
            Interval interval = new Interval(report.getReportDate(), DateTime.now(DateTimeZone.UTC));
            Duration duration = interval.toDuration();
            view.setText(String.format("%d hrs ago", duration.toStandardHours().getHours()));
            return view;
        }
    }

}
