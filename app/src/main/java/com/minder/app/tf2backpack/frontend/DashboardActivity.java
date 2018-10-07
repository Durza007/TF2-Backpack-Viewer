package com.minder.app.tf2backpack.frontend;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.frontend.NewsFragment.NewsHeaderClickedListener;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class DashboardActivity extends FragmentActivity {
	private NewsFragment newsFragment;
	private DashboardFragment dashboardFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.dashboard);
        
        FragmentManager manager = getSupportFragmentManager();
        
        newsFragment = (NewsFragment)manager.findFragmentById(R.id.fragmentNews);
        dashboardFragment = (DashboardFragment)manager.findFragmentById(R.id.fragmentDashboard);
        
        newsFragment.addNewsHeaderClickedListener(headerListener);
        
        if (!DataManager.isGameSchemeDownloading()) {
			if (!DataManager.isGameSchemeReady() ||
					!DataManager.isGameSchemeUpToDate()) {
				App.getDataManager().requestSchemaFilesDownload(false);
			}
        }
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    	setIntent(intent);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Intent intent = getIntent();
    	if (intent != null) {
	        String action = getIntent().getAction();
	        if (action != null && action.equals("download_gamescheme")) {
				setIntent(null);
	        	if (DataManager.isGameSchemeDownloading()) {
	        		DownloadProgressDialog.show(getSupportFragmentManager(), (DownloadProgressDialog.ClosedListener) null);
	        		return;
				}
	        }
    	}

    	Exception e = DataManager.getPendingGameSchemeException();
    	if (e != null) {
    		GenericDialogHC.newInstance(
    				getResources().getString(R.string.failed_download),
					getResources().getString(R.string.download_gamescheme_error_message) + " " + e.getLocalizedMessage())
					.setPositiveButtonText(R.string.try_again)
					.setNegativeButtonText(R.string.dismiss)
					.setClickListener(new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialogInterface, int which) {
							dialogInterface.dismiss();
							if (which == DialogInterface.BUTTON_POSITIVE) {
								App.getDataManager().requestSchemaFilesDownload(false);
								DownloadProgressDialog.show(getSupportFragmentManager(), (DownloadProgressDialog.ClosedListener) null);
							}
						}
					})
					.show(getFragmentManager(), "errorDialog");

		}
		else {
    		if (!DataManager.isGameSchemeDownloading() && DataManager.shouldGameSchemeBeChecked()) {
    			App.getDataManager().requestSchemaFilesDownload(true);
			}
		}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	newsFragment.removeNewsHeaderClickedListener(headerListener);
    }

    private NewsHeaderClickedListener headerListener = new NewsHeaderClickedListener() {	
		public void onNewsHeaderClicked() {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.hide(dashboardFragment);
			transaction.addToBackStack(null);
			transaction.commit();
		}
	};
}
