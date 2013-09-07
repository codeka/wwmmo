package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import android.content.Context;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.design.Design;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.Building;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.EmpireBuildingStatistics;
import au.com.codeka.common.model.SituationReport;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class BuildManager {
    private static BuildManager sInstance = new BuildManager();
    public static BuildManager getInstance() {
        return sInstance;
    }

    private TreeMap<String, Integer> mBuildingDesignCounts;
    private ArrayList<BuildRequest> mBuildRequests;

    private BuildManager() {
        mBuildingDesignCounts = new TreeMap<String, Integer>();
        mBuildRequests = new ArrayList<BuildRequest>();
    }

    public void setup(EmpireBuildingStatistics empire_building_statistics_pb,
                      List<BuildRequest> buildRequests) {
        mBuildingDesignCounts.clear();
        for (EmpireBuildingStatistics.DesignCount design_count_pb : empire_building_statistics_pb.counts) {
            mBuildingDesignCounts.put(design_count_pb.design_id, design_count_pb.num_buildings);
        }

        mBuildRequests = new ArrayList<BuildRequest>(buildRequests);
    }

    public int getTotalBuildingsInEmpire(String designId) {
        if (mBuildingDesignCounts.get(designId) == null) {
            return 0;
        } else {
            return mBuildingDesignCounts.get(designId);
        }
    }

    public List<BuildRequest> getBuildRequests() {
        return mBuildRequests;
    }

    /**
     * Called when we get a situation report from the server. If it's a build complete, say, we'll
     * need to update our cache of things building...
     */
    public void notifySituationReport(SituationReport sitrep) {
        if (sitrep.build_complete_record != null &&
                sitrep.build_complete_record.build_request_key != null) {
            String buildRequestKey = sitrep.build_complete_record.build_request_key;
            for (int i = 0; i < mBuildRequests.size(); i++) {
                if (mBuildRequests.get(i).key.equals(buildRequestKey)) {
                    mBuildRequests.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * Called when a star is refreshed. We may need to update the build requests on that star.
     */
    public void onStarUpdate(Star star) {
        ArrayList<BuildRequest> newBuildRequests = new ArrayList<BuildRequest>();
        for (BuildRequest br : mBuildRequests) {
            if (br.star_key.equals(star.key)) {
                continue;
            }
            newBuildRequests.add(br);
        }
        if (star.build_requests != null) for (BuildRequest br : star.build_requests) {
            newBuildRequests.add(br);
        }
        mBuildRequests = newBuildRequests;
    }

    public void build(final Context context, final Colony colony,
                      final Design design, final Building existingBuilding, final int count) {
        new BackgroundRunner<BuildRequest>() {
            private int mErrorCode;
            private String mErrorMsg;

            @Override
            protected BuildRequest doInBackground() {
                BuildRequest.BUILD_KIND kind;
                if (design.getDesignKind() == DesignKind.BUILDING) {
                    kind = BuildRequest.BUILD_KIND.BUILDING;
                } else {
                    kind = BuildRequest.BUILD_KIND.SHIP;
                }

                BuildRequest build = new BuildRequest.Builder()
                        .build_kind(kind)
                        .star_key(colony.star_key)
                        .colony_key(colony.key)
                        .empire_key(colony.empire_key)
                        .design_id(design.getID())
                        .count(count)
                        .existing_building_key(existingBuilding == null ? "" : existingBuilding.key)
                        .build();
                try {
                    return ApiClient.postProtoBuf("buildqueue", build, BuildRequest.class);
                } catch (ApiException e) {
                    if (e.getServerErrorCode() > 0) {
                        mErrorCode = e.getServerErrorCode();
                        mErrorMsg = e.getServerErrorMessage();
                    }
                }

                return null;
            }
            @Override
            protected void onComplete(BuildRequest buildRequest) {
                if (mErrorCode > 0) {
                    try {
                        new StyledDialog.Builder(context)
                                        .setTitle("Cannot Build")
                                        .setMessage(mErrorMsg)
                                        .setPositiveButton("Close", true, null)
                                        .create().show();
                    } catch(Exception e) {
                        // we can get a WindowManager.BadTokenException here if the activity has
                        // finished, we should probably do something about it but it's kinda too
                        // late...
                    }
                } else if (buildRequest != null) {
                    mBuildRequests.add(buildRequest);

                    StarManager.i.refreshStar(colony.star_key);
                }
            }
        }.execute();
    }
}
