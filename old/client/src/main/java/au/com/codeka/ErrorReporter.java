package au.com.codeka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Random;

import org.joda.time.DateTime;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Debug;
import android.view.View;
import android.widget.CheckBox;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class ErrorReporter {
  private static final Log log = new Log("ErrorReporter");
  private static String versionName;
  private static String packageName;
  private static String reportPath;
  private static String phoneModel;
  private static String androidVersion;
  private static final Random RANDOM = new Random();

  public static void register(Context context) {
    PackageManager pm = context.getPackageManager();
    try {
      PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
      versionName = pi.versionName;
      packageName = pi.packageName;
      reportPath = context.getFilesDir().getAbsolutePath() + "/error-reports/";
      phoneModel = android.os.Build.MODEL;
      androidVersion = android.os.Build.VERSION.RELEASE;
    } catch (NameNotFoundException e) {
      e.printStackTrace();
    }

    UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
    // don't register again if already registered
    if (!(currentHandler instanceof ExceptionHandler)) {
      Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(currentHandler));
    }

    ReportSender sender = new ReportSender();
    sender.send(context);
  }

  public static class ReportSender {
    public void send(Context context) {
      String[] unsentErrorReports = findUnsentErrorReports();
      if (unsentErrorReports.length > 0) {
        GlobalOptions.AutoSendCrashReport autoSend = new GlobalOptions().getAutoSendCrashReport();

        if (autoSend == GlobalOptions.AutoSendCrashReport.Never) {
          clearErrorReports(unsentErrorReports);
        } else if (autoSend == GlobalOptions.AutoSendCrashReport.Always) {
          sendErrorReports(unsentErrorReports);
        } else /* Ask */ {
          askToSend(context, unsentErrorReports);
        }
      }
    }

    private void askToSend(Context context, final String[] errorReportFiles) {
      final View view = View.inflate(context, R.layout.error_report_send_dlg, null);
      final CheckBox alwaysChk = (CheckBox) view.findViewById(R.id.always_chk);

      new StyledDialog.Builder(context).setView(view).setTitle("Error reports")
          .setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              if (alwaysChk.isChecked()) {
                saveAutoSend(GlobalOptions.AutoSendCrashReport.Always);
              }

              sendErrorReports(errorReportFiles);
              dialog.dismiss();
            }
          }).setNegativeButton("Don't Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              if (alwaysChk.isChecked()) {
                saveAutoSend(GlobalOptions.AutoSendCrashReport.Never);
              }

              clearErrorReports(errorReportFiles);
              dialog.dismiss();
            }
          })
          .create().show();
    }

    private void sendErrorReports(final String[] errorReportFiles) {
      new BackgroundRunner<Boolean>() {
        @Override
        protected Boolean doInBackground() {
          try {
            log.debug("Sending %d error reports...", errorReportFiles.length);
            Messages.ErrorReports.Builder error_reports_pb = Messages.ErrorReports.newBuilder();
            for (String file : errorReportFiles) {
              FileInputStream ins = null;
              try {
                ins = new FileInputStream(reportPath + file);
                error_reports_pb.addReports(Messages.ErrorReport.parseFrom(ins));
              } finally {
                if (ins != null) {
                  try {
                    ins.close();
                  } catch (IOException e) {
                    // Ignore.
                  }
                }
              }
            }

            RequestManager.i.sendRequest(new ApiRequest.Builder("error-reports", "POST")
              .body(error_reports_pb.build())
              .build());
          } catch (Exception e) {
            log.error("Exception caught sending error reports.", e);
            return false;
          }

          return true;
        }

        @Override
        protected void onComplete(Boolean result) {
          if (result) {
            clearErrorReports(errorReportFiles);
          }
        }
      }.execute();
    }

    private void clearErrorReports(String[] errorReportFiles) {
      try {
        for (String file : errorReportFiles) {
          new File(reportPath + file).delete();
        }
      } catch (Exception e) {
        log.error("Exception caught removing error reports.", e);
      }
    }

    private void saveAutoSend(GlobalOptions.AutoSendCrashReport autoSend) {
      new GlobalOptions().setAutoSendCrashReport(autoSend);
    }

    /**
     * Fetches the filename of all saved error reports.
     */
    private static String[] findUnsentErrorReports() {
      // try to create the files folder if it doesn't exist
      File dir = new File(reportPath);
      dir.mkdir();

      // Filter for ".pb" files
      return dir.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".pb");
        }
      });
    }
  }

  /**
   * This is our implementation of the @see UncaughtExceptionHandler that saves crash reports
   * to a directory
   * on the SD card.
   */
  public static class ExceptionHandler implements UncaughtExceptionHandler {
    private UncaughtExceptionHandler oldDefaultExceptionHandler;

    // allocate up-front in case we run out of memory later...
    private static Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();

    public ExceptionHandler(UncaughtExceptionHandler oldDefaultExceptionHandler) {
      this.oldDefaultExceptionHandler = oldDefaultExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
      String fileName =
          versionName + "-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(99999);
      OutputStream outs = null;
      try {
        final String fullPath = reportPath + fileName + ".pb";
        log.debug("Writing unhandled exception to: %s", fullPath);
        outs = new FileOutputStream(fullPath);

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);

        // write all the other stack traces as well
        try {
          writeAllStackTraces(thread, printWriter);
        } catch (Exception e) {
          // ignore errors
        }

        Debug.getMemoryInfo(memoryInfo);
        Messages.ErrorReport.Builder error_report_pb =
            Messages.ErrorReport.newBuilder().setAndroidVersion(androidVersion)
                .setAppVersion(versionName).setPackageName(packageName).setPhoneModel(phoneModel)
                .setStackTrace(stringWriter.toString()).setMessage(throwable.getMessage())
                .setExceptionClass(throwable.getClass().getName())
                .setReportTime(DateTime.now().getMillis()).setHeapSize(Debug.getNativeHeapSize())
                .setHeapAllocated(Debug.getNativeHeapAllocatedSize())
                .setHeapFree(Debug.getNativeHeapFreeSize())
                .setTotalRunTime(BackgroundDetector.i.getTotalRunTime())
                .setForegroundRunTime(BackgroundDetector.i.getTotalForegroundTime());

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire != null) {
          error_report_pb.setEmpireId(Integer.parseInt(myEmpire.getKey()));
        }
        if (BackgroundDetector.i.getLastActivityName() != null) {
          error_report_pb.setContext(BackgroundDetector.i.getLastActivityName());
        }

        error_report_pb.build().writeTo(outs);
      } catch (Exception e) {
        // we'll have to ignore errors here...
      } finally {
        if (outs != null) {
          try {
            outs.close();
          } catch (IOException e) {
            // ... and here.
          }
        }
      }

      if (oldDefaultExceptionHandler != null) {
        oldDefaultExceptionHandler.uncaughtException(thread, throwable);
      }
    }

    /**
     * Writes stack traces for all threads not the current thread to the given PrintWriter.
     */
    private void writeAllStackTraces(Thread currentThread, PrintWriter pw) {
      try {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
          if (entry.getKey() == currentThread) {
            continue;
          }

          Thread thread = entry.getKey();
          pw.println();
          pw.println("---------- THREAD: " + thread.getName());
          for (StackTraceElement ste : entry.getValue()) {
            pw.print("   ");
            pw.println(ste);
          }
        }
      } catch (Exception e) {
        // ignore
      }
    }
  }
}
