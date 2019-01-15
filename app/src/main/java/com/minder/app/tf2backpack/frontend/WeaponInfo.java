package com.minder.app.tf2backpack.frontend;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.Attribute;
import com.minder.app.tf2backpack.Attribute.StrangeQuality;
import com.minder.app.tf2backpack.ImageLoader;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.Item;
import com.minder.app.tf2backpack.backend.Item.ItemAttribute;

public class WeaponInfo extends Activity {
	private static final int blueColor = Color.rgb(153, 204, 255);
	private static final int redColor = Color.rgb(255, 64, 64);
	private static final int whiteColor = Color.rgb(236, 227, 203);

	private ImageLoader imageLoader;
	private TextView tvAttributes;
	private SpannableStringBuilder attributeText;
	private boolean hideLargeCraftOrder;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // no titlebar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weapon_info);

		imageLoader = new ImageLoader(this);
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        hideLargeCraftOrder = sp.getBoolean("hidelargecraftnumber", false);
        
        Item item = getIntent().getParcelableExtra("com.minder.app.tf2backpack.PlayerItemParser.Item");

        SQLiteDatabase sqlDb = App.getDatabaseHelper().getDatabase();
        
        Cursor c = sqlDb.rawQuery("SELECT name, type_name, description, image_url, image_url_large FROM items WHERE defindex = "
        		+ item.getDefIndex(), null);
        
        ItemAttribute[] array = item.getAttributeList();
        LinkedList<ItemAttribute> itemAttributeList = new LinkedList<ItemAttribute>();
        
        if (array != null) {
            Collections.addAll(itemAttributeList, array);
        }
        
        String sqlParam = null;
        for (ItemAttribute ia : itemAttributeList){
        	if (sqlParam == null){
        		sqlParam = "";
        		sqlParam += "WHERE defindex = " + ia.getAttributeDefIndex();
        	} else {
        		sqlParam += " OR defindex = " + ia.getAttributeDefIndex();
        	}
        }
        
        Cursor cAttribute = null;
        final boolean isPlayerItem = sqlParam != null;
        if (isPlayerItem){
        	cAttribute = sqlDb.rawQuery("SELECT description_string, defindex, description_format, effect_type, hidden FROM attributes " + sqlParam, null);
        } else {
        	cAttribute = sqlDb.rawQuery("SELECT description_string, value, description_format, effect_type, hidden, defindex FROM item_attributes, attributes WHERE itemdefindex = " + item.getDefIndex() + " AND defindex = attributedefindex", null);
        }
        
        final int defIndexColumn = cAttribute.getColumnIndex("defindex");
        final int descriptionColumn = cAttribute.getColumnIndex("description_string");
        final int descriptionFormatColumn = cAttribute.getColumnIndex("description_format");
        final int effectTypeColumn = cAttribute.getColumnIndex("effect_type");
        final int valueColumn = cAttribute.getColumnIndex("value");
        final int hiddenColumn = cAttribute.getColumnIndex("hidden");

		final ImageView imgWeapon = findViewById(R.id.ImageViewWeapon);
        TextView tvName = findViewById(R.id.TextViewWeaponName);
        TextView tvLevel = findViewById(R.id.TextViewWeaponLevel);
        TextView tvDescription = findViewById(R.id.TextViewDescription);
        TextView tvTradable = findViewById(R.id.TextViewTradable);
        tvAttributes = findViewById(R.id.TextViewAttributes);
        
