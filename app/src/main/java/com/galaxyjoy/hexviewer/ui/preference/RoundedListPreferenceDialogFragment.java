package com.galaxyjoy.hexviewer.ui.preference;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreferenceDialogFragmentCompat;

import com.galaxyjoy.hexviewer.R;

public class RoundedListPreferenceDialogFragment extends ListPreferenceDialogFragmentCompat {

    public static RoundedListPreferenceDialogFragment newInstance(String key) {
        final RoundedListPreferenceDialogFragment fragment = new RoundedListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Set rounded background
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.bg_alert_dialog);
        }

        return dialog;
    }
}
