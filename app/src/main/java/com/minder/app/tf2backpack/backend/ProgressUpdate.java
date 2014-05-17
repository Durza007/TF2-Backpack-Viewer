package com.minder.app.tf2backpack.backend;

public class ProgressUpdate {
	public final int updateType;
	public final float percentage;
	public final int totalCount;
	public final int count;
	public final Object data;
	
	public ProgressUpdate(int updateType, float percentage) {
		this(updateType, percentage, 0, 0, null);
	}
	
	public ProgressUpdate(int updateType, int totalCount, int count) {
		this(updateType, 0.0f, totalCount, count, null);
	}
	
	public ProgressUpdate(Object data) {
		this(0, 0.0f, 0, 0, data);
	}
	
	public ProgressUpdate(int updateType, float percentage, int totalCount, int count, Object data) {
		this.updateType = updateType;
		this.percentage = percentage;
		this.totalCount = totalCount;
		this.count = count;
		this.data = data;
	}
}
