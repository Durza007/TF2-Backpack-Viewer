package com.minder.app.tf2backpack;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

public final class AdMobActivity extends Activity {
    public static AdMobActivity AdMobMemoryLeakWorkAroundActivity;

    public AdMobActivity() {
        super();
        if (AdMobMemoryLeakWorkAroundActivity != null) {
            throw new IllegalStateException("This activity should be created only once during the entire application life");
        }
        AdMobMemoryLeakWorkAroundActivity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("TF2Backpack", "in onCreate - AdMobActivity");
        finish();
    }

    public static final void startAdMobActivity(Activity activity) {
        Log.i("TF2Backpack", "in startAdMobActivity");
        Intent i = new Intent();
        i.setComponent(new ComponentName(activity.getApplicationContext(), AdMobActivity.class));
        activity.startActivity(i);
    }
    
    public static final void createAdmobActivity(Activity activity) {
        // Start the dummy admob activity.  Don't try to start it twice or an exception will be thrown
        if (AdMobActivity.AdMobMemoryLeakWorkAroundActivity == null) {
            Log.i("TF2Backpack", "starting the AdMobActivity");
            AdMobActivity.startAdMobActivity(activity);
        }
    }
    
    public static final AdView createAdView(AdView adView, Activity activity) {
        // DYNAMICALLY CREATE AD START
        LinearLayout adviewLayout = (LinearLayout)activity.findViewById(R.id.includeAd);
        // Create an ad.
        if (adviewLayout != null) {
	        if (adView == null) {
	            adView = new AdView(AdMobActivity.AdMobMemoryLeakWorkAroundActivity, AdSize.BANNER, "a14cc88802a11a9");
	            // Create an ad request.
	            AdRequest adRequest = new AdRequest();
	            adRequest.addTestDevice(AdRequest.TEST_EMULATOR);

	            // Start loading the ad in the background.
	            adView.loadAd(adRequest);
	            // Add the AdView to the view hierarchy. The view will have no size until the ad is loaded.
	            adviewLayout.addView(adView);
	        }
	        else {
	            ((LinearLayout)adView.getParent()).removeAllViews();
	            adviewLayout.addView(adView);
	            // Reload Ad if necessary.  Loaded ads are lost when the activity is paused.
	            if (!adView.isReady() || !adView.isRefreshing()) {
	                AdRequest adRequest = new AdRequest();
		            adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
	                // Start loading the ad in the background.
	                adView.loadAd(adRequest);
	            }
	        }
        }
        return adView;
    }
    
    public static final void removeAdView(Activity activity) {
    	LinearLayout adviewLayout = (LinearLayout)activity.findViewById(R.id.includeAd);
    	adviewLayout.removeAllViews();
    }
}
