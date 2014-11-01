package au.com.codeka.warworlds.server.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;

/** Ensures the database schema is working and up-to-date. */
public class SchemaUpdater {
  private static final Log log = new Log("SchemaUpdater");

  /** Verifies the schema is up-to-date, and runs update scripts of it's not. */
  public void verifySchema() throws SchemaException {
    int databaseVersion = getDatabaseVersion();
    int expectedVersion = getExpectedVersion();
    log.debug("databaseVersion=%d, expectedVersion=%d", databaseVersion, expectedVersion);
    for (int version = databaseVersion + 1; version <= expectedVersion; version++) {
      applyVersionUpgrade(version);
    }
    log.info("Schema is up-to-date at version %d.", expectedVersion);
  }

  private void applyVersionUpgrade(int version) throws SchemaException {
    String fileName = new File(Configuration.i.getDataDirectory(),
        String.format("schema/schema-%03d.sql", version)).getAbsolutePath();
    ScriptReader reader = null;
    try {
      reader = new ScriptReader(new BufferedReader(new FileReader(fileName)));
      String sql;
      while ((sql = reader.readStatement()) != null) {
        try (SqlStmt stmt = DB.prepare(sql)) {
          stmt.update();
        }
      }
    } catch(Exception e) {
      throw new SchemaException("Error upgrading to schema version " + version, e);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }

    try (SqlStmt stmt = DB.prepare("UPDATE schema_version SET version = ?")) {
      stmt.setInt(1, version);
      stmt.update();
    } catch (Exception e) {
      throw new SchemaException("Error upgrading to schema version " + version, e);
    }
  }

  /** Work out the version the database is currently at. */
  private int getDatabaseVersion() throws SchemaException {
    try (SqlStmt stmt = DB.prepare("SELECT version FROM schema_version")) {
      return (int) stmt.selectFirstValue(Integer.class);
    } catch (SQLException e) {
      if (SqlStateTranslater.translate(e) == SqlStateTranslater.ErrorCode.TableDoesNotExist) {
        // no schema_version table, could mean they're version 0 or 1 schema, keep looking
      } else {
        throw new SchemaException("SQL exception: " + e.getSQLState(), e);
      }
    } catch (Exception e) {
      throw new SchemaException("Unknown exception.", e);
    }

    try (SqlStmt stmt = DB.prepare("SELECT id FROM stars")) {
      stmt.selectFirstValue(Long.class);
      // If that succeed, it means we're at version 1 (initial schema already exists).
      return 1;
    } catch (SQLException e) {
      if (SqlStateTranslater.translate(e) == SqlStateTranslater.ErrorCode.TableDoesNotExist) {
        // No stars table means we're at schema version 0, and need to create from scratch.
        return 0;
      } else {
        throw new SchemaException("SQL exception: " + e.getSQLState(), e);
      }
    } catch (Exception e) {
      throw new SchemaException("Unknown exception.", e);
    }
  }

  /** Gets the version we currently except the database to be at. */
  private int getExpectedVersion() {
    int maxVersion = 0;
    Pattern pattern = Pattern.compile("schema-([0-9]+)\\.sql");
    File folder = new File(Configuration.i.getDataDirectory(), "schema");
    log.debug("Scanning '%s' for schema files.", folder);
    for (File file : folder.listFiles()) {
      Matcher matcher = pattern.matcher(file.getName());
      if (file.isFile() && matcher.matches()) {
        int version = Integer.parseInt(matcher.group(1));
        log.debug("Found schema file '%s', version: %d", file.getName(), version);
        if (version > maxVersion) {
          maxVersion = version;
        }
      }
    }
    return maxVersion;
  }

  /**
   * Helper class for parsing SQL scripts into individual statements.
   * <p>
   * We read lines from the script file one by one. Statements end when we find a semi-colon. We
   * also support $-quoted strings (of the form "$xxx$...$xxx$") which are required to support
   * reading function definitions from scripts.
   */
  private static class ScriptReader {
    private final BufferedReader reader;

    /** If non-null, the remainder of the previously-read line (after the semi-colon) */
    @Nullable private String remainder;

    public ScriptReader(BufferedReader reader) {
      this.reader = reader;
    }

    /** Reads the next statement from the script, returns {@code null} if there's no more. */
    public String readStatement() throws IOException {
      StringBuilder sb = new StringBuilder();
      if (remainder != null) {
        sb.append(remainder);
        remainder = null;
      }

      String endQuote = null;
      String line;
      while ((line = reader.readLine()) != null) {
        // If we're in the middle of a quoted string, then just keep adding strings until
        // we find the endQuote.
        if (endQuote != null) {
          int endQuoteIndex = line.indexOf(endQuote);
          if (endQuoteIndex < 0) {
            sb.append(line);
            sb.append("\n");
            continue;
          } else {
            // Append up to the end of the quote, then continue processing right after it
            sb.append(line.substring(0, endQuoteIndex + endQuote.length()));
            line = line.substring(endQuoteIndex + endQuote.length());
          }
        }

        // Strip off comments.
        int comment = line.indexOf("--");
        if (comment >= 0) {
          line = line.substring(0, comment);
        }

        // If there's $...$, that's the start of a multi-line quote.
        int firstDollar = line.indexOf('$');
        if (firstDollar >= 0) {
          int secondDollar = line.indexOf('$', firstDollar + 1);
          if (secondDollar > 0) {
            endQuote = line.substring(firstDollar, secondDollar);
            // TODO: this assumes the quote spans at least one line. Is that OK?
            sb.append(line);
            sb.append("\n");
            continue;
          }
        }

        // If there's a semi-colon, we're at the end.
        int endOfLine = line.indexOf(';');
        if (endOfLine >= 0) {
          sb.append(line.substring(0, endOfLine));
          remainder = line.substring(endOfLine + 1);
          break;
        }

        sb.append(line);
        sb.append("\n");
      }

      String statement = sb.toString().trim();
      if (statement.equals("")) {
        return null;
      }
      return statement;
    }

    public void close() {
      try {
        reader.close();
      } catch (IOException e) { }
    }
  }
}
