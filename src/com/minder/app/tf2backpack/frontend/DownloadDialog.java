package com.minder.app.tf2backpack.frontend;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.minder.app.tf2backpack.R;

public class DownloadDialog extends DialogFragment {
	private TextView textViewTask;
	private ProgressBar progressBar;
	private TextView textViewImageCount;
	
    private OnClickListener listener = new OnClickListener() {
		public void onClick(View view) {
			getDialog().dismiss();
		}
	};
	
	public DownloadDialog() {
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
    	progressBar.setProgress(35);
    	
        return view;
    }
}
