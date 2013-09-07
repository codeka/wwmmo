package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.EmpireRanks;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

/**
 * This class manages rank histories. Since the history never changes, we can cache it
 * forever.
 */
public class RankHistoryManager {
    private static Logger log = LoggerFactory.getLogger(RankHistoryManager.class);
    public static RankHistoryManager i = new RankHistoryManager();

    public void getRankHistory(DateTime dt, RankHistoryFetchedHandler handler) {
        getRankHistory(dt.getYear(), dt.getMonthOfYear(), handler);
    }

    public void getRankHistory(final int year, final int month, final RankHistoryFetchedHandler handler) {
        new BackgroundRunner<EmpireRanks>() {
            @Override
            protected EmpireRanks doInBackground() {
                String url = "rankings/"+year+"/"+month;
                try {
                    return ApiClient.getProtoBuf(url, EmpireRanks.class);
                } catch(ApiException e) {
                    log.error("Error fetching rankings.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(EmpireRanks rankHistory) {
                if (rankHistory != null) {
                    handler.onRankHistoryFetched(rankHistory);
                }
            }
        }.execute();
    }

    public interface RankHistoryFetchedHandler {
        public void onRankHistoryFetched(EmpireRanks rankHistory);
    }
}
