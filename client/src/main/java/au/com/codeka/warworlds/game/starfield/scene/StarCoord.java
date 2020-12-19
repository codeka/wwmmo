package au.com.codeka.warworlds.game.starfield.scene;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.game.starfield.StarfieldFragment;

/**
 * A {@link Parcelable} type that can be used to pass the coordinates of a star to the
 * {@link StarfieldFragment}.
 */
public class StarCoord implements Parcelable {
  private final long sectorX;
  private final long sectorY;
  private final int offsetX;
  private final int offsetY;
  private final long starID;

  public static StarCoord from(BaseStar star) {
    return new StarCoord(
        star.getSectorX(), star.getSectorY(), star.getOffsetX(), star.getOffsetY(),
        Integer.parseInt(star.getKey()));
  }

  public long getSectorX() {
    return sectorX;
  }

  public long getSectorY() {
    return sectorY;
  }

  public int getOffsetX() {
    return offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public long getStarID() {
    return starID;
  }

  private StarCoord(long sectorX, long sectorY, int offsetX, int offsetY, long starID) {
    this.sectorX = sectorX;
    this.sectorY = sectorY;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.starID = starID;
  }

  private StarCoord(Parcel in) {
    sectorX = in.readLong();
    sectorY = in.readLong();
    offsetX = in.readInt();
    offsetY = in.readInt();
    starID = in.readLong();
  }

  public static final Creator<StarCoord> CREATOR = new Creator<StarCoord>() {
    @Override
    public StarCoord createFromParcel(Parcel in) {
      return new StarCoord(in);
    }

    @Override
    public StarCoord[] newArray(int size) {
      return new StarCoord[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(sectorX);
    dest.writeLong(sectorY);
    dest.writeInt(offsetX);
    dest.writeInt(offsetY);
    dest.writeLong(starID);
  }
}
