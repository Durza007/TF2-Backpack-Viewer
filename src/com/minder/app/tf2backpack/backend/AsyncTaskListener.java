package com.minder.app.tf2backpack.backend;

public interface AsyncTaskListener {
	public void onPreExecute();
	public void onProgressUpdate(ProgressUpdate object);
	public void onPostExecute(Object object);
	//public void onCancelled(Object object);
}
