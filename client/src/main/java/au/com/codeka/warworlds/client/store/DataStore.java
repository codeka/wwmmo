package au.com.codeka.warworlds.client.store;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

//import org.mapdb.DB;
//import org.mapdb.DBMaker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.prefs.PreferenceChangeEvent;

/**
 * Wraps the main data store, ensures we have it available when it's needed.
 */
public class DataStore {
  //private DB db;

  public void open(Context applicationContext) {
  //  File dbPath;
  //  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
  //    dbPath = applicationContext.getFilesDir();
  //  } else {
      // On lollipop, we want to make sure this directory isn't backed up (after all, it can be re-
      // created by syncing again with the server).
  //    dbPath = applicationContext.getNoBackupFilesDir();
  //  }
  //  dbPath = new File(dbPath, "store.db");

  //  db = DBMaker
  //      .fileDB(dbPath)
  //      .make();
  }
}
