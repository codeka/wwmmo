package au.com.codeka.warworlds;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Account selections activity - handles device registration and unregistration.
 */
public class AccountsActivity extends BaseActivity {
  private int accountSelectedPosition = 0;
  private Context context = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    Util.setup(this);
    Util.loadProperties();
    SharedPreferences prefs = Util.getSharedPreferences();
    String accountName = prefs.getString("AccountName", null);

    setContentView(R.layout.accounts);

    List<String> accounts = getGoogleAccounts();
    if (accounts.size() == 0) {
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

    final ListView listView = (ListView) findViewById(R.id.select_account);
    final Button logInButton = (Button) findViewById(R.id.log_in_btn);
    logInButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        // Get account name
        accountSelectedPosition = listView.getCheckedItemPosition();
        TextView account = (TextView) listView.getChildAt(accountSelectedPosition);

        // Save the new account name, and force another 'hello'
        Util.getSharedPreferences().edit()
            .putString("AccountName", account.getText().toString())
            .apply();
        if (RealmContext.i.getCurrentRealm() != null) {
          RealmContext.i.getCurrentRealm().getAuthenticator().logout();
        }
        ServerGreeter.clearHello();

        finish();
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
