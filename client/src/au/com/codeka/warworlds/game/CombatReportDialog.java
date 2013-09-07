package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.design.ShipDesign;
import au.com.codeka.common.model.CombatReport;
import au.com.codeka.common.model.CombatRound;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpireManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;

public class CombatReportDialog extends DialogFragment {
    private Star mStar;
    private String mCombatReportKey;
    private View mView;
    private ReportAdapter mReportAdapter;
    private TreeMap<String, Empire> mEmpires;

    public void loadCombatReport(Star star, String combatReportKey) {
        mStar = star;
        mCombatReportKey = combatReportKey;

        if (mEmpires == null) {
            mEmpires = new TreeMap<String, Empire>();
            mEmpires.put("", EmpireManager.i.getNativeEmpire());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.combat_report_dlg, null);

        final View progressBar = mView.findViewById(R.id.progress_bar);
        final ListView reportItems = (ListView) mView.findViewById(R.id.report_items);

        progressBar.setVisibility(View.VISIBLE);
        reportItems.setVisibility(View.GONE);

        mReportAdapter = new ReportAdapter();
        reportItems.setAdapter(mReportAdapter);

        MyEmpireManager.i.fetchCombatReport(
                mStar.key, mCombatReportKey, 
                new MyEmpireManager.FetchCombatReportCompleteHandler() {
            @Override
            public void onComplete(CombatReport report) {
                progressBar.setVisibility(View.GONE);
                reportItems.setVisibility(View.VISIBLE);

                TreeSet<String> empireKeys = new TreeSet<String>();
                for (CombatRound round : report.rounds) {
                    for (CombatRound.FleetSummary fleet : round.fleets) {
                        if (fleet.empire_key == null || fleet.empire_key.length() == 0) {
                            continue;
                        }
                        if (mEmpires.containsKey(fleet.empire_key)) {
                            continue;
                        }
                        if (!empireKeys.contains(fleet.empire_key)) {
                            empireKeys.add(fleet.empire_key);
                        }
                    }
                }
                for (String empireKey : empireKeys) {
                    EmpireManager.i.fetchEmpire(empireKey,
                            new EmpireManager.EmpireFetchedHandler() {
                                @Override
                                public void onEmpireFetched(Empire empire) {
                                    mEmpires.put(empire.key, empire);
                                    mReportAdapter.notifyDataSetChanged();
                                }
                            });
                }

                mReportAdapter.setReport(report);
            }
        });

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setNeutralButton("Close", null);

        return b.create();
    }

    private class ReportAdapter extends BaseAdapter {
        private CombatReport mCombatReport;
        private ArrayList<Record> mRecords;

        private static final int ROUND_HEADER_TYPE = 0;
        private static final int ROUND_ENTRY_TYPE = 1;

