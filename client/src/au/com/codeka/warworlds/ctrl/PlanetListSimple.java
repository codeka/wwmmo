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
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;

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

        mPlanets = new ArrayList<Planet>();
        for (BasePlanet p : mStar.getPlanets()) {
            mPlanets.add((Planet) p);
        }

        removeAllViews();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        for (Planet planet : mPlanets) {
            View rowView = getRowView(inflater, planet);
            addView(rowView);
        }
    }

    private View getRowView(LayoutInflater inflater, Planet planet) {
        View view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);

        final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
        final PlanetImageManager pim = PlanetImageManager.getInstance();

        Sprite sprite = pim.getSprite(mContext, planet);
        icon.setImageDrawable(new SpriteDrawable(sprite));

        TextView planetTypeTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
        planetTypeTextView.setText(planet.getPlanetType().getDisplayName());

        BaseColony colony = null;
        for(BaseColony c : mStar.getColonies()) {
            if (c.getPlanetIndex() == planet.getIndex()) {
                colony = c;
                break;
            }
        }

        final TextView colonyTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
        if (colony != null) {
            colonyTextView.setText("Colonized");
            EmpireManager.i.fetchEmpire(mContext, colony.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
                @Override
                public void onEmpireFetched(Empire empire) {
                    colonyTextView.setText(empire.getDisplayName());
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
