package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

/**
 * This class manages rank histories. Since the history never changes, we can cache it
 * forever.
 */
public class RankHistoryManager {
    private static final Log log = new Log("RankHistoryManager");
    public static RankHistoryManager i = new RankHistoryManager();

    public void getRankHistory(DateTime dt, RankHistoryFetchedHandler handler) {
        getRankHistory(dt.getYear(), dt.getMonthOfYear(), handler);
    }

    public void getRankHistory(final int year, final int month, final RankHistoryFetchedHandler handler) {
        new BackgroundRunner<RankHistory>() {
            @Override
            protected RankHistory doInBackground() {
                String url = "rankings/"+year+"/"+month;
                try {
                    Messages.EmpireRanks pb = ApiClient.getProtoBuf(url, Messages.EmpireRanks.class);
                    RankHistory rankHistory = new RankHistory();
                    rankHistory.fromProtocolBuffer(pb);
                    return rankHistory;
                } catch(ApiException e) {
                    log.error("Error fetching rankings.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(RankHistory rankHistory) {
                if (rankHistory != null) {
                    handler.onRankHistoryFetched(rankHistory);
                }
            }
        }.execute();
    }

    public interface RankHistoryFetchedHandler {
        public void onRankHistoryFetched(RankHistory rankHistory);
    }
}
