package com.minder.app.tf2backpack.backend;

import com.minder.app.tf2backpack.backend.DataManager.Request;

public interface OnRequestReadyListener {
	/**
	 * Called when the request finished
	 * @param request The request
	 */
	public void onRequestReady(Request request);
}