        public void setReport(CombatReport combatReport) {
            mCombatReport = combatReport;
            mRecords = new ArrayList<Record>();
            for (CombatRound round : mCombatReport.rounds) {
                mRecords.add(new Record(round));
                for (CombatRound.FleetJoinedRecord record : round.fleets_joined) {
                    mRecords.add(new Record(record));
                }
                for (CombatRound.FleetTargetRecord record : round.fleets_targetted) {
                    mRecords.add(new Record(record));
                }
                for (CombatRound.FleetAttackRecord record : round.fleets_attacked) {
                    mRecords.add(new Record(record));
                }
                for (CombatRound.FleetDamagedRecord record : round.fleets_damaged) {
                    mRecords.add(new Record(record));
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mRecords == null)
                return 0;

            if (mRecords.get(position).combatRound != null) {
                return ROUND_HEADER_TYPE;
            } else {
                return ROUND_ENTRY_TYPE;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return (mRecords != null ? mRecords.size() : 0);
        }

        @Override
        public Object getItem(int position) {
            return mRecords.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Record record = mRecords.get(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                if (record.combatRound != null) {
                    view = inflater.inflate(R.layout.combat_report_header_row, null);
                } else {
                    view = inflater.inflate(R.layout.combat_report_entry_row, null);
                }
            }

            if (record.combatRound != null) {
                int roundIndex = 0;
                for (; roundIndex < mCombatReport.rounds.size(); roundIndex++) {
                    if (mCombatReport.rounds.get(roundIndex) == record.combatRound)
                        break;
                }

                TextView roundNumber = (TextView) view.findViewById(R.id.round_number);
                TextView roundTime = (TextView) view.findViewById(R.id.round_time);

                roundNumber.setText(String.format(Locale.ENGLISH, "Round #%d",
                                                  roundIndex + 1));
                roundTime.setText(String.format(Locale.ENGLISH, "%d min",
                                                  roundIndex));
            } else {
                ImageView fleetIcon = (ImageView) view.findViewById(R.id.fleet_icon);
                ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                ImageView targetFleetIcon = (ImageView) view.findViewById(R.id.target_fleet_icon);
                ImageView targetEmpireIcon = (ImageView) view.findViewById(R.id.target_empire_icon);
                TextView recordTitle = (TextView) view.findViewById(R.id.record_title);
                TextView recordDescription = (TextView) view.findViewById(R.id.record_description);

                empireIcon.setImageBitmap(null);
                targetFleetIcon.setImageDrawable(null);
                targetEmpireIcon.setImageBitmap(null);
                recordDescription.setText("");

                if (record.fleetJoinedRecord != null) {
                    CombatRound.FleetSummary fleet = record.combatRound.fleets.get(record.fleetJoinedRecord.fleet_index);
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
                    Empire empire = mEmpires.get(fleet.empire_key);

                    recordTitle.setText("Fleet Joined Battle");
                    fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));
                    if (empire != null) {
                        empireIcon.setImageBitmap(EmpireHelper.getShield(getActivity(), empire));
                        recordDescription.setText(String.format(Locale.ENGLISH,
                                "%s %s joined the battle",
                                empire.display_name,
                                getFleetName(design, fleet.num_ships)));
                    }
                } else if (record.fleetTargetRecord != null) {
                    CombatRound.FleetSummary fleet = record.combatRound.fleets.get(record.fleetTargetRecord.fleet_index);
                    CombatRound.FleetSummary target = record.combatRound.fleets.get(record.fleetTargetRecord.target_index);
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
                    ShipDesign targetDesign = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, target.design_id);
                    Empire empire = mEmpires.get(fleet.empire_key);
                    Empire targetEmpire = mEmpires.get(target.empire_key);

                    recordTitle.setText("Fleet Targetted");
                    fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));
                    targetFleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(targetDesign.getSpriteName())));
                    if (empire != null && targetEmpire != null) {
                        empireIcon.setImageBitmap(EmpireHelper.getShield(getActivity(), empire));
                        targetEmpireIcon.setImageBitmap(EmpireHelper.getShield(getActivity(), targetEmpire));

                        recordDescription.setText(String.format(Locale.ENGLISH,
                                "%s %s targetted %s %s",
                                empire.display_name, getFleetName(design, fleet.num_ships),
                                targetEmpire.display_name, getFleetName(targetDesign, target.num_ships)));
                    }
                } else if (record.fleetAttackRecord != null) {
                    CombatRound.FleetSummary fleet = record.combatRound.fleets.get(record.fleetAttackRecord.fleet_index);
                    CombatRound.FleetSummary target = record.combatRound.fleets.get(record.fleetAttackRecord.target_index);
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
                    ShipDesign targetDesign = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, target.design_id);
                    Empire empire = mEmpires.get(fleet.empire_key);
                    Empire targetEmpire = mEmpires.get(target.empire_key);

                    recordTitle.setText("Fleet Attacked");
                    fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));
                    targetFleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(targetDesign.getSpriteName())));
                    if (empire != null && targetEmpire != null) {
                        empireIcon.setImageBitmap(EmpireHelper.getShield(getActivity(), empire));
                        targetEmpireIcon.setImageBitmap(EmpireHelper.getShield(getActivity(), targetEmpire));

                        recordDescription.setText(String.format(Locale.ENGLISH,
                                "%s %s attacked %s %s for %.1f points",
                                empire.display_name, getFleetName(design, fleet.num_ships),
                                targetEmpire.display_name, getFleetName(targetDesign, target.num_ships),
                                record.fleetAttackRecord.damage));
                    }
                } else if (record.fleetDamagedRecord != null) {
                    CombatRound.FleetSummary fleet = record.combatRound.fleets.get(record.fleetDamagedRecord.fleet_index);
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
                    Empire empire = mEmpires.get(fleet.empire_key);

                    recordTitle.setText("Fleet Damaged");
                    fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));
                    if (empire != null) {
                        empireIcon.setImageBitmap(EmpireHelper.getShield(getActivity(), empire));

                        recordDescription.setText(String.format(Locale.ENGLISH,
                                "%s %s was hit for %.1f damage",
                                empire.display_name, getFleetName(design, fleet.num_ships),
                                record.fleetDamagedRecord.damage));
                    }
                }
            }

            return view;
        }

        private String getFleetName(ShipDesign design, float numShips) {
            int intNumShips = (int)(Math.ceil(numShips));
            if (intNumShips == 1) {
                return design.getDisplayName();
            } else {
                return String.format(Locale.ENGLISH, "%d × %s",
                        intNumShips, design.getDisplayName(intNumShips > 1));
            }
        }

        private class Record {
            // only one of these will be non-null and it represents the record
            // we're displaying in this particular row

            // if non-null, this is the "header" row for that round.
            public CombatRound combatRound;

            public CombatRound.FleetJoinedRecord fleetJoinedRecord;
            public CombatRound.FleetTargetRecord fleetTargetRecord;
            public CombatRound.FleetAttackRecord fleetAttackRecord;
            public CombatRound.FleetDamagedRecord fleetDamagedRecord;

            public Record(CombatRound combatRound) {
                this.combatRound = combatRound;
            }
            public Record(CombatRound.FleetJoinedRecord record) {
                this.fleetJoinedRecord = record;
            }
            public Record(CombatRound.FleetTargetRecord record) {
                this.fleetTargetRecord = record;
            }
            public Record(CombatRound.FleetAttackRecord record) {
                this.fleetAttackRecord = record;
            }
            public Record(CombatRound.FleetDamagedRecord record) {
                this.fleetDamagedRecord = record;
            }
        }
    }
}
