package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.ColonyList;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ScoutReport;
import au.com.codeka.warworlds.model.Star;

public class ScoutReportDialog extends DialogFragment {
    private ReportAdapter mReportAdapter;
    private Star mStar;
    private View mView;

    public ScoutReportDialog() {
    }

    public void setStar(Star star) {
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.scout_report_dlg, null);

        final View progressBar = mView.findViewById(R.id.progress_bar);
        final View reportList = mView.findViewById(R.id.report_items);
        final View reportDate = mView.findViewById(R.id.report_date);
        final View newerButton = mView.findViewById(R.id.newer_btn);
        final View olderButton = mView.findViewById(R.id.older_btn);
        final ListView reportItems = (ListView) mView.findViewById(R.id.report_items);

        progressBar.setVisibility(View.VISIBLE);
        reportList.setVisibility(View.GONE);
        reportDate.setEnabled(false);
        newerButton.setEnabled(false);
        olderButton.setEnabled(false);

        mReportAdapter = new ReportAdapter();
        reportItems.setAdapter(mReportAdapter);

        progressBar.setVisibility(View.VISIBLE);
        reportList.setVisibility(View.GONE);

        EmpireManager.getInstance().getEmpire().fetchScoutReports(
                mStar, new MyEmpire.FetchScoutReportCompleteHandler() {
            @Override
            public void onComplete(List<ScoutReport> reports) {
                progressBar.setVisibility(View.GONE);
                reportList.setVisibility(View.VISIBLE);

                refreshReports(reports);
            }
        });

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setView(mView);

        b.setNeutralButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return b.create();
    }

    private void refreshReports(List<ScoutReport> reports) {

        Spinner reportDates = (Spinner) mView.findViewById(R.id.report_date);
        reportDates.setAdapter(new ReportDatesAdapter(reports));

        if (reports.size() > 0) {
            final View reportDate = mView.findViewById(R.id.report_date);
            final View newerButton = mView.findViewById(R.id.newer_btn);
            final View olderButton = mView.findViewById(R.id.older_btn);
            reportDate.setEnabled(true);
            newerButton.setEnabled(true);
            olderButton.setEnabled(true);

            mReportAdapter.setReport(reports.get(0));
        }
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
                view = new TextView(getActivity());
                view.setGravity(Gravity.CENTER_VERTICAL);
            }

            ScoutReport report = mReports.get(position);
            view.setText(TimeInHours.format(report.getReportDate()));
            return view;
        }
    }

    private class ReportAdapter extends BaseAdapter {
        private List<ReportItem> mItems;
        private Star mStar;

        public ReportAdapter() {
            mItems = new ArrayList<ReportItem>();
        }

        public void setReport(ScoutReport report) {
            mItems.clear();
            mStar = report.getStarSnapshot();

            Star star = report.getStarSnapshot();
            for (Colony colony : star.getColonies()) {
                ReportItem item = new ReportItem();
                item.colony = colony;
                mItems.add(item);
            }

            for (Fleet fleet : star.getFleets()) {
                ReportItem item = new ReportItem();
                item.fleet = fleet;
                mItems.add(item);
            }

            Collections.sort(mItems, new Comparator<ReportItem>() {
                @Override
                public int compare(ReportItem lhs, ReportItem rhs) {
                    if (lhs.colony != null && rhs.colony == null) {
                        return -1;
                    } else if (lhs.fleet != null && rhs.fleet == null) {
                        return 1;
                    }

                    if (lhs.colony != null) {
                        final Colony lhsColony = lhs.colony;
                        final Colony rhsColony = rhs.colony;

                        return lhsColony.getPlanetIndex() - rhsColony.getPlanetIndex();
                    } else {
                        final Fleet lhsFleet = lhs.fleet;
                        final Fleet rhsFleet = rhs.fleet;

                        if (lhsFleet.getDesignID().equals(rhsFleet.getDesignID())) {
                            return lhsFleet.getNumShips() - rhsFleet.getNumShips();
                        } else {
                            return lhsFleet.getDesignID().compareTo(rhsFleet.getDesignID());
                        }
                    }
                }
            });

            notifyDataSetChanged();
        }

        /**
         * Two kinds of views, one for the colonies and one for the fleets.
         */
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        /**
         * We just check whether it's a colony or fleet in that position.
         */
        @Override
        public int getItemViewType(int position) {
            if (mItems.get(position).colony != null) {
                return 0;
            } else {
                return 1;
            }
        }

        /**
         * None of the items in the list are selectable.
         */
        @Override
        public boolean isEnabled(int position) {
            return false;
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
            ReportItem item = mItems.get(position);
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (item.colony != null) {
                    view = inflater.inflate(R.layout.colony_list_row, null);
                } else {
                    view = inflater.inflate(R.layout.fleet_list_row, null);
                }
            }

            if (item.colony != null) {
                Colony colony = item.colony;

                ColonyList.populateColonyListRow(getActivity(), view, colony, mStar);
                TextView uncollectedTaxes = (TextView) view.findViewById(R.id.colony_taxes);
                uncollectedTaxes.setText("");
            } else {
                Fleet fleet = item.fleet;
                FleetList.populateFleetRow(getActivity(), null, view, fleet);
            }

            return view;

        }

        private class ReportItem {
            public Colony colony;
            public Fleet fleet;
        }
    }
}
