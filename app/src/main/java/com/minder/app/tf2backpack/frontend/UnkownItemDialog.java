package com.minder.app.tf2backpack.frontend;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;

import com.minder.app.tf2backpack.R;

public class UnkownItemDialog extends DialogFragment {
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return new AlertDialog.Builder(getActivity())
				.setIcon(R.drawable.alert_dialog_icon)
				.setTitle(R.string.dialog_notice)
				.setMessage(R.string.message_unknown_item)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								DownloadGameSchemeDialog
										.show(getFragmentManager(), false);
							}
						})
				.setNeutralButton(R.string.alert_dialog_dont_show_again,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								SharedPreferences sp = PreferenceManager
										.getDefaultSharedPreferences(getActivity());
								Editor editor = sp.edit();
								editor.putBoolean("skipunknownitemdialog", true);
								editor.commit();
							}
						})
				.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						})
				.create();
	}
}
