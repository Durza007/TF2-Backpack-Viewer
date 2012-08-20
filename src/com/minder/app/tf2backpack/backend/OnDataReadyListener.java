package com.minder.app.tf2backpack.backend;

import com.minder.app.tf2backpack.backend.DataManager.Request;

public interface OnDataReadyListener {
	public void onDataReady(Request request);
}
