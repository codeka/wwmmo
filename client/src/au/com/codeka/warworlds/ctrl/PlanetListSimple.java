package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.EnemyEmpireActivity;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;

/**
 * The planet list that shows up on the starfield view. It also includes details about empires
 * around the star.
 */
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
                    if (v.getTag() instanceof String) {
                        String empireKey = (String) v.getTag();
                        Intent intent = new Intent(mContext, EnemyEmpireActivity.class);
                        intent.putExtra("au.com.codeka.warworlds.EmpireKey", empireKey);
                        mContext.startActivity(intent);
                    } else {
                        Planet planet = (Planet) v.getTag();
                        if (mPlanetSelectedHandler != null) {
                            mPlanetSelectedHandler.onPlanetSelected(planet);
                        }
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

        MyEmpire myEmpire = EmpireManager.i.getEmpire();

        HashSet<String> empires = new HashSet<String>();
        for (BaseFleet fleet : mStar.getFleets()) {
            if (fleet.getEmpireKey() == null) {
                continue;
            }
            if (fleet.getEmpireKey().equals(myEmpire.getKey())) {
                continue;
            }
            if (empires.contains(fleet.getEmpireKey())) {
                continue;
            }
            empires.add(fleet.getEmpireKey());
        }
        for (BaseColony colony : mStar.getColonies()) {
            if (colony.getEmpireKey() == null) {
                continue;
            }
            if (colony.getEmpireKey().equals(myEmpire.getKey())) {
                continue;
            }
            if (empires.contains(colony.getEmpireKey())) {
                continue;
            }
            empires.add(colony.getEmpireKey());
        }
        for (String empireKey : empires) {
            View rowView = getEmpireRowView(inflater, empireKey);
            addView(rowView);
        }
        if (!empires.isEmpty()) {
            // add a spacer...
            View spacer = new View(mContext);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(10, 10));
            addView(spacer);
            spacer = new View(mContext);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            spacer.setBackgroundColor(0x33ffffff);
            addView(spacer);
        }
        for (Planet planet : mPlanets) {
            View rowView = getPlanetRowView(inflater, planet);
            addView(rowView);
        }
    }

    private View getPlanetRowView(LayoutInflater inflater, Planet planet) {
        View view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);

        final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
        final PlanetImageManager pim = PlanetImageManager.getInstance();

        Sprite sprite = pim.getSprite(planet);
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
            EmpireManager.i.fetchEmpire(colony.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
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

    private View getEmpireRowView(LayoutInflater inflater, String empireKey) {
        final View view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);
        final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
        final TextView empireName = (TextView) view.findViewById(R.id.starfield_planet_type);
        final TextView allianceName = (TextView) view.findViewById(R.id.starfield_planet_colony);

        EmpireManager.i.fetchEmpire(empireKey, new EmpireManager.EmpireFetchedHandler() {
            @Override
            public void onEmpireFetched(Empire empire) {
                Bitmap bmp = EmpireShieldManager.i.getShield(mContext, empire);
                icon.setImageBitmap(bmp);

                empireName.setText(empire.getDisplayName());
                if (empire.getAlliance() != null) {
                    allianceName.setText(empire.getAlliance().getName());
                }
            }
        });

        view.setOnClickListener(mOnClickListener);
        view.setTag(empireKey);
        return view;
    }

    public interface PlanetSelectedHandler {
        void onPlanetSelected(Planet planet);
    }
}
