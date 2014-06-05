package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

import com.google.protobuf.Message;

public class PurchaseController {
    private DataBase db;

    public PurchaseController() {
        db = new DataBase();
    }
    public PurchaseController(Transaction trans) {
        db = new DataBase(trans);
    }

    public void addPurchase(int empireID, Messages.PurchaseInfo purchaseInfo, Message purchaseExtra)
                throws RequestException{
        try {
            db.addPurchase(empireID, purchaseInfo, purchaseExtra.toByteArray());
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void addPurchase(int empireID, Messages.PurchaseInfo purchaseInfo,
                byte[] purchaseExtra) throws Exception {
            String sql = "INSERT INTO purchases (empire_id, sku, token, order_id, price," +
                                               " developer_payload, time, sku_extra)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                stmt.setString(2, purchaseInfo.getSku());
                stmt.setString(3, purchaseInfo.getToken());
                stmt.setString(4, purchaseInfo.getOrderId());
                stmt.setString(5, purchaseInfo.getPrice());
                stmt.setString(6, purchaseInfo.getDeveloperPayload());
                stmt.setDateTime(7, DateTime.now());
                stmt.setBytes(8, purchaseExtra);
                stmt.update();
            }
        }
    }
}
