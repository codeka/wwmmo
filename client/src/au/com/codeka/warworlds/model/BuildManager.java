package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import android.content.Context;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;

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

    public void setup(Messages.EmpireBuildingStatistics empire_building_statistics_pb,
                      List<Messages.BuildRequest> build_request_pbs) {
        mBuildingDesignCounts.clear();
        for (Messages.EmpireBuildingStatistics.DesignCount design_count_pb : empire_building_statistics_pb.getCountsList()) {
            mBuildingDesignCounts.put(design_count_pb.getDesignId(), design_count_pb.getNumBuildings());
        }

        mBuildRequests.clear();
        for (Messages.BuildRequest build_request_pb : build_request_pbs) {
            BuildRequest br = new BuildRequest();
            br.fromProtocolBuffer(build_request_pb);
            mBuildRequests.add(br);
        }
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
    public void notifySituationReport(Messages.SituationReport sitrep) {
        if (sitrep.getBuildCompleteRecord() != null &&
                sitrep.getBuildCompleteRecord().getBuildRequestKey() != null &&
                sitrep.getBuildCompleteRecord().getBuildRequestKey().length() > 0) {
            String buildRequestKey = sitrep.getBuildCompleteRecord().getBuildRequestKey();
            for (int i = 0; i < mBuildRequests.size(); i++) {
                if (mBuildRequests.get(i).getKey().equals(buildRequestKey)) {
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
            if (br.getStarKey().equals(star.getKey())) {
                continue;
            }
            newBuildRequests.add(br);
        }
        for (BaseBuildRequest br : star.getBuildRequests()) {
            newBuildRequests.add((BuildRequest) br);
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
                Messages.BuildRequest.BUILD_KIND kind;
                if (design.getDesignKind() == DesignKind.BUILDING) {
                    kind = Messages.BuildRequest.BUILD_KIND.BUILDING;
                } else {
                    kind = Messages.BuildRequest.BUILD_KIND.SHIP;
                }

                Messages.BuildRequest build = Messages.BuildRequest.newBuilder()
                        .setBuildKind(kind)
                        .setStarKey(colony.getStarKey())
                        .setColonyKey(colony.getKey())
                        .setEmpireKey(colony.getEmpireKey())
                        .setDesignName(design.getID())
                        .setCount(count)
                        .setExistingBuildingKey(existingBuilding == null ? "" : existingBuilding.getKey())
                        .build();
                try {
                    build = ApiClient.postProtoBuf("buildqueue", build, Messages.BuildRequest.class);

                    BuildRequest br = new BuildRequest();
                    br.fromProtocolBuffer(build);
                    return br;
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

                    StarManager.getInstance().refreshStar(context, colony.getStarKey());
                }
            }
        }.execute();
    }
}
