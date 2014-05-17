package com.minder.app.tf2backpack.frontend;

import com.minder.app.tf2backpack.SteamUser;

/**
 * Interface used by other classes to register notifications when a Steam
 * user has been selected
 */
public interface OnPlayerSelectedListener {
	public void onPlayerSelected(SteamUser user, int index);
}