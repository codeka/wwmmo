package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.GameHistory;

public class GameHistoryController {
  private DataBase db;

  // We can cache this here because it almost never changes.
  private static GameHistory currentHistory;

  public GameHistoryController() {
    db = new DataBase();
  }

  public GameHistoryController(Transaction trans) {
    db = new DataBase(trans);
  }

  /**
   * Gets the current game history, or null if there's no game currently running (e.g. we could be
   * in the middle of a reset).
   *
   * @return The current {@link GameHistory}, or null if there is no current game.
   */
  @Nullable
  public GameHistory getCurrent() throws RequestException {
    if (currentHistory == null) {
      try {
        currentHistory = db.getCurrent();
      } catch (Exception e) {
        throw new RequestException(e);
      }
    }
    return currentHistory;
  }

  public GameHistory markResetting() throws RequestException {
    GameHistory current = getCurrent();
    if (current == null) {
      throw new RequestException(500, "Cannot mark resetting, no current game!");
    }

    try {
      db.markResetting(current);
      currentHistory = null;
    } catch (Exception e) {
      throw new RequestException(e);
    }

    return current;
  }

  public void startNewGame() throws RequestException {
    GameHistory current = getCurrent();
    if (current != null) {
      throw new RequestException(500, "Cannot start new game, one is already in progress.");
    }

    try {
      db.startNewGame();
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  private static class DataBase extends BaseDataBase {
    DataBase() {
      super();
    }

    DataBase(Transaction trans) {
      super(trans);
    }

    @Nullable
    GameHistory getCurrent() throws Exception {
      String sql = "SELECT * FROM game_history WHERE state = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, GameHistory.State.NORMAL.getValue());
        SqlResult res = stmt.select();

        if (res.next()) {
          return new GameHistory(res);
        }
      }

      return null;
    }

    void markResetting(GameHistory game) throws Exception {
      String sql = "UPDATE game_history SET state = ?, date_finished = ? WHERE id = ? AND state = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, GameHistory.State.RESETTING.getValue());
        stmt.setDateTime(2, DateTime.now());
        stmt.setLong(3, game.getId());
        stmt.setInt(4, game.getState().getValue());
        if (stmt.update() != 1) {
          throw new RequestException(500, "No game to reset.");
        }
      }
    }

    void startNewGame() throws Exception {
      String sql = "UPDATE game_history SET state = ? WHERE state = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, GameHistory.State.FINISHED.getValue());
        stmt.setInt(2, GameHistory.State.RESETTING.getValue());
        if (stmt.update() != 1) {
          throw new RequestException(500, "No game is resetting.");
        }
      }

      sql = "INSERT INTO game_history (date_created, state) VALUES (?, ?)";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setDateTime(1, DateTime.now());
        stmt.setInt(2, GameHistory.State.NORMAL.getValue());
        stmt.update();
      }
    }
  }
}
