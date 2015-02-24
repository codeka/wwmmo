package au.com.codeka.warworlds;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;

/**
 * This activity lets you set up your Empire before you actually join the game. You need
 * to give your Empire a name, race and what-not.
 */
public class EmpireSetupActivity extends BaseActivity {
  private final Context context = this;

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    setContentView(R.layout.empire_setup);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    final TextView empireName = (TextView) findViewById(R.id.empire_setup_name);
    final Button doneButton = (Button) findViewById(R.id.empire_setup_done);

    empireName.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        saveEmpire();
        return true;
      }
    });

    doneButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        saveEmpire();
      }
    });

  }

  private void saveEmpire() {
    final TextView empireName = (TextView) findViewById(R.id.empire_setup_name);
    saveEmpire(empireName.getText().toString());
  }

  private void saveEmpire(final String empireName) {
    // if we've saved off the authentication cookie, cool!
    SharedPreferences prefs = Util.getSharedPreferences();
    final String accountName = prefs.getString("AccountName", null);
    if (accountName == null) {
      // TODO error!
      return;
    }

    final ProgressDialog pleaseWaitDialog =
        ProgressDialog.show(context, null, "Please wait...", true);

    Messages.Empire empire = Messages.Empire.newBuilder()
        .setDisplayName(empireName)
        .setState(Messages.Empire.EmpireState.ACTIVE)
        .setEmail(accountName).build();

    RequestManager.i.sendRequest(new ApiRequest.Builder("empires", "PUT").body(empire)
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            pleaseWaitDialog.dismiss();

            // say 'hello' again, to reset the empire details
            ServerGreeter.clearHello();

            EmpireSetupActivity.this.setResult(RESULT_OK);
            EmpireSetupActivity.this.finish();
          }
        }).errorCallback(new ApiRequest.ErrorCallback() {
          @Override
          public void onRequestError(ApiRequest request, Messages.GenericError error) {
            pleaseWaitDialog.dismiss();

            new StyledDialog.Builder(context).setTitle("Error").setMessage(error.getErrorMessage())
                .setNeutralButton("OK", null).create().show();
          }
        }).build());
  }
}
