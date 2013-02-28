package com.minder.app.tf2backpack.frontend;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class GenericDialog extends DialogFragment {
	private int titleId = -1;
	private int messageId = -1;
	private DialogInterface.OnClickListener clickListener;
	private int buttonTextId = -1;
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
    
    public void setNeutralButtonText(int textId) {
    	this.buttonTextId = textId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	
    	if (titleId != -1)
    		builder.setTitle(titleId);
    	
    	if (messageId != -1)
    		builder.setMessage(messageId);
    	
    	if (clickListener != null) {
	    	if (buttonTextId != -1) {
	    		builder.setNeutralButton(buttonTextId, clickListener);
	    	}
    	}
    	
    	return builder.create();
    }
}
