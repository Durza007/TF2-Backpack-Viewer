package com.minder.app.tf2backpack.frontend;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GenericDialogHC extends DialogFragment {
	private int titleId = -1;
	private int messageId = -1;
	private int iconId = -1;
	private String titleString = null;
	private String messageString = null;
	private DialogInterface.OnClickListener clickListener;
	private int positiveButtonTextId = -1;
	private int neutralButtonTextId = -1;
	private int negativeButtonTextId = -1;
	private boolean showProgress = false;
	
    public static GenericDialogHC newInstance(int titleId, int messageId) {
    	GenericDialogHC dialog = new GenericDialogHC();
    	dialog.titleId = titleId;
    	dialog.messageId = messageId;
        return dialog;
    }

	public static GenericDialogHC newInstance(String title, String message) {
		GenericDialogHC dialog = new GenericDialogHC();
		dialog.titleString = title;
		dialog.messageString = message;
		return dialog;
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	this.setRetainInstance(true);
    }
    
    public GenericDialogHC setClickListener(DialogInterface.OnClickListener listener) {
    	this.clickListener = listener;
    	return this;
    }
    
    public GenericDialogHC setPositiveButtonText(int textId) {
    	this.positiveButtonTextId = textId;
    	return this;
    }
    
    public GenericDialogHC setNeutralButtonText(int textId) {
    	this.neutralButtonTextId = textId;
    	return this;
    }
    
    public GenericDialogHC setNegativeButtonText(int textId) {
    	this.negativeButtonTextId = textId;
    	return this;
    }
    
    public void setShowProgress(boolean showProgress) {
    	this.showProgress = showProgress;
    }
    
    public GenericDialogHC setIcon(int iconId) {
    	this.iconId = iconId;
    	return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	
    	if (titleId != -1)
    		builder.setTitle(titleId);
    	else if (titleString != null)
    		builder.setTitle(titleString);
    	
    	if (messageId != -1)
    		builder.setMessage(messageId);
    	else if (messageString != null)
    		builder.setMessage(messageString);
    	
    	if (iconId != -1)
    		builder.setIcon(iconId);
    	
    	if (showProgress) {
    		// TODO implement!
    	}
    	
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