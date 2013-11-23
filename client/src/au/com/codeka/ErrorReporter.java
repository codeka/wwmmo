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
import java.util.Random;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Debug;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class ErrorReporter {
    private static final Logger log = LoggerFactory.getLogger(ErrorReporter.class);
    private static String sVersionName;
    private static String sPackageName;
    private static String sReportPath;
    private static String sPhoneModel;
    private static String sAndroidVersion;
    private static Random sRandom = new Random();

    public static void register(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            sVersionName = pi.versionName;
            sPackageName = pi.packageName;
            sReportPath = context.getFilesDir().getAbsolutePath() + "/error-reports/";
            sPhoneModel = android.os.Build.MODEL;
            sAndroidVersion = android.os.Build.VERSION.RELEASE;
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
                GlobalOptions.AutoSendCrashReport autosend = new GlobalOptions().getAutoSendCrashReport();

                if (autosend == GlobalOptions.AutoSendCrashReport.Never) {
                    clearErrorReports(unsentErrorReports);
                } else if (autosend == GlobalOptions.AutoSendCrashReport.Always) {
                    sendErrorReports(unsentErrorReports);
                } else /* Ask */ {
                    askToSend(context, unsentErrorReports);
                }
            }
        }

        private void askToSend(Context context, final String[] errorReportFiles) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View view = inflater.inflate(R.layout.error_report_send_dlg, null);
            final CheckBox alwaysChk = (CheckBox) view.findViewById(R.id.always_chk);

            new StyledDialog.Builder(context)
                    .setView(view)
                    .setTitle("Error reports")
                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (alwaysChk.isChecked()) {
                                saveAutoSend(GlobalOptions.AutoSendCrashReport.Always);
                            }

                            sendErrorReports(errorReportFiles);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Don't Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (alwaysChk.isChecked()) {
                                saveAutoSend(GlobalOptions.AutoSendCrashReport.Never);
                            }

                            clearErrorReports(errorReportFiles);
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        }

        private void sendErrorReports(final String[] errorReportFiles) {
            new BackgroundRunner<Boolean>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        log.debug("Sending "+errorReportFiles.length+" error reports...");
                        Messages.ErrorReports.Builder error_reports_pb = Messages.ErrorReports.newBuilder();
                        for (String file : errorReportFiles) {
                            FileInputStream ins = new FileInputStream(sReportPath + file);
                            error_reports_pb.addReports(Messages.ErrorReport.parseFrom(ins));
                        }

                        ApiClient.postProtoBuf("error-reports", error_reports_pb.build());
                    } catch(Exception e) {
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
                    new File(sReportPath + file).delete();
                }
            } catch (Exception e) {
                log.error("Exception caught removing error reports.", e);
            }
        }

        private void saveAutoSend(GlobalOptions.AutoSendCrashReport autoSend) {
            new GlobalOptions().setAutoSendCrashReport(autoSend);
        }

        /** Fetches the filename of all saved error reports. */
        private static String[] findUnsentErrorReports() {
            // try to create the files folder if it doesn't exist
            File dir = new File(sReportPath);
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
     * This is our implementation of the @see UncaughtExceptionHandler that saves crash reports to a directory
     * on the SD card. 
     */
    public static class ExceptionHandler implements UncaughtExceptionHandler {
        private UncaughtExceptionHandler mOldDefaultExceptionHandler;
        private static Debug.MemoryInfo sMemoryInfo = new Debug.MemoryInfo(); // allocate up-front in case we run out of memory later...

        public ExceptionHandler(UncaughtExceptionHandler oldDefaultExceptionHandler) {
            mOldDefaultExceptionHandler = oldDefaultExceptionHandler;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            String fileName = sVersionName + "-" + System.currentTimeMillis() + "-" + sRandom.nextInt(99999);
            OutputStream outs = null;
            try {
                final String fullPath = sReportPath + fileName + ".pb";
                log.debug("Writing unhandled exception to: " + fullPath);
                outs = new FileOutputStream(fullPath);

                final StringWriter stringWriter = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(stringWriter);
                throwable.printStackTrace(printWriter);

                Debug.getMemoryInfo(sMemoryInfo);
                Messages.ErrorReport.Builder error_report_pb = Messages.ErrorReport.newBuilder()
                        .setAndroidVersion(sAndroidVersion)
                        .setAppVersion(sVersionName)
                        .setPackageName(sPackageName)
                        .setPhoneModel(sPhoneModel)
                        .setStackTrace(stringWriter.toString())
                        .setMessage(throwable.getMessage())
                        .setExceptionClass(throwable.getClass().getName())
                        .setReportTime(DateTime.now().getMillis())
                        .setHeapSize(Debug.getNativeHeapSize())
                        .setHeapAllocated(Debug.getNativeHeapAllocatedSize())
                        .setHeapFree(Debug.getNativeHeapFreeSize());

                MyEmpire myEmpire = EmpireManager.i.getEmpire();
                if (myEmpire != null) {
                    error_report_pb.setEmpireId(Integer.parseInt(myEmpire.getKey()));
                }
                if (BackgroundDetector.i.getLastActivityName() != null) {
                    error_report_pb.setContext(BackgroundDetector.i.getLastActivityName());
                }

                error_report_pb.build().writeTo(outs);
            } catch (Exception ebos) {
                // we'll have to ignore errors here...
            } finally {
                if (outs != null) {
                    try {
                        outs.close();
                    } catch (IOException e) {
                    }
                }
            }

            if (mOldDefaultExceptionHandler != null) {
                mOldDefaultExceptionHandler.uncaughtException(thread, throwable);
            }
        }
    }
}
