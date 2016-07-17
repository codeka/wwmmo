package au.com.codeka.warworlds.client.fleets;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.common.base.CaseFormat;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.world.ArrayListStarCollection;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This fragment contains a list of fleets, and lets you do all the interesting stuff on them (like
 * merge, split, move, etc).
 */
public class FleetsFragment extends BaseFragment {
  private static final Log log = new Log("FleetFragment");
  private static final String STAR_ID_KEY = "StarID";

  /** The star whose fleets we're displaying, null if we're displaying all stars fleets. */
  @Nullable private Star star;

  private ArrayListStarCollection starCollection;
  private FleetExpandableStarListAdapter adapter;

  public static Bundle createArguments(long starId) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starId);
    return args;
  }

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_fleets;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Spinner stanceSpinner = (Spinner) view.findViewById(R.id.stance);
    stanceSpinner.setAdapter(new StanceAdapter());

    ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.fleet_list);
    starCollection = new ArrayListStarCollection();
    adapter = new FleetExpandableStarListAdapter(
        getLayoutInflater(savedInstanceState), starCollection);
    listView.setAdapter(adapter);
  }

  @Override
  public void onStart() {
    super.onStart();

    long starID = getArguments().getLong(STAR_ID_KEY);
    star = StarManager.i.getStar(starID);
    starCollection.getStars().clear();
    starCollection.getStars().add(star);
    adapter.notifyDataSetChanged();
  }

  public class StanceAdapter extends BaseAdapter implements SpinnerAdapter {
    Fleet.FLEET_STANCE[] values;

    public StanceAdapter() {
      values = Fleet.FLEET_STANCE.values();
    }

    @Override
    public int getCount() {
      return values.length;
    }

    @Override
    public Object getItem(int position) {
      return values[position];
    }

    @Override
    public long getItemId(int position) {
      return values[position].getValue();
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

      ViewGroup.LayoutParams lp = new AbsListView.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT);
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
        view = new TextView(getContext());
        view.setGravity(Gravity.CENTER_VERTICAL);
      }

      Fleet.FLEET_STANCE value = values[position];
      view.setText(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value.toString()));
      return view;
    }
  }
}
