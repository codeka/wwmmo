package au.com.codeka.warworlds.client.game.welcome;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;

/**
 * Layout for {@link CreateEmpireScreen}.
 */
public class CreateEmpireLayout extends RelativeLayout {
  public interface Callbacks {
    void onDoneClick(String empireName);
  }

  private final Callbacks callbacks;
  private final EditText empireName;

  public CreateEmpireLayout(Context context, Callbacks callbacks) {
    super(context);
    this.callbacks = callbacks;
    inflate(context, R.layout.create_empire, this);

    empireName = findViewById(R.id.empire_name);

    ViewBackgroundGenerator.setBackground(this);
    findViewById(R.id.next_btn).setOnClickListener(
        v -> callbacks.onDoneClick(empireName.getText().toString()));
  }

  public void showSpinner() {
    findViewById(R.id.empire_name).setVisibility(View.GONE);
    findViewById(R.id.switch_account_btn).setVisibility(View.GONE);
    findViewById(R.id.progress).setVisibility(View.VISIBLE);
  }

  /** Hide the spinner again (so the user can try again) but show an error message as well. */
  public void showError(String msg) {
    findViewById(R.id.empire_name).setVisibility(View.VISIBLE);
    findViewById(R.id.switch_account_btn).setVisibility(View.VISIBLE);
    findViewById(R.id.progress).setVisibility(View.GONE);
    ((TextView) findViewById(R.id.setup_name)).setText(msg);
  }
}
