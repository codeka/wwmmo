package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.PlanetType;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;

public class PlanetListSimple extends LinearLayout {
    private Context mContext;
    private Star mStar;
    private List<Planet> mPlanets;
    private PlanetSelectedHandler mPlanetSelectedHandler;
    private View.OnClickListener mOnClickListener;

    public PlanetListSimple(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public PlanetListSimple(Context context) {
        super(context);
        mContext = context;
    }

    public void setPlanetSelectedHandler(PlanetSelectedHandler handler) {
        mPlanetSelectedHandler = handler;
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
        if (mOnClickListener == null) {
            mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Planet planet = (Planet) v.getTag();
                    if (mPlanetSelectedHandler != null) {
                        mPlanetSelectedHandler.onPlanetSelected(planet);
                    }
                }
            };
        }

        mPlanets = new ArrayList<Planet>(mStar.planets);

        removeAllViews();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        for (Planet planet : mPlanets) {
            View rowView = getRowView(inflater, mStar, planet);
            addView(rowView);
        }
    }

    private View getRowView(LayoutInflater inflater, Star star, Planet planet) {
        View view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);

        final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
        final PlanetImageManager pim = PlanetImageManager.getInstance();

        Sprite sprite = pim.getSprite(star, planet);
        icon.setImageDrawable(new SpriteDrawable(sprite));

        TextView planetTypeTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
        planetTypeTextView.setText(PlanetType.get(planet).getDisplayName());

        Colony colony = null;
        for(Colony c : mStar.colonies) {
            if (c.planet_index == planet.index) {
                colony = c;
                break;
            }
        }

        final TextView colonyTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
        if (colony != null) {
            colonyTextView.setText("Colonized");
            EmpireManager.i.fetchEmpire(colony.empire_key, new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    colonyTextView.setText(empire.display_name);
                }
            });
        } else {
            colonyTextView.setText("");
        }

        view.setOnClickListener(mOnClickListener);
        view.setTag(planet);
        return view;
    }

    public interface PlanetSelectedHandler {
        void onPlanetSelected(Planet planet);
    }
}
