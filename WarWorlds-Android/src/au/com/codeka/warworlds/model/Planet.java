package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import au.com.codeka.planetrender.Colour;
import au.com.codeka.planetrender.Image;
import au.com.codeka.planetrender.PlanetRenderer;
import au.com.codeka.planetrender.Template;
import au.com.codeka.planetrender.TemplateException;


public class Planet {
    private static Logger log = LoggerFactory.getLogger(Planet.class);

    private static PlanetType[] sPlanetTypes = {
        new PlanetType.Builder().setDisplayName("Gas Giant")
                                .setBitmapBasePath("planets/gasgiant")
                                .build(),
        new PlanetType.Builder().setDisplayName("Radiated")
                                .setBitmapBasePath("planets/radiated")
                                .build(),
        new PlanetType.Builder().setDisplayName("Inferno")
                                .setBitmapBasePath("planets/inferno")
                                .build(),
        new PlanetType.Builder().setDisplayName("Asteroids")
                                .setBitmapBasePath("planets/asteroids")
                                .build(),
        new PlanetType.Builder().setDisplayName("Water")
                                .setBitmapBasePath("planets/water")
                                .build(),
        new PlanetType.Builder().setDisplayName("Toxic")
                                .setBitmapBasePath("planets/toxic")
                                .build(),
        new PlanetType.Builder().setDisplayName("Desert")
                                .setBitmapBasePath("planets/desert")
                                .build(),
        new PlanetType.Builder().setDisplayName("Swamp")
                                .setBitmapBasePath("planets/swamp")
                                .build(),
        new PlanetType.Builder().setDisplayName("Terran")
                                .setBitmapBasePath("planets/terran")
                                .build()
    };

    private Star mStar;
    private String mKey;
    private int mIndex;
    private PlanetType mPlanetType;
    private int mSize;
    private int mPopulationCongeniality;
    private int mFarmingCongeniality;
    private int mMiningCongeniality;
    private Bitmap mBitmap;

    public Star getStar() {
        return mStar;
    }
    public String getKey() {
        return mKey;
    }
    public int getIndex() {
        return mIndex;
    }
    public PlanetType getPlanetType() {
        return mPlanetType;
    }
    public int getSize() {
        return mSize;
    }
    public int getPopulationCongeniality() {
        return mPopulationCongeniality;
    }
    public int getFarmingCongeniality() {
        return mFarmingCongeniality;
    }
    public int getMiningCongeniality() {
        return mMiningCongeniality;
    }
    public Bitmap getBitmap(AssetManager assetManager) {
        if (mBitmap == null) {
            // TODO: do this in a thread!!
            mBitmap = generateBitmap(assetManager);
        }
        return mBitmap;
    }

    private Bitmap generateBitmap(AssetManager assetManager) {
        String basePath = mPlanetType.getBitmapBasePath();

        // TODO: better seed
        long seed = mKey.hashCode();
        Random rand = new Random(seed);

        String[] fileNames = null;
        try {
            fileNames = assetManager.list(basePath);
        } catch(IOException e) {
            return null; // should never happen!
        }

        String fullPath = basePath + "/";
        if (fileNames.length == 0) {
            return null;
        } else if (fileNames.length == 1) {
            fullPath += fileNames[0];
        } else {
            fullPath += fileNames[rand.nextInt(fileNames.length - 1)];
        }
        Template tmpl = null;
        InputStream ins = null;
        try {
            log.info("generating planet "+fullPath+"...");
            ins = assetManager.open(fullPath);
            tmpl = Template.parse(ins);
        } catch (IOException e) {
            log.error("Error loading planet definition: "+fullPath, e);
        } catch (TemplateException e) {
            log.error("Error parsing planet definition: "+fullPath, e);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }

        if (tmpl != null) {
            PlanetRenderer renderer = new PlanetRenderer((Template.PlanetTemplate) tmpl.getTemplate(), rand);

            Image img = new Image(128, 128, Colour.TRANSPARENT);
            renderer.render(img);

            return Bitmap.createBitmap(img.getArgb(), 128, 128, Config.ARGB_8888);
        }

        return null;
    }

    /**
     * Converts the given Planet protocol buffer into a \c Planet.
     */
    public static Planet fromProtocolBuffer(Star star, warworlds.Warworlds.Planet pb) {
        Planet p = new Planet();
        p.mStar = star;
        p.mKey = pb.getKey();
        p.mIndex = pb.getIndex();
        p.mPlanetType = sPlanetTypes[pb.getPlanetType().getNumber() - 1];
        p.mSize = pb.getSize();
        if (pb.hasPopulationCongeniality()) {
            p.mPopulationCongeniality = pb.getPopulationCongeniality();
        }
        if (pb.hasFarmingCongeniality()) {
            p.mFarmingCongeniality = pb.getFarmingCongeniality();
        }
        if (pb.hasMiningCongeniality()) {
            p.mMiningCongeniality = pb.getMiningCongeniality();
        }

        return p;
    }

    public static class PlanetType {
        private String mDisplayName;
        private String mBitmapBasePath;

        public String getDisplayName() {
            return mDisplayName;
        }
        public String getBitmapBasePath() {
            return mBitmapBasePath;
        }

        public static class Builder {
            private PlanetType mPlanetType;

            public Builder() {
                mPlanetType = new PlanetType();
            }

            public Builder setDisplayName(String displayName) {
                mPlanetType.mDisplayName = displayName;
                return this;
            }

            public Builder setBitmapBasePath(String path) {
                mPlanetType.mBitmapBasePath = path;
                return this;
            }

            public PlanetType build() {
                return mPlanetType;
            }
        }
    }
}
