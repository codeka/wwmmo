package au.com.codeka.warworlds.api.greeter;

import android.annotation.SuppressLint;
import android.os.Debug;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import au.com.codeka.warworlds.Util;

public class StrictModeEnabler {
  public static void maybeEnableStrictMode() {
    if (Util.isDebug()) {
      enableStrictMode();
    }
  }

  @SuppressLint({"NewApi"}) // StrictMode doesn't work on < 3.0 and some of the tests are even newer
  private static void enableStrictMode() {
    try {
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork()
              .penaltyLog().build());
      StrictMode.setVmPolicy(
          new StrictMode.VmPolicy.Builder().detectActivityLeaks().detectLeakedClosableObjects()
              .detectLeakedRegistrationObjects().detectLeakedSqlLiteObjects().penaltyLog()
              // TODO: too many things out of our control do this .penaltyDeath()
              .build());

      // Replace System.err with one that'll monitor for StrictMode killing us and perform a hprof
      // heap dump just before it does.
      System.setErr(new PrintStreamThatDumpsHprofWhenStrictModeKillsUs(System.err));
    } catch (Exception e) {
      // ignore errors
    }
  }

  /**
   * This is quite a hack, but we want a heap dump when strict mode is about to kill us,
   * so we monitor System.err for the message from StrictMode that it's going to do that
   * and then do a manual heap dump.
   */
  private static class PrintStreamThatDumpsHprofWhenStrictModeKillsUs extends PrintStream {
    public PrintStreamThatDumpsHprofWhenStrictModeKillsUs(OutputStream outs) {
      super(outs);
    }

    @Override
    public synchronized void println(String str) {
      super.println(str);
      if (str.equals("StrictMode VmPolicy violation with POLICY_DEATH; shutting down.")) {
        // StrictMode is about to terminate us... do a heap dump!
        try {
          File dir = Environment.getExternalStorageDirectory();
          File file = new File(dir, "wwmmo-strictmode-violation.hprof");
          super.println("Dumping HPROF to: " + file);
          Debug.dumpHprofData(file.getAbsolutePath());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
