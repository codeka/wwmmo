package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.ModelManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;

/**
 * The \c StarfieldActivity is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldActivity extends Activity {
    Context mContext = this;
    StarfieldSurfaceView mStarfield;
    TextView mUsername;
    TextView mMoney;
    TextView mStarName;
    ViewGroup mLoadingContainer;
    ListView mPlanetList;
    PlanetListAdapter mPlanetListAdapter;

    Star mCurrentStar;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
    }

    @Override
    public void onResume() {
        super.onResume();

        setContentView(R.layout.starfield);

        mStarfield = (StarfieldSurfaceView) findViewById(R.id.starfield);
        mUsername = (TextView) findViewById(R.id.username);
        mMoney = (TextView) findViewById(R.id.money);
        mStarName = (TextView) findViewById(R.id.star_name);
        mLoadingContainer = (ViewGroup) findViewById(R.id.star_loading_container);
        mPlanetList = (ListView) findViewById(R.id.starfield_planet_list);

        mPlanetList.setVisibility(View.GONE);

        EmpireManager empireManager = EmpireManager.getInstance();
        mUsername.setText(empireManager.getEmpire().getDisplayName());
        mMoney.setText("$ 12,345"); // TODO: empire.getCash()
        mStarName.setText("");

        mPlanetListAdapter = new PlanetListAdapter();
        mPlanetList.setAdapter(mPlanetListAdapter);

        mStarfield.addStarSelectedListener(new StarfieldSurfaceView.OnStarSelectedListener() {
            @Override
            public void onStarSelected(Star star) {
                mStarName.setText(star.getName());

                // load the rest of the star's details as well
                mLoadingContainer.setVisibility(View.VISIBLE);
                mPlanetList.setVisibility(View.GONE);

                ModelManager.requestStar(star.getSector().getX(), star.getSector().getY(),
                        star.getKey(), new ModelManager.StarFetchedHandler() {
                    /**
                     * This is called on the main thread when the star is actually fetched.
                     */
                    @Override
                    public void onStarFetched(Star star) {
                        mLoadingContainer.setVisibility(View.GONE);
                        mPlanetList.setVisibility(View.VISIBLE);

                        mPlanetListAdapter.setStar(star);
                    }
                });
            }
        });

        mPlanetList. setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Star star = mStarfield.getSelectedStar();
                if (star == null) {
                    return; //??
                }

                Intent intent = new Intent(mContext, SolarSystemActivity.class);
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSector().getX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSector().getY());
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                intent.putExtra("au.com.codeka.warworlds.PlanetIndex", position);
                mContext.startActivity(intent);
            }
        });
    }

    class PlanetListAdapter extends BaseAdapter {
        private Star mStar;

        public void setStar(Star star) {
            mStar = star;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mStar == null) {
                return 0;
            }

            return mStar.getNumPlanets();
        }

        @Override
        public Object getItem(int position) {
            return mStar.getPlanets()[position];
        }

        @Override
        public long getItemId(int position) {
            return position; // TODO??
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

            ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
            Planet planet = mStar.getPlanets()[position];
            icon.setImageResource(planet.getPlanetType().getIconID());

            TextView planetTypeTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
            planetTypeTextView.setText(planet.getPlanetType().getDisplayName());

            Colony colony = null;
            for(Colony c : mStar.getColonies()) {
                if (c.getPlanetKey().equals(planet.getKey())) {
                    colony = c;
                    break;
                }
            }

            TextView colonyTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
            if (colony != null) {
                colonyTextView.setText("Colonized");
            } else {
                colonyTextView.setText("");
            }

            return view;
        }
    }
}
