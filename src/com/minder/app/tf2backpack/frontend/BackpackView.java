package com.minder.app.tf2backpack.frontend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.Item;

public class BackpackView extends TableLayout {
	public interface OnLayoutReadyListener {
		public abstract void onReady();
	}
	
	private static class Holder {
		public static TextView textCount;
		public static TextView textEquipped;
		public static ImageButton imageButton;
		public static ImageView colorSplat;
		
		public static void setView(View v){
			colorSplat = (ImageView)v.findViewById(R.id.ImageViewItemColor);
			textCount = (TextView)v.findViewById(R.id.TextViewCount);
			textEquipped = (TextView)v.findViewById(R.id.TextViewEquipped);
			imageButton = (ImageButton)v.findViewById(R.id.ImageButtonCell);
		}
		
		public static void clear() {
			colorSplat = null;
			textCount = null;
			textEquipped = null;
			imageButton = null;
		}
	}
	
	private final static int BACKPACK_CELL_COUNT = 50;
	
	private Context context;
	private int fixedWidth;
	public int backpackCellSize;
	private View[] buttonList;
	private boolean coloredCells;
	private Bitmap colorSplat;
	private Bitmap colorTeamSpirit;
	private boolean[] buttonsChanged; 
	private boolean isTableCreated = false;
	
	private OnClickListener onClickListener;
	private OnLayoutReadyListener onReady;
	
	public boolean isTableCreated() {
		return this.isTableCreated;
	}
	
	public void setOnReadyListener(OnLayoutReadyListener o) {
		onReady = o;
	}
	
	public void setColoredCells(boolean useColoredCells) {
		this.coloredCells = true;
	}
	
	public void setFixedWidth(int fixedWidth) {
		this.fixedWidth = fixedWidth;
		
		final HorizontalScrollView.LayoutParams params = (HorizontalScrollView.LayoutParams) this.getLayoutParams();
		params.width = fixedWidth;
		this.setLayoutParams(params);
	}

	public BackpackView(Context context) {
		super(context);
		
		init(context, null);
	}
	
