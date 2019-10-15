package au.com.codeka.warworlds.server.model;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.sql.SQLException;

import au.com.codeka.warworlds.server.data.SqlResult;

public class GameHistory {
  private long id;
  private DateTime created;
  private DateTime finished;
  private State state;

  public GameHistory() {
  }

  public GameHistory(SqlResult res) throws SQLException {
    id = res.getLong("id");
    created = res.getDateTime("date_created");
    finished = res.getDateTime("date_finished");
    state = State.fromValue(res.getInt("state"));
  }

  public long getId() {
    return id;
  }

  public DateTime getCreated() {
    return created;
  }

  public State getState() {
    return state;
  }

  public enum State {
    NORMAL(0),
    FINISHED(1),
    RESETTING(2);

    private int value;

    State(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    public static State fromValue(int value) {
      for (State state : values()) {
        if (state.value == value) {
          return state;
        }
      }

      return State.NORMAL;
    }
  }
}
