package au.com.codeka.warworlds;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * This activity lets you set up your Empire before you actually join the game. You need
 * to give your Empire a name, race and what-not.
 */
public class EmpireSetupFragment extends BaseFragment {
  private TextView empireName;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.empire_setup, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    empireName = view.findViewById(R.id.empire_setup_name);
    ActivityBackgroundGenerator.setBackground(view);

    final Button doneButton = view.findViewById(R.id.empire_setup_done);
    final Button switchAccountBtn = view.findViewById(R.id.switch_account_btn);

    empireName.setOnEditorActionListener((v, actionId, event) -> {
      saveEmpire();
      return true;
    });
    switchAccountBtn.setOnClickListener(v -> switchAccount());
    doneButton.setOnClickListener(v -> saveEmpire());
  }

  private void saveEmpire() {
    saveEmpire(empireName.getText().toString());
  }

  private void switchAccount() {
    SharedPreferences prefs = Util.getSharedPreferences();
    prefs.edit().putString("AccountName", null).apply();
    NavHostFragment.findNavController(this).navigate(
        EmpireSetupFragmentDirections.actionEmpireSetupFragmentToAccountsFragment());
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
        ProgressDialog.show(requireContext(), null, "Please wait, " + empireName, true);

    Messages.Empire empire = Messages.Empire.newBuilder()
        .setDisplayName(empireName)
        .setState(Messages.Empire.EmpireState.ACTIVE)
        .setEmail(accountName).build();

    RequestManager.i.sendRequest(new ApiRequest.Builder("empires", "PUT").body(empire)
        .completeCallback(request -> {
          pleaseWaitDialog.dismiss();

          // say 'hello' again, to reset the empire details
          requireMainActivity().getServerGreeter().clearHello();

          NavHostFragment.findNavController(EmpireSetupFragment.this).popBackStack();
        }).errorCallback((request, error) -> {
          pleaseWaitDialog.dismiss();

          if (error != null) {
            StyledDialog.showErrorMessage(requireContext(), error.getErrorMessage());
          } else {
            StyledDialog.showErrorMessage(requireContext(), "An expected error occurred.");
          }
        }).build());
  }
}
