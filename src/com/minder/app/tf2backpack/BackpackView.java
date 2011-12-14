package com.minder.app.tf2backpack;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.minder.app.tf2backpack.Backpack.Holder;
import com.minder.app.tf2backpack.Backpack.OnLayoutReadyListener;

public class BackpackView extends TableLayout {
	public int backpackCellSize;
	public View buttonList[];
	private boolean isTableCreated = false;
	
	private OnClickListener onClickListener;
	private OnLayoutReadyListener onReady;
	
	public boolean isTableCreated() {
		return this.isTableCreated;
	}
	
	public void setOnReadyListener(OnLayoutReadyListener o) {
		onReady = o;
	}

	public BackpackView(Context context) {
		super(context);
	}
	
	public BackpackView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private void createTable() {
		Log.d("BackpackView", "createTable");
        LayoutInflater mInflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        buttonList = new View[50];
        
        for (int f = 0; f < 5; f++) {
            TableRow tr = new TableRow(this.getContext());
            for (int c = 0; c < 10; c++) {
                View b = mInflater.inflate(R.layout.backpack_cell, null);
                b.setId(f * 10 + c);
                Holder.SetView(b);
                //b.setImageBitmap(gw);
                //b.setBackgroundResource(R.drawable.backpack_cell);
                Holder.imageButton.setOnClickListener(onButtonBackpackClick);
                //b.setPadding(0, 0, 0, 0);
                tr.addView(b, backpackCellSize, backpackCellSize);
                buttonList[b.getId()] = b;
            }
            this.addView(tr);
        }
        isTableCreated = true;
        if (onReady != null){
        	onReady.onReady();
        }
	}
	
	@Override
	public void setOnClickListener(OnClickListener l) {
		onClickListener = l;
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {      
        Log.d("BackpackView", "onSizeChanged h = " + h);
      
        backpackCellSize  = h / 5;
        
        if (!isTableCreated) {
        	createTable();
        } else { 
	        TableRow.LayoutParams cellLp = new TableRow.LayoutParams(
	        		backpackCellSize,
	        		backpackCellSize,
	                1.0f);
	        
	        for (View v : buttonList) {
	        	v.setLayoutParams(cellLp);
	        }
        }
        
        // update layout
    	Handler handler = new Handler();
    	handler.post(new Runnable() {

    	    public void run() {
    	        requestLayout();
    	    }
    	});
	}

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }
    
    OnClickListener onButtonBackpackClick = new OnClickListener(){
		public void onClick(View v) {
			if (onClickListener != null) {
				onClickListener.onClick(v);
			}
		}
    };

}
