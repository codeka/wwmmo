package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;

public class PlanetDetailsView extends FrameLayout {
    private Context mContext;
    private View mView;
    private Star mStar;
    private Planet mPlanet;
    private boolean mIsAttachedToWindow;

    @SuppressWarnings("unused") // for now...
    private Colony mColony;

    public PlanetDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mView = inflate(context, R.layout.planet_details_ctrl, null);
        this.addView(mView);
    }

    public void setup(Star star, Planet planet, Colony colony) {
        mStar = star;
        mPlanet = planet;
        mColony = colony;

        if (mIsAttachedToWindow) {
            refresh();
        }
    }

    @Override
    public void onAttachedToWindow() {
        if (isInEditMode()) {
            return;
        }
        mIsAttachedToWindow = true;

        if (mStar == null || mPlanet == null) {
            return;
        }

        PlanetImageManager.getInstance().addBitmapGeneratedListener(mPlanetBitmapGeneratedListener);
        refresh();
    }

    @Override
    public void onDetachedFromWindow() {
        mIsAttachedToWindow = false;

        PlanetImageManager.getInstance().removeBitmapGeneratedListener(mPlanetBitmapGeneratedListener);
    }

    private void refresh() {
        ImageView planetIcon = (ImageView) mView.findViewById(R.id.planet_icon);
        ProgressBar populationCongenialityProgressBar = (ProgressBar) mView.findViewById(R.id.population_congeniality);
        ProgressBar farmingCongenialityProgressBar = (ProgressBar) mView.findViewById(R.id.farming_congeniality);
        ProgressBar miningCongenialityProgressBar = (ProgressBar) mView.findViewById(R.id.mining_congeniality);
        TextView populationCongenialityTextView = (TextView) mView.findViewById(R.id.population_congeniality_value);
        TextView farmingCongenialityTextView = (TextView) mView.findViewById(R.id.farming_congeniality_value);
        TextView miningCongenialityTextView = (TextView) mView.findViewById(R.id.mining_congeniality_value);

        Sprite planetSprite = PlanetImageManager.getInstance().getSprite(mContext, mPlanet);
        if (planetSprite != null) {
            planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));
        }

        populationCongenialityProgressBar.setMax(1000);
        populationCongenialityProgressBar.setProgress(mPlanet.getPopulationCongeniality());
        populationCongenialityTextView.setText(Integer.toString(mPlanet.getPopulationCongeniality()));
        farmingCongenialityProgressBar.setMax(100);
        farmingCongenialityProgressBar.setProgress(mPlanet.getFarmingCongeniality());
        farmingCongenialityTextView.setText(Integer.toString(mPlanet.getFarmingCongeniality()));
        miningCongenialityProgressBar.setMax(100);
        miningCongenialityProgressBar.setProgress(mPlanet.getMiningCongeniality());
        miningCongenialityTextView.setText(Integer.toString(mPlanet.getMiningCongeniality()));
    }

    private ImageManager.BitmapGeneratedListener mPlanetBitmapGeneratedListener = new ImageManager.BitmapGeneratedListener() {
        @Override
        public void onBitmapGenerated(String key, Bitmap bmp) {
            refresh();
        }
    };
}
