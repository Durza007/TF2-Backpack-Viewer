package com.minder.app.tf2backpack.frontend;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.minder.app.tf2backpack.GameSchemeDownloaderService;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.backend.DownloadSchemaOverviewTask;

import java.util.Date;

public class DownloadProgressDialog extends DialogFragment implements DownloadSchemaOverviewTask.ProgressListener {
	public interface ClosedListener {
		void onClosed(boolean dismissed);
	}

	private final static int REFRESH_INTERVALL_MS = 500;
	
	private final Handler updateHandler;
	private boolean keepUpdating;
	private TextView textViewTask;
	private ProgressBar progressBar;
	private TextView textViewImageCount;

	private float progress = -1;
	private boolean uiReady = false;
	
    private ClosedListener closedListener;
	
	public static void show(FragmentManager manager, ClosedListener closedListener) {
        final DownloadProgressDialog progressDialog = new DownloadProgressDialog();
        progressDialog.closedListener = closedListener;
        progressDialog.show(manager, "download_dialog");
	}
	
	public DownloadProgressDialog() {
		updateHandler = new Handler();
		DataManager.addGameSchemeDownloadListener(this);
	}

	public void onProgress(float t) {
		progress = t;
		if (uiReady) {
			progressBar.setIndeterminate(false);
			progressBar.setMax(100);
			progressBar.setProgress((int)Math.max(0, Math.min(100, progress * 100)));
		}
	}

	public void onComplete(Date dataLastModified) {
		getDialog().dismiss();
	}

	public void onError(Exception error) {

	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.download_progress_dialog, container);
    	getDialog().setTitle(R.string.download);
    	
    	textViewTask = view.findViewById(R.id.textViewTask);
    	progressBar = view.findViewById(R.id.progressBarDownload);
    	textViewImageCount = view.findViewById(R.id.textViewImageCount);
    	
    	final Button buttonOk = view.findViewById(R.id.buttonDismiss);
    	buttonOk.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				getDialog().dismiss();
			}
		});
    	
    	textViewImageCount.setVisibility(View.GONE);
		if (progress == -1) {
			progressBar.setIndeterminate(true);
		}
		else {
			progressBar.setIndeterminate(false);
			progressBar.setMax(100);
			progressBar.setProgress((int)Math.max(0, Math.min(100, progress * 100)));
		}
		
		uiReady = true;
    	
        return view;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	keepUpdating = true;
    	//this.run();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	
		DataManager.removeGameSchemeDownloadListener(this);
		uiReady = false;
    	keepUpdating = false;
    	if (closedListener != null) {
    		closedListener.onClosed(!DataManager.isGameSchemeReady());
		}
    }

	/*public void run() {
		if (!keepUpdating)
			return;
		
		if (!DataManager.isGameSchemeDownloading()) {
			if (DataManager.isGameSchemeReady()) {
				GenericDialog dialog = GenericDialog.newInstance(R.string.download, R.string.download_successful);
				dialog.setNeutralButtonText(android.R.string.ok);
				dialog.setClickListener(new DialogInterface.OnClickListener() {				
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				});
				dialog.show(getActivity().getSupportFragmentManager(), "successDialog");
			}
			
			getDialog().dismiss();
		} else {
			if (GameSchemeDownloaderService.currentTaskStringId != 0)
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
			} else if (GameSchemeDownloaderService.currentTaskStringId == R.string.downloading_schema) {
				// update progressbar
				progressBar.setIndeterminate(false);
				progressBar.setMax((int)GameSchemeDownloaderService.totalBytes);
				progressBar.setProgress((int)GameSchemeDownloaderService.currentBytes);
				
				textViewImageCount.setVisibility(View.VISIBLE);
				textViewImageCount.setText((GameSchemeDownloaderService.currentBytes / 1024)
						+ "/" 
						+ (GameSchemeDownloaderService.totalBytes / 1024) + " [kB]");
			} else {
				progressBar.setIndeterminate(true);
				textViewImageCount.setVisibility(View.GONE);
			}
		}
		
		if (keepUpdating) {
			updateHandler.postAtTime(this, SystemClock.uptimeMillis() + REFRESH_INTERVALL_MS);
		}
	}*/
}
