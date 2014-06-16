package au.com.codeka.warworlds.game.starfield;

import java.util.HashMap;
import java.util.Map;

import org.andengine.entity.scene.Scene;

import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.Star;

public class StarfieldScene extends Scene {
    private StarfieldSceneManager mStarfield;
    private SelectableEntity mSelectingEntity;
    private SelectionIndicatorEntity mSelectionIndicator;
    private RadarIndicatorEntity mRadarIndicator;

    private Map<String, StarEntity> mStars;
    private Map<String, FleetEntity> mFleets;
    private StarEntity mSelectedStarEntity;
    private FleetEntity mSelectedFleetEntity;

    private String mStarToSelect;

    public StarfieldScene(SectorSceneManager sectorSceneManager) {
        mStarfield = (StarfieldSceneManager) sectorSceneManager;
        mSelectionIndicator = new SelectionIndicatorEntity(
                sectorSceneManager.getActivity().getEngine(),
                sectorSceneManager.getActivity().getVertexBufferObjectManager());
        mRadarIndicator = new RadarIndicatorEntity(
                sectorSceneManager.getActivity().getVertexBufferObjectManager());

        mFleets = new HashMap<String, FleetEntity>();
        mStars = new HashMap<String, StarEntity>();

    }

    public void attachChild(StarEntity starEntity) {
        super.attachChild(starEntity);
        mStars.put(starEntity.getStar().getKey(), starEntity);
    }

    public void attachChild(FleetEntity fleetEntity) {
        super.attachChild(fleetEntity);
        mFleets.put(fleetEntity.getFleet().getKey(), fleetEntity);
    }

    /** Makes sure whatever was selected in the given scene is also selected in this scene. */
    public void copySelection(StarfieldScene scene) {
        if (scene.mSelectedStarEntity != null) {
            selectStar(mStars.get(scene.mSelectedStarEntity.getStar().getKey()));
        }
        if (scene.mSelectedFleetEntity != null) {
            selectFleet(mFleets.get(scene.mSelectedFleetEntity.getFleet().getKey()));
        }
        if (mStarToSelect != null) {
            selectStar(mStarToSelect);
        }
    }

    public Map<String, StarEntity> getStars() {
        return mStars;
    }

    public Map<String, FleetEntity> getFleets() {
        return mFleets;
    }

    public void cancelSelect() {
        mSelectingEntity = null;
    }

    public SelectableEntity getSelectingEntity() {
        return mSelectingEntity;
    }

    /** Sets the sprite that we've tapped down on, but not yet tapped up on. */
    public void setSelectingEntity(SelectableEntity entity) {
        mSelectingEntity = entity;
    }

    public void onStarFetched(Star s) {
        // if it's the selected star, we'll want to update the selection
        if (s != null && mSelectedStarEntity != null &&
                s.getKey().equals(mSelectedStarEntity.getStar().getKey())) {
            mSelectedStarEntity.setStar(s);
            refreshSelectionIndicator();
        }
    }

    public void selectStar(final StarEntity selectedStarEntity) {
        mStarfield.getActivity().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                mSelectedStarEntity = selectedStarEntity;
                mSelectedFleetEntity = null;

                refreshSelectionIndicator();
                if (mSelectedStarEntity == null) {
                    mStarfield.fireSelectionChanged((Star) null);
                } else {
                    mStarfield.fireSelectionChanged(mSelectedStarEntity.getStar());
                }
            }
        });
    }

    public void selectStar(String starKey) {
        if (mStars == null) {
            // this can happen if we haven't refreshed the scene yet.
            mStarToSelect = starKey;
            return;
        }

        if (starKey == null) {
            selectStar((StarEntity) null);
            return;
        }

        if (!mStars.containsKey(starKey)) {
            mStarToSelect = starKey;
            return;
        }

        selectStar(mStars.get(starKey));
    }

    public void selectFleet(final FleetEntity fleet) {
        mStarfield.getActivity().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                mSelectedStarEntity = null;
                mSelectedFleetEntity = fleet;

                refreshSelectionIndicator();
                mStarfield.fireSelectionChanged(
                        mSelectedFleetEntity == null ? null : mSelectedFleetEntity.getFleet());
            }
        });
    }

    public void selectFleet(String fleetKey) {
        if (fleetKey == null) {
            selectFleet((FleetEntity) null);
            return;
        }

        if (mFleets == null) {
            // TODO: handle this better
            return;
        }

        selectFleet(mFleets.get(fleetKey));
    }

    /** Deselects the fleet or star you currently have selected. */
    public void selectNothing(final long sectorX, final long sectorY, final int offsetX,
            final int offsetY) {
        mStarfield.getActivity().runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                if (mSelectedStarEntity != null) {
                    mSelectedStarEntity = null;
                    refreshSelectionIndicator();
                    mStarfield.fireSelectionChanged((Star) null);
                }

                if (mSelectedFleetEntity != null) {
                    mSelectedFleetEntity = null;
                    refreshSelectionIndicator();
                    mStarfield.fireSelectionChanged((Fleet) null);
                }

                mStarfield.fireSpaceTapListener(sectorX, sectorY, offsetX, offsetY);
            }
        });
    }

    public void refreshSelectionIndicator() {
        if (mSelectionIndicator.getParent() != null) {
            mSelectionIndicator.getParent().detachChild(mSelectionIndicator);
        }
        if (mRadarIndicator.getParent() != null) {
            mRadarIndicator.getParent().detachChild(mRadarIndicator);
        }

        if (mSelectedStarEntity != null) {
            mSelectionIndicator.setSelectedEntity(mSelectedStarEntity);
            mSelectedStarEntity.attachChild(mSelectionIndicator);

            // if the selected star has a radar, pick the one with the biggest radius to display
            float radarRadius = mSelectedStarEntity.getStar().getRadarRange(EmpireManager.i.getEmpire().getKey());
            if (radarRadius > 0.0f) {
                mSelectedStarEntity.attachChild(mRadarIndicator);
                mRadarIndicator.setScale(radarRadius * Sector.PIXELS_PER_PARSEC * 2.0f);
            }
        }
        if (mSelectedFleetEntity != null) {
            mSelectionIndicator.setSelectedEntity(mSelectedFleetEntity);
            mSelectedFleetEntity.attachChild(mSelectionIndicator);
        }
    }

}
