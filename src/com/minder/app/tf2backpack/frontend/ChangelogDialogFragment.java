package com.minder.app.tf2backpack.frontend;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.minder.app.tf2backpack.R;

public class ChangelogDialogFragment extends DialogFragment {
    public static ChangelogDialogFragment newInstance() {
    	ChangelogDialogFragment frag = new ChangelogDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
	        .setTitle(R.string.changelog)
	        .setMessage(R.string.changelog_text)
	        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            }
	        })
	        .create();
    }
}
