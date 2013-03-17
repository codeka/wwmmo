package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;


public class Empire implements Parcelable {
    private String mKey;
    private String mDisplayName;
    private Bitmap mEmpireShield;
    protected float mCash;
    private EmpireRank mRank;
    private StarSummary mHomeStar;
    private Alliance mAlliance;

    private static Bitmap sBaseShield;

    public String getKey() {
        return mKey;
    }
    public String getDisplayName() {
        return mDisplayName;
    }
    public float getCash() {
        return mCash;
    }
    public EmpireRank getRank() {
        return mRank;
    }
    public StarSummary getHomeStar() {
        return mHomeStar;
    }
    public Alliance getAlliance() {
        return mAlliance;
    }

    /**
     * Gets (or creates, if there isn't one) the \c Bitmap that represents this Empire's
     * shield (i.e. their icon).
     */
    public Bitmap getShield(Context context) {
        if (mEmpireShield == null) {
            if (sBaseShield == null) {
                AssetManager assetManager = context.getAssets();
                InputStream ins;
                try {
                    ins = assetManager.open("img/shield.png");
                } catch (IOException e) {
                    // should never happen!
                    return null;
                }

                try {
                    sBaseShield = BitmapFactory.decodeStream(ins);
                } finally {
                    try {
                        ins.close();
                    } catch (IOException e) {
                    }
                }
            }

            int width = sBaseShield.getWidth();
            int height = sBaseShield.getHeight();
            int[] pixels = new int[width * height];
            sBaseShield.getPixels(pixels, 0, width, 0, 0, width, height);

            int newColour = getShieldColor();
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] == Color.MAGENTA) {
                    pixels[i] = newColour;
                }
            }

            mEmpireShield = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        }

        return mEmpireShield;
    }

    public int getShieldColor() {
        Random rand = new Random(mKey.hashCode());
        return Color.rgb(rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mDisplayName);
        parcel.writeParcelable(mHomeStar, flags);
        parcel.writeParcelable(mAlliance, flags);
    }

    protected void readFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mDisplayName = parcel.readString();
        mHomeStar = (StarSummary) parcel.readParcelable(StarSummary.class.getClassLoader());
        mAlliance = (Alliance) parcel.readParcelable(Alliance.class.getClassLoader());
    }

    public static final Parcelable.Creator<Empire> CREATOR
                = new Parcelable.Creator<Empire>() {
        @Override
        public Empire createFromParcel(Parcel parcel) {
            Empire e = new Empire();
            e.readFromParcel(parcel);
            return e;
        }

        @Override
        public Empire[] newArray(int size) {
            return new Empire[size];
        }
    };

    public static Empire fromProtocolBuffer(Messages.Empire pb) {
        if (!pb.hasKey() || pb.getKey() == null) {
            return new NativeEmpire();
        }

        Empire empire = new Empire();
        empire.populateFromProtocolBuffer(pb);
        return empire;
    }

    protected void populateFromProtocolBuffer(Messages.Empire pb) {
        mKey = pb.getKey();
        mDisplayName = pb.getDisplayName();
        mCash = pb.getCash();

        if (pb.getRank() != null && pb.getRank().getEmpireKey() != null &&
                pb.getRank().getEmpireKey().length() > 0) {
            mRank = EmpireRank.fromProtocolBuffer(pb.getRank());
        }

        if (pb.getHomeStar() != null && pb.getHomeStar().getKey() != null &&
                 pb.getHomeStar().getKey().length() > 0) {
            mHomeStar = StarSummary.fromProtocolBuffer(pb.getHomeStar());
        }

        if (pb.getAlliance() != null && pb.getAlliance().getKey() != null &&
                pb.getAlliance().getKey().length() > 0) {
            mAlliance = Alliance.fromProtocolBuffer(pb.getAlliance());
        }
    }
}
