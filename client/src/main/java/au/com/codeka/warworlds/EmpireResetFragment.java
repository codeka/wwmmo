package au.com.codeka.warworlds;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import org.jetbrains.annotations.NotNull;

import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * This activity is shown when we're notified by the server that the player's empire was
 * reset. Usually that's because their last colony was destroyed.
 */
public class EmpireResetFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.warm_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View rootView = view.findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        TransparentWebView welcome = view.findViewById(R.id.welcome);
        String msg;

        String reason = null; // TODO: getIntent().getStringExtra("au.com.codeka.warworlds.ResetReason");
        if (reason == null) {
            msg = TransparentWebView.getHtmlFile(requireContext(), "html/empire-reset.html");
        } else if (reason.equals("as-requested")) {
            msg =
                TransparentWebView.getHtmlFile(
                    requireContext(), "html/empire-reset-requested.html");
        } else {
            msg = TransparentWebView.getHtmlFile(requireContext(), "html/empire-reset-reason.html");
            msg = String.format(msg, reason);
        }
        welcome.loadHtml("html/skeleton.html", msg);

        Button startBtn = view.findViewById(R.id.start_btn);
        startBtn.setOnClickListener(v -> {
            // now we can move to the WarWorlds activity again and get started.
            NavHostFragment.findNavController(this).navigate(
                EmpireResetFragmentDirections.actionEmpireResetFragmentToWelcomeFragment());
        });
    }
}