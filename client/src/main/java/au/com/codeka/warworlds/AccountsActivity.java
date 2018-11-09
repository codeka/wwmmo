package au.com.codeka.warworlds;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * Account selections activity - handles device registration and unregistration.
 */
public class AccountsActivity extends BaseActivity {
  private int accountSelectedPosition = 0;
  private Context context = this;

  private final int GET_ACCOUNTS_PERMISSION = 1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    Util.setup(this);
    Util.loadProperties();
    setContentView(R.layout.accounts);

    final ListView listView = findViewById(R.id.select_account);
    final Button logInButton = findViewById(R.id.log_in_btn);
    logInButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        // Get account name
        accountSelectedPosition = listView.getCheckedItemPosition();
        TextView account = (TextView) listView.getChildAt(accountSelectedPosition);

        SharedPreferences prefs = Util.getSharedPreferences();
        String accountName = prefs.getString("AccountName", null);

        // If they're currently anonymous, then we should associate them before continuing.
        if (accountName != null && accountName.endsWith("@anon.war-worlds.com")) {
          logInButton.setEnabled(false);
          associateAnonAccount(account.getText().toString());
          return;
        }

        // Otherwise, just save the new account name and we're done.
        saveAccountName(account.getText().toString());
      }
    });

    findViewById(R.id.help_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
        startActivity(i);
      }
    });

    findViewById(R.id.privacy_policy_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/privacy-policy"));
        startActivity(i);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    SharedPreferences prefs = Util.getSharedPreferences();
    String accountName = prefs.getString("AccountName", null);
    if (accountName != null && !accountName.endsWith("@anon.war-worlds.com")) {
      findViewById(R.id.anon_overwrite_notice).setVisibility(View.GONE);
    }

    List<String> accounts = getGoogleAccounts();
    if (accounts.size() == 0) {
      // If we don't have GET_ACCOUNTS permission, we'll want to request that and try again.
      int permissionCheck = ContextCompat.checkSelfPermission(
          AccountsActivity.this, android.Manifest.permission.GET_ACCOUNTS);
      if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
        // Show a dialog and invoke the "Add Account" activity if requested
        StyledDialog.Builder builder = new StyledDialog.Builder(context);
        builder.setMessage("In order to fetch accounts on your phone, we need to ask for 'Contacts'"
            + " permission. War Worlds doesn't actually need access to your Contacts, but"
            + " because of the way permissions are grouped, this is how we have to ask for it.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            ActivityCompat.requestPermissions(AccountsActivity.this,
                new String[]{android.Manifest.permission.GET_ACCOUNTS},
                GET_ACCOUNTS_PERMISSION);
            dialog.dismiss();
          }
        });
        builder.setTitle("Permissions");
        builder.create().show();
      } else {
        // Show a dialog and invoke the "Add Account" activity if requested
        StyledDialog.Builder builder = new StyledDialog.Builder(context);
        builder.setMessage("You need a Google Account in order to be able to play War Worlds.");
        builder.setPositiveButton("Add Account", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
          }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });
        builder.setTitle("No Google Account");
        builder.create().show();
      }
    } else {
      if (accountName != null) {
        for (int i = 0; i < accounts.size(); i++) {
          if (accounts.get(i).equals(accountName)) {
            accountSelectedPosition = i;
            break;
          }
        }
      }

      ListView listView = (ListView) findViewById(R.id.select_account);
      listView.setAdapter(new ArrayAdapter<>(context, R.layout.account, accounts));
      listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
      listView.setItemChecked(accountSelectedPosition, true);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case GET_ACCOUNTS_PERMISSION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // TODO: granted, do we need to do anything else?
        } else {
          finish();
        }
      }
    }
  }
  /** Associates the current anonymous empire with the given new account name. */
  private void associateAnonAccount(final String newAccountName) {
    RequestManager.i.sendRequest(new ApiRequest.Builder("anon-associate", "POST")
        .body(Messages.AnonUserAssociate.newBuilder().setUserEmail(newAccountName).build())
        .completeCallback(new ApiRequest.CompleteCallback() {
              @Override
              public void onRequestComplete(ApiRequest request) {
                saveAccountName(newAccountName);
              }
            })
        .errorCallback(new ApiRequest.ErrorCallback() {
          @Override
          public void onRequestError(ApiRequest request, Messages.GenericError error) {
            MyEmpire empire = EmpireManager.i.getEmpire();
            new StyledDialog.Builder(AccountsActivity.this).setTitle("Cannot associate")
                .setMessage(Html.fromHtml("<p>" + error.getErrorMessage() + "</p>"
                    + "<p>Do you want to switch to this account anyway? Doing so will leave "
                    + empire.getDisplayName() + " abandoned.</p>"))
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        findViewById(R.id.log_in_btn).setEnabled(true);
                      }
                    })
                .setPositiveButton("Switch", new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        findViewById(R.id.log_in_btn).setEnabled(true);
                        saveAccountName(newAccountName);
                      }
                    })
                .create().show();
          }
        })
        .build());
  }

  /** Saves the new account name and finishes this activity. */
  private void saveAccountName(String accountName) {
    Util.getSharedPreferences().edit().putString("AccountName", accountName).apply();
    if (RealmContext.i.getCurrentRealm() != null) {
      RealmContext.i.getCurrentRealm().getAuthenticator().logout();
    }
    ServerGreeter.clearHello();

    finish();
  }

  /**
   * Returns a list of registered Google account names. If no Google accounts are registered on the
   * device, a zero-length list is returned.
   */
  private List<String> getGoogleAccounts() {
    ArrayList<String> result = new ArrayList<>();
    Account[] accounts = AccountManager.get(context).getAccounts();
    for (Account account : accounts) {
      if (account.type.equals("com.google")) {
        result.add(account.name);
      }
    }

    return result;
  }
}
