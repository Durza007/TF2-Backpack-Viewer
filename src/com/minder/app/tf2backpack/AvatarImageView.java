package com.minder.app.tf2backpack;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class AvatarImageView extends ImageView {
	public AvatarImageView(Context context) {
		super(context);
	}

	public AvatarImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AvatarImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
	    int parent_height = MeasureSpec.getSize(heightMeasureSpec);
	    this.setMeasuredDimension(parent_height, parent_height);
	    Log.d("BOARD_VIEW", "BoardView.onMeasure : width = " + this.getMeasuredWidth() + ", height = " 
	            + this.getMeasuredHeight());
	}
}