	public BackpackView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}
	
	public BackpackView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		
		init(context, attrs);
	}
	
	private void init(Context context, AttributeSet attrs) {
		this.context = context;
		this.setStretchAllColumns(true);
		
        Resources r = this.getResources();

        colorSplat = BitmapFactory.decodeResource(r, R.drawable.color_circle);
        colorTeamSpirit = BitmapFactory.decodeResource(r, R.drawable.color_circle_team_spirit);
	}
	
	private void createTable() {
		Log.d("BackpackView", "createTable");
        LayoutInflater mInflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        buttonList = new View[BACKPACK_CELL_COUNT];
        buttonsChanged = new boolean[BACKPACK_CELL_COUNT];
        
        for (int f = 0; f < 5; f++) {
            TableRow tr = new TableRow(this.getContext());
            for (int c = 0; c < 10; c++) {
                View b = mInflater.inflate(R.layout.backpack_cell, null);
                b.setId(f * 10 + c);
                Holder.setView(b);
                //b.setImageBitmap(gw);
                //b.setBackgroundResource(R.drawable.backpack_cell);
                Holder.imageButton.setOnClickListener(onButtonBackpackClick);
                //b.setPadding(0, 0, 0, 0);
                tr.addView(b, backpackCellSize, backpackCellSize);
                buttonList[b.getId()] = b;
            }
            this.addView(tr);
        }
        
        Holder.clear();
        
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
        Log.d("BackpackView", "onSizeChanged w = " + w + " h = " + h);
        
        if (fixedWidth != 0) {
        	backpackCellSize = fixedWidth / 10;
        	
        	// make sure nothing ends up outside the screen
        	if (backpackCellSize * 5 > h) {
        		backpackCellSize  = h / 5;
        	}
        } else {
        	backpackCellSize  = h / 5;
        }
        
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
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    
    OnClickListener onButtonBackpackClick = new OnClickListener(){
		public void onClick(View v) {
			if (onClickListener != null) {
				onClickListener.onClick(v);
			}
		}
    };
    
    /**
     * Set which items to display in the grid
     * @param itemList The list of items
     * @param cellIndexOffset The backpack pos offset - this view is not aware of pages
     * 		  so we need to make sure that the pos index of all items lay within 0 to 49
     */
    public boolean setItems(List<Item> itemList, int cellIndexOffset) {
    	boolean allItemsWereKnown = true;
    	for (Item item : itemList) {
    		final int backpackPos = item.getBackpackPosition() - cellIndexOffset;
    		
    		if (backpackPos < 0 || backpackPos >= BACKPACK_CELL_COUNT)
    			throw new ArrayIndexOutOfBoundsException("Item with bad backpack pos: " + backpackPos);
    		
    		Holder.setView(buttonList[backpackPos]);
    		buttonsChanged[backpackPos] = true;
    		
    		// give an pointer to the item object to the cell
			Holder.imageButton.setTag(item);
    		try {
				FileInputStream in = context.openFileInput(item.getDefIndex() + ".png");
				
				Bitmap image = BitmapFactory.decodeStream(in);
				if (image != null){
					Bitmap newImage = Bitmap.createScaledBitmap(image, backpackCellSize, backpackCellSize, false);
					if (newImage != image) {
						image.recycle();
					}
					image = newImage;
				} else {
					throw new FileNotFoundException();
				}
				
				Holder.imageButton.setImageBitmap(image);
    		} catch (FileNotFoundException e) {
				Holder.imageButton.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.unknown), backpackCellSize, backpackCellSize, false));
				Holder.imageButton.setTag(item);
				e.printStackTrace();
				
				allItemsWereKnown = false;
			}
    		
			if (coloredCells) {
				final int quality = item.getQuality();
				if (quality >=1 && quality <= 13){
					if (quality != 4 || quality != 6 || quality != 2 || quality != 12){
						Holder.imageButton.setBackgroundResource(R.drawable.backpack_cell_white);
						Holder.imageButton.getBackground().setColorFilter(Util.getItemColor(quality), PorterDuff.Mode.MULTIPLY);
					}
				}
			}
			
			if (item.getQuantity() > 1){
				Holder.textCount.setVisibility(View.VISIBLE);
				Holder.textCount.setText(String.valueOf(item.getQuantity()));
			} else {
				Holder.textCount.setVisibility(View.GONE);
			}
			
			if (item.isEquipped()){
				Holder.textEquipped.setVisibility(View.VISIBLE);
			} else {
				Holder.textEquipped.setVisibility(View.GONE);
			}
			
			int color = item.getColor();
			int color2 = item.getColor2();
			if (color != 0){
				if (color2 != 0){		
		    		Holder.colorSplat.setImageBitmap(colorTeamSpirit);
					Holder.colorSplat.setVisibility(View.VISIBLE);
		    		Holder.colorSplat.setColorFilter(null);
				} else {
					//ColorFilter filter = new LightingColorFilter((0xFF << 24) | color, 1);
					Holder.colorSplat.setImageBitmap(colorSplat);
					Holder.colorSplat.setVisibility(View.VISIBLE);
		    		Holder.colorSplat.setColorFilter(null);
					Holder.colorSplat.setColorFilter((0xFF << 24) | color, PorterDuff.Mode.SRC_ATOP);
				}
			} else {
				Holder.colorSplat.setVisibility(View.GONE);
			}
    	}
    	
    	// reset anything we haven't changed
    	final int count = buttonsChanged.length;
    	for(int index = 0; index < count; index++){
    		if (!buttonsChanged[index]){
        		Holder.setView(buttonList[index]);
        		Holder.imageButton.setImageBitmap(null);
        		if (coloredCells){
        			Holder.imageButton.getBackground().clearColorFilter();
        			Holder.imageButton.setBackgroundResource(R.drawable.backpack_cell);
        		}
        		Holder.textCount.setVisibility(View.GONE);
        		Holder.textEquipped.setVisibility(View.GONE);
        		Holder.colorSplat.setVisibility(View.GONE);
        		Holder.imageButton.setTag(null);
        		
        		Holder.clear();
    		} else {
        		buttonsChanged[index] = false;
    		}
    	}
    	
    	return allItemsWereKnown;
    }
}
