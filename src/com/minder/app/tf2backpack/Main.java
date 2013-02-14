package com.minder.app.tf2backpack;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.minder.app.tf2backpack.frontend.DashboardActivity;
import com.minder.app.tf2backpack.frontend.SelectPlayerActivity;

public class Main extends Activity {
	private SharedPreferences playerPrefs;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		playerPrefs = this.getSharedPreferences("player", MODE_PRIVATE);

		String playerId = playerPrefs.getString("id", null);
		if (playerId != null) {
			startActivity(new Intent(this, DashboardActivity.class));
			finish();
		} else {
			startActivityForResult(new Intent(this, SelectPlayerActivity.class).putExtra(
					"title", "Enter your user name"), 0);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			SharedPreferences.Editor editor = playerPrefs.edit();
			SteamUser user = data.getExtras().getParcelable("user");
			
			if (user != null) {
				editor.putLong("id", user.steamdId64);
				editor.commit();
				startActivity(new Intent(this, DashboardActivity.class));
			}
		}
		finish();
	}

}