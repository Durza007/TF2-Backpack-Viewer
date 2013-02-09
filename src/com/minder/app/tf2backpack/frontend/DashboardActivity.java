package com.minder.app.tf2backpack.frontend;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.frontend.NewsFragment.NewsHeaderClickedListener;

import android.os.Bundle;
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
