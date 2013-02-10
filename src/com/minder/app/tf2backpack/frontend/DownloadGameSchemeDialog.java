package com.minder.app.tf2backpack.frontend;

import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class DownloadGameSchemeDialog extends DialogFragment {
	private CheckBox checkBoxResetImages;
	private CheckBox checkBoxHighresImages;
	private Button buttonCancel;
	private Button buttonDownload;
	
	public static void show(FragmentManager fragmentManager) {
		final DownloadGameSchemeDialog downloadDialog = new DownloadGameSchemeDialog();
		downloadDialog.show(fragmentManager, "DownloadGameSchemeDialog");
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.dialog_download_reset_images, container);
    	getDialog().setTitle(R.string.download_notice);
    	
    	checkBoxResetImages = (CheckBox)view.findViewById(R.id.checkBoxRefreshImages);
    	checkBoxHighresImages = (CheckBox)view.findViewById(R.id.checkBoxHighres);
    	
    	checkBoxResetImages.setOnCheckedChangeListener(resetCheckedChangeListener);
    	
    	buttonCancel = (Button)view.findViewById(R.id.buttonCancel);
    	buttonDownload = (Button)view.findViewById(R.id.buttonDownload);
    	
    	buttonCancel.setOnClickListener(cancelClickListener);
    	buttonDownload.setOnClickListener(downloadClickListener);
    	
    	checkBoxHighresImages.setChecked(GameSchemeDownloaderService.isCurrentGameSchemeHighres());
    	updateCheckBoxes();
    	return view;
    }
    
    private void updateCheckBoxes() {  	
    	if (GameSchemeDownloaderService.isGameSchemeReady()) {
    		checkBoxResetImages.setEnabled(true);
    		checkBoxHighresImages.setEnabled(checkBoxResetImages.isChecked());
    		
    		if (!checkBoxResetImages.isChecked())
    			checkBoxHighresImages.setChecked(GameSchemeDownloaderService.isCurrentGameSchemeHighres());
    	} else {
    		checkBoxResetImages.setEnabled(false);
    	}
    }
    
    private OnCheckedChangeListener resetCheckedChangeListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateCheckBoxes();
		}
	};
    
    private OnClickListener cancelClickListener = new OnClickListener() {	
		public void onClick(View v) {
			DownloadGameSchemeDialog.this.dismiss();
		}
	};
	
	private OnClickListener downloadClickListener = new OnClickListener() {	
		public void onClick(View v) {
			GameSchemeDownloaderService.startGameSchemeDownload(getActivity(), 
					checkBoxResetImages.isChecked(),
					checkBoxHighresImages.isChecked());
			DownloadGameSchemeDialog.this.dismiss();
			
			DownloadDialog dialog = new DownloadDialog();
			dialog.show(getActivity().getSupportFragmentManager(), "download_dialog");
		}
	};
}
