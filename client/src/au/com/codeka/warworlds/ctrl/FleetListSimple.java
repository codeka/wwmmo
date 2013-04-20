package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

public class FleetListSimple extends ListView {
    private Context mContext;
    private Star mStar;
    private FleetListAdapter mFleetListAdapter;
    private FleetSelectedHandler mFleetSelectedHandler;

    public FleetListSimple(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public FleetListSimple(Context context) {
        super(context);
        mContext = context;
    }

    public void setFleetSelectedHandler(FleetSelectedHandler handler) {
        mFleetSelectedHandler = handler;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setStar(Star s) {
        mStar = s;
        refresh();
    }

    private void refresh() {
        if (mFleetListAdapter == null) {
            mFleetListAdapter = new FleetListAdapter();
            setAdapter(mFleetListAdapter);

            setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Fleet fleet = (Fleet) mFleetListAdapter.getItem(position);
                    if (mFleetSelectedHandler != null) {
                        mFleetSelectedHandler.onFleetSelected(fleet);
                    }
                }
            });
        }

        mFleetListAdapter.setStar(mStar);
    }


    class FleetListAdapter extends BaseAdapter {
        private Star mStar;
        private List<Fleet> mFleets;

        public void setStar(Star star) {
            mStar = star;
            mFleets = new ArrayList<Fleet>();

            if (star.getFleets() != null) {
                for (BaseFleet f : star.getFleets()) {
                    if (!f.getState().equals(Fleet.State.MOVING)) {
                        mFleets.add((Fleet) f);
                    }
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mStar == null) {
                return 0;
            }

            return mFleets.size();
        }

        @Override
        public Object getItem(int position) {
            return mFleets.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);
            }

            final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
            final Fleet fleet = mFleets.get(position);
            Design design = DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());

            icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

            TextView shipKindTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
            shipKindTextView.setText(String.format("%d × %s",
                    (int) Math.ceil(fleet.getNumShips()), design.getDisplayName(fleet.getNumShips() > 1)));

            final TextView shipCountTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
            shipCountTextView.setText(String.format("%s",
                    StringUtils.capitalize(fleet.getStance().toString().toLowerCase(Locale.ENGLISH))));

            return view;
        }
    }

    public interface FleetSelectedHandler {
        void onFleetSelected(Fleet fleet);
    }
}
