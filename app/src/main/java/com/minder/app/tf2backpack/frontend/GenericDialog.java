package com.minder.app.tf2backpack.frontend;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.minder.app.tf2backpack.R;

public class GenericDialog extends DialogFragment {
	private int titleId = -1;
	private int messageId = -1;
	private int iconId = -1;
	private DialogInterface.OnClickListener clickListener;
	private int positiveButtonTextId = -1;
	private int neutralButtonTextId = -1;
	private int negativeButtonTextId = -1;
	private boolean showProgress = false;
	
    public static GenericDialog newInstance(int titleId, int messageId) {
    	GenericDialog dialog = new GenericDialog();
    	dialog.titleId = titleId;
    	dialog.messageId = messageId;
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	this.setRetainInstance(true);
    }
    
    public void setClickListener(DialogInterface.OnClickListener listener) {
    	this.clickListener = listener;
    }
    
    public GenericDialog setPositiveButtonText(int textId) {
    	this.positiveButtonTextId = textId;
		return this;
    }
    
    public GenericDialog setNeutralButtonText(int textId) {
    	this.neutralButtonTextId = textId;
		return this;
    }
    
    public GenericDialog setNegativeButtonText(int textId) {
    	this.negativeButtonTextId = textId;
		return this;
    }
    
    public GenericDialog setShowProgress(boolean showProgress) {
    	this.showProgress = showProgress;
    	return this;
    }
    
    public GenericDialog setIcon(int iconId) {
    	this.iconId = iconId;
    	return this;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (showProgress) {
			ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
			if (titleId != -1)
				dialog.setTitle(titleId);
			if (messageId != -1)
				dialog.setMessage(getActivity().getResources().getString(messageId));
			dialog.setIndeterminate(true);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			if (iconId != -1)
				dialog.setIcon(iconId);
			if (clickListener != null) {
				if (neutralButtonTextId != -1) {
					dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getActivity().getResources().getString(neutralButtonTextId), clickListener);
				}
				if (positiveButtonTextId != -1) {
					dialog.setButton(DialogInterface.BUTTON_POSITIVE, getActivity().getResources().getString(positiveButtonTextId), clickListener);
				}
				if (negativeButtonTextId != -1) {
					dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getResources().getString(negativeButtonTextId), clickListener);
				}
			}
			return dialog;
		}
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	
    	if (titleId != -1)
    		builder.setTitle(titleId);
    	
    	if (messageId != -1)
    		builder.setMessage(messageId);
    	
    	if (iconId != -1)
    		builder.setIcon(iconId);
    	
    	if (clickListener != null) {
	    	if (neutralButtonTextId != -1) {
	    		builder.setNeutralButton(neutralButtonTextId, clickListener);
	    	}
	    	if (positiveButtonTextId != -1) {
	    		builder.setPositiveButton(positiveButtonTextId, clickListener);
	    	}
	    	if (negativeButtonTextId != -1) {
	    		builder.setNegativeButton(negativeButtonTextId, clickListener);
	    	}
    	}
    	
    	return builder.create();
    }
}