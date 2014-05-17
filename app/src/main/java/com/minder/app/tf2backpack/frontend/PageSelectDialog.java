package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;

import com.minder.app.tf2backpack.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class PageSelectDialog extends DialogFragment {
	public static interface OnPageSelectedListener {
		public void onPageSelected(int selectedPage);
	}
	
	private WeakReference<OnPageSelectedListener> listener;
	private int maxPageNumber;
	
	public static PageSelectDialog newInstance(OnPageSelectedListener l, int maxPageNumber) {
		PageSelectDialog p = new PageSelectDialog();
		p.listener = new WeakReference<OnPageSelectedListener>(l);
		p.maxPageNumber = maxPageNumber;
		
		return p;
	}
	
	public PageSelectDialog() {
		this.listener = new WeakReference<OnPageSelectedListener>(null);
	}
	
	@Override
	public void onCreate(Bundle bundle) {
	    this.setRetainInstance(true);
	    super.onCreate(bundle);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String[] items = new String[maxPageNumber];
		for (int index = 0; index < items.length; index++) {
			items[index] = (index + 1) + "/" + maxPageNumber;
		}

		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.select_page)
			.setItems(items, new OnClickListener() {			
				public void onClick(DialogInterface dialog, int which) {
					final OnPageSelectedListener l = listener.get();
					if (l != null) {
						l.onPageSelected(which + 1);
					}
				}
			})
			.create();
	}
	
	@Override
	public void onDestroyView() {
	    if (getDialog() != null && getRetainInstance())
	        getDialog().setDismissMessage(null);
	    super.onDestroyView();
	}
}