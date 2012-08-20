package com.minder.app.tf2backpack.backend;

import com.minder.app.tf2backpack.backend.DataManager.Request;

public interface OnDataFailedListener {
	public void onDataFailed(Exception e, Request request);
}
