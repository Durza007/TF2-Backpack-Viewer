package com.minder.app.tf2backpack.backend;

public interface AsyncTaskListener {
	public void onPreExecute();
	public void onProgressUpdate(Object object);
	public void onPostExecute(Object object);
	//public void onCancelled(Object object);
}
