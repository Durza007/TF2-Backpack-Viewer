package com.minder.app.tf2backpack.frontend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
		public TextView textCount;
		public TextView textEquipped;
		public ImageButton imageButton;
		public ImageView colorSplat;
		
		public Holder(View v) {
			setView(v);
		}
		
		public void setView(View v){
			colorSplat = (ImageView)v.findViewById(R.id.ImageViewItemColor);
			textCount = (TextView)v.findViewById(R.id.TextViewCount);
			textEquipped = (TextView)v.findViewById(R.id.TextViewEquipped);
			imageButton = (ImageButton)v.findViewById(R.id.ImageButtonCell);
		}
	}
	
	private static class PaintColor {
		public Bitmap bitmap;
		public final int color;
		public final int color2;
		
		public PaintColor(int color, int color2) {
			this.color = color;
			this.color2 = color2;
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
	
	private List<PaintColor> paintCache;
	
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
		
		paintCache = new LinkedList<PaintColor>();
		
        Resources r = this.getResources();

        colorSplat = BitmapFactory.decodeResource(r, R.drawable.full_paint);
        colorTeamSpirit = BitmapFactory.decodeResource(r, R.drawable.team_half_paint);
	}
	
	private Bitmap getPaintImage(int color, int color2) {
		for (PaintColor p : paintCache) {
			if (p.color == color && p.color2 == color2)
				return p.bitmap;
		}
		
		// Paintcolor did not exist -> create it!
		PaintColor p = new PaintColor(color, color2);
		p.bitmap = Bitmap.createBitmap(colorSplat.getWidth(), colorSplat.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(p.bitmap);
		Paint paint = new Paint();
		//paint.setColorFilter(new PorterDuffColorFilter((0xFF << 24) | color, PorterDuff.Mode.SRC_ATOP));
		paint.setColorFilter(new LightingColorFilter((0xFF << 24) | color, 1));
		// draw paintcan
		canvas.drawBitmap(colorSplat, 0, 0, paint);
		
		if (color2 != 0) {
			paint.setColorFilter(new LightingColorFilter((0xFF << 24) | color2, 1));
			// draw paintcan
			canvas.drawBitmap(colorTeamSpirit, 0, 0, paint);
		}
		
		paintCache.add(p);	
		return p.bitmap;
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
                
                final Holder holder = new Holder(b);
                //b.setImageBitmap(gw);
                //b.setBackgroundResource(R.drawable.backpack_cell);
                holder.imageButton.setOnClickListener(onButtonBackpackClick);
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
    		
    		
    		final Holder holder = new Holder(buttonList[backpackPos]);
    		buttonsChanged[backpackPos] = true;
    		
    		// give an pointer to the item object to the cell
    		holder.imageButton.setTag(item);
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
				
				holder.imageButton.setImageBitmap(image);
    		} catch (FileNotFoundException e) {
    			holder.imageButton.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.unknown), backpackCellSize, backpackCellSize, false));
    			holder.imageButton.setTag(item);
				e.printStackTrace();
				
				allItemsWereKnown = false;
			}
    		
			if (coloredCells) {
				final int quality = item.getQuality();
				if (quality >=1 && quality <= 13){
					if (quality != 4 || quality != 6 || quality != 2 || quality != 12){
						holder.imageButton.setBackgroundResource(R.drawable.backpack_cell_white);
						holder.imageButton.getBackground().setColorFilter(Util.getItemColor(quality), PorterDuff.Mode.MULTIPLY);
					}
				}
			}
			
			if (item.getQuantity() > 1){
				holder.textCount.setVisibility(View.VISIBLE);
				holder.textCount.setText(String.valueOf(item.getQuantity()));
			} else {
				holder.textCount.setVisibility(View.INVISIBLE);
			}
			
			if (item.isEquipped()){
				holder.textEquipped.setVisibility(View.VISIBLE);
			} else {
				holder.textEquipped.setVisibility(View.GONE);
			}
			
			int color = item.getColor();
			int color2 = item.getColor2();
			if (color != 0){
				holder.colorSplat.setImageBitmap(getPaintImage(color, color2));
				holder.colorSplat.setVisibility(View.VISIBLE);
			} else {
				holder.colorSplat.setVisibility(View.GONE);
			}
    	}
    	
    	// reset anything we haven't changed
    	final int count = buttonsChanged.length;
    	for(int index = 0; index < count; index++){
    		if (!buttonsChanged[index]){
    			final Holder holder = new Holder(buttonList[index]);
    			holder.imageButton.setImageBitmap(null);
        		if (coloredCells) {
        			holder.imageButton.getBackground().clearColorFilter();
        			holder.imageButton.setBackgroundResource(R.drawable.backpack_cell);
        		}
        		holder.textCount.setVisibility(View.GONE);
        		holder.textEquipped.setVisibility(View.GONE);
        		holder.colorSplat.setVisibility(View.GONE);
        		holder.imageButton.setTag(null);
    		} else {
        		buttonsChanged[index] = false;
    		}
    	}
    	
    	return allItemsWereKnown;
    }
}