        if (c != null && c.moveToFirst()) {
            final String imageUrl = c.getString(c.getColumnIndex("image_url"));
        	final String imageUrlLarge = c.getString(c.getColumnIndex("image_url_large"));
			imageLoader.displayImageWithCachedTemporary(imageUrlLarge, imageUrl, new ImageLoader.ImageLoadedInterface() {
				public void imageReady(String url, Bitmap bitmap) {
					if (url == imageUrlLarge) {
						imgWeapon.setImageBitmap(bitmap);
					}
				}
			}, 512, false);

	        String weaponClass = c.getString(c.getColumnIndex("type_name"));
	        if (weaponClass.contains("TF_")){
	        	weaponClass = "";
	        }
	        
	        if (item.getLevel() != -1) {
	        	tvLevel.setText(getResources().getString(R.string.item_level) + " " + item.getLevel() + " " + weaponClass);
	        } else {
	        	tvLevel.setVisibility(View.GONE);
	        }
	        
	        if (item.getCustomDesc() != null){
	        	tvDescription.setText("\"" + item.getCustomDesc() + "\"");
	        } else {
		        String descr = c.getString(c.getColumnIndex("description"));
		        
		        if (descr != null && !descr.equals("null") && !(descr.length() == 0)) {
		        	tvDescription.setText(descr);
		        } else {
		        	tvDescription.setVisibility(View.GONE);
		        }
	        }
	        
	        if (item.getParticleEffect() != 0){
	        	tvAttributes.setText(getResources().getString(R.string.particle_effect) + 
	        			getParticleName(sqlDb, item.getParticleEffect()));
	        	tvAttributes.setVisibility(View.VISIBLE);
	        }
	        
	        if (item.isNotTradable() || item.isNotCraftable()){
	        	tvTradable.setVisibility(View.VISIBLE);
	        	
	        	if (item.isNotTradable() && !item.isNotCraftable()) {
	        		tvTradable.setText(R.string.not_tradable);
	        	} else if (!item.isNotTradable() && item.isNotCraftable()) {
	        		tvTradable.setText(R.string.not_craftable);
	        	}
	        }
	        
	        tvName.setTextColor(Util.getItemColor(item.getQuality()));
	        
	        /**
	         * Attributes
	         */
			boolean skip = false;
	        boolean crateAttrib = false;
	        boolean hireAttrib = false;
	        long craftOrder = 0;
	        
	        StrangeQuality[] strangeQualities = new StrangeQuality[StrangeQuality.MAX_NUM_STRANGE_PARTS];
	        for (int i = 0; i < strangeQualities.length; i++) {
	        	strangeQualities[i] = new StrangeQuality();
			}
	        
	        if (cAttribute != null) {
	        	attributeText = new SpannableStringBuilder();
	        	int textIndex = 0;
	        	while (cAttribute.moveToNext()) {
	        		int attributeDefIndex = cAttribute.getInt(defIndexColumn);
	        		
	        		// check if hidden == false
	        		if (cAttribute.getInt(hiddenColumn) == 0 || attributeDefIndex == 228) {
	        			String description = cAttribute.getString(descriptionColumn);
	        			int descriptionFormat = cAttribute.getInt(descriptionFormatColumn);
	        			int effectType = cAttribute.getInt(effectTypeColumn);
	        			double value = 1337;
	        			
	        			if (description == null) continue;
	        			
	        			if (!isPlayerItem)
	        				value = cAttribute.getDouble(valueColumn);
	        			
	        			String personaName = null;
	        			
	        			skip = false;
	        			
	        			// set correct value for unique attributes
	        	        for (ItemAttribute ia : itemAttributeList) {
	        	        	if (ia.getAttributeDefIndex() == cAttribute.getInt(defIndexColumn)) {
	        	        		if (ia.getSteamUser() != null)
	        	        			personaName = ia.getSteamUser().steamName;
	        	        		
	        	        		// hide duplicate attributes
	        	        		if (ia.getAttributeDefIndex() == 187) {
	        	        			if (crateAttrib) {
		        	        			skip = true;
		        	        			continue;
	        	        			} else {
	        	        				crateAttrib = true;
	        	        			}
	        	        		}
	        	        		
	        	        		if (ia.getAttributeDefIndex() == 143) {
	        	        			if (hireAttrib){
		        	        			skip = true;
		        	        			continue;
	        	        			} else {
	        	        				hireAttrib = true;
	        	        			}
	        	        		}      	        		
	        	        		
	        	        		if (descriptionFormat == Attribute.FORMAT_DATE){
	        	        			value = ia.getValue();
	        	        		} else if (ia.getFloatValue() == 0){
	        	        			value = ia.getValue();
	        	        		} else {
	        	        			value = ia.getFloatValue();
	        	        		}
	        	        		
	        	        		break;
	        	        	}
	        	        }
	        	        
	        	        if (skip) continue;
	        			
	        			switch(descriptionFormat) {
	        			case Attribute.FORMAT_PERCENTAGE:
		        			if (value < 1){
		        				value = 1 - value;
		        				description = description.replace("%s1", "-" + String.valueOf((int)Math.round(value * 100))) + "\n";
		        			} else {
		        				value -= 1;
		        				description = description.replace("%s1", String.valueOf((int)Math.round(value * 100))) + "\n";
		        			}
	        				break;
	        				
	        			case Attribute.FORMAT_INVERTED_PERCENTAGE:
		        			value = 1 - value;
		        			description = description.replace("%s1", String.valueOf((int)Math.round(value * 100))) + "\n";
	        				break;
	        				
	        			case Attribute.FORMAT_ADDITIVE:
	        				if (value != (int)value) {
	        					DecimalFormat df = new DecimalFormat("#.##");
	        					description = description.replace("%s1", df.format(value)) + "\n";
	        				} else {
	        					description = description.replace("%s1", String.valueOf((int)value)) + "\n";
	        				}
	        				break;
	        				
	        			case Attribute.FORMAT_ADDITIVE_PERCENTAGE:
	        				description = description.replace("%s1", String.valueOf((int)Math.round(value * 100))) + "\n";
	        				break;
	        				
	        			case Attribute.FORMAT_DATE:
	        				DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
	        				Date date = getDateFromUnix(((long)value) * 1000);
	        				description = description.replace("%s1", formatter.format(date)) + "\n";
	        				break;
	        				
	        			case Attribute.FORMAT_PARTICLE_INDEX:   					
	        				description = description.replace("%s1", getParticleName(sqlDb, (int)value)) + "\n";
	        				break;
	        				
	        			case Attribute.FORMAT_ACCOUNT_ID:
	        				description = description.replace("%s1", personaName) + "\n";
	        				break;
	        				
	        			default:
	        				description = description.replace("%s1", String.valueOf((int)value)) + "\n";
	        				break;
	        			}
	        			
	        			
	        			attributeText.append(description);
	        			if (effectType == Attribute.EFFECT_POSITIVE){
	        				attributeText.setSpan(new ForegroundColorSpan(blueColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        			} else if (effectType == Attribute.EFFECT_NEGATIVE) {
	        				attributeText.setSpan(new ForegroundColorSpan(redColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        			} else {
	        				attributeText.setSpan(new ForegroundColorSpan(whiteColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        			}
	        			
	        			textIndex += description.length();
	        		} else if (attributeDefIndex == 214 || attributeDefIndex == 294 || attributeDefIndex == 494 ||
	        				attributeDefIndex == 379 || attributeDefIndex == 381 || attributeDefIndex == 383) {
	        			/**
	        			 * Strange weapon kills
	        			 */
	        			int index;
	        			for (index = 0; index < StrangeQuality.STRANGE_SCORE_ARRAY.length; index++) {
	        				if (attributeDefIndex == StrangeQuality.STRANGE_SCORE_ARRAY[index])
	        					break;
	        			}
	        			
	        			long value = 1337;
	        			
	        			if (!isPlayerItem)
	        				value = (long) cAttribute.getDouble(valueColumn);
	        			
	        			// set correct value for unique attributes
	        	        for (ItemAttribute ia : itemAttributeList){
	        	        	if (ia.getAttributeDefIndex() == attributeDefIndex) {
	        	        		value = ia.getValue();
	        	        		break;
	        	        	}
	        	        }
	        	        
	        	        strangeQualities[index].setValue((int)value);
	        		} else if (attributeDefIndex == 292 || attributeDefIndex == 293 || attributeDefIndex == 495 || 
	        				attributeDefIndex == 380 || attributeDefIndex == 382 || attributeDefIndex == 384) {
	        			/*
	        			 * Strange score type
	        			 */
	        			int index;
	        			for (index = 0; index < StrangeQuality.STRANGE_SCORE_TYPES_ARRAY.length; index++) {
	        				if (attributeDefIndex == StrangeQuality.STRANGE_SCORE_TYPES_ARRAY[index])
	        					break;
	        			}
	        			
	        			double value = 1337;
	        			
	        			if (!isPlayerItem)
	        				value = cAttribute.getDouble(valueColumn);
	    	
	        			// set correct value for unique attributes
	        	        for (ItemAttribute ia : itemAttributeList){
	        	        	if (ia.getAttributeDefIndex() == attributeDefIndex) {	
	        	        		if (ia.getFloatValue() == 0){
	        	        			value = ia.getValue();
	        	        		} else {
	        	        			value = ia.getFloatValue();
	        	        		}
	        	        		break;
	        	        	}
	        	        }
	        	        
	        	        strangeQualities[index].setStrangeType((int)value);        
	        		} else if (attributeDefIndex == 229) {
	        			/**
	        			 * Craft order attribute
	        			 */
	        			double value = 1337;
	        			
	        			if (!isPlayerItem)
	        				value = cAttribute.getDouble(valueColumn);
	        			
	        			// set correct value for unique attributes
	        	        for (ItemAttribute ia : itemAttributeList){
	        	        	if (ia.getAttributeDefIndex() == cAttribute.getInt(defIndexColumn)){
	        	        		value = ia.getValue();
	        	        		break;
	        	        	}
	        	        }
	        	        
	        	        if (value <= 100 || !hideLargeCraftOrder) {
                            craftOrder = (long)value;
	        	        }
	        		}
	        	}
	        	
	        	if (textIndex != 0) { 
		        	tvAttributes.setText(attributeText.subSequence(0, attributeText.length() - 1));
        			tvAttributes.setVisibility(View.VISIBLE);
	        	}
	        }
	        
	        // handle strange qualities
	        final StringBuilder strangeTextBuilder = new StringBuilder();
	        boolean namePrefixSet = false;
	        String strangeNamePrefix = "";
	        
	        if (item.getQuality() == 11) {
                for (StrangeQuality strangeQuality : strangeQualities) {
                    if (strangeQuality.isChanged()) {
                        Cursor strangeType =
                                sqlDb.rawQuery("SELECT type_name, level_data FROM strange_score_types WHERE type=" + strangeQuality.getStrangeType(), null);

                        if (strangeType.moveToFirst()) {
                            Cursor strangeLevel =
                                    sqlDb.rawQuery("SELECT COALESCE(" +
                                            "(SELECT name FROM " + strangeType.getString(1) + " WHERE required_score>" + strangeQuality.getValue() + " LIMIT 1)," +
                                            "(SELECT name FROM (SELECT MAX(required_score), name FROM " + strangeType.getString(1) + ")))", null);

                            if (strangeLevel.moveToFirst()) {
                                if (namePrefixSet) {
                                    strangeTextBuilder
                                            .append('(')
                                            .append(strangeType.getString(0))
                                            .append(": ")
                                            .append(strangeQuality.getValue())
                                            .append(")\n");
                                } else {
                                    strangeTextBuilder
                                            .append(strangeLevel.getString(0))
                                            .append(' ')
                                            .append(weaponClass)
                                            .append(" - ")
                                            .append(strangeType.getString(0))
                                            .append(": ")
                                            .append(strangeQuality.getValue())
                                            .append('\n');

                                    strangeNamePrefix = strangeLevel.getString(0) + " ";
                                    namePrefixSet = true;
                                }
                            } else {
                                // TODO this should NOT happen!!!
                            }
                        } else {
                            // TODO handle missing strange type
                        }
                    }
                }
		        
		        // do we have any stuff to add?
		        if (strangeTextBuilder.length() != 0) {
		        	tvLevel.setText(strangeTextBuilder.toString());
		        	tvLevel.setVisibility(View.VISIBLE);
		        }
	        }
	        
	        String namePrefix = "";
	        if (item.getQuality() == 11) {
	        	namePrefix = strangeNamePrefix;
	        } else if (item.getQuality() != 6) {
		        Cursor quality = sqlDb.rawQuery("SELECT name FROM item_qualities WHERE id = " + item.getQuality(), null);	   
		        
		        if (quality.moveToFirst()) {
		        	namePrefix = quality.getString(0) + " ";
		        }
		        
		        quality.close();
	        }

	        String nameSuffix = "";
	        if (craftOrder != 0) {
	            nameSuffix = " #" + craftOrder;
            }
	     
	        if (item.getCustomName() != null){
	        	tvName.setText("\"" + item.getCustomName() + "\"");
	        } else {
	        	tvName.setText(namePrefix + c.getString(c.getColumnIndex("name")) + nameSuffix);
	        }
        } else {
        	tvName.setText(getResources().getString(R.string.unknown_item) + item.getDefIndex());
        	tvLevel.setVisibility(View.GONE);
        	tvDescription.setText(R.string.unknown_item_description);
        }
        
        cAttribute.close();
        c.close();
    }
    
    private String getParticleName(SQLiteDatabase sqlDb, int index) {
		Cursor particleEffect = sqlDb.rawQuery("SELECT name FROM particles WHERE id=" + index, null);
		
		String name;
		if (particleEffect.moveToFirst()) {	        					
			name =  particleEffect.getString(0);
		} else {
			name =  getResources().getString(R.string.unknown_particle_effect);
		}
		
		particleEffect.close();
		return name;
    }
    
    private Date getDateFromUnix(long timestamp) {
    	final Calendar cal = Calendar.getInstance();
    	cal.setTimeInMillis(timestamp);
    	return cal.getTime();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	if (event.getAction() == MotionEvent.ACTION_UP){
    		finish();
    	}
    	
		return true;   	
    }
}