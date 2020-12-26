package au.com.codeka.warworlds;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * Account selections activity - handles device registration and unregistration.
 */
public class AccountsFragment extends BaseFragment {
  private int accountSelectedPosition = 0;

  private TextView anonOverwriteNoticeView;
  private ListView accountsListView;
  private Button logInButton;

  private final int GET_ACCOUNTS_PERMISSION = 1;
  private final int SELECT_ACCOUNT_REQUEST_CODE = 3522;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.accounts, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ActivityBackgroundGenerator.setBackground(view);

    anonOverwriteNoticeView = view.findViewById(R.id.anon_overwrite_notice);
    accountsListView = view.findViewById(R.id.select_account);
    logInButton = view.findViewById(R.id.log_in_btn);

    logInButton.setOnClickListener(v -> {
      // Get account name
      accountSelectedPosition = accountsListView.getCheckedItemPosition();
      TextView account = (TextView) accountsListView.getChildAt(accountSelectedPosition);
      if (account == null) {
        return;
      }

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
    });

    accountsListView.setOnItemClickListener((adapterView, view1, position, l) -> {
      if (position >= getGoogleAccounts().size()) {
        // Prompt the user to choose an account that we'll then be able to see.
        Intent intent = AccountManager.newChooseAccountIntent(
            null, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, SELECT_ACCOUNT_REQUEST_CODE);
      }
    });

    view.findViewById(R.id.help_btn).setOnClickListener(v -> {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
      startActivity(i);
    });

    view.findViewById(R.id.privacy_policy_btn).setOnClickListener(v -> {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://www.war-worlds.com/privacy-policy"));
      startActivity(i);
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    SharedPreferences prefs = Util.getSharedPreferences();
    String accountName = prefs.getString("AccountName", null);
    if (accountName != null && !accountName.endsWith("@anon.war-worlds.com")) {
      anonOverwriteNoticeView.setVisibility(View.GONE);
    }

    List<String> accounts = getGoogleAccounts();
    if (accounts.size() == 0) {
      // If we don't have GET_ACCOUNTS permission, we'll want to request that and try again. On
      // Android > O, this permission is no longer required, and we just go straight into the
      // newChooseAccountIntent.
      int permissionCheck = ContextCompat.checkSelfPermission(
          requireContext(), android.Manifest.permission.GET_ACCOUNTS);
      if (permissionCheck != PackageManager.PERMISSION_GRANTED
          && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        // Show a dialog and invoke the "Add Account" activity if requested
        StyledDialog.Builder builder = new StyledDialog.Builder(requireContext());
        builder.setMessage("In order to fetch accounts on your phone, we need to ask for 'Contacts'"
            + " permission. War Worlds doesn't actually need access to your Contacts, but"
            + " because of the way permissions are grouped, this is how we have to ask for it.");
        builder.setPositiveButton("OK", (dialog, which) -> {
          ActivityCompat.requestPermissions(requireMainActivity(),
              new String[]{android.Manifest.permission.GET_ACCOUNTS},
              GET_ACCOUNTS_PERMISSION);
          dialog.dismiss();
        });
        builder.setTitle("Permissions");
        builder.create().show();
      } else {
        // Prompt the user to choose an account that we'll then be able to see.
        Intent intent = AccountManager.newChooseAccountIntent(
            null, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, SELECT_ACCOUNT_REQUEST_CODE);
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

      accountsListView.setAdapter(new AccountListAdapter(requireContext(), accounts));
      accountsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
      accountsListView.setItemChecked(accountSelectedPosition, true);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == GET_ACCOUNTS_PERMISSION) {
      // If request is cancelled, the result arrays are empty.
      if (grantResults.length == 0
          || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        NavHostFragment.findNavController(this).popBackStack();
      }
    }
  }
  /** Associates the current anonymous empire with the given new account name. */
  private void associateAnonAccount(final String newAccountName) {
    RequestManager.i.sendRequest(new ApiRequest.Builder("anon-associate", "POST")
        .body(Messages.AnonUserAssociate.newBuilder().setUserEmail(newAccountName).build())
        .completeCallback(request -> saveAccountName(newAccountName))
        .errorCallback((request, error) -> {
          MyEmpire empire = EmpireManager.i.getEmpire();
          new StyledDialog.Builder(requireContext()).setTitle("Cannot associate")
              .setMessage(Html.fromHtml("<p>" + error.getErrorMessage() + "</p>"
                  + "<p>Do you want to switch to this account anyway? Doing so will leave "
                  + empire.getDisplayName() + " abandoned.</p>"))
              .setNegativeButton("Cancel", (dialog, which) -> logInButton.setEnabled(true))
              .setPositiveButton("Switch", (dialog, which) -> {
                logInButton.setEnabled(true);
                saveAccountName(newAccountName);
              })
              .create().show();
        })
        .build());
  }

  /** Saves the new account name and finishes this activity. */
  private void saveAccountName(String accountName) {
    Util.getSharedPreferences().edit().putString("AccountName", accountName).apply();
    if (RealmContext.i.getCurrentRealm() != null) {
      RealmContext.i.getCurrentRealm().getAuthenticator().logout();
    }

    requireMainActivity().getServerGreeter().clearHello();

    NavHostFragment.findNavController(this).popBackStack();
  }

  /**
   * Returns a list of registered Google account names. If no Google accounts are registered on the
   * device, a zero-length list is returned.
   */
  private List<String> getGoogleAccounts() {
    ArrayList<String> result = new ArrayList<>();
    Account[] accounts = AccountManager.get(requireContext()).getAccounts();

    for (Account account : accounts) {
      if (account.type.equals("com.google")) {
        result.add(account.name);
      }
    }

    return result;
  }

  /**
   * A list adapter to display the list of accounts. We have one extra row on the bottom for
   * "+ Add Account", which lets you choose additional accounts.
   */
  public static class AccountListAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> accounts;

    public AccountListAdapter(Context context, List<String> accounts) {
      this.context = context;
      this.accounts = accounts;
    }

    @Override
    public int getCount() {
      return accounts.size() + 1;
    }

    @Override
    public Object getItem(int position) {
      if (position < accounts.size()) {
        return accounts.get(position);
      }
      return null;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        if (position < accounts.size()) {
          view = LayoutInflater.from(context).inflate(R.layout.account, parent, false);
        } else {
          view = LayoutInflater.from(context).inflate(R.layout.account_add, parent, false);
        }
      }
      if (position < accounts.size()) {
        ((TextView) view).setText(accounts.get(position));
      }

      return view;
    }
  }
}
