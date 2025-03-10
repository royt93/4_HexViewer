package com.galaxyjoy.hexviewer.ui.dlg;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.util.UIHelper;
import com.google.android.material.textfield.TextInputLayout;

public class SaveDialog {
    private final ActMain mActivity;
    private final String mTitle;

    public interface DialogPositiveClick {
        void onClick(AlertDialog dialog,
                     EditText editText,
                     TextInputLayout editTextLayout);
    }

    public SaveDialog(ActMain activity,
                      String title) {
        mActivity = activity;
        mTitle = title;
    }

    /**
     * Displays the dialog
     *
     * @param defaultValue  Default value.
     * @param positiveClick Listener called when clicking on the validation button.
     * @return AlertDialog
     */
    @SuppressLint("InflateParams")
    public AlertDialog show(String defaultValue,
                            DialogPositiveClick positiveClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setCancelable(false)
                .setIcon(R.drawable.ic_done)
                .setTitle(mTitle)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                });
        LayoutInflater factory = LayoutInflater.from(mActivity);
        builder.setView(factory.inflate(R.layout.dlg_content_dialog_save, null));
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        EditText et = dialog.findViewById(R.id.editText);
        TextInputLayout layout = dialog.findViewById(R.id.tilEditText);
        if (et != null && layout != null) {
            et.setText(defaultValue);
            et.addTextChangedListener(UIHelper.getResetLayoutWatcher(layout, false));
        }
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v ->
                positiveClick.onClick(dialog, et, layout));
        return dialog;
    }

}
