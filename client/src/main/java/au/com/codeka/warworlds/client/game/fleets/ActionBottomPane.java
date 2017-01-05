package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.common.base.CaseFormat;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.Fleet;

/**
 * Bottom pane of the fleets view that contains the main action buttons (split, move, etc).
 */
public class ActionBottomPane extends RelativeLayout {
  public ActionBottomPane(Context context, AttributeSet attrs) {
    super(context, attrs);

    inflate(context, R.layout.ctrl_fleet_action_bottom_pane, this);

    Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
    stanceSpinner.setAdapter(new StanceAdapter());

    findViewById(R.id.split_btn).setOnClickListener(this::onSplitClick);
  }

  /** Called when you click 'split'. */
  private void onSplitClick(View view) {

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
