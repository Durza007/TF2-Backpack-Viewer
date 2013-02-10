package com.minder.app.tf2backpack.frontend;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;

public class DownloadDialog extends DialogFragment implements Runnable {
	private final Handler updateHandler;
	private boolean keepUpdating;
	private TextView textViewTask;
	private ProgressBar progressBar;
	private TextView textViewImageCount;
	
    private OnClickListener listener = new OnClickListener() {
		public void onClick(View view) {
			getDialog().dismiss();
		}
	};
	
	public DownloadDialog() {
		updateHandler = new Handler();
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.download_progress_dialog, container);
    	getDialog().setTitle(R.string.download);
    	
    	textViewTask = (TextView)view.findViewById(R.id.textViewTask);
    	progressBar = (ProgressBar)view.findViewById(R.id.progressBarDownload);
    	textViewImageCount = (TextView)view.findViewById(R.id.textViewImageCount);
    	
    	final Button buttonOk = (Button)view.findViewById(R.id.buttonDismiss);
    	buttonOk.setOnClickListener(listener);
    	
    	textViewImageCount.setVisibility(View.GONE);
    	progressBar.setMax(100);
    	progressBar.setProgress(0);	
    	
        return view;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	keepUpdating = true;
    	this.run();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	
    	keepUpdating = false;
    }

	public void run() {
		if (keepUpdating) {
			updateHandler.postAtTime(this, SystemClock.uptimeMillis() + 1000);
		} else {
			return;
		}
		
		if (!GameSchemeDownloaderService.isGameSchemeDownloading())
			getDialog().dismiss();
		
		textViewTask.setText(GameSchemeDownloaderService.currentTaskStringId);
		if (GameSchemeDownloaderService.currentTaskStringId == R.string.downloading_images) {
			// update progressbar
			progressBar.setIndeterminate(false);
			progressBar.setMax(GameSchemeDownloaderService.totalImages);
			progressBar.setProgress(GameSchemeDownloaderService.currentAmountImages);
			
			textViewImageCount.setVisibility(View.VISIBLE);
			textViewImageCount.setText(GameSchemeDownloaderService.currentAmountImages 
					+ "/" 
					+ GameSchemeDownloaderService.totalImages);
		} else {
			progressBar.setIndeterminate(true);
		}
	}
}
