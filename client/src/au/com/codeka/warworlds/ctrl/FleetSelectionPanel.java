package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.common.model.BaseFleet.State;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetList.OnFleetActionListener;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.FleetUpgrade.BoostFleetUpgrade;

/** Control displayed below a fleet list with controls to interact with the selected fleet. */
public class FleetSelectionPanel extends FrameLayout {
    private Context mContext;
    private Star mStar;
    private Fleet mFleet;
    private FleetList.OnFleetActionListener mFleetActionListener;

    public FleetSelectionPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public FleetSelectionPanel(Context context) {
        super(context);
        initialize(context);
    }

    public void setOnFleetActionListener(OnFleetActionListener listener) {
        mFleetActionListener = listener;
    }

    public void setSelectedFleet(Star star, Fleet fleet) {
        mStar = star;
        mFleet = fleet;

        final Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        final Button mergeBtn = (Button) findViewById(R.id.merge_btn);

        moveBtn.setText("Move");
        if (mFleet != null) {
            stanceSpinner.setEnabled(true);
            if (viewBtn != null) {
                viewBtn.setEnabled(true);
            }

            if (mFleet.getState() == State.IDLE) {
                moveBtn.setEnabled(true);
                splitBtn.setEnabled(true);
                mergeBtn.setEnabled(true);
            } else if (mFleet.getState() == State.MOVING) {
                BoostFleetUpgrade boost = (BoostFleetUpgrade) mFleet.getUpgrade("boost");
                if (boost != null) {
                    moveBtn.setText("Boost");
                    moveBtn.setEnabled(!boost.isBoosting());
                } else {
                    moveBtn.setEnabled(false);
                }
                splitBtn.setEnabled(false);
                mergeBtn.setEnabled(false);
            } else {
                moveBtn.setEnabled(false);
                splitBtn.setEnabled(false);
                mergeBtn.setEnabled(false);
            }

            stanceSpinner.setSelection(mFleet.getStance().getValue() - 1);
        } else {
            stanceSpinner.setEnabled(false);
            if (viewBtn != null) {
                viewBtn.setEnabled(false);
            }
            moveBtn.setEnabled(false);
            splitBtn.setEnabled(false);
            mergeBtn.setEnabled(false);
        }

    }

    private void initialize(Context context) {
        mContext = context;
        inflate(context, R.layout.fleet_selection_panel_ctrl, this);

        final Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
        stanceSpinner.setAdapter(new StanceAdapter());

        stanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fleet.Stance stance = Fleet.Stance.values()[position];
                if (mFleet == null) {
                    return;
                }

                if (mFleet.getStance() != stance && mFleetActionListener != null) {
                    mFleetActionListener.onFleetStanceModified(mStar, mFleet, stance);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        splitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetSplit(mStar, mFleet);
                }
            }
        });

        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    if (mFleet.getState() == State.MOVING && mFleet.hasUpgrade("boost")) {
                        mFleetActionListener.onFleetBoost(mStar, mFleet);
                    } else if (mFleet.getState() == State.IDLE) {
                        mFleetActionListener.onFleetMove(mStar, mFleet);
                    }
                }
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        if (viewBtn != null) viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetView(mStar, mFleet);
                }
            }
        });

        final Button mergeBtn = (Button) findViewById(R.id.merge_btn);
        mergeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleet == null) {
                    return;
                }

                if (mFleetActionListener != null) {
                    List<Fleet> fleets = new ArrayList<Fleet>();
                    for (int i = 0; i < mStar.getFleets().size(); i++) {
                        fleets.add((Fleet) mStar.getFleets().get(i));
                    }
                    mFleetActionListener.onFleetMerge(mFleet, fleets);
                }
            }
        });
    }

    public class StanceAdapter extends BaseAdapter implements SpinnerAdapter {
        Fleet.Stance[] mValues;

        public StanceAdapter() {
            mValues = Fleet.Stance.values();
        }

        @Override
        public int getCount() {
            return mValues.length;
        }

        @Override
        public Object getItem(int position) {
            return mValues[position];
        }

        @Override
        public long getItemId(int position) {
            return mValues[position].getValue();
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

            Fleet.Stance value = mValues[position];
            view.setText(StringUtils.capitalize(value.toString().toLowerCase(Locale.ENGLISH)));
            return view;
        }
    }
}
